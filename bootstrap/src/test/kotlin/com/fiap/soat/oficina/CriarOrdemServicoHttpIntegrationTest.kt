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
class CriarOrdemServicoHttpIntegrationTest {

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

    private fun corpo(documento: String = "123.456.789-09") = objectMapper.writeValueAsString(
        mapOf(
            "documento" to documento,
            "placa" to "ABC1D23",
            "marca" to "Fiat",
            "modelo" to "Uno",
            "ano" to 2020,
            "relato" to "Barulho ao frear",
        ),
    )

    @Test
    fun `cria OS com dados validos retorna 201`() {
        mockMvc.post("/api/admin/ordens") {
            header("Authorization", "Bearer ${token()}")
            contentType = MediaType.APPLICATION_JSON
            content = corpo()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.status") { value("RECEBIDA") }
        }
    }

    @Test
    fun `cria OS com CPF invalido retorna 400`() {
        mockMvc.post("/api/admin/ordens") {
            header("Authorization", "Bearer ${token()}")
            contentType = MediaType.APPLICATION_JSON
            content = corpo(documento = "111.111.111-11")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.erro") { value("CPF/CNPJ inválido") }
        }
    }

    @Test
    fun `cria OS sem token retorna 401`() {
        mockMvc.post("/api/admin/ordens") {
            contentType = MediaType.APPLICATION_JSON
            content = corpo()
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
