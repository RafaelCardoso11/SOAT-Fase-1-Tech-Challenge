package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.ConsultarTempoMedioExecucaoService
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.cucumber.java.pt.Dado
import io.cucumber.java.pt.Entao
import io.cucumber.java.pt.Quando
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TempoMedioExecucaoSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val service = ConsultarTempoMedioExecucaoService(repositorio)

    private val oss = mutableListOf<OrdemServico>()
    private var resultado: Duration? = null

    init {
        every { repositorio.buscarComExecucaoConcluida() } returns oss
    }

    private fun instante(texto: String): Instant =
        LocalDateTime.parse(texto, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).toInstant(ZoneOffset.UTC)

    private fun os(id: Long, iniciada: Instant?, finalizada: Instant?) = OrdemServico.reconstituir(
        id = id,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = StatusOS.FINALIZADA,
        transicoes = listOf(TransicaoStatus(StatusOS.FINALIZADA, Instant.now())),
        execucaoIniciadaEm = iniciada,
        execucaoFinalizadaEm = finalizada,
    )

    @Dado("que o atendente está autenticado nas APIs administrativas")
    fun autenticado() = Unit

    @Dado("a OS {string} executou de {string} a {string}")
    fun osExecutou(id: String, inicio: String, fim: String) {
        oss.add(os(id.toLong(), instante(inicio), instante(fim)))
    }

    @Dado("a OS {string} iniciou a execução em {string} mas não finalizou")
    fun osSemFim(id: String, inicio: String) {
        oss.add(os(id.toLong(), instante(inicio), null))
    }

    @Dado("que nenhuma OS tem execução iniciada e finalizada")
    fun nenhuma() = oss.clear()

    @Quando("o atendente consultar o tempo médio de execução")
    fun consultar() {
        resultado = service.consultar()
    }

    @Entao("o tempo médio de execução deve ser de {int} minutos")
    fun tempoMedioDeve(minutos: Int) {
        assertThat(resultado?.toMinutes()).isEqualTo(minutos.toLong())
    }

    @Entao("o sistema deve informar que não há dados suficientes")
    fun semDados() {
        assertThat(resultado).isNull()
    }
}
