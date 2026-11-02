package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.ExpirarOrcamentosService
import com.fiap.soat.oficina.ordemservico.domain.event.OrcamentoExpirado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
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

class ExpirarOrcamentoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>()
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = ExpirarOrcamentosService(repositorio, eventos, estoque, notificacao)

    private lateinit var ordem: OrdemServico
    private val publicados = mutableListOf<DomainEvent>()

    private fun prepara(status: StatusOS, validoAte: Instant, expiravel: Boolean, peca: String = "Pastilha de freio") {
        ordem = OrdemServico.reconstituir(
            id = 1L,
            documento = Documento.of("123.456.789-09"),
            veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
            relato = "Barulho ao frear",
            status = status,
            transicoes = listOf(TransicaoStatus(status, Instant.now())),
            liberadoParaOrcamento = true,
            orcamento = Orcamento(
                listOf(ItemOrcamento(TipoItem.PECA, peca, Dinheiro.of(BigDecimal("200.00")), itemEstoqueId = 50L, quantidade = 1)),
                validoAte = validoAte,
            ),
        )
        every { repositorio.buscarExpiraveis(any()) } returns if (expiravel) listOf(ordem) else emptyList()
        every { repositorio.salvar(any()) } returns 1L
        every { eventos.publicar(capture(publicados)) } just Runs
    }

    @Dado("uma OS aguardando aprovação com orçamento vencido e a peça {string} reservada")
    fun vencidaComPeca(peca: String) =
        prepara(StatusOS.AGUARDANDO_APROVACAO, Instant.now().minusSeconds(3600), expiravel = true, peca = peca)

    @Dado("uma OS aguardando aprovação com orçamento no prazo para expiração")
    fun noPrazo() =
        prepara(StatusOS.AGUARDANDO_APROVACAO, Instant.now().plusSeconds(3600), expiravel = false)

    @Dado("uma OS já em execução com orçamento vencido")
    fun jaEmExecucao() =
        prepara(StatusOS.EM_EXECUCAO, Instant.now().minusSeconds(3600), expiravel = false)

    @Quando("o sistema executar a verificação de validade")
    fun executarVerificacao() {
        service.executar()
    }

    @Entao("a expiração deve registrar os eventos {string} e {string}")
    fun registraEventos(e1: String, e2: String) {
        listOf(e1, e2).forEach { nome -> assertThat(publicados).anyMatch { classeDe(nome).isInstance(it) } }
    }

    @Entao("a expiração não deve registrar o evento {string}")
    fun naoRegistra(nome: String) {
        assertThat(publicados).noneMatch { classeDe(nome).isInstance(it) }
    }

    @Entao("o status da OS após a expiração deve ser {string}")
    fun statusDeveSer(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("o status da OS após a expiração deve continuar {string}")
    fun statusContinua(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("a reserva da peça {string} deve ser liberada na expiração")
    fun reservaLiberada(peca: String) {
        verify { estoque.liberarReservas(1L) }
    }

    @Entao("o atendente deve ser notificado da expiração")
    fun atendenteNotificado() {
        verify { notificacao.notificarOrcamentoExpirado(1L) }
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
        "orçamento expirado", "orcamento expirado" -> OrcamentoExpirado::class
        "os encerrada" -> OsEncerrada::class
        else -> error("evento desconhecido: $nome")
    }
}
