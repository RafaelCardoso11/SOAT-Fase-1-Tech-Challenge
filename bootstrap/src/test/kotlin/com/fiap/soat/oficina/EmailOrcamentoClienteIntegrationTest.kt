package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(properties = ["notificacao.email.enabled=true", "management.health.mail.enabled=false"])
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class EmailOrcamentoClienteIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var mailSender: JavaMailSender

    private fun post(url: String, token: String, body: String? = null) = mockMvc.post(url) {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        if (body != null) content = body
    }

    @Test
    fun `email do orcamento vai para o cliente cadastrado com o link da requisicao`() {
        val login = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"nomeUsuario":"admin","senha":"admin@123"}"""
        }.andReturn().response.contentAsString
        val token = objectMapper.readTree(login).get("token").asText()
        post(
            "/api/v1/admin/clientes",
            token,
            """{"documento":"123.456.789-09","nome":"Maria Silva","email":"maria@email.com"}""",
        ).andExpect { status { isCreated() } }
        val criada = post(
            "/api/admin/ordens",
            token,
            """{"documento":"123.456.789-09","placa":"ABC1D23","marca":"Fiat","modelo":"Uno","ano":2020,"relato":"Barulho"}""",
        ).andReturn().response.contentAsString
        val id = objectMapper.readTree(criada).get("id").asLong()
        post("/api/admin/ordens/$id/diagnostico/iniciar", token)
        post("/api/admin/ordens/$id/diagnostico/finalizar", token, """{ "pecaIdentificada": true }""")

        post(
            "/api/admin/ordens/$id/orcamento",
            token,
            """{ "itens": [ { "tipo": "serviço", "descricao": "Troca", "valor": 150.00 } ] }""",
        ).andExpect { status { isOk() } }

        val mensagem = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(mailSender).send(mensagem.capture())
        assertThat(mensagem.value.to).containsExactly("maria@email.com")
        assertThat(mensagem.value.text).contains("http://localhost/api/public/orcamentos/")
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
