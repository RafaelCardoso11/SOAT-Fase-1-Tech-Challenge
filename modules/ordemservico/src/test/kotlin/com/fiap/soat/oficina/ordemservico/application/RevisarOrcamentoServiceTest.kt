package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.RevisarOrcamentoService
import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoRevisado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.model.Orcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.CatalogoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.ItemCatalogo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class RevisarOrcamentoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val catalogo = mockk<CatalogoPort> {
        every { buscarPeca(any()) } returns ItemCatalogo("Peça do estoque", Dinheiro.of(BigDecimal("100.00")))
        every { buscarServico(10L) } returns ItemCatalogo("Troca de óleo", Dinheiro.of(BigDecimal("120.00")))
        every { buscarServico(99L) } returns null
    }
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = RevisarOrcamentoService(repositorio, eventos, estoque, catalogo, notificacao, validadeDias = 7L)

    private fun os(status: StatusOS) = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = status,
        transicoes = listOf(TransicaoStatus(status, Instant.now())),
        liberadoParaOrcamento = true,
        orcamento = Orcamento(
            listOf(ItemOrcamento(TipoItem.SERVICO, "Troca de pastilhas", Dinheiro.of(BigDecimal("150.00")))),
            validoAte = Instant.now().plusSeconds(3600),
        ),
    )

    @Test
    fun `revisa ajustando valor mantem status e reenvia`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.revisar(1L, listOf(ItemSolicitado(TipoItem.SERVICO, "Troca de pastilhas", BigDecimal("120.00"))))

        assertThat(ordem.status).isEqualTo(StatusOS.AGUARDANDO_APROVACAO)
        assertThat(ordem.orcamento!!.total.valor).isEqualByComparingTo("120.00")
        verify { eventos.publicar(OrcamentoRevisado(1L)) }
        verify { notificacao.notificarOrcamentoGerado(1L, any(), any(), any()) }
    }

    @Test
    fun `revisa orcamento so com ids usando os precos do catalogo`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L
        every { estoque.reservar(1L, 50L, 2) } returns true

        service.revisar(
            1L,
            listOf(
                ItemSolicitado(TipoItem.SERVICO, servicoId = 10L),
                ItemSolicitado(TipoItem.PECA, itemEstoqueId = 50L, quantidade = 2),
            ),
        )

        assertThat(ordem.orcamento!!.total.valor).isEqualByComparingTo("320.00")
        assertThat(ordem.orcamento!!.itens.map { it.descricao }).containsExactly("Troca de óleo", "Peça do estoque")
    }

    @Test
    fun `servico inexistente na revisao nao altera a OS`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.revisar(1L, listOf(ItemSolicitado(TipoItem.SERVICO, servicoId = 99L))) }
            .isInstanceOf(NoSuchElementException::class.java)

        assertThat(ordem.orcamento!!.total.valor).isEqualByComparingTo("150.00")
        verify(exactly = 0) { repositorio.salvar(any()) }
        verify(exactly = 0) { estoque.reservar(any(), any(), any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `revisa trocando peca solicita reserva`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L
        every { estoque.reservar(1L, 60L, 1) } returns true

        service.revisar(
            1L,
            listOf(ItemSolicitado(TipoItem.PECA, "Pastilha alternativa", BigDecimal("180.00"), itemEstoqueId = 60L, quantidade = 1)),
        )

        verify { estoque.reservar(1L, 60L, 1) }
    }

    @Test
    fun `libera as reservas anteriores antes de reservar as pecas revisadas`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L
        every { estoque.reservar(1L, 50L, 1) } returns true

        service.revisar(
            1L,
            listOf(ItemSolicitado(TipoItem.PECA, "Pastilha de freio", BigDecimal("200.00"), itemEstoqueId = 50L, quantidade = 1)),
        )

        verifyOrder {
            estoque.liberarReservas(1L)
            estoque.reservar(1L, 50L, 1)
        }
    }

    @Test
    fun `peca indisponivel na revisao encerra a OS e libera as reservas`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L
        every { estoque.reservar(1L, 50L, 1) } returns false

        service.revisar(
            1L,
            listOf(ItemSolicitado(TipoItem.PECA, "Pastilha de freio", BigDecimal("200.00"), itemEstoqueId = 50L, quantidade = 1)),
        )

        assertThat(ordem.status).isEqualTo(StatusOS.FINALIZADA)
        verify { eventos.publicar(OsEncerrada(1L)) }
        verify(exactly = 0) { eventos.publicar(OrcamentoRevisado(1L)) }
    }

    @Test
    fun `bloqueia revisao fora de aguardando aprovacao`() {
        val ordem = os(StatusOS.EM_EXECUCAO)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy {
            service.revisar(1L, listOf(ItemSolicitado(TipoItem.SERVICO, "X", BigDecimal("10.00"))))
        }.isInstanceOf(IllegalStateException::class.java)
        verify(exactly = 0) { repositorio.salvar(any()) }
    }

    @Test
    fun `OS inexistente lanca NoSuchElement`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThatThrownBy {
            service.revisar(99L, listOf(ItemSolicitado(TipoItem.SERVICO, "X", BigDecimal("10.00"))))
        }.isInstanceOf(NoSuchElementException::class.java)
    }
}
