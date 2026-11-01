package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.ObjectMapper
import com.fiap.soat.oficina.estoque.domain.error.DuplicidadeException
import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.GerenciarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
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
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class EstoqueItensIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var itens: ItemEstoqueRepository

    @Autowired
    lateinit var gerenciar: GerenciarItemUseCase

    @Autowired
    lateinit var consultar: ConsultarItemUseCase

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var transacao: TransactionTemplate

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
            "PUT" -> mockMvc.put(caminho) { autenticado(corpo) }
            else -> mockMvc.get(caminho) { autenticado(corpo) }
        }

    private fun idDe(resultado: ResultActionsDsl): Long =
        objectMapper.readTree(resultado.andReturn().response.contentAsString).get("id").asLong()

    private fun MockMvcResultMatchersDsl.erro(campo: String?, mensagem: String) {
        jsonPath("$.erro") { value(mensagem) }
        if (campo != null) jsonPath("$.campo") { value(campo) }
    }

    private fun criarItem(
        nome: String = nomeUnico(),
        tipo: String = "PECA",
        preco: Any = 200,
        saldoInicial: Int? = 10,
    ): Long = idDe(
        enviar(
            "POST",
            "/api/v1/admin/estoque/itens",
            json("nome" to nome, "descricao" to "Descrição", "tipo" to tipo, "preco" to preco, "saldoInicial" to saldoInicial),
        ).andExpect { status { isCreated() } },
    )

    @Test
    fun `rotas de estoque sem token retornam 401`() {
        mockMvc.get("/api/v1/admin/estoque/itens").andExpect { status { isUnauthorized() } }
        mockMvc.post("/api/v1/admin/estoque/itens/1/entradas").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `fluxo completo de item com entrada e inativacao`() {
        val nome = nomeUnico()
        val id = idDe(
            enviar(
                "POST",
                "/api/v1/admin/estoque/itens",
                json("nome" to " $nome ", "descricao" to "Dianteira", "tipo" to "peca", "preco" to 200, "saldoInicial" to 10),
            ).andExpect {
                status { isCreated() }
                header { exists("Location") }
                jsonPath("$.nome") { value(nome) }
                jsonPath("$.tipo") { value("PECA") }
                jsonPath("$.preco") { value(200.0) }
                jsonPath("$.saldo") { value(10) }
                jsonPath("$.reservado") { value(0) }
                jsonPath("$.disponivel") { value(10) }
                jsonPath("$.ativo") { value(true) }
            },
        )

        enviar("GET", "/api/v1/admin/estoque/itens/$id").andExpect { status { isOk() }; jsonPath("$.id") { value(id) } }
        enviar("PUT", "/api/v1/admin/estoque/itens/$id", json("nome" to nome, "descricao" to null, "tipo" to "INSUMO", "preco" to 99.9))
            .andExpect {
                status { isOk() }
                jsonPath("$.tipo") { value("INSUMO") }
                jsonPath("$.preco") { value(99.9) }
                jsonPath("$.saldo") { value(10) }
            }
        enviar("POST", "/api/v1/admin/estoque/itens/$id/entradas", json("quantidade" to 5))
            .andExpect { status { isOk() }; jsonPath("$.saldo") { value(15) }; jsonPath("$.disponivel") { value(15) } }

        enviar("POST", "/api/v1/admin/estoque/itens/$id/inativar").andExpect { status { isNoContent() } }
        enviar("POST", "/api/v1/admin/estoque/itens/$id/inativar").andExpect { status { isNoContent() } }
        enviar("GET", "/api/v1/admin/estoque/itens/$id").andExpect { jsonPath("$.ativo") { value(false) } }
        enviar("GET", "/api/v1/admin/estoque/itens/$id?ativo=true").andExpect { status { isNotFound() } }
        enviar("GET", "/api/v1/admin/estoque/itens?ativo=false")
            .andExpect { status { isOk() }; jsonPath("$[?(@.id==$id)]") { isNotEmpty() } }
        enviar("GET", "/api/v1/admin/estoque/itens?ativo=true")
            .andExpect { status { isOk() }; jsonPath("$[?(@.id==$id)]") { isEmpty() } }
    }

    @Test
    fun `saldo inicial e opcional`() {
        val id = criarItem(saldoInicial = null)

        enviar("GET", "/api/v1/admin/estoque/itens/$id").andExpect { jsonPath("$.saldo") { value(0) } }
    }

    @Test
    fun `item invalido retorna 400 com campo e mensagem`() {
        val base = mapOf("nome" to nomeUnico(), "tipo" to "PECA", "preco" to 10)

        fun cadastro(vararg alteracoes: Pair<String, Any?>) =
            enviar("POST", "/api/v1/admin/estoque/itens", objectMapper.writeValueAsString(base + alteracoes))

        cadastro("nome" to " ").andExpect { status { isBadRequest() }; erro("nome", "Nome é obrigatório") }
        cadastro("tipo" to "SERVICO").andExpect { status { isBadRequest() }; erro("tipo", "Tipo inválido: use PECA ou INSUMO") }
        cadastro("tipo" to null).andExpect { status { isBadRequest() }; erro("tipo", "Tipo é obrigatório") }
        cadastro("preco" to -1).andExpect { status { isBadRequest() }; erro("preco", "Preço não pode ser negativo") }
        cadastro("preco" to null).andExpect { status { isBadRequest() }; erro("preco", "Preço é obrigatório") }
        cadastro("saldoInicial" to -5)
            .andExpect { status { isBadRequest() }; erro("saldo", "Saldo inicial não pode ser negativo") }
        enviar("POST", "/api/v1/admin/estoque/itens", "{}").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `entrada com quantidade invalida retorna 400`() {
        val id = criarItem()

        enviar("POST", "/api/v1/admin/estoque/itens/$id/entradas", json("quantidade" to 0))
            .andExpect { status { isBadRequest() }; erro("quantidade", "Quantidade deve ser maior que zero") }
        enviar("POST", "/api/v1/admin/estoque/itens/$id/entradas", "{}")
            .andExpect { status { isBadRequest() }; erro("quantidade", "Quantidade é obrigatória") }
    }

    @Test
    fun `item inexistente retorna 404`() {
        enviar("GET", "/api/v1/admin/estoque/itens/999999")
            .andExpect { status { isNotFound() }; erro(null, "Item de estoque não encontrado") }
        enviar("POST", "/api/v1/admin/estoque/itens/999999/entradas", json("quantidade" to 1))
            .andExpect { status { isNotFound() } }
        enviar("PUT", "/api/v1/admin/estoque/itens/999999", json("nome" to "X", "tipo" to "PECA", "preco" to 1))
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `nome duplicado retorna 409 mesmo com caixa diferente ou item inativo`() {
        val nome = nomeUnico()
        val id = criarItem(nome)

        enviar("POST", "/api/v1/admin/estoque/itens", json("nome" to nome.uppercase(), "tipo" to "PECA", "preco" to 1))
            .andExpect { status { isConflict() }; erro("nome", "Já existe um item de estoque com este nome") }

        enviar("POST", "/api/v1/admin/estoque/itens/$id/inativar").andExpect { status { isNoContent() } }
        enviar("POST", "/api/v1/admin/estoque/itens", json("nome" to nome, "tipo" to "PECA", "preco" to 1))
            .andExpect { status { isConflict() } }
    }

    @Test
    fun `renomear para nome de outro item retorna 409`() {
        val nome = nomeUnico()
        criarItem(nome)
        val outro = criarItem()

        enviar("PUT", "/api/v1/admin/estoque/itens/$outro", json("nome" to nome.lowercase(), "tipo" to "PECA", "preco" to 1))
            .andExpect { status { isConflict() }; jsonPath("$.campo") { value("nome") } }
    }

    @Test
    fun `item inativo nao pode ser alterado nem receber entrada`() {
        val id = criarItem()
        enviar("POST", "/api/v1/admin/estoque/itens/$id/inativar").andExpect { status { isNoContent() } }

        enviar("PUT", "/api/v1/admin/estoque/itens/$id", json("nome" to nomeUnico(), "tipo" to "PECA", "preco" to 1))
            .andExpect { status { isConflict() }; erro(null, "Item inativo não pode ser alterado") }
        enviar("POST", "/api/v1/admin/estoque/itens/$id/entradas", json("quantidade" to 1))
            .andExpect { status { isConflict() }; erro(null, "Item inativo não pode receber entrada de estoque") }
    }

    @Test
    fun `o banco garante a unicidade do nome ignorando a caixa`() {
        val nome = nomeUnico()
        itens.salvar(ItemEstoque.criar(nome, null, "PECA", BigDecimal.TEN, 0))

        assertThatThrownBy { itens.salvar(ItemEstoque.criar(nome.uppercase(), null, "INSUMO", BigDecimal.ONE, 0)) }
            .isInstanceOfSatisfying(DuplicidadeException::class.java) { assertThat(it.campo).isEqualTo("nome") }
    }

    @Test
    fun `o banco impede reservado maior que o saldo e saldo negativo`() {
        val sql = "insert into estoque.item (nome, tipo, preco, saldo, reservado) values (?, 'PECA', 10, ?, ?)"

        assertThatThrownBy { jdbc.update(sql, nomeUnico(), 1, 2) }.isInstanceOf(DataIntegrityViolationException::class.java)
        assertThatThrownBy { jdbc.update(sql, nomeUnico(), -1, 0) }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `a leitura com lock traz o item dentro de uma transacao`() {
        val id = requireNotNull(itens.salvar(ItemEstoque.criar(nomeUnico(), null, "PECA", BigDecimal.TEN, 3)).id)

        val bloqueado = transacao.execute { itens.buscarPorIdParaAtualizar(id) }

        assertThat(bloqueado?.saldo).isEqualTo(3)
    }

    @Test
    fun `entradas simultaneas nao perdem atualizacoes gracas ao lock`() {
        val id = criarItem(saldoInicial = 0)
        val pool = Executors.newFixedThreadPool(8)

        val tarefas = (1..40).map { pool.submit { gerenciar.registrarEntrada(id, 1) } }
        tarefas.forEach { it.get() }
        pool.shutdown()

        assertThat(consultar.buscarPorId(id).saldo).isEqualTo(40)
    }

    @Test
    fun `documentacao openapi publica as rotas de itens de estoque`() {
        val resposta = mockMvc.get("/v3/api-docs").andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        val paths = objectMapper.readTree(resposta).get("paths").fieldNames().asSequence().toList()

        assertThat(paths).contains(
            "/api/v1/admin/estoque/itens",
            "/api/v1/admin/estoque/itens/{id}",
            "/api/v1/admin/estoque/itens/{id}/entradas",
            "/api/v1/admin/estoque/itens/{id}/inativar",
        )
    }

    companion object {
        private val sequencia = AtomicInteger(1000)

        private fun nomeUnico(): String = "Item de estoque ${sequencia.incrementAndGet()}"

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
