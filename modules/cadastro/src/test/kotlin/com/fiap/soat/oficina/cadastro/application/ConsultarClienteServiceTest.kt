package com.fiap.soat.oficina.cadastro.application

import com.fiap.soat.oficina.cadastro.application.usecase.ConsultarClienteService
import com.fiap.soat.oficina.cadastro.assertDadoInvalido
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ClienteRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ConsultarClienteServiceTest {

    private val clientes = mockk<ClienteRepository>()
    private val service = ConsultarClienteService(clientes)

    private val ativo = Cliente.reconstituir(1, "12345678909", "Maria", "maria@email.com", true)
    private val inativo = ativo.inativar()

    @Test
    fun `busca por identificador`() {
        every { clientes.buscarPorId(1) } returns ativo

        assertThat(service.buscarPorId(1)).isSameAs(ativo)
    }

    @Test
    fun `identificador inexistente falha`() {
        every { clientes.buscarPorId(9) } returns null

        assertThatThrownBy { service.buscarPorId(9) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
            .hasMessage("Cliente não encontrado")
    }

    @Test
    fun `cliente inativo e devolvido sem filtro e nao encontrado com somenteAtivos`() {
        every { clientes.buscarPorId(1) } returns inativo

        assertThat(service.buscarPorId(1)).isSameAs(inativo)
        assertThatThrownBy { service.buscarPorId(1, somenteAtivos = true) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
    }

    @Test
    fun `busca por documento mascarado normaliza antes de consultar`() {
        every { clientes.buscarPorDocumento(Documento.of("12345678909")) } returns ativo

        assertThat(service.buscarPorDocumento("123.456.789-09")).isSameAs(ativo)
    }

    @Test
    fun `documento invalido e erro de dado e nao de busca`() {
        assertDadoInvalido("documento", "CPF/CNPJ inválido") { service.buscarPorDocumento("111.111.111-11") }
    }

    @Test
    fun `documento inativo nao e encontrado com somenteAtivos`() {
        every { clientes.buscarPorDocumento(Documento.of("12345678909")) } returns inativo

        assertThatThrownBy { service.buscarPorDocumento("12345678909", somenteAtivos = true) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
    }

    @Test
    fun `lista com e sem filtro`() {
        every { clientes.listar(null) } returns listOf(ativo, inativo)
        every { clientes.listar(true) } returns listOf(ativo)

        assertThat(service.listar()).hasSize(2)
        assertThat(service.listar(true)).containsExactly(ativo)
    }
}
