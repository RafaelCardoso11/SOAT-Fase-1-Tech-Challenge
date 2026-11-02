package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.RevisarOrcamentoService
import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoRevisado
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
import com.fiap.soat.oficina.shared.domain.event.DomainEvent
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.cucumber.java.pt.Dado
import io.cucumber.java.pt.Entao
import io.cucumber.java.pt.Quando
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.Instant

class RevisarOrcamentoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>()
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = RevisarOrcamentoService(repositorio, eventos, estoque, notificacao, validadeDias = 7L)

    private lateinit var ordem: OrdemServico
    private val publicados = mutableListOf<DomainEvent>()

    private fun prepara(status: StatusOS) {
        ordem = OrdemServico.reconstituir(
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
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(any()) } returns 1L
        every { eventos.publicar(capture(publicados)) } just Runs
        every { estoque.reservar(any(), any(), any()) } returns true
    }

    @Dado("uma OS aguardando aprovação com orçamento para revisão")
    fun aguardando() = prepara(StatusOS.AGUARDANDO_APROVACAO)

    @Dado("uma OS para revisão no status {string}")
    fun noStatus(status: String) = prepara(statusDe(status))

    @Quando("o mecânico ajustar o orçamento para o valor total {string}")
    fun ajustarValor(total: String) {
        service.revisar(1L, listOf(ItemOrcamento(TipoItem.SERVICO, "Troca de pastilhas", Dinheiro.of(BigDecimal(total)))))
    }

    @Quando("o mecânico revisar o orçamento incluindo a peça {string}")
    fun trocarPeca(peca: String) {
        service.revisar(
            1L,
            listOf(ItemOrcamento(TipoItem.PECA, peca, Dinheiro.of(BigDecimal("180.00")), itemEstoqueId = 50L, quantidade = 1)),
        )
    }

    @Quando("o mecânico tentar revisar o orçamento")
    fun tentarRevisar() {
        runCatching {
            service.revisar(1L, listOf(ItemOrcamento(TipoItem.SERVICO, "Item", Dinheiro.of(BigDecimal("10.00")))))
        }
    }

    @Entao("a revisão deve registrar o evento {string}")
    fun registraEvento(nome: String) {
        assertThat(publicados).anyMatch { it is OrcamentoRevisado }
    }

    @Entao("a nova proposta deve ser reenviada ao cliente na revisão")
    fun reenviada() {
        verify { notificacao.notificarOrcamentoGerado(1L, any()) }
    }

    @Entao("a reserva da peça {string} deve ser solicitada na revisão")
    fun reservaSolicitada(peca: String) {
        verify { estoque.reservar(1L, 50L, 1) }
    }

    @Entao("o status da OS após a revisão deve continuar {string}")
    fun statusContinua(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("a revisão não deve ser registrada na OS")
    fun revisaoNaoRegistrada() {
        assertThat(publicados).noneMatch { it is OrcamentoRevisado }
    }

    private fun statusDe(nome: String): StatusOS = when (nome.trim().lowercase()) {
        "recebida" -> StatusOS.RECEBIDA
        "em diagnóstico", "em diagnostico" -> StatusOS.EM_DIAGNOSTICO
        "aguardando aprovação", "aguardando aprovacao" -> StatusOS.AGUARDANDO_APROVACAO
        "em execução", "em execucao" -> StatusOS.EM_EXECUCAO
        "finalizada" -> StatusOS.FINALIZADA
        "entregue" -> StatusOS.ENTREGUE
        else -> error("status desconhecido: $nome")
    }
}
