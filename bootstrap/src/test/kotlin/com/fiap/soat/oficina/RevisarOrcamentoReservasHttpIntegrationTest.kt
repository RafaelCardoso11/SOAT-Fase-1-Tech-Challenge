package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.JsonNode
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
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RevisarOrcamentoReservasHttpIntegrationTest {

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

    private fun criarPeca(preco: String): Long = idDe(
        post(
            "/api/v1/admin/estoque/itens",
            """{ "nome": "Peca ${UUID.randomUUID().toString().take(8)}", "tipo": "PECA", "preco": $preco, "saldoInicial": 10 }""",
        ),
    )

    private fun item(id: Long): JsonNode {
        val corpo = mockMvc.get("/api/v1/admin/estoque/itens/$id") {
            header("Authorization", "Bearer $token")
        }.andReturn().response.contentAsString
        return objectMapper.readTree(corpo)
    }

    private fun reservado(id: Long) = item(id).get("reservado").asInt()

    private fun saldo(id: Long) = item(id).get("saldo").asInt()

    private fun osComOrcamento(peca: Long, quantidade: Int): Long {
        val corpo = """
            { "documento": "123.456.789-09", "placa": "ABC1D23", "marca": "Fiat", "modelo": "Uno",
              "ano": 2020, "relato": "Barulho ao frear" }
        """.trimIndent()
        val os = idDe(post("/api/admin/ordens", corpo))
        post("/api/admin/ordens/$os/diagnostico/iniciar")
        post("/api/admin/ordens/$os/diagnostico/finalizar", """{ "pecaIdentificada": true }""")
        post("/api/admin/ordens/$os/orcamento", orcamento(100.00, peca, quantidade)).andExpect { status { isOk() } }
        return os
    }

    private fun orcamento(maoDeObra: Double, peca: Long, quantidade: Int) = """
        { "itens": [
            { "tipo": "servico", "descricao": "Mao de obra", "valor": $maoDeObra },
            { "tipo": "peca", "itemEstoqueId": $peca, "quantidade": $quantidade }
        ] }
    """.trimIndent()

    @Test
    fun `revisar mantendo a mesma peca responde 200 e reserva a quantidade revisada`() {
        val peca = criarPeca("200.00")
        val os = osComOrcamento(peca, quantidade = 1)

        post("/api/admin/ordens/$os/orcamento/revisar", orcamento(80.00, peca, quantidade = 2)).andExpect {
            status { isOk() }
            jsonPath("$.status") { value("AGUARDANDO_APROVACAO") }
            jsonPath("$.total") { value(480.00) }
        }

        assertThat(reservado(peca)).isEqualTo(2)
    }

    @Test
    fun `revisar trocando a peca libera a reserva da peca antiga`() {
        val original = criarPeca("200.00")
        val alternativa = criarPeca("250.00")
        val os = osComOrcamento(original, quantidade = 1)

        post("/api/admin/ordens/$os/orcamento/revisar", orcamento(100.00, alternativa, quantidade = 1))
            .andExpect { status { isOk() } }

        assertThat(reservado(original)).isEqualTo(0)
        assertThat(reservado(alternativa)).isEqualTo(1)
    }

    @Test
    fun `iniciar a execucao depois da revisao baixa so as pecas do orcamento revisado`() {
        val original = criarPeca("200.00")
        val alternativa = criarPeca("250.00")
        val os = osComOrcamento(original, quantidade = 1)
        post("/api/admin/ordens/$os/orcamento/revisar", orcamento(100.00, alternativa, quantidade = 1))
        post("/api/admin/ordens/$os/orcamento/aprovar")

        post("/api/admin/ordens/$os/execucao/iniciar").andExpect { status { isOk() } }

        assertThat(saldo(original)).isEqualTo(10)
        assertThat(reservado(original)).isEqualTo(0)
        assertThat(saldo(alternativa)).isEqualTo(9)
        assertThat(reservado(alternativa)).isEqualTo(0)
    }

    @Test
    fun `peca indisponivel na revisao encerra a OS e nao deixa reserva presa`() {
        val original = criarPeca("200.00")
        val os = osComOrcamento(original, quantidade = 1)

        post("/api/admin/ordens/$os/orcamento/revisar", orcamento(100.00, original, quantidade = 999))
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.status") { value("FINALIZADA") } }

        assertThat(reservado(original)).isEqualTo(0)
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
