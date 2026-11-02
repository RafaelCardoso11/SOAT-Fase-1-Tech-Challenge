package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.FinalizarExecucaoService
import com.fiap.soat.oficina.ordemservico.domain.event.ExecucaoFinalizada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
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

class FinalizarExecucaoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = FinalizarExecucaoService(repositorio, eventos, notificacao)

    private fun os(status: StatusOS) = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = status,
        transicoes = listOf(TransicaoStatus(status, Instant.now())),
    )

    @Test
    fun `finaliza com tudo aplicado e notifica cliente`() {
        val ordem = os(StatusOS.EM_EXECUCAO)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.finalizar(1L, todosAplicados = true)

        assertThat(ordem.status).isEqualTo(StatusOS.FINALIZADA)
        verify { eventos.publicar(ExecucaoFinalizada(1L)) }
        verify { notificacao.notificarVeiculoPronto(1L) }
    }

    @Test
    fun `bloqueia finalizacao com itens pendentes`() {
        val ordem = os(StatusOS.EM_EXECUCAO)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.finalizar(1L, todosAplicados = false) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(ordem.status).isEqualTo(StatusOS.EM_EXECUCAO)
        verify(exactly = 0) { repositorio.salvar(any()) }
    }

    @Test
    fun `bloqueia finalizacao fora de em execucao`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.finalizar(1L, todosAplicados = true) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(ordem.status).isEqualTo(StatusOS.AGUARDANDO_APROVACAO)
    }

    @Test
    fun `OS inexistente lanca NoSuchElement`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThatThrownBy { service.finalizar(99L, todosAplicados = true) }
            .isInstanceOf(NoSuchElementException::class.java)
    }
}
