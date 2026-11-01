package com.fiap.soat.oficina

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RealizarDiagnosticoUseCase
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
class RealizarDiagnosticoPersistenceIntegrationTest {

    @Autowired
    lateinit var repositorio: OrdemServicoRepository

    @Autowired
    lateinit var realizarDiagnostico: RealizarDiagnosticoUseCase

    @Test
    fun `iniciar diagnostico atualiza o status e registra a transicao`() {
        val id = repositorio.salvar(
            OrdemServico.criar(
                Documento.of("123.456.789-09"),
                Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
                "Barulho ao frear",
            ),
        )

        realizarDiagnostico.iniciar(id)

        val atualizada = repositorio.buscarPorId(id)
        assertThat(atualizada).isNotNull
        assertThat(atualizada!!.status).isEqualTo(StatusOS.EM_DIAGNOSTICO)
        assertThat(atualizada.transicoes).hasSize(2)
        assertThat(atualizada.transicoes.last().status).isEqualTo(StatusOS.EM_DIAGNOSTICO)
    }

    @Test
    fun `finalizar com peca libera para orcamento sem mudar status`() {
        val id = repositorio.salvar(
            OrdemServico.criar(
                Documento.of("123.456.789-09"),
                Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
                "Barulho ao frear",
            ),
        )
        realizarDiagnostico.iniciar(id)

        realizarDiagnostico.finalizar(id, pecaIdentificada = true)

        val atualizada = repositorio.buscarPorId(id)!!
        assertThat(atualizada.status).isEqualTo(StatusOS.EM_DIAGNOSTICO)
        assertThat(atualizada.liberadoParaOrcamento).isTrue()
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
    }
}
