package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ConsultarOrdensAdminHttpIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val token: String by lazy {
        val resposta = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("nomeUsuario" to "admin", "senha" to "admin@123"))
        }.andReturn().response.contentAsString
        objectMapper.readTree(resposta).get("token").asText()
    }

    private fun enviar(url: String, corpo: String? = null): ResultActionsDsl = mockMvc.post(url) {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        if (corpo != null) content = corpo
    }

    private fun consultar(url: String): ResultActionsDsl = mockMvc.get(url) { header("Authorization", "Bearer $token") }

    private fun criarOrdem(): Long {
        val corpo = objectMapper.writeValueAsString(
            mapOf(
                "documento" to "123.456.789-09",
                "placa" to "ABC1D23",
                "marca" to "Fiat",
                "modelo" to "Uno",
                "ano" to 2020,
                "relato" to "Barulho ao frear",
            ),
        )
        val resposta = enviar("/api/admin/ordens", corpo).andReturn().response.contentAsString
        return objectMapper.readTree(resposta).get("id").asLong()
    }

    private fun criarItemDeEstoque(saldo: Int): Long {
        val corpo = objectMapper.writeValueAsString(
            mapOf(
                "nome" to "Pastilha de freio ${sequencia.incrementAndGet()}",
                "tipo" to "PECA",
                "preco" to 200,
                "saldoInicial" to saldo,
            ),
        )
        val resposta = enviar("/api/v1/admin/estoque/itens", corpo).andReturn().response.contentAsString
        return objectMapper.readTree(resposta).get("id").asLong()
    }

    private fun levarAteEmExecucao(id: Long, itemEstoqueId: Long) {
        enviar("/api/admin/ordens/$id/diagnostico/iniciar").andExpect { status { isOk() } }
        enviar("/api/admin/ordens/$id/diagnostico/finalizar", """{ "pecaIdentificada": true }""")
            .andExpect { status { isOk() } }
        enviar(
            "/api/admin/ordens/$id/orcamento",
            """
            { "itens": [
                { "tipo": "serviço", "descricao": "Troca de pastilhas", "valor": 150.00 },
                { "tipo": "peça", "descricao": "Pastilha de freio", "valor": 200.00,
                  "itemEstoqueId": $itemEstoqueId, "quantidade": 2 }
            ] }
            """.trimIndent(),
        ).andExpect { status { isOk() } }
        enviar("/api/admin/ordens/$id/orcamento/aprovar").andExpect { status { isOk() } }
        enviar("/api/admin/ordens/$id/execucao/iniciar").andExpect { status { isOk() } }
    }

    @Test
    fun `listagem e detalhe sem token retornam 401`() {
        mockMvc.get("/api/admin/ordens").andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/admin/ordens/1").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `lista as OS paginadas, da mais recente para a mais antiga`() {
        val primeira = criarOrdem()
        val segunda = criarOrdem()

        val resposta = consultar("/api/admin/ordens?size=2").andExpect {
            status { isOk() }
            jsonPath("$.conteudo.length()") { value(2) }
            jsonPath("$.pagina") { value(0) }
            jsonPath("$.tamanho") { value(2) }
            jsonPath("$.totalElementos") { exists() }
            jsonPath("$.totalPaginas") { exists() }
        }.andReturn().response.contentAsString

        val numeros = objectMapper.readTree(resposta).get("conteudo").map { it.get("numero").asLong() }
        assertThat(numeros).containsExactly(segunda, primeira)
    }

    @Test
    fun `cada item da lista traz status, documento, veiculo e relato`() {
        val id = criarOrdem()

        consultar("/api/admin/ordens?size=1").andExpect {
            status { isOk() }
            jsonPath("$.conteudo[0].numero") { value(id) }
            jsonPath("$.conteudo[0].status") { value("RECEBIDA") }
            jsonPath("$.conteudo[0].documento") { value("12345678909") }
            jsonPath("$.conteudo[0].placa") { value("ABC1D23") }
            jsonPath("$.conteudo[0].marca") { value("Fiat") }
            jsonPath("$.conteudo[0].modelo") { value("Uno") }
            jsonPath("$.conteudo[0].ano") { value(2020) }
            jsonPath("$.conteudo[0].relato") { value("Barulho ao frear") }
        }
    }

    @Test
    fun `filtra por status sem diferenciar maiusculas`() {
        val recebida = criarOrdem()
        val emDiagnostico = criarOrdem()
        enviar("/api/admin/ordens/$emDiagnostico/diagnostico/iniciar").andExpect { status { isOk() } }

        val resposta = consultar("/api/admin/ordens?status=em_diagnostico&size=100").andExpect {
            status { isOk() }
            jsonPath("$.conteudo[?(@.status!='EM_DIAGNOSTICO')]") { isEmpty() }
        }.andReturn().response.contentAsString

        val numeros = objectMapper.readTree(resposta).get("conteudo").map { it.get("numero").asLong() }
        assertThat(numeros).contains(emDiagnostico).doesNotContain(recebida)
    }

    @Test
    fun `paginas diferentes trazem OS diferentes`() {
        repeat(3) { criarOrdem() }

        val pagina0 = consultar("/api/admin/ordens?page=0&size=1").andReturn().response.contentAsString
        val pagina1 = consultar("/api/admin/ordens?page=1&size=1").andReturn().response.contentAsString

        val numero0 = objectMapper.readTree(pagina0).get("conteudo").get(0).get("numero").asLong()
        val numero1 = objectMapper.readTree(pagina1).get("conteudo").get(0).get("numero").asLong()
        assertThat(numero0).isGreaterThan(numero1)
    }

    @Test
    fun `parametros invalidos retornam 400`() {
        consultar("/api/admin/ordens?status=INEXISTENTE").andExpect {
            status { isBadRequest() }
            jsonPath("$.erro") { value("Status inválido: use RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, EM_EXECUCAO, FINALIZADA, ENTREGUE") }
        }
        consultar("/api/admin/ordens?page=-1").andExpect {
            status { isBadRequest() }
            jsonPath("$.erro") { value("A página deve ser maior ou igual a zero") }
        }
        consultar("/api/admin/ordens?size=0").andExpect {
            status { isBadRequest() }
            jsonPath("$.erro") { value("O tamanho da página deve estar entre 1 e 100") }
        }
        consultar("/api/admin/ordens?size=101").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `detalhe de OS recem criada nao tem orcamento nem execucao`() {
        val id = criarOrdem()

        consultar("/api/admin/ordens/$id").andExpect {
            status { isOk() }
            jsonPath("$.numero") { value(id) }
            jsonPath("$.status") { value("RECEBIDA") }
            jsonPath("$.documento") { value("12345678909") }
            jsonPath("$.relato") { value("Barulho ao frear") }
            jsonPath("$.orcamento") { value(null) }
            jsonPath("$.execucao.iniciadaEm") { value(null) }
            jsonPath("$.historico.length()") { value(1) }
            jsonPath("$.historico[0].status") { value("RECEBIDA") }
        }
    }

    @Test
    fun `detalhe traz servicos, pecas, orcamento, execucao e historico`() {
        val itemEstoqueId = criarItemDeEstoque(saldo = 5)
        val id = criarOrdem()
        levarAteEmExecucao(id, itemEstoqueId)

        consultar("/api/admin/ordens/$id").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("EM_EXECUCAO") }
            jsonPath("$.liberadoParaOrcamento") { value(true) }
            jsonPath("$.orcamento.total") { value(350.0) }
            jsonPath("$.orcamento.aprovadoEm") { exists() }
            jsonPath("$.orcamento.validoAte") { exists() }
            jsonPath("$.orcamento.motivoRejeicao") { value(null) }
            jsonPath("$.orcamento.servicos.length()") { value(1) }
            jsonPath("$.orcamento.servicos[0].descricao") { value("Troca de pastilhas") }
            jsonPath("$.orcamento.servicos[0].valor") { value(150.0) }
            jsonPath("$.orcamento.pecas.length()") { value(1) }
            jsonPath("$.orcamento.pecas[0].descricao") { value("Pastilha de freio") }
            jsonPath("$.orcamento.pecas[0].valor") { value(200.0) }
            jsonPath("$.orcamento.pecas[0].itemEstoqueId") { value(itemEstoqueId) }
            jsonPath("$.orcamento.pecas[0].quantidade") { value(2) }
            jsonPath("$.execucao.iniciadaEm") { exists() }
            jsonPath("$.execucao.finalizadaEm") { value(null) }
            jsonPath("$.historico.length()") { value(4) }
            jsonPath("$.historico[0].status") { value("RECEBIDA") }
            jsonPath("$.historico[1].status") { value("EM_DIAGNOSTICO") }
            jsonPath("$.historico[2].status") { value("AGUARDANDO_APROVACAO") }
            jsonPath("$.historico[3].status") { value("EM_EXECUCAO") }
        }
    }

    @Test
    fun `detalhe mostra o motivo quando o orcamento foi rejeitado`() {
        val itemEstoqueId = criarItemDeEstoque(saldo = 5)
        val id = criarOrdem()
        enviar("/api/admin/ordens/$id/diagnostico/iniciar").andExpect { status { isOk() } }
        enviar("/api/admin/ordens/$id/diagnostico/finalizar", """{ "pecaIdentificada": true }""")
        enviar(
            "/api/admin/ordens/$id/orcamento",
            """{ "itens": [ { "tipo": "peça", "descricao": "Pastilha", "valor": 200.00,
                 "itemEstoqueId": $itemEstoqueId, "quantidade": 1 } ] }""",
        ).andExpect { status { isOk() } }
        enviar("/api/admin/ordens/$id/orcamento/rejeitar", """{ "motivo": "Valor acima do esperado" }""")
            .andExpect { status { isOk() } }

        consultar("/api/admin/ordens/$id").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("FINALIZADA") }
            jsonPath("$.orcamento.motivoRejeicao") { value("Valor acima do esperado") }
            jsonPath("$.orcamento.aprovadoEm") { value(null) }
            jsonPath("$.orcamento.servicos.length()") { value(0) }
        }
    }

    @Test
    fun `a consulta administrativa mostra o documento completo e a publica mascara`() {
        val id = criarOrdem()

        consultar("/api/admin/ordens/$id").andExpect { jsonPath("$.documento") { value("12345678909") } }
        mockMvc.get("/api/public/ordens/$id").andExpect { jsonPath("$.documento") { value("*********09") } }
    }

    @Test
    fun `detalhe de OS inexistente retorna 404`() {
        consultar("/api/admin/ordens/999999").andExpect {
            status { isNotFound() }
            jsonPath("$.erro") { value("Ordem de serviço não encontrada") }
        }
    }

    @Test
    fun `documentacao openapi publica a listagem e o detalhe`() {
        val resposta = mockMvc.get("/v3/api-docs").andExpect { status { isOk() } }.andReturn().response.contentAsString
        val paths = objectMapper.readTree(resposta).get("paths")

        assertThat(paths.get("/api/admin/ordens").has("get")).isTrue()
        assertThat(paths.get("/api/admin/ordens/{id}").has("get")).isTrue()
    }

    companion object {
        private val sequencia = AtomicInteger(1000)

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
