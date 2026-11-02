package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.FinalizarExecucaoService
import com.fiap.soat.oficina.ordemservico.domain.event.ExecucaoFinalizada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.NotificacaoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.event.DomainEvent
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
import java.time.Instant

class FinalizarExecucaoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>()
    private val notificacao = mockk<NotificacaoPort>(relaxed = true)
    private val service = FinalizarExecucaoService(repositorio, eventos, notificacao)

    private lateinit var ordem: OrdemServico
    private val publicados = mutableListOf<DomainEvent>()
    private var todosAplicados = true
    private var erro: Throwable? = null

    private fun prepara(status: StatusOS, aplicados: Boolean) {
        todosAplicados = aplicados
        ordem = OrdemServico.reconstituir(
            id = 1L,
            documento = Documento.of("123.456.789-09"),
            veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
            relato = "Barulho ao frear",
            status = status,
            transicoes = listOf(TransicaoStatus(status, Instant.now())),
        )
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(any()) } returns 1L
        every { eventos.publicar(capture(publicados)) } just Runs
    }

    @Dado("uma OS em execução com todos os itens aplicados")
    fun comTudoAplicado() = prepara(StatusOS.EM_EXECUCAO, aplicados = true)

    @Dado("uma OS em execução com itens pendentes")
    fun comPendentes() = prepara(StatusOS.EM_EXECUCAO, aplicados = false)

    @Dado("uma OS para finalizar no status {string}")
    fun noStatus(status: String) = prepara(statusDe(status), aplicados = true)

    @Quando("o mecânico registrar a conclusão")
    fun registrar() {
        service.finalizar(1L, todosAplicados)
    }

    @Quando("o mecânico tentar registrar a conclusão")
    fun tentarRegistrar() {
        erro = runCatching { service.finalizar(1L, todosAplicados) }.exceptionOrNull()
    }

    @Entao("a finalização deve registrar o evento {string}")
    fun registraEvento(nome: String) {
        assertThat(publicados).anyMatch { it is ExecucaoFinalizada }
    }

    @Entao("o status da OS após finalizar deve ser {string}")
    fun statusDeveSer(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("o status da OS após finalizar deve continuar {string}")
    fun statusContinua(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("o cliente deve ser notificado de que o veículo está pronto")
    fun clienteNotificado() {
        verify { notificacao.notificarVeiculoPronto(1L) }
    }

    @Entao("o mecânico deve ser informado de que há itens pendentes")
    fun informadoPendentes() {
        assertThat(erro).isNotNull
        assertThat(erro!!.message).contains("pendentes")
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
