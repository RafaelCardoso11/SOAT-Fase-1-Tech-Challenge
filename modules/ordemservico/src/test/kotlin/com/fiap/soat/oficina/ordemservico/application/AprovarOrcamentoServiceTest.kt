package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.AprovarOrcamentoService
import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoAprovado
import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.Orcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
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

class AprovarOrcamentoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val service = AprovarOrcamentoService(repositorio, eventos)

    private fun os(status: StatusOS, orcamento: Orcamento?) = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = status,
        transicoes = listOf(TransicaoStatus(status, Instant.now())),
        liberadoParaOrcamento = true,
        orcamento = orcamento,
    )

    private fun orcamento(validoAte: Instant) = Orcamento(
        listOf(ItemOrcamento(TipoItem.SERVICO, "Troca de pastilhas", Dinheiro.of(BigDecimal("150.00")))),
        validoAte = validoAte,
    )

    @Test
    fun `aprova dentro da validade muda para em execucao e registra data`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO, orcamento(Instant.now().plusSeconds(3600)))
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(ordem) } returns 1L

        service.aprovar(1L)

        assertThat(ordem.status).isEqualTo(StatusOS.EM_EXECUCAO)
        assertThat(ordem.orcamento!!.aprovadoEm).isNotNull
        verify { eventos.publicar(OrcamentoAprovado(1L)) }
    }

    @Test
    fun `bloqueia aprovacao de orcamento expirado`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO, orcamento(Instant.now().minusSeconds(3600)))
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.aprovar(1L) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("expirado")

        assertThat(ordem.status).isEqualTo(StatusOS.AGUARDANDO_APROVACAO)
        assertThat(ordem.orcamento!!.aprovadoEm).isNull()
        verify(exactly = 0) { repositorio.salvar(any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `bloqueia aprovacao fora de aguardando aprovacao`() {
        val ordem = os(StatusOS.EM_DIAGNOSTICO, orcamento = null)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.aprovar(1L) }.isInstanceOf(IllegalStateException::class.java)
        assertThat(ordem.status).isEqualTo(StatusOS.EM_DIAGNOSTICO)
    }

    @Test
    fun `OS inexistente lanca NoSuchElement`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThatThrownBy { service.aprovar(99L) }.isInstanceOf(NoSuchElementException::class.java)
    }
}
