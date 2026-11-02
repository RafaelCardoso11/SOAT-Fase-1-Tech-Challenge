package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.ConsultarTempoMedioExecucaoService
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class ConsultarTempoMedioExecucaoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val service = ConsultarTempoMedioExecucaoService(repositorio)

    private fun os(id: Long, iniciada: Instant?, finalizada: Instant?) = OrdemServico.reconstituir(
        id = id,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = StatusOS.FINALIZADA,
        transicoes = listOf(TransicaoStatus(StatusOS.FINALIZADA, Instant.now())),
        execucaoIniciadaEm = iniciada,
        execucaoFinalizadaEm = finalizada,
    )

    @Test
    fun `media entre inicio e fim`() {
        val base = Instant.parse("2026-01-10T08:00:00Z")
        every { repositorio.buscarComExecucaoConcluida() } returns listOf(
            os(1L, base, base.plus(Duration.ofHours(2))),
            os(2L, base, base.plus(Duration.ofHours(4))),
        )

        assertThat(service.consultar()).isEqualTo(Duration.ofHours(3))
    }

    @Test
    fun `considera apenas OS com inicio e fim`() {
        val base = Instant.parse("2026-02-01T08:00:00Z")
        every { repositorio.buscarComExecucaoConcluida() } returns listOf(
            os(1L, base, base.plus(Duration.ofHours(1))),
            os(2L, base, null),
        )

        assertThat(service.consultar()).isEqualTo(Duration.ofHours(1))
    }

    @Test
    fun `sem dados devolve nulo`() {
        every { repositorio.buscarComExecucaoConcluida() } returns emptyList()

        assertThat(service.consultar()).isNull()
    }
}
