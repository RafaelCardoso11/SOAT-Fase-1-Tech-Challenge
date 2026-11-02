package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.IniciarExecucaoService
import com.fiap.soat.oficina.ordemservico.domain.event.ExecucaoIniciada
import com.fiap.soat.oficina.ordemservico.domain.event.PecasBaixadas
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
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
import kotlin.reflect.KClass

class IniciarExecucaoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>()
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val service = IniciarExecucaoService(repositorio, eventos, estoque)

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
        )
        every { repositorio.buscarPorId(1L) } returns ordem
        every { eventos.publicar(capture(publicados)) } just Runs
    }

    @Dado("uma OS em execução pronta para iniciar")
    fun emExecucao() = prepara(StatusOS.EM_EXECUCAO)

    @Dado("uma OS para execução no status {string}")
    fun noStatus(status: String) = prepara(statusDe(status))

    @Quando("o mecânico iniciar a execução")
    fun iniciar() {
        service.iniciar(1L)
    }

    @Quando("o mecânico tentar iniciar a execução")
    fun tentarIniciar() {
        runCatching { service.iniciar(1L) }
    }

    @Entao("a execução deve registrar os eventos {string} e {string}")
    fun registraEventos(e1: String, e2: String) {
        listOf(e1, e2).forEach { nome -> assertThat(publicados).anyMatch { classeDe(nome).isInstance(it) } }
    }

    @Entao("o status da OS após iniciar deve continuar {string}")
    fun statusContinua(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("a baixa das peças deve ser feita no estoque")
    fun baixaFeita() {
        verify { estoque.baixarPecas(1L) }
    }

    @Entao("a execução não deve ser iniciada")
    fun execucaoNaoIniciada() {
        assertThat(publicados).noneMatch { it is ExecucaoIniciada }
    }

    @Entao("nenhuma peça deve ser baixada do estoque")
    fun nenhumaBaixa() {
        verify(exactly = 0) { estoque.baixarPecas(any()) }
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
        "execução iniciada", "execucao iniciada" -> ExecucaoIniciada::class
        "peças baixadas", "pecas baixadas" -> PecasBaixadas::class
        else -> error("evento desconhecido: $nome")
    }
}
