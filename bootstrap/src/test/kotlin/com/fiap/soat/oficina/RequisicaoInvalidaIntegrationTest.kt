package com.fiap.soat.oficina

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class RequisicaoInvalidaIntegrationTest {

    @LocalServerPort
    var porta = 0

    private val http = HttpClient.newHttpClient()

    private fun post(caminho: String, corpo: String, token: String? = null): HttpResponse<String> {
        val requisicao = HttpRequest.newBuilder(URI("http://localhost:$porta$caminho"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(corpo))
        token?.let { requisicao.header("Authorization", "Bearer $it") }
        return http.send(requisicao.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun token(): String {
        val resposta = post("/api/auth/login", """{"nomeUsuario":"admin","senha":"admin@123"}""").body()
        return Regex("\"token\":\"([^\"]+)\"").find(resposta)!!.groupValues[1]
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "/api/admin/ordens | {",
            "/api/admin/ordens | {}",
            "/api/admin/ordens/1/orcamento | {}",
            "/api/v1/admin/clientes | {",
            "/api/v1/admin/estoque/itens | {\"nome\":\"x\",\"tipo\":\"PECA\",\"preco\":\"abc\"}",
            "/api/admin/ordens/abc/entrega | {}",
        ],
    )
    fun `requisicao invalida com token valido responde 400 com mensagem`(caminho: String, corpo: String) {
        val resposta = post(caminho, corpo, token())

        assertThat(resposta.statusCode()).isEqualTo(400)
        assertThat(resposta.body()).contains("\"erro\"")
    }

    @Test
    fun `login com corpo invalido responde 400`() {
        assertThat(post("/api/auth/login", "{").statusCode()).isEqualTo(400)
    }

    @Test
    fun `rota protegida sem token continua respondendo 401`() {
        assertThat(post("/api/admin/ordens", "{").statusCode()).isEqualTo(401)
    }

    @Test
    fun `rota inexistente com token responde 404`() {
        assertThat(post("/api/admin/nao-existe", "{}", token()).statusCode()).isEqualTo(404)
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
