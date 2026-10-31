package com.fiap.soat.oficina

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ConsultarOrdensPersistenceIntegrationTest {

    @Autowired
    lateinit var repositorio: OrdemServicoRepository

    private fun novaOs() = OrdemServico.criar(
        Documento.of("123.456.789-09"),
        Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        "Barulho ao frear",
    )

    @Test
    fun `consulta por numero traz a OS com a linha do tempo`() {
        val id = repositorio.salvar(novaOs())

        val encontrada = repositorio.buscarPorId(id)

        assertThat(encontrada).isNotNull
        assertThat(encontrada!!.status).isEqualTo(StatusOS.RECEBIDA)
        assertThat(encontrada.transicoes).singleElement()
            .extracting { it.status }.isEqualTo(StatusOS.RECEBIDA)
    }

    @Test
    fun `consulta por placa e documento traz a lista`() {
        repositorio.salvar(novaOs())

        assertThat(repositorio.buscarPorPlaca("ABC1D23")).isNotEmpty
        assertThat(repositorio.buscarPorDocumento("12345678909")).isNotEmpty
    }

    @Test
    fun `identificador sem OS devolve vazio`() {
        assertThat(repositorio.buscarPorId(999_999L)).isNull()
        assertThat(repositorio.buscarPorPlaca("ZZZ9Z99")).isEmpty()
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
