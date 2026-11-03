package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class OrcamentoComPrecosDoCatalogoHttpIntegrationTest {

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

    private fun post(url: String, body: String? = null): ResultActionsDsl = mockMvc.post(url) {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        if (body != null) content = body
    }

    private fun idDe(resposta: ResultActionsDsl): Long =
        objectMapper.readTree(resposta.andReturn().response.contentAsString).get("id").asLong()

    private fun sufixo() = UUID.randomUUID().toString().take(8)

    private fun criarServico(preco: String): Long =
        idDe(post("/api/v1/admin/servicos", """{ "nome": "Servico ${sufixo()}", "preco": $preco }"""))

    private fun criarPeca(preco: String, saldo: Int = 10): Long = idDe(
        post(
            "/api/v1/admin/estoque/itens",
            """{ "nome": "Peca ${sufixo()}", "tipo": "PECA", "preco": $preco, "saldoInicial": $saldo }""",
        ),
    )

    private fun osEmDiagnosticoFinalizado(): Long {
        val corpo = """
            { "documento": "123.456.789-09", "placa": "ABC1D23", "marca": "Fiat", "modelo": "Uno",
              "ano": 2020, "relato": "Barulho ao frear" }
        """.trimIndent()
        val id = idDe(post("/api/admin/ordens", corpo))
        post("/api/admin/ordens/$id/diagnostico/iniciar")
        post("/api/admin/ordens/$id/diagnostico/finalizar", """{ "pecaIdentificada": true }""")
        return id
    }

    @Test
    fun `gera orcamento so com ids e soma os precos do cadastro e do estoque`() {
        val servico = criarServico("150.00")
        val peca = criarPeca("200.00")
        val os = osEmDiagnosticoFinalizado()

        post(
            "/api/admin/ordens/$os/orcamento",
            """{ "itens": [
                { "tipo": "servico", "servicoId": $servico },
                { "tipo": "peca", "itemEstoqueId": $peca, "quantidade": 2 }
            ] }""",
        ).andExpect {
            status { isOk() }
            jsonPath("$.status") { value("AGUARDANDO_APROVACAO") }
            jsonPath("$.total") { value(550.00) }
        }
    }

    @Test
    fun `valor informado prevalece sobre o preco do catalogo`() {
        val servico = criarServico("150.00")
        val os = osEmDiagnosticoFinalizado()

        post(
            "/api/admin/ordens/$os/orcamento",
            """{ "itens": [ { "tipo": "servico", "servicoId": $servico, "descricao": "Com desconto", "valor": 100.00 } ] }""",
        ).andExpect {
            status { isOk() }
            jsonPath("$.total") { value(100.00) }
        }
    }

    @Test
    fun `item avulso com descricao e valor continua aceito`() {
        val os = osEmDiagnosticoFinalizado()

        post(
            "/api/admin/ordens/$os/orcamento",
            """{ "itens": [ { "tipo": "servico", "descricao": "Lavagem", "valor": 40.00 } ] }""",
        ).andExpect {
            status { isOk() }
            jsonPath("$.total") { value(40.00) }
        }
    }

    @Test
    fun `revisa orcamento com os precos atuais do catalogo`() {
        val servico = criarServico("150.00")
        val os = osEmDiagnosticoFinalizado()
        post("/api/admin/ordens/$os/orcamento", """{ "itens": [ { "tipo": "servico", "descricao": "Avulso", "valor": 10.00 } ] }""")

        post(
            "/api/admin/ordens/$os/orcamento/revisar",
            """{ "itens": [ { "tipo": "servico", "servicoId": $servico } ] }""",
        ).andExpect {
            status { isOk() }
            jsonPath("$.total") { value(150.00) }
        }
    }

    @Test
    fun `servico inexistente retorna 404`() {
        val os = osEmDiagnosticoFinalizado()

        post("/api/admin/ordens/$os/orcamento", """{ "itens": [ { "tipo": "servico", "servicoId": 999999 } ] }""")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `servico inativo retorna 404`() {
        val servico = criarServico("150.00")
        post("/api/v1/admin/servicos/$servico/inativar")
        val os = osEmDiagnosticoFinalizado()

        post("/api/admin/ordens/$os/orcamento", """{ "itens": [ { "tipo": "servico", "servicoId": $servico } ] }""")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `item de estoque inexistente ou inativo retorna 404`() {
        val inativa = criarPeca("200.00")
        post("/api/v1/admin/estoque/itens/$inativa/inativar")
        val os = osEmDiagnosticoFinalizado()

        post(
            "/api/admin/ordens/$os/orcamento",
            """{ "itens": [ { "tipo": "peca", "itemEstoqueId": $inativa, "quantidade": 1 } ] }""",
        ).andExpect { status { isNotFound() } }
        post(
            "/api/admin/ordens/$os/orcamento",
            """{ "itens": [ { "tipo": "peca", "itemEstoqueId": 999999, "quantidade": 1 } ] }""",
        ).andExpect { status { isNotFound() } }
    }

    @Test
    fun `item sem id e sem valor retorna 400`() {
        val os = osEmDiagnosticoFinalizado()

        post("/api/admin/ordens/$os/orcamento", """{ "itens": [ { "tipo": "servico", "descricao": "Sem preco" } ] }""")
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `peca por id sem quantidade e sem valor retorna 400`() {
        val peca = criarPeca("200.00")
        val os = osEmDiagnosticoFinalizado()

        post("/api/admin/ordens/$os/orcamento", """{ "itens": [ { "tipo": "peca", "itemEstoqueId": $peca } ] }""")
            .andExpect { status { isBadRequest() } }
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
