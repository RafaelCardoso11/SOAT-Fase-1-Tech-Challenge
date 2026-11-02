package com.fiap.soat.oficina.ordemservico.application

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
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class IniciarExecucaoServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val eventos = mockk<PublicadorDeEventos>(relaxed = true)
    private val estoque = mockk<EstoquePort>(relaxed = true)
    private val service = IniciarExecucaoService(repositorio, eventos, estoque)

    private fun os(status: StatusOS) = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = status,
        transicoes = listOf(TransicaoStatus(status, Instant.now())),
    )

    @Test
    fun `inicia execucao baixa pecas e publica eventos`() {
        val ordem = os(StatusOS.EM_EXECUCAO)
        every { repositorio.buscarPorId(1L) } returns ordem

        service.iniciar(1L)

        assertThat(ordem.status).isEqualTo(StatusOS.EM_EXECUCAO)
        verify { estoque.baixarPecas(1L) }
        verify { eventos.publicar(ExecucaoIniciada(1L)) }
        verify { eventos.publicar(PecasBaixadas(1L)) }
    }

    @Test
    fun `bloqueia execucao sem orcamento aprovado e nao baixa`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO)
        every { repositorio.buscarPorId(1L) } returns ordem

        assertThatThrownBy { service.iniciar(1L) }.isInstanceOf(IllegalStateException::class.java)
        verify(exactly = 0) { estoque.baixarPecas(any()) }
        verify(exactly = 0) { eventos.publicar(any()) }
    }

    @Test
    fun `OS inexistente lanca NoSuchElement`() {
        every { repositorio.buscarPorId(99L) } returns null

        assertThatThrownBy { service.iniciar(99L) }.isInstanceOf(NoSuchElementException::class.java)
    }
}
