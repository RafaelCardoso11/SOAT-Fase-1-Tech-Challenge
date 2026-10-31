package com.fiap.soat.oficina.cadastro.application

import com.fiap.soat.oficina.cadastro.application.usecase.GerenciarClienteService
import com.fiap.soat.oficina.cadastro.assertDadoInvalido
import com.fiap.soat.oficina.cadastro.assertDuplicidade
import com.fiap.soat.oficina.cadastro.domain.error.ConflitoException
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ClienteRepository
import com.fiap.soat.oficina.cadastro.domain.port.outbound.VeiculoRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class GerenciarClienteServiceTest {

    private val clientes = mockk<ClienteRepository>()
    private val veiculos = mockk<VeiculoRepository>()
    private val service = GerenciarClienteService(clientes, veiculos)

    private val documento = Documento.of("12345678909")
    private val ativo = Cliente.reconstituir(1, "12345678909", "Maria", "maria@email.com", true)

    @Test
    fun `cadastra cliente novo`() {
        every { clientes.existePorDocumento(documento) } returns false
        every { clientes.salvar(any()) } answers { firstArg() }

        val resultado = service.cadastrar("123.456.789-09", "Maria", "maria@email.com")

        assertThat(resultado.documento).isEqualTo(documento)
        assertThat(resultado.ativo).isTrue()
        verify(exactly = 1) { clientes.salvar(any()) }
    }

    @Test
    fun `rejeita documento ja cadastrado`() {
        every { clientes.existePorDocumento(documento) } returns true

        assertDuplicidade("documento") { service.cadastrar("123.456.789-09", "Maria", "maria@email.com") }
        verify(exactly = 0) { clientes.salvar(any()) }
    }

    @Test
    fun `rejeita dado invalido sem consultar o repositorio`() {
        assertDadoInvalido("documento", "CPF/CNPJ inválido") { service.cadastrar("111.111.111-11", "Maria", "m@e.com") }
        verify(exactly = 0) { clientes.existePorDocumento(any()) }
    }

    @Test
    fun `altera nome e e-mail`() {
        every { clientes.buscarPorId(1) } returns ativo
        every { clientes.salvar(any()) } answers { firstArg() }

        val resultado = service.alterar(1, "Maria Souza", "souza@email.com")

        assertThat(resultado.nome).isEqualTo("Maria Souza")
        assertThat(resultado.email.valor).isEqualTo("souza@email.com")
    }

    @Test
    fun `alterar cliente inexistente falha`() {
        every { clientes.buscarPorId(9) } returns null

        assertThatThrownBy { service.alterar(9, "Maria", "m@e.com") }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
            .hasMessage("Cliente não encontrado")
    }

    @Test
    fun `inativa cliente sem veiculos ativos`() {
        val salvo = slot<Cliente>()
        every { clientes.buscarPorId(1) } returns ativo
        every { veiculos.existeAtivoDoCliente(1) } returns false
        every { clientes.salvar(capture(salvo)) } answers { salvo.captured }

        service.inativar(1)

        assertThat(salvo.captured.ativo).isFalse()
    }

    @Test
    fun `nao inativa cliente com veiculos ativos`() {
        every { clientes.buscarPorId(1) } returns ativo
        every { veiculos.existeAtivoDoCliente(1) } returns true

        assertThatThrownBy { service.inativar(1) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Cliente possui veículos ativos e não pode ser inativado")
        verify(exactly = 0) { clientes.salvar(any()) }
    }

    @Test
    fun `inativar cliente ja inativo nao faz nada`() {
        every { clientes.buscarPorId(1) } returns ativo.inativar()

        service.inativar(1)

        verify(exactly = 0) { clientes.salvar(any()) }
        verify(exactly = 0) { veiculos.existeAtivoDoCliente(any()) }
    }

    @Test
    fun `inativar cliente inexistente falha`() {
        every { clientes.buscarPorId(9) } returns null

        assertThatThrownBy { service.inativar(9) }.isInstanceOf(RegistroNaoEncontradoException::class.java)
    }
}
