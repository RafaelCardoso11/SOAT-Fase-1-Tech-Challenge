package com.fiap.soat.oficina

import com.fasterxml.jackson.databind.ObjectMapper
import com.fiap.soat.oficina.cadastro.domain.error.DuplicidadeException
import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import com.fiap.soat.oficina.cadastro.domain.model.Servico
import com.fiap.soat.oficina.cadastro.domain.model.Veiculo
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ClienteRepository
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ServicoRepository
import com.fiap.soat.oficina.cadastro.domain.port.outbound.VeiculoRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CadastroIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var clientes: ClienteRepository

    @Autowired
    lateinit var veiculos: VeiculoRepository

    @Autowired
    lateinit var servicos: ServicoRepository

    private val token: String by lazy {
        val resposta = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json("nomeUsuario" to "admin", "senha" to "admin@123")
        }.andReturn().response.contentAsString
        objectMapper.readTree(resposta).get("token").asText()
    }

    private fun json(vararg campos: Pair<String, Any?>): String = objectMapper.writeValueAsString(mapOf(*campos))

    private fun MockHttpServletRequestDsl.autenticado(corpo: String?) {
        header("Authorization", "Bearer $token")
        if (corpo != null) {
            contentType = MediaType.APPLICATION_JSON
            content = corpo
        }
    }

    private fun enviar(metodo: String, caminho: String, corpo: String? = null): ResultActionsDsl =
        when (metodo) {
            "POST" -> mockMvc.post(caminho) { autenticado(corpo) }
            "PUT" -> mockMvc.put(caminho) { autenticado(corpo) }
            else -> mockMvc.get(caminho) { autenticado(corpo) }
        }

    private fun idDe(resultado: ResultActionsDsl): Long =
        objectMapper.readTree(resultado.andReturn().response.contentAsString).get("id").asLong()

    private fun MockMvcResultMatchersDsl.erro(campo: String?, mensagem: String) {
        jsonPath("$.erro") { value(mensagem) }
        if (campo != null) jsonPath("$.campo") { value(campo) }
    }

    private fun criarCliente(
        documento: String = cpf(),
        nome: String = "Maria Silva",
        email: String = "maria@email.com",
    ): Long = idDe(
        enviar("POST", "/api/v1/admin/clientes", json("documento" to documento, "nome" to nome, "email" to email))
            .andExpect { status { isCreated() } },
    )

    private fun criarVeiculo(clienteId: Long, placa: String = placa()): Long = idDe(
        enviar(
            "POST",
            "/api/v1/admin/veiculos",
            json("placa" to placa, "marca" to "Fiat", "modelo" to "Uno", "ano" to 2020, "clienteId" to clienteId),
        ).andExpect { status { isCreated() } },
    )

    private fun criarServico(nome: String = nomeUnico(), preco: Any = 150): Long = idDe(
        enviar("POST", "/api/v1/admin/servicos", json("nome" to nome, "descricao" to "Descrição", "preco" to preco))
            .andExpect { status { isCreated() } },
    )

    @Test
    fun `rotas de cadastro sem token retornam 401`() {
        listOf("clientes", "veiculos", "servicos").forEach {
            mockMvc.get("/api/v1/admin/$it").andExpect { status { isUnauthorized() } }
        }
    }

    @Test
    fun `fluxo completo de cliente`() {
        val documento = cpf()
        val id = idDe(
            enviar(
                "POST",
                "/api/v1/admin/clientes",
                json("documento" to mascarar(documento), "nome" to " Maria Silva ", "email" to "Maria@Email.com"),
            ).andExpect {
                status { isCreated() }
                header { exists("Location") }
                jsonPath("$.documento") { value(documento) }
                jsonPath("$.nome") { value("Maria Silva") }
                jsonPath("$.email") { value("maria@email.com") }
                jsonPath("$.ativo") { value(true) }
            },
        )

        enviar("GET", "/api/v1/admin/clientes/$id").andExpect { status { isOk() }; jsonPath("$.id") { value(id) } }
        enviar("GET", "/api/v1/admin/clientes/documento/${mascarar(documento)}")
            .andExpect { status { isOk() }; jsonPath("$.id") { value(id) } }
        enviar("PUT", "/api/v1/admin/clientes/$id", json("nome" to "Maria Souza", "email" to "souza@email.com"))
            .andExpect { status { isOk() }; jsonPath("$.nome") { value("Maria Souza") } }
        enviar("POST", "/api/v1/admin/clientes/$id/inativar").andExpect { status { isNoContent() } }
        enviar("POST", "/api/v1/admin/clientes/$id/inativar").andExpect { status { isNoContent() } }

        enviar("GET", "/api/v1/admin/clientes/$id").andExpect { jsonPath("$.ativo") { value(false) } }
        enviar("GET", "/api/v1/admin/clientes/$id?ativo=true").andExpect { status { isNotFound() } }
        enviar("GET", "/api/v1/admin/clientes/documento/$documento?ativo=true").andExpect { status { isNotFound() } }
        enviar("GET", "/api/v1/admin/clientes?ativo=false")
            .andExpect { status { isOk() }; jsonPath("$[?(@.id==$id)]") { isNotEmpty() } }
        enviar("GET", "/api/v1/admin/clientes?ativo=true")
            .andExpect { status { isOk() }; jsonPath("$[?(@.id==$id)]") { isEmpty() } }
    }

    @Test
    fun `cliente invalido retorna 400 com campo e mensagem`() {
        enviar("POST", "/api/v1/admin/clientes", json("documento" to "111.111.111-11", "nome" to "A", "email" to "a@b.co"))
            .andExpect { status { isBadRequest() }; erro("documento", "CPF/CNPJ inválido") }
        enviar("POST", "/api/v1/admin/clientes", "{}")
            .andExpect { status { isBadRequest() }; erro("documento", "CPF/CNPJ é obrigatório") }
        enviar("POST", "/api/v1/admin/clientes", json("documento" to cpf(), "nome" to "A", "email" to "invalido"))
            .andExpect { status { isBadRequest() }; erro("email", "E-mail inválido") }
    }

    @Test
    fun `cliente inexistente retorna 404`() {
        enviar("GET", "/api/v1/admin/clientes/999999")
            .andExpect { status { isNotFound() }; erro(null, "Cliente não encontrado") }
        enviar("PUT", "/api/v1/admin/clientes/999999", json("nome" to "A", "email" to "a@b.co"))
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `documento duplicado retorna 409 mesmo com o cliente inativo`() {
        val documento = cpf()
        val id = criarCliente(documento)

        enviar("POST", "/api/v1/admin/clientes", json("documento" to documento, "nome" to "Outra", "email" to "o@e.co"))
            .andExpect { status { isConflict() }; erro("documento", "Já existe um cliente com este CPF/CNPJ") }

        enviar("POST", "/api/v1/admin/clientes/$id/inativar").andExpect { status { isNoContent() } }
        enviar("POST", "/api/v1/admin/clientes", json("documento" to documento, "nome" to "Outra", "email" to "o@e.co"))
            .andExpect { status { isConflict() } }
    }

    @Test
    fun `fluxo completo de veiculo com troca de dono`() {
        val dono = criarCliente()
        val novoDono = criarCliente()
        val placa = placa()
        val id = idDe(
            enviar(
                "POST",
                "/api/v1/admin/veiculos",
                json("placa" to placa.lowercase(), "marca" to "Fiat", "modelo" to "Uno", "ano" to 2020, "clienteId" to dono),
            ).andExpect {
                status { isCreated() }
                header { exists("Location") }
                jsonPath("$.placa") { value(placa) }
                jsonPath("$.clienteId") { value(dono) }
            },
        )

        enviar("GET", "/api/v1/admin/veiculos/placa/${placa.lowercase()}")
            .andExpect { status { isOk() }; jsonPath("$.id") { value(id) } }
        enviar("PUT", "/api/v1/admin/veiculos/$id", json("marca" to "VW", "modelo" to "Gol", "ano" to 2021, "clienteId" to novoDono))
            .andExpect {
                status { isOk() }
                jsonPath("$.marca") { value("VW") }
                jsonPath("$.clienteId") { value(novoDono) }
                jsonPath("$.placa") { value(placa) }
            }
        enviar("GET", "/api/v1/admin/veiculos?clienteId=$novoDono")
            .andExpect { status { isOk() }; jsonPath("$[?(@.id==$id)]") { isNotEmpty() } }
        enviar("GET", "/api/v1/admin/veiculos?clienteId=$dono").andExpect { jsonPath("$") { isEmpty() } }

        enviar("POST", "/api/v1/admin/veiculos/$id/inativar").andExpect { status { isNoContent() } }
        enviar("GET", "/api/v1/admin/veiculos/$id?ativo=true").andExpect { status { isNotFound() } }
        enviar("GET", "/api/v1/admin/veiculos?clienteId=$novoDono&ativo=true").andExpect { jsonPath("$") { isEmpty() } }
        enviar("PUT", "/api/v1/admin/veiculos/$id", json("marca" to "VW", "modelo" to "Gol", "ano" to 2021, "clienteId" to novoDono))
            .andExpect { status { isConflict() } }
    }

    @Test
    fun `veiculo com dado invalido, cliente inexistente ou inativo e placa duplicada`() {
        val cliente = criarCliente()
        val placa = placa()
        criarVeiculo(cliente, placa)

        fun cadastro(placa: String, ano: Int, clienteId: Long) =
            enviar(
                "POST",
                "/api/v1/admin/veiculos",
                json("placa" to placa, "marca" to "Fiat", "modelo" to "Uno", "ano" to ano, "clienteId" to clienteId),
            )

        cadastro(placa, 2020, cliente)
            .andExpect { status { isConflict() }; erro("placa", "Já existe um veículo com esta placa") }
        cadastro("12345", 2020, cliente)
            .andExpect { status { isBadRequest() }; erro("placa", "Placa inválida") }
        cadastro(placa(), 1899, cliente).andExpect { status { isBadRequest() }; jsonPath("$.campo") { value("ano") } }
        cadastro(placa(), 2020, 999999)
            .andExpect { status { isNotFound() }; erro(null, "Cliente não encontrado") }

        val inativo = criarCliente()
        enviar("POST", "/api/v1/admin/clientes/$inativo/inativar").andExpect { status { isNoContent() } }
        cadastro(placa(), 2020, inativo).andExpect { status { isConflict() } }
    }

    @Test
    fun `nao inativa cliente com veiculo ativo`() {
        val cliente = criarCliente()
        val veiculo = criarVeiculo(cliente)

        enviar("POST", "/api/v1/admin/clientes/$cliente/inativar").andExpect {
            status { isConflict() }
            erro(null, "Cliente possui veículos ativos e não pode ser inativado")
        }

        enviar("POST", "/api/v1/admin/veiculos/$veiculo/inativar").andExpect { status { isNoContent() } }
        enviar("POST", "/api/v1/admin/clientes/$cliente/inativar").andExpect { status { isNoContent() } }
    }

    @Test
    fun `fluxo completo de servico`() {
        val nome = nomeUnico()
        val id = idDe(
            enviar("POST", "/api/v1/admin/servicos", json("nome" to nome, "descricao" to "Óleo e filtro", "preco" to 150))
                .andExpect {
                    status { isCreated() }
                    header { exists("Location") }
                    jsonPath("$.preco") { value(150.0) }
                    jsonPath("$.ativo") { value(true) }
                },
        )

        enviar("GET", "/api/v1/admin/servicos/$id").andExpect { status { isOk() }; jsonPath("$.nome") { value(nome) } }
        enviar("PUT", "/api/v1/admin/servicos/$id", json("nome" to nome, "descricao" to null, "preco" to 99.9))
            .andExpect { status { isOk() }; jsonPath("$.preco") { value(99.9) } }
        enviar("POST", "/api/v1/admin/servicos/$id/inativar").andExpect { status { isNoContent() } }
        enviar("GET", "/api/v1/admin/servicos/$id?ativo=true").andExpect { status { isNotFound() } }
        enviar("GET", "/api/v1/admin/servicos?ativo=false")
            .andExpect { status { isOk() }; jsonPath("$[?(@.id==$id)]") { isNotEmpty() } }
        enviar("PUT", "/api/v1/admin/servicos/$id", json("nome" to nome, "preco" to 10))
            .andExpect { status { isConflict() } }
    }

    @Test
    fun `servico com nome duplicado, preco negativo ou renomeado para nome existente`() {
        val nome = nomeUnico()
        criarServico(nome)
        val outro = criarServico()

        enviar("POST", "/api/v1/admin/servicos", json("nome" to nome.uppercase(), "preco" to 10))
            .andExpect { status { isConflict() }; erro("nome", "Já existe um serviço com este nome") }
        enviar("PUT", "/api/v1/admin/servicos/$outro", json("nome" to nome.lowercase(), "preco" to 10))
            .andExpect { status { isConflict() }; jsonPath("$.campo") { value("nome") } }
        enviar("POST", "/api/v1/admin/servicos", json("nome" to nomeUnico(), "preco" to -1))
            .andExpect { status { isBadRequest() }; erro("preco", "Preço não pode ser negativo") }
        enviar("POST", "/api/v1/admin/servicos", json("nome" to nomeUnico()))
            .andExpect { status { isBadRequest() }; erro("preco", "Preço é obrigatório") }
    }

    @Test
    fun `o banco garante a unicidade de documento, placa e nome de servico`() {
        val documento = cpf()
        val cliente = clientes.salvar(Cliente.criar(documento, "Maria", "maria@email.com"))
        assertDuplicidade("documento") { clientes.salvar(Cliente.criar(documento, "Outra", "outra@email.com")) }

        val placa = placa()
        veiculos.salvar(Veiculo.criar(placa, "Fiat", "Uno", 2020, requireNotNull(cliente.id), 2100))
        assertDuplicidade("placa") {
            veiculos.salvar(Veiculo.criar(placa, "VW", "Gol", 2021, requireNotNull(cliente.id), 2100))
        }

        val nome = nomeUnico()
        servicos.salvar(Servico.criar(nome, null, BigDecimal.TEN))
        assertDuplicidade("nome") { servicos.salvar(Servico.criar(nome.uppercase(), null, BigDecimal.TEN)) }
    }

    private fun assertDuplicidade(campo: String, bloco: () -> Unit) {
        assertThatThrownBy { bloco() }.isInstanceOfSatisfying(DuplicidadeException::class.java) {
            assertThat(it.campo).isEqualTo(campo)
        }
    }

    @Test
    fun `persistencia preserva os dados e a listagem respeita os filtros`() {
        val cliente = clientes.salvar(Cliente.criar(cpf(), "Maria", "maria@email.com"))

        val lido = clientes.buscarPorId(requireNotNull(cliente.id))
        assertThat(lido?.nome).isEqualTo("Maria")
        assertThat(clientes.listar(true).map { it.id }).contains(cliente.id)
        assertThat(clientes.listar(false).map { it.id }).doesNotContain(cliente.id)
    }

    @Test
    fun `documentacao openapi publica as rotas de cadastro`() {
        val resposta = mockMvc.get("/v3/api-docs").andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        val paths = objectMapper.readTree(resposta).get("paths").fieldNames().asSequence().toList()

        assertThat(paths).contains(
            "/api/v1/admin/clientes",
            "/api/v1/admin/clientes/{id}",
            "/api/v1/admin/clientes/documento/{documento}",
            "/api/v1/admin/veiculos/placa/{placa}",
            "/api/v1/admin/servicos/{id}/inativar",
        )
    }

    private fun mascarar(cpf: String) = "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9)}"

    companion object {
        private val sequencia = AtomicInteger(1000)

        private fun proximo(): Int = sequencia.incrementAndGet()

        private fun cpf(): String {
            val base = proximo().toString().padStart(9, '1')
            fun dv(digitos: String, peso: Int): Int {
                val resto = digitos.mapIndexed { i, c -> (c - '0') * (peso - i) }.sum() % 11
                return if (resto < 2) 0 else 11 - resto
            }
            val d1 = dv(base, 10)
            val d2 = dv(base + d1, 11)
            return "$base$d1$d2"
        }

        private fun placa(): String {
            val n = proximo()
            return "ABC${n / 100 % 10}D${(n % 100).toString().padStart(2, '0')}"
        }

        private fun nomeUnico(): String = "Servico ${proximo()}"

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
