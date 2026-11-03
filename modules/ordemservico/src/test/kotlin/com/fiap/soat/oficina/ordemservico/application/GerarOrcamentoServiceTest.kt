package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.GerarOrcamentoService
import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoGerado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class GerarOrcamentoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val catalogo = mockk<CatalogoPort> {
        every { buscarPeca(any()) } returns ItemCatalogo("Peça do estoque", Dinheiro.of(BigDecimal("100.00")))
        every { buscarServico(10L) } returns ItemCatalogo("Troca de óleo", Dinheiro.of(BigDecimal("120.00")))
        every { buscarServico(99L) } returns null
    }
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = GerarOrcamentoService(repositorio, eventos, estoque, catalogo, notificacao, validadeDias = 7L)

    private fun os(status: StatusOS, liberado: Boolean) = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = status,
        transicoes = listOf(TransicaoStatus(status, Instant.now())),
        liberadoParaOrcamento = liberado,
    )

    private val itens = listOf(
        ItemSolicitado(TipoItem.SERVICO, "Troca de pastilhas", BigDecimal("150.00")),
        ItemSolicitado(TipoItem.PECA, "Pastilha de freio", BigDecimal("200.00"), itemEstoqueId = 50L, quantidade = 2),
    )

    @Test
    fun `gera orcamento com total, muda status e aciona portas`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO, liberado = true)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L
        every { estoque.reservar(1L, 50L, 2) } returns true

        service.gerar(1L, itens)

        assertThat(ordem.status).isEqualTo(StatusOS.AGUARDANDO_APROVACAO)
        assertThat(ordem.orcamento!!.total.valor).isEqualByComparingTo("350.00")
        verify(exactly = 1) { estoque.reservar(1L, 50L, 2) }
        verify { eventos.publicar(OrcamentoGerado(1L)) }
        verify { notificacao.notificarOrcamentoGerado(1L, any()) }
    }

    @Test
    fun `gera orcamento so com ids usando os precos do cadastro e do estoque`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO, liberado = true)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L
        every { estoque.reservar(1L, 50L, 2) } returns true

        service.gerar(
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
    fun `servico inexistente nao altera a OS nem aciona portas`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO, liberado = true)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.gerar(1L, listOf(ItemSolicitado(TipoItem.SERVICO, servicoId = 99L))) }
            .isInstanceOf(NoSuchElementException::class.java)

        assertThat(ordem.status).isEqualTo(StatusOS.EM_DIAGNOSTICO)
        verify(exactly = 0) { repositorio.salvar(any()) }
        verify(exactly = 0) { estoque.reservar(any(), any(), any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `encerra a OS quando peca indisponivel`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO, liberado = true)
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L
        every { estoque.reservar(1L, 50L, 2) } returns false

        service.gerar(1L, itens)

        assertThat(ordem.status).isEqualTo(StatusOS.FINALIZADA)
        verify { estoque.liberarReservas(1L) }
        verify { eventos.publicar(OsEncerrada(1L)) }
        verify(exactly = 0) { eventos.publicar(OrcamentoGerado(1L)) }
    }

    @Test
    fun `bloqueia sem diagnostico finalizado`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO, liberado = false)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.gerar(1L, itens) }.isInstanceOf(IllegalStateException::class.java)
        verify(exactly = 0) { repositorio.salvar(any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `bloqueia fora da etapa de diagnostico`() {
        val ordem = os(StatusOS.RECEBIDA, liberado = false)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.gerar(1L, itens) }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `OS inexistente lanca NoSuchElement`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThatThrownBy { service.gerar(99L, itens) }.isInstanceOf(NoSuchElementException::class.java)
    }
}
