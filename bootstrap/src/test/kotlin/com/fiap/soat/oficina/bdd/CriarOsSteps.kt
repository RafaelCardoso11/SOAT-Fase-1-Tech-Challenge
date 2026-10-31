package com.fiap.soat.oficina.bdd

import com.fiap.soat.oficina.ordemservico.application.usecase.CriarOrdemServicoService
import com.fiap.soat.oficina.ordemservico.domain.event.OrdemServicoCriada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.CriarOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import io.cucumber.java.pt.Dado
import io.cucumber.java.pt.Entao
import io.cucumber.java.pt.Quando
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat

class CriarOsSteps {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val service = CriarOrdemServicoService(repositorio, eventos)
    private val osSalva = slot<OrdemServico>()

    private var documento = ""
    private var placa = ""
    private var marca = ""
    private var modelo = ""
    private var ano = 0
    private var relato = ""
    private var idCriado: Long? = null
    private var erro: String? = null

    @Dado("que o atendente informou o CPF {string} do cliente")
    fun informouCpf(cpf: String) {
        documento = cpf
    }

    @Dado("que o atendente informou o documento {string} do cliente")
    fun informouDocumento(doc: String) {
        documento = doc
    }

    @Dado("informou o veículo de placa {string}, marca {string}, modelo {string} e ano {int}")
    fun informouVeiculoCompleto(placa: String, marca: String, modelo: String, ano: Int) {
        this.placa = placa
        this.marca = marca
        this.modelo = modelo
        this.ano = ano
    }

    @Dado("informou o veículo de placa {string}")
    fun informouVeiculoPlaca(placa: String) {
        this.placa = placa
    }

    @Dado("registrou o relato {string}")
    fun registrouRelato(relato: String) {
        this.relato = relato
    }

    @Quando("o atendente solicitar a criação da OS")
    fun solicitarCriacao() {
        every { repositorio.salvar(capture(osSalva)) } returns 1L
        try {
            idCriado = service.criar(CriarOrdemServico(documento, placa, marca, modelo, ano, relato))
        } catch (e: IllegalArgumentException) {
            erro = e.message
        }
    }

    @Entao("a OS deve ser criada com status {string}")
    fun osCriadaComStatus(status: String) {
        assertThat(idCriado).isNotNull()
        assertThat(osSalva.captured.status.name).isEqualTo(status.uppercase())
    }

    @Entao("o evento {string} deve ser registrado")
    fun eventoRegistrado(evento: String) {
        verify { eventos.publicar(any<OrdemServicoCriada>()) }
    }

    @Entao("o atendente deve receber a confirmação da criação")
    fun confirmacaoRecebida() {
        assertThat(idCriado).isNotNull()
    }

    @Entao("a OS não deve ser criada")
    fun osNaoCriada() {
        assertThat(idCriado).isNull()
    }

    @Entao("o sistema deve exibir a mensagem {string}")
    fun exibeMensagem(mensagem: String) {
        assertThat(erro).isEqualTo(mensagem)
    }
}
