package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.RealizarDiagnosticoService
import com.fiap.soat.oficina.ordemservico.domain.event.DiagnosticoFinalizado
import com.fiap.soat.oficina.ordemservico.domain.event.DiagnosticoIniciado
import com.fiap.soat.oficina.ordemservico.domain.event.DiagnosticoRealizado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class RealizarDiagnosticoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val service = RealizarDiagnosticoService(repositorio, eventos)

    private fun os(status: StatusOS) = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = status,
        transicoes = listOf(TransicaoStatus(status, Instant.now())),
    )

    @Test
    fun `iniciar muda para em diagnostico e publica evento`() {
        val ordem = os(StatusOS.RECEBIDA)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.iniciar(1L)

        assertThat(ordem.status).isEqualTo(StatusOS.EM_DIAGNOSTICO)
        verify { repositorio.salvar(ordem) }
        verify { eventos.publicar(DiagnosticoIniciado(1L)) }
    }

    @Test
    fun `iniciar fora de recebida eh bloqueado e nada eh salvo`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.iniciar(1L) }.isInstanceOf(IllegalStateException::class.java)

        verify(exactly = 0) { repositorio.salvar(any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `registrar inspecao grava observacoes e publica evento`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.registrarInspecao(1L, "Pastilhas gastas")

        assertThat(ordem.observacoes).isEqualTo("Pastilhas gastas")
        verify { eventos.publicar(DiagnosticoRealizado(1L)) }
    }

    @Test
    fun `finalizar com peca libera e mantem status`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.finalizar(1L, pecaIdentificada = true)

        assertThat(ordem.status).isEqualTo(StatusOS.EM_DIAGNOSTICO)
        assertThat(ordem.liberadoParaOrcamento).isTrue()
        verify { eventos.publicar(DiagnosticoFinalizado(1L)) }
    }

    @Test
    fun `finalizar sem peca encerra a OS`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.finalizar(1L, pecaIdentificada = false)

        assertThat(ordem.status).isEqualTo(StatusOS.FINALIZADA)
        verify { eventos.publicar(OsEncerrada(1L)) }
    }

    @Test
    fun `OS inexistente lanca NoSuchElement`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThatThrownBy { service.iniciar(99L) }.isInstanceOf(NoSuchElementException::class.java)
    }
}
