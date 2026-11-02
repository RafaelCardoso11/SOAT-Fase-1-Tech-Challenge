package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.ExpirarOrcamentosService
import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoExpirado
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
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class ExpirarOrcamentosServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = ExpirarOrcamentosService(repositorio, eventos, estoque, notificacao)

    private fun vencida() = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = StatusOS.AGUARDANDO_APROVACAO,
        transicoes = listOf(TransicaoStatus(StatusOS.AGUARDANDO_APROVACAO, Instant.now())),
        liberadoParaOrcamento = true,
        orcamento = Orcamento(
            listOf(ItemOrcamento(TipoItem.PECA, "Pastilha de freio", Dinheiro.of(BigDecimal("200.00")), itemEstoqueId = 50L, quantidade = 1)),
            validoAte = Instant.now().minusSeconds(3600),
        ),
    )

    @Test
    fun `expira as vencidas encerrando e liberando reserva`() {
        val ordem = vencida()
        every { repositorio.buscarExpiraveis(any()) } returns listOf(ordem)
        every { repositorio.salvar(ordem) } returns 1L

        val expirados = service.executar()

        assertThat(expirados).isEqualTo(1)
        assertThat(ordem.status).isEqualTo(StatusOS.FINALIZADA)
        verify { estoque.liberarReservas(1L) }
        verify { eventos.publicar(OrcamentoExpirado(1L)) }
        verify { eventos.publicar(OsEncerrada(1L)) }
        verify { eventos.publicar(ReservaLiberada(1L)) }
        verify { notificacao.notificarOrcamentoExpirado(1L) }
    }

    @Test
    fun `sem vencidas nao faz nada`() {
        every { repositorio.buscarExpiraveis(any()) } returns emptyList()

        assertThat(service.executar()).isEqualTo(0)
        verify(exactly = 0) { repositorio.salvar(any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }
}
