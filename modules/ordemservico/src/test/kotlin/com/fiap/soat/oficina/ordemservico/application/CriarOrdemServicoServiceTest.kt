package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.CriarOrdemServicoService
import com.fiap.soat.oficina.ordemservico.domain.event.OrdemServicoCriada
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.CriarOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CriarOrdemServicoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val service = CriarOrdemServicoService(repositorio, eventos)

    private fun comando(documento: String = "123.456.789-09", placa: String = "ABC1D23") =
        CriarOrdemServico(documento, placa, "Fiat", "Uno", 2020, "Barulho ao frear")

    @Test
    fun `cria OS recebida e publica evento`() {
        val salva = slot<OrdemServico>()
        every { repositorio.salvar(capture(salva)) } returns 10L

        val id = service.criar(comando())

        assertThat(id).isEqualTo(10L)
        assertThat(salva.captured.status).isEqualTo(StatusOS.RECEBIDA)
        verify { eventos.publicar(any<OrdemServicoCriada>()) }
    }

    @Test
    fun `CPF obrigatorio`() {
        assertThatThrownBy { service.criar(comando(documento = "")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("CPF/CNPJ é obrigatório")
    }

    @Test
    fun `CPF invalido`() {
        assertThatThrownBy { service.criar(comando(documento = "111.111.111-11")) }
            .hasMessage("CPF/CNPJ inválido")
    }

    @Test
    fun `Placa obrigatoria`() {
        assertThatThrownBy { service.criar(comando(placa = "")) }
            .hasMessage("Placa é obrigatória")
    }

    @Test
    fun `Placa invalida`() {
        assertThatThrownBy { service.criar(comando(placa = "12345")) }
            .hasMessage("Placa inválida")
    }
}
