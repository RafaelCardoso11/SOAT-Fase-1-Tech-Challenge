package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class GerarOrcamentoHttpIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun token(): String {
        val resposta = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("nomeUsuario" to "admin", "senha" to "admin@123"))
        }.andReturn().response.contentAsString
        return objectMapper.readTree(resposta).get("token").asText()
    }

    private fun criarOrdem(token: String): Long {
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
        val resposta = mockMvc.post("/api/admin/ordens") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = corpo
        }.andReturn().response.contentAsString
        return objectMapper.readTree(resposta).get("id").asLong()
    }

    private fun post(url: String, token: String, body: String? = null) = mockMvc.post(url) {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        if (body != null) content = body
    }

    private val corpoOrcamento = """
        { "itens": [
            { "tipo": "serviço", "descricao": "Troca de pastilhas", "valor": 150.00 },
            { "tipo": "serviço", "descricao": "Alinhamento", "valor": 200.00 }
        ] }
    """.trimIndent()

    private fun idCriado(url: String, token: String, corpo: String): Long {
        val resposta = post(url, token, corpo).andReturn().response.contentAsString
        return objectMapper.readTree(resposta).get("id").asLong()
    }

    private fun ordemProntaParaOrcamento(token: String): Long {
        val id = criarOrdem(token)
        post("/api/admin/ordens/$id/diagnostico/iniciar", token)
        post("/api/admin/ordens/$id/diagnostico/finalizar", token, """{ "pecaIdentificada": true }""")
        return id
    }

    private fun servicoCadastrado(token: String, preco: String) = idCriado(
        "/api/v1/admin/servicos",
        token,
        """{ "nome": "Alinhamento ${System.nanoTime()}", "preco": $preco }""",
    )

    private fun pecaCadastrada(token: String, preco: String) = idCriado(
        "/api/v1/admin/estoque/itens",
        token,
        """{ "nome": "Pastilha ${System.nanoTime()}", "tipo": "PECA", "preco": $preco, "saldoInicial": 10 }""",
    )

    @Test
    fun `orcamento so com ids usa os precos cadastrados`() {
        val token = token()
        val id = ordemProntaParaOrcamento(token)
        val servico = servicoCadastrado(token, "100.00")
        val peca = pecaCadastrada(token, "30.00")

        post(
            "/api/admin/ordens/$id/orcamento",
            token,
            """{ "itens": [
                { "tipo": "serviço", "servicoId": $servico },
                { "tipo": "peça", "itemEstoqueId": $peca, "quantidade": 2 }
            ] }""",
        ).andExpect {
            status { isOk() }
            jsonPath("$.status") { value("AGUARDANDO_APROVACAO") }
            jsonPath("$.total") { value(160.00) }
        }
    }

    @Test
    fun `valor informado tem prioridade sobre o preco cadastrado`() {
        val token = token()
        val id = ordemProntaParaOrcamento(token)
        val servico = servicoCadastrado(token, "100.00")

        post(
            "/api/admin/ordens/$id/orcamento",
            token,
            """{ "itens": [{ "tipo": "serviço", "servicoId": $servico, "valor": 80.00 }] }""",
        ).andExpect {
            status { isOk() }
            jsonPath("$.total") { value(80.00) }
        }
    }

    @Test
    fun `revisao tambem usa os precos cadastrados`() {
        val token = token()
        val id = ordemProntaParaOrcamento(token)
        val servico = servicoCadastrado(token, "100.00")
        post("/api/admin/ordens/$id/orcamento", token, corpoOrcamento)

        post(
            "/api/admin/ordens/$id/orcamento/revisar",
            token,
            """{ "itens": [{ "tipo": "serviço", "servicoId": $servico }] }""",
        ).andExpect {
            status { isOk() }
            jsonPath("$.total") { value(100.00) }
        }
    }

    @Test
    fun `servico inexistente retorna 404`() {
        val token = token()
        val id = ordemProntaParaOrcamento(token)

        post(
            "/api/admin/ordens/$id/orcamento",
            token,
            """{ "itens": [{ "tipo": "serviço", "servicoId": 999999 }] }""",
        ).andExpect {
            status { isNotFound() }
            jsonPath("$.erro") { value("Serviço 999999 não encontrado") }
        }
    }

    @Test
    fun `item avulso sem valor retorna 400`() {
        val token = token()
        val id = ordemProntaParaOrcamento(token)

        post(
            "/api/admin/ordens/$id/orcamento",
            token,
            """{ "itens": [{ "tipo": "serviço", "descricao": "Revisão extra" }] }""",
        ).andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `gera orcamento apos diagnostico finalizado retorna 200`() {
        val token = token()
        val id = criarOrdem(token)
        post("/api/admin/ordens/$id/diagnostico/iniciar", token)
        post(
            "/api/admin/ordens/$id/diagnostico/finalizar",
            token,
            """{ "pecaIdentificada": true }""",
        )

        post("/api/admin/ordens/$id/orcamento", token, corpoOrcamento).andExpect {
            status { isOk() }
            jsonPath("$.status") { value("AGUARDANDO_APROVACAO") }
            jsonPath("$.total") { value(350.00) }
        }
    }

    @Test
    fun `gera orcamento sem diagnostico finalizado retorna 409`() {
        val token = token()
        val id = criarOrdem(token)

        post("/api/admin/ordens/$id/orcamento", token, corpoOrcamento).andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `gera orcamento de OS inexistente retorna 404`() {
        post("/api/admin/ordens/999999/orcamento", token(), corpoOrcamento).andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `gera orcamento sem token retorna 401`() {
        mockMvc.post("/api/admin/ordens/1/orcamento") {
            contentType = MediaType.APPLICATION_JSON
            content = corpoOrcamento
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
