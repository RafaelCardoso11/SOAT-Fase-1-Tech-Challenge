package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.RealizarDiagnosticoService
import com.fiap.soat.oficina.ordemservico.domain.event.DiagnosticoFinalizado
import com.fiap.soat.oficina.ordemservico.domain.event.DiagnosticoIniciado
import com.fiap.soat.oficina.ordemservico.domain.event.DiagnosticoRealizado
import com.fiap.soat.oficina.ordemservico.domain.event.OsEncerrada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.event.DomainEvent
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.cucumber.java.pt.Dado
import io.cucumber.java.pt.Entao
import io.cucumber.java.pt.Quando
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant
import kotlin.reflect.KClass

class RealizarDiagnosticoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>()
    private val service = RealizarDiagnosticoService(repositorio, eventos)

    private lateinit var ordem: OrdemServico
    private val publicados = mutableListOf<DomainEvent>()

    @Dado("que existe uma OS com status {string}")
    fun existeOs(status: String) {
        ordem = OrdemServico.reconstituir(
            id = 1L,
            documento = Documento.of("123.456.789-09"),
            veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
            relato = "Barulho ao frear",
            status = statusDe(status),
            transicoes = listOf(TransicaoStatus(statusDe(status), Instant.now())),
        )
        every { repositorio.buscarPorId(1L) } returns ordem
        every { repositorio.salvar(any()) } returns 1L
        every { eventos.publicar(capture(publicados)) } just Runs
    }

    @Dado("o mecânico registrou as observações da inspeção")
    fun registrouObservacoes() = service.registrarInspecao(1L, "Pastilhas gastas")

    @Quando("o mecânico assumir a OS")
    fun assumirOs() = service.iniciar(1L)

    @Quando("o mecânico registrar as observações da inspeção")
    fun registrarObservacoes() = service.registrarInspecao(1L, "Pastilhas gastas")

    @Quando("o mecânico concluir a análise informando serviços e peças necessários")
    fun concluirComPeca() = service.finalizar(1L, pecaIdentificada = true)

    @Quando("o mecânico concluir a inspeção sem identificar a peça necessária")
    fun concluirSemPeca() = service.finalizar(1L, pecaIdentificada = false)

    @Quando("o mecânico tentar iniciar o diagnóstico")
    fun tentarIniciar() {
        runCatching { service.iniciar(1L) }
    }

    @Entao("o status da OS deve ser {string}")
    fun statusDeveSer(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("o status da OS deve continuar {string}")
    fun statusDeveContinuar(status: String) {
        assertThat(ordem.status).isEqualTo(statusDe(status))
    }

    @Entao("deve registrar o evento {string}")
    fun deveRegistrarEvento(nome: String) {
        assertThat(publicados).anyMatch { classeDe(nome).isInstance(it) }
    }

    @Entao("não deve registrar o evento {string}")
    fun naoDeveRegistrarEvento(nome: String) {
        assertThat(publicados).noneMatch { classeDe(nome).isInstance(it) }
    }

    @Entao("a OS deve ficar liberada para geração de orçamento")
    fun deveFicarLiberada() {
        assertThat(ordem.liberadoParaOrcamento).isTrue()
    }

    @Entao("o diagnóstico não deve ser iniciado")
    fun diagnosticoNaoIniciado() {
        assertThat(publicados).noneMatch { it is DiagnosticoIniciado }
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
        "diagnóstico iniciado", "diagnostico iniciado" -> DiagnosticoIniciado::class
        "diagnóstico realizado", "diagnostico realizado" -> DiagnosticoRealizado::class
        "diagnóstico finalizado", "diagnostico finalizado" -> DiagnosticoFinalizado::class
        "os encerrada" -> OsEncerrada::class
        else -> error("evento desconhecido: $nome")
    }
}
