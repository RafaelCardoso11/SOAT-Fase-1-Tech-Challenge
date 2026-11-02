package com.fiap.soat.oficina.bdd

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
import kotlin.reflect.KClass

class RejeitarOrcamentoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>()
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = RejeitarOrcamentoService(repositorio, eventos, estoque, notificacao)

    private lateinit var ordem: OrdemServico
    private val publicados = mutableListOf<DomainEvent>()

    private fun prepara(status: StatusOS, peca: String?) {
        val orcamento = peca?.let {
            Orcamento(
                listOf(ItemOrcamento(TipoItem.PECA, it, Dinheiro.of(BigDecimal("200.00")), itemEstoqueId = 50L, quantidade = 1)),
                validoAte = Instant.now().plusSeconds(3600),
            )
        }
        ordem = OrdemServico.reconstituir(
            id = 1L,
            documento = Documento.of("123.456.789-09"),
            veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
            relato = "Barulho ao frear",
            status = status,
            transicoes = listOf(TransicaoStatus(status, Instant.now())),
            liberadoParaOrcamento = true,
            orcamento = orcamento,
        )
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(any()) } returns 1L
        every { eventos.publicar(capture(publicados)) } just Runs
    }

    @Dado("uma OS aguardando aprovação com a peça {string} reservada")
    fun comPecaReservada(peca: String) = prepara(StatusOS.AGUARDANDO_APROVACAO, peca)

    @Dado("uma OS aguardando aprovação para rejeição")
    fun aguardandoParaRejeicao() = prepara(StatusOS.AGUARDANDO_APROVACAO, peca = "Pastilha de freio")

    @Dado("uma OS para rejeição no status {string}")
    fun noStatus(status: String) = prepara(statusDe(status), peca = null)

    @Quando("o cliente rejeitar o orçamento com o motivo {string}")
    fun rejeitarComMotivo(motivo: String) {
        service.rejeitar(1L, motivo)
    }

    @Quando("o cliente rejeitar o orçamento sem informar motivo")
    fun rejeitarSemMotivo() {
        service.rejeitar(1L, null)
    }

    @Quando("o cliente tentar rejeitar o orçamento")
    fun tentarRejeitar() {
        runCatching { service.rejeitar(1L, null) }
    }

    @Entao("o motivo {string} deve constar no orçamento")
    fun motivoConsta(motivo: String) {
        assertThat(ordem.orcamento!!.motivoRejeicao).isEqualTo(motivo)
    }

    @Entao("a rejeição deve registrar os eventos {string}, {string} e {string}")
    fun registraTresEventos(e1: String, e2: String, e3: String) {
        listOf(e1, e2, e3).forEach { nome ->
            assertThat(publicados).anyMatch { classeDe(nome).isInstance(it) }
        }
    }

    @Entao("a rejeição deve registrar o evento {string}")
    fun registraEvento(nome: String) {
        assertThat(publicados).anyMatch { classeDe(nome).isInstance(it) }
    }

    @Entao("o status da OS após a rejeição deve ser {string}")
    fun statusDeveSer(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("a reserva da peça {string} deve ser liberada no estoque")
    fun reservaLiberada(peca: String) {
        verify { estoque.liberarReservas(1L) }
    }

    @Entao("a oficina deve ser notificada da rejeição")
    fun oficinaNotificada() {
        verify { notificacao.notificarOrcamentoRejeitado(1L, any()) }
    }

    @Entao("a rejeição não deve ser registrada")
    fun rejeicaoNaoRegistrada() {
        assertThat(ordem.orcamento?.motivoRejeicao).isNull()
        assertThat(publicados).noneMatch { it is OrcamentoRejeitado }
    }

    @Entao("o status da OS após a rejeição deve continuar {string}")
    fun statusContinua(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
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

    private fun classeDe(nome: String): KClass<out DomainEvent> = when (nome.trim().lowercase()) {
        "orçamento rejeitado", "orcamento rejeitado" -> OrcamentoRejeitado::class
        "os encerrada" -> OsEncerrada::class
        "reserva liberada" -> ReservaLiberada::class
        else -> error("evento desconhecido: $nome")
    }
}
