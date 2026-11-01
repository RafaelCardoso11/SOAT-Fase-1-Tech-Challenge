package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.RejeitarOrcamentoService
import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoRejeitado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.event.ReservaLiberada
import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.Orcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class RejeitarOrcamentoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = RejeitarOrcamentoService(repositorio, eventos, estoque, notificacao)

    private fun os(status: StatusOS, comPeca: Boolean) = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = status,
        transicoes = listOf(TransicaoStatus(status, Instant.now())),
        liberadoParaOrcamento = true,
        orcamento = if (comPeca) {
            Orcamento(
                listOf(ItemOrcamento(TipoItem.PECA, "Pastilha de freio", Dinheiro.of(BigDecimal("200.00")))),
                validoAte = Instant.now().plusSeconds(3600),
            )
        } else {
            null
        },
    )

    @Test
    fun `rejeita com motivo encerra a OS e libera a reserva`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO, comPeca = true)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.rejeitar(1L, "Valor acima do esperado")

        assertThat(ordem.status).isEqualTo(StatusOS.FINALIZADA)
        assertThat(ordem.orcamento!!.motivoRejeicao).isEqualTo("Valor acima do esperado")
        verify { estoque.liberarReserva("Pastilha de freio") }
        verify { eventos.publicar(OrcamentoRejeitado(1L)) }
        verify { eventos.publicar(OsEncerrada(1L)) }
        verify { eventos.publicar(ReservaLiberada(1L)) }
        verify { notificacao.notificarOrcamentoRejeitado(1L, "Valor acima do esperado") }
    }

    @Test
    fun `rejeita sem motivo encerra a OS`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO, comPeca = true)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.rejeitar(1L, null)

        assertThat(ordem.status).isEqualTo(StatusOS.FINALIZADA)
        verify { eventos.publicar(OrcamentoRejeitado(1L)) }
    }

    @Test
    fun `bloqueia rejeicao fora de aguardando aprovacao`() {
        val ordem = os(StatusOS.EM_EXECUCAO, comPeca = false)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.rejeitar(1L, null) }.isInstanceOf(IllegalStateException::class.java)
        assertThat(ordem.status).isEqualTo(StatusOS.EM_EXECUCAO)
        verify(exactly = 0) { repositorio.salvar(any()) }
    }

    @Test
    fun `OS inexistente lanca NoSuchElement`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThatThrownBy { service.rejeitar(99L, null) }.isInstanceOf(NoSuchElementException::class.java)
    }
}
