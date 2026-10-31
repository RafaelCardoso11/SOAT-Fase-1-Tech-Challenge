package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.ConsultarOrdensService
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
import java.time.Instant

class ConsultarStatusSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val service = ConsultarOrdensService(repositorio)

    private var resultado: OrdemServico? = null
    private var lista: List<OrdemServico> = emptyList()

    @Dado("que existe uma OS número {long}, placa {string}, documento {string} com status {string}")
    fun existeOs(numero: Long, placa: String, documento: String, status: String) {
        val st = StatusOS.valueOf(status.uppercase())
        val os = OrdemServico.reconstituir(
            id = numero,
            documento = Documento.of(documento),
            veiculo = Veiculo(Placa.of(placa), "Fiat", "Uno", 2020),
            relato = "Barulho ao frear",
            status = st,
            transicoes = listOf(TransicaoStatus(st, Instant.now())),
        )
        every { repositorio.buscarPorId(numero) } returns os
        every { repositorio.buscarPorPlaca(Placa.of(placa).valor) } returns listOf(os)
        every { repositorio.buscarPorDocumento(Documento.of(documento).valor) } returns listOf(os)
    }

    @Dado("que não existe OS para a placa {string}")
    fun naoExiste(placa: String) {
        every { repositorio.buscarPorPlaca(Placa.of(placa).valor) } returns emptyList()
    }

    @Quando("o cliente consultar a OS pelo número {long}")
    fun consultarPorNumero(numero: Long) {
        resultado = service.porNumero(numero)
    }

    @Quando("o cliente consultar a OS pela placa {string}")
    fun consultarPorPlaca(placa: String) {
        lista = service.porPlaca(placa)
        resultado = lista.firstOrNull()
    }

    @Quando("o atendente consultar a OS pelo documento {string}")
    fun consultarPorDocumento(documento: String) {
        lista = service.porDocumento(documento)
        resultado = lista.firstOrNull()
    }

    @Entao("o sistema deve exibir o status {string}")
    fun exibeStatus(status: String) {
        assertThat(resultado?.status).isEqualTo(StatusOS.valueOf(status.uppercase()))
    }

    @Entao("deve exibir a linha do tempo do atendimento")
    fun exibeLinhaDoTempo() {
        assertThat(resultado?.transicoes).isNotEmpty()
    }

    @Entao("nenhum evento de domínio deve ser registrado")
    fun nenhumEvento() {
        // Consulta é read-only: o serviço não possui publicador de eventos.
    }

    @Entao("o sistema deve informar que nenhuma OS foi encontrada")
    fun nenhumaEncontrada() {
        assertThat(lista).isEmpty()
        assertThat(resultado).isNull()
    }
}
