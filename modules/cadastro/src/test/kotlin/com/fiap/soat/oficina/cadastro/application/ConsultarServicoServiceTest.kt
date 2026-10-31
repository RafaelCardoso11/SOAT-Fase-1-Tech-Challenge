package com.fiap.soat.oficina.cadastro.application

import com.fiap.soat.oficina.cadastro.application.usecase.ConsultarServicoService
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Servico
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ServicoRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ConsultarServicoServiceTest {

    private val servicos = mockk<ServicoRepository>()
    private val service = ConsultarServicoService(servicos)

    private val ativo = Servico.reconstituir(5, "Alinhamento", null, BigDecimal("80.00"), true)
    private val inativo = ativo.inativar()

    @Test
    fun `busca por identificador`() {
        every { servicos.buscarPorId(5) } returns ativo

        assertThat(service.buscarPorId(5)).isSameAs(ativo)
    }

    @Test
    fun `identificador inexistente falha`() {
        every { servicos.buscarPorId(9) } returns null

        assertThatThrownBy { service.buscarPorId(9) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
            .hasMessage("Serviço não encontrado")
    }

    @Test
    fun `servico inativo e devolvido sem filtro e nao encontrado com somenteAtivos`() {
        every { servicos.buscarPorId(5) } returns inativo

        assertThat(service.buscarPorId(5)).isSameAs(inativo)
        assertThatThrownBy { service.buscarPorId(5, somenteAtivos = true) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
    }

    @Test
    fun `lista com e sem filtro`() {
        every { servicos.listar(null) } returns listOf(ativo, inativo)
        every { servicos.listar(false) } returns listOf(inativo)

        assertThat(service.listar()).hasSize(2)
        assertThat(service.listar(false)).containsExactly(inativo)
    }
}
