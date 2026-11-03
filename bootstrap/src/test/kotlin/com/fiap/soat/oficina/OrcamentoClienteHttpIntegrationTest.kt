package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.ObjectMapper
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class OrcamentoClienteHttpIntegrationTest {


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

    @Autowired
    lateinit var repositorio: OrdemServicoRepository

    private fun ordemAguardandoAprovacao(token: String): Long {
        val id = criarOrdem(token)
        post("/api/admin/ordens/$id/diagnostico/iniciar", token)
        post("/api/admin/ordens/$id/diagnostico/finalizar", token, """{ "pecaIdentificada": true }""")
        post(
            "/api/admin/ordens/$id/orcamento",
            token,
            """{ "itens": [ { "tipo": "serviço", "descricao": "Troca", "valor": 150.00 } ] }""",
        )
        return id
    }

    private fun tokenAprovacao(id: Long) = repositorio.buscarPorId(id)!!.orcamento!!.tokenAprovacao

    private fun postPublico(url: String, body: String? = null) = mockMvc.post(url) {
        contentType = MediaType.APPLICATION_JSON
        if (body != null) content = body
    }

    @Test
    fun `cliente consulta o orcamento pelo token sem login`() {
        val id = ordemAguardandoAprovacao(token())

        mockMvc.get("/api/public/orcamentos/${tokenAprovacao(id)}").andExpect {
            status { isOk() }
            jsonPath("$.numero") { value(id) }
            jsonPath("$.status") { value("AGUARDANDO_APROVACAO") }
            jsonPath("$.itens[0].descricao") { value("Troca") }
            jsonPath("$.total") { value(150.00) }
        }
    }

    @Test
    fun `cliente aprova pelo token sem login`() {
        val id = ordemAguardandoAprovacao(token())

        postPublico("/api/public/orcamentos/${tokenAprovacao(id)}/aprovar").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("EM_EXECUCAO") }
        }
    }

    @Test
    fun `cliente rejeita pelo token com motivo`() {
        val id = ordemAguardandoAprovacao(token())

        postPublico("/api/public/orcamentos/${tokenAprovacao(id)}/rejeitar", """{ "motivo": "Muito caro" }""").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("FINALIZADA") }
        }
        assert(repositorio.buscarPorId(id)!!.orcamento!!.motivoRejeicao == "Muito caro")
    }

    @Test
    fun `token ja usado responde 409`() {
        val id = ordemAguardandoAprovacao(token())
        val tokenOrcamento = tokenAprovacao(id)
        postPublico("/api/public/orcamentos/$tokenOrcamento/aprovar")

        postPublico("/api/public/orcamentos/$tokenOrcamento/rejeitar").andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `revisao troca o token e o antigo deixa de valer`() {
        val token = token()
        val id = ordemAguardandoAprovacao(token)
        val antigo = tokenAprovacao(id)
        post(
            "/api/admin/ordens/$id/orcamento/revisar",
            token,
            """{ "itens": [ { "tipo": "serviço", "descricao": "Troca revisada", "valor": 120.00 } ] }""",
        )

        postPublico("/api/public/orcamentos/$antigo/aprovar").andExpect { status { isNotFound() } }
        postPublico("/api/public/orcamentos/${tokenAprovacao(id)}/aprovar").andExpect { status { isOk() } }
    }

    @Test
    fun `token desconhecido responde 404`() {
        mockMvc.get("/api/public/orcamentos/nao-existe").andExpect { status { isNotFound() } }
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
