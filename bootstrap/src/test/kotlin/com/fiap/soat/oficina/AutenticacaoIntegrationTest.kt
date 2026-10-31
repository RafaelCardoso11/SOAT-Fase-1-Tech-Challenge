package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.ObjectMapper
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
class AutenticacaoIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun corpoLogin(nomeUsuario: String, senha: String): String =
        objectMapper.writeValueAsString(mapOf("nomeUsuario" to nomeUsuario, "senha" to senha))

    @Test
    fun `login com usuario de demonstracao retorna token`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = corpoLogin("admin", "admin@123")
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { exists() }
            jsonPath("$.tipo") { value("Bearer") }
        }
    }

    @Test
    fun `login com senha invalida retorna 401`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = corpoLogin("admin", "errada")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `rota administrativa sem token retorna 401`() {
        mockMvc.get("/api/admin/qualquer").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `rota publica ping e acessivel sem token`() {
        mockMvc.get("/api/ping").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `swagger-ui html e acessivel sem token`() {
        mockMvc.get("/swagger-ui.html").andExpect {
            status { is3xxRedirection() }
        }
    }

    @Test
    fun `rota administrativa com token valido nao retorna 401`() {
        val resposta = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = corpoLogin("admin", "admin@123")
        }.andReturn().response.contentAsString
        val token = objectMapper.readTree(resposta).get("token").asText()

        mockMvc.get("/api/admin/qualquer") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNotFound() }
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
