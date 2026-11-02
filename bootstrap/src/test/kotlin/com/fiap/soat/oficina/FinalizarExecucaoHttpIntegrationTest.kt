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
class FinalizarExecucaoHttpIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

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
                "documento" to "123.456.789-09", "placa" to "ABC1D23", "marca" to "Fiat",
                "modelo" to "Uno", "ano" to 2020, "relato" to "Barulho ao frear",
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

    private fun ateEmExecucao(token: String): Long {
        val id = criarOrdem(token)
        post("/api/admin/ordens/$id/diagnostico/iniciar", token)
        post("/api/admin/ordens/$id/diagnostico/finalizar", token, """{ "pecaIdentificada": true }""")
        post("/api/admin/ordens/$id/orcamento", token, """{ "itens": [ { "tipo": "serviço", "descricao": "Troca", "valor": 150.00 } ] }""")
        post("/api/admin/ordens/$id/orcamento/aprovar", token)
        post("/api/admin/ordens/$id/execucao/iniciar", token)
        return id
    }

    @Test
    fun `finaliza execucao retorna 200 finalizada`() {
        val token = token()
        val id = ateEmExecucao(token)

        post("/api/admin/ordens/$id/execucao/finalizar", token, """{ "todosAplicados": true }""").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("FINALIZADA") }
        }
    }

    @Test
    fun `finaliza com itens pendentes retorna 409`() {
        val token = token()
        val id = ateEmExecucao(token)

        post("/api/admin/ordens/$id/execucao/finalizar", token, """{ "todosAplicados": false }""").andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `finaliza fora de execucao retorna 409`() {
        val token = token()
        val id = criarOrdem(token)

        post("/api/admin/ordens/$id/execucao/finalizar", token, """{ "todosAplicados": true }""").andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `finaliza sem token retorna 401`() {
        mockMvc.post("/api/admin/ordens/1/execucao/finalizar") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "todosAplicados": true }"""
        }.andExpect { status { isUnauthorized() } }
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
