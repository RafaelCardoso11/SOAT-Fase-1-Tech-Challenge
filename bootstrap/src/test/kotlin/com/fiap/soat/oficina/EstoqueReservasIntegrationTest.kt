package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.ObjectMapper
import com.fiap.soat.oficina.estoque.domain.error.DuplicidadeException
import com.fiap.soat.oficina.estoque.domain.event.PecaIndisponivel
import com.fiap.soat.oficina.estoque.domain.event.PecaReservada
import com.fiap.soat.oficina.estoque.domain.event.PecasBaixadas
import com.fiap.soat.oficina.estoque.domain.event.ReservaLiberada
import com.fiap.soat.oficina.estoque.domain.model.Quantidade
import com.fiap.soat.oficina.estoque.domain.model.Reserva
import com.fiap.soat.oficina.estoque.domain.model.ResultadoReserva
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.ReservarPecaUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Testcontainers(disabledWithoutDocker = true)
class EstoqueReservasIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var reservarPeca: ReservarPecaUseCase

    @Autowired
    lateinit var consultarItem: ConsultarItemUseCase

    @Autowired
    lateinit var reservas: ReservaRepository

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var eventos: ApplicationEvents

    private val token: String by lazy {
        val resposta = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json("nomeUsuario" to "admin", "senha" to "admin@123")
        }.andReturn().response.contentAsString
        objectMapper.readTree(resposta).get("token").asText()
    }

    private fun json(vararg campos: Pair<String, Any?>): String = objectMapper.writeValueAsString(mapOf(*campos))

    private fun MockHttpServletRequestDsl.autenticado(corpo: String?) {
        header("Authorization", "Bearer $token")
        if (corpo != null) {
            contentType = MediaType.APPLICATION_JSON
            content = corpo
        }
    }

    private fun enviar(metodo: String, caminho: String, corpo: String? = null): ResultActionsDsl =
        when (metodo) {
            "POST" -> mockMvc.post(caminho) { autenticado(corpo) }
            else -> mockMvc.get(caminho) { autenticado(corpo) }
        }

    private fun idDe(resultado: ResultActionsDsl): Long =
        objectMapper.readTree(resultado.andReturn().response.contentAsString).get("id").asLong()

    private fun MockMvcResultMatchersDsl.erro(campo: String?, mensagem: String) {
        jsonPath("$.erro") { value(mensagem) }
        if (campo != null) jsonPath("$.campo") { value(campo) }
    }

    private fun criarItem(saldoInicial: Int): Long = idDe(
        enviar(
            "POST",
            "/api/v1/admin/estoque/itens",
            json("nome" to nomeUnico(), "tipo" to "PECA", "preco" to 100, "saldoInicial" to saldoInicial),
        ).andExpect { status { isCreated() } },
    )

    private fun reservar(osId: Long, itemId: Long?, quantidade: Int?): ResultActionsDsl =
        enviar("POST", "/api/v1/admin/estoque/ordens/$osId/reservas", json("itemId" to itemId, "quantidade" to quantidade))

    private fun item(id: Long): ResultActionsDsl = enviar("GET", "/api/v1/admin/estoque/itens/$id")

    @Test
    fun `rotas de reserva sem token retornam 401`() {
        mockMvc.get("/api/v1/admin/estoque/ordens/1/reservas").andExpect { status { isUnauthorized() } }
        mockMvc.post("/api/v1/admin/estoque/ordens/1/baixa").andExpect { status { isUnauthorized() } }
        mockMvc.post("/api/v1/admin/estoque/ordens/1/reservas/liberar").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `reserva com disponivel cria a reserva ativa, reduz o disponivel e publica o evento`() {
        val itemId = criarItem(3)
        val os = novaOs()

        reservar(os, itemId, 1).andExpect {
            status { isCreated() }
            jsonPath("$.osId") { value(os) }
            jsonPath("$.itemId") { value(itemId) }
            jsonPath("$.quantidade") { value(1) }
            jsonPath("$.situacao") { value("ATIVA") }
        }

        item(itemId).andExpect {
            jsonPath("$.saldo") { value(3) }
            jsonPath("$.reservado") { value(1) }
            jsonPath("$.disponivel") { value(2) }
        }
        enviar("GET", "/api/v1/admin/estoque/ordens/$os/reservas")
            .andExpect { status { isOk() }; jsonPath("$.length()") { value(1) }; jsonPath("$[0].situacao") { value("ATIVA") } }
        assertThat(eventos.stream(PecaReservada::class.java).toList()).contains(PecaReservada(os, itemId, 1))
    }

    @Test
    fun `reservar exatamente o disponivel zera o disponivel`() {
        val itemId = criarItem(3)

        reservar(novaOs(), itemId, 3).andExpect { status { isCreated() } }

        item(itemId).andExpect { jsonPath("$.disponivel") { value(0) }; jsonPath("$.reservado") { value(3) } }
    }

    @Test
    fun `sem disponivel suficiente retorna 409, nao reserva nada e publica o evento de indisponivel`() {
        val itemId = criarItem(2)
        val os = novaOs()

        reservar(os, itemId, 5).andExpect {
            status { isConflict() }
            erro("quantidade", "Quantidade indisponível em estoque (disponível: 2)")
        }

        item(itemId).andExpect { jsonPath("$.reservado") { value(0) }; jsonPath("$.disponivel") { value(2) } }
        enviar("GET", "/api/v1/admin/estoque/ordens/$os/reservas").andExpect { jsonPath("$.length()") { value(0) } }
        assertThat(eventos.stream(PecaIndisponivel::class.java).toList()).contains(PecaIndisponivel(os, itemId, 5, 2))
    }

    @Test
    fun `unidade reservada para uma OS nao e prometida a outra`() {
        val itemId = criarItem(1)

        reservar(novaOs(), itemId, 1).andExpect { status { isCreated() } }
        reservar(novaOs(), itemId, 1).andExpect { status { isConflict() }; erro("quantidade", "Quantidade indisponível em estoque (disponível: 0)") }
    }

    @Test
    fun `a mesma OS nao reserva o mesmo item duas vezes, mas pode de novo depois de liberar`() {
        val itemId = criarItem(5)
        val os = novaOs()
        reservar(os, itemId, 1).andExpect { status { isCreated() } }

        reservar(os, itemId, 1).andExpect { status { isConflict() }; erro(null, "A OS já possui uma reserva ativa para este item") }

        enviar("POST", "/api/v1/admin/estoque/ordens/$os/reservas/liberar").andExpect { status { isNoContent() } }
        reservar(os, itemId, 2).andExpect { status { isCreated() } }
        enviar("GET", "/api/v1/admin/estoque/ordens/$os/reservas")
            .andExpect { jsonPath("$[?(@.situacao=='LIBERADA')]") { isNotEmpty() }; jsonPath("$[?(@.situacao=='ATIVA')]") { isNotEmpty() } }
    }

    @Test
    fun `dado invalido, item inexistente e item inativo`() {
        val itemId = criarItem(3)

        reservar(novaOs(), itemId, 0).andExpect { status { isBadRequest() }; erro("quantidade", "Quantidade deve ser maior que zero") }
        reservar(novaOs(), itemId, null).andExpect { status { isBadRequest() }; erro("quantidade", "Quantidade é obrigatória") }
        reservar(novaOs(), null, 1).andExpect { status { isBadRequest() }; erro("itemId", "Item é obrigatório") }
        reservar(novaOs(), 999999, 1).andExpect { status { isNotFound() }; erro(null, "Item de estoque não encontrado") }

        enviar("POST", "/api/v1/admin/estoque/itens/$itemId/inativar").andExpect { status { isNoContent() } }
        reservar(novaOs(), itemId, 1).andExpect { status { isConflict() }; erro(null, "Item inativo não pode ser reservado") }
    }

    @Test
    fun `libera so a reserva do item informado e depois todas, de forma idempotente`() {
        val item1 = criarItem(10)
        val item2 = criarItem(10)
        val os = novaOs()
        reservar(os, item1, 3).andExpect { status { isCreated() } }
        reservar(os, item2, 2).andExpect { status { isCreated() } }

        enviar("POST", "/api/v1/admin/estoque/ordens/$os/reservas/liberar?itemId=$item1").andExpect { status { isNoContent() } }

        item(item1).andExpect { jsonPath("$.reservado") { value(0) }; jsonPath("$.disponivel") { value(10) } }
        item(item2).andExpect { jsonPath("$.reservado") { value(2) } }
        assertThat(eventos.stream(ReservaLiberada::class.java).toList()).contains(ReservaLiberada(os, item1, 3))

        enviar("POST", "/api/v1/admin/estoque/ordens/$os/reservas/liberar").andExpect { status { isNoContent() } }
        enviar("POST", "/api/v1/admin/estoque/ordens/$os/reservas/liberar").andExpect { status { isNoContent() } }

        item(item2).andExpect { jsonPath("$.reservado") { value(0) }; jsonPath("$.disponivel") { value(10) } }
        assertThat(eventos.stream(ReservaLiberada::class.java).filter { it.osId == os }.count()).isEqualTo(2)
    }

    @Test
    fun `baixa de dois itens reduz o saldo, consome as reservas e e idempotente`() {
        val item1 = criarItem(10)
        val item2 = criarItem(5)
        val os = novaOs()
        reservar(os, item1, 3).andExpect { status { isCreated() } }
        reservar(os, item2, 2).andExpect { status { isCreated() } }

        enviar("POST", "/api/v1/admin/estoque/ordens/$os/baixa")
            .andExpect { status { isOk() }; jsonPath("$.length()") { value(2) }; jsonPath("$[?(@.situacao!='CONSUMIDA')]") { isEmpty() } }

        item(item1).andExpect { jsonPath("$.saldo") { value(7) }; jsonPath("$.reservado") { value(0) }; jsonPath("$.disponivel") { value(7) } }
        item(item2).andExpect { jsonPath("$.saldo") { value(3) }; jsonPath("$.reservado") { value(0) } }
        val baixadas = eventos.stream(PecasBaixadas::class.java).filter { it.osId == os }.toList()
        assertThat(baixadas).hasSize(1)
        assertThat(baixadas.single().itens.map { it.itemId to it.quantidade }).containsExactlyInAnyOrder(item1 to 3, item2 to 2)

        enviar("POST", "/api/v1/admin/estoque/ordens/$os/baixa").andExpect { status { isOk() }; jsonPath("$.length()") { value(0) } }
        item(item1).andExpect { jsonPath("$.saldo") { value(7) } }
        enviar("GET", "/api/v1/admin/estoque/ordens/$os/reservas")
            .andExpect { jsonPath("$.length()") { value(2) }; jsonPath("$[?(@.situacao!='CONSUMIDA')]") { isEmpty() } }
    }

    @Test
    fun `item com reserva ativa nao e inativado ate liberar`() {
        val itemId = criarItem(3)
        val os = novaOs()
        reservar(os, itemId, 1).andExpect { status { isCreated() } }

        enviar("POST", "/api/v1/admin/estoque/itens/$itemId/inativar")
            .andExpect { status { isConflict() }; erro(null, "Item possui reservas ativas e não pode ser inativado") }

        enviar("POST", "/api/v1/admin/estoque/ordens/$os/reservas/liberar").andExpect { status { isNoContent() } }
        enviar("POST", "/api/v1/admin/estoque/itens/$itemId/inativar").andExpect { status { isNoContent() } }
    }

    @Test
    fun `as reservas de uma OS nao aparecem em outra`() {
        val itemId = criarItem(5)
        val osA = novaOs()
        val osB = novaOs()
        reservar(osA, itemId, 1).andExpect { status { isCreated() } }

        enviar("GET", "/api/v1/admin/estoque/ordens/$osB/reservas").andExpect { jsonPath("$.length()") { value(0) } }
        enviar("POST", "/api/v1/admin/estoque/ordens/$osB/baixa").andExpect { status { isOk() }; jsonPath("$.length()") { value(0) } }
        item(itemId).andExpect { jsonPath("$.reservado") { value(1) } }
    }

    @Test
    fun `disputa simultanea pela ultima unidade cria exatamente uma reserva`() {
        val itemId = criarItem(1)

        val resultados = dispararReservas(itemId, quantidade = 1, tentativas = 8)

        assertThat(resultados.filterIsInstance<ResultadoReserva.Reservada>()).hasSize(1)
        assertThat(resultados.filterIsInstance<ResultadoReserva.Indisponivel>()).hasSize(7)
        val depois = consultarItem.buscarPorId(itemId)
        assertThat(depois.reservado).isEqualTo(1)
        assertThat(depois.disponivel).isZero()
        assertThat(jdbc.queryForObject("select count(*) from estoque.reserva where item_id = ? and situacao = 'ATIVA'", Long::class.java, itemId))
            .isEqualTo(1L)
    }

    @Test
    fun `disputa por varias unidades nunca reserva alem do saldo`() {
        val itemId = criarItem(5)

        val resultados = dispararReservas(itemId, quantidade = 1, tentativas = 20)

        assertThat(resultados.filterIsInstance<ResultadoReserva.Reservada>()).hasSize(5)
        assertThat(resultados.filterIsInstance<ResultadoReserva.Indisponivel>()).hasSize(15)
        assertThat(consultarItem.buscarPorId(itemId).reservado).isEqualTo(5)
    }

    @Test
    fun `o indice unico parcial impede duas reservas ativas da mesma OS e item`() {
        val itemId = criarItem(5)
        val os = novaOs()
        reservas.salvar(Reserva.criar(os, itemId, Quantidade.of(1)))

        assertThatThrownBy { reservas.salvar(Reserva.criar(os, itemId, Quantidade.of(1))) }
            .isInstanceOfSatisfying(DuplicidadeException::class.java) { assertThat(it.campo).isEqualTo("itemId") }
    }

    @Test
    fun `reserva liberada nao conta no indice parcial`() {
        val itemId = criarItem(5)
        val os = novaOs()
        val primeira = reservas.salvar(Reserva.criar(os, itemId, Quantidade.of(1)))
        reservas.salvar(primeira.liberar())

        val segunda = reservas.salvar(Reserva.criar(os, itemId, Quantidade.of(1)))

        assertThat(segunda.ativa).isTrue()
        assertThat(reservas.listarPorOs(os)).hasSize(2)
    }

    @Test
    fun `o banco impede quantidade invalida, situacao invalida e item inexistente`() {
        val itemId = criarItem(5)
        val sql = "insert into estoque.reserva (item_id, os_id, quantidade, situacao) values (?, ?, ?, ?)"

        assertThatThrownBy { jdbc.update(sql, itemId, novaOs(), 0, "ATIVA") }.isInstanceOf(DataIntegrityViolationException::class.java)
        assertThatThrownBy { jdbc.update(sql, itemId, novaOs(), 1, "INVALIDA") }.isInstanceOf(DataIntegrityViolationException::class.java)
        assertThatThrownBy { jdbc.update(sql, 999999, novaOs(), 1, "ATIVA") }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `documentacao openapi publica as rotas de reserva`() {
        val resposta = mockMvc.get("/v3/api-docs").andExpect { status { isOk() } }.andReturn().response.contentAsString
        val paths = objectMapper.readTree(resposta).get("paths").fieldNames().asSequence().toList()

        assertThat(paths).contains(
            "/api/v1/admin/estoque/ordens/{osId}/reservas",
            "/api/v1/admin/estoque/ordens/{osId}/reservas/liberar",
            "/api/v1/admin/estoque/ordens/{osId}/baixa",
        )
    }

    private fun dispararReservas(itemId: Long, quantidade: Int, tentativas: Int): List<ResultadoReserva> {
        val pool = Executors.newFixedThreadPool(tentativas)
        val largada = CountDownLatch(1)
        val futuros = (1..tentativas).map {
            val os = novaOs()
            pool.submit<ResultadoReserva> {
                largada.await()
                reservarPeca.reservar(os, itemId, quantidade)
            }
        }
        largada.countDown()
        val resultados = futuros.map { it.get() }
        pool.shutdown()
        return resultados
    }

    companion object {
        private val sequencia = AtomicLong(1_000_000)

        private fun novaOs(): Long = sequencia.incrementAndGet()

        private fun nomeUnico(): String = "Item de reserva ${sequencia.incrementAndGet()}"

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
