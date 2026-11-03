package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.DecidirOrcamentoPeloClienteService
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.AprovarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RejeitarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DecidirOrcamentoPeloClienteServiceTest {

    private val repositorio = mockk<OrdemServicoRepository>()
    private val aprovar = mockk<AprovarOrcamentoUseCase>()
    private val rejeitar = mockk<RejeitarOrcamentoUseCase>()
    private val servico = DecidirOrcamentoPeloClienteService(repositorio, aprovar, rejeitar)
    private val ordem = mockk<OrdemServico> { every { id } returns 7L }

    @Test
    fun `consulta devolve a OS do token`() {
        every { repositorio.buscarPorTokenAprovacao("tok") } returns ordem

        assertThat(servico.consultar("tok")).isSameAs(ordem)
    }

    @Test
    fun `token desconhecido e tratado como nao encontrado`() {
        every { repositorio.buscarPorTokenAprovacao("x") } returns null

        assertThatThrownBy { servico.aprovar("x") }.isInstanceOf(NoSuchElementException::class.java)
        verify(exactly = 0) { aprovar.aprovar(any()) }
    }

    @Test
    fun `aprovar pelo token aprova a OS correspondente`() {
        every { repositorio.buscarPorTokenAprovacao("tok") } returns ordem
        every { aprovar.aprovar(7L) } returns ordem

        servico.aprovar("tok")

        verify { aprovar.aprovar(7L) }
    }

    @Test
    fun `rejeitar pelo token repassa o motivo`() {
        every { repositorio.buscarPorTokenAprovacao("tok") } returns ordem
        every { rejeitar.rejeitar(7L, "caro") } returns ordem

        servico.rejeitar("tok", "caro")

        verify { rejeitar.rejeitar(7L, "caro") }
    }
}
