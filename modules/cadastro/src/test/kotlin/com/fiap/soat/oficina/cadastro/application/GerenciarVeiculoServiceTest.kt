package com.fiap.soat.oficina.cadastro.application

import com.fiap.soat.oficina.cadastro.application.usecase.GerenciarVeiculoService
import com.fiap.soat.oficina.cadastro.assertDadoInvalido
import com.fiap.soat.oficina.cadastro.assertDuplicidade
import com.fiap.soat.oficina.cadastro.domain.error.ConflitoException
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import com.fiap.soat.oficina.cadastro.domain.model.Veiculo
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ClienteRepository
import com.fiap.soat.oficina.cadastro.domain.port.outbound.VeiculoRepository
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class GerenciarVeiculoServiceTest {

    private val veiculos = mockk<VeiculoRepository>()
    private val clientes = mockk<ClienteRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC)
    private val service = GerenciarVeiculoService(veiculos, clientes, clock)

    private val placa = Placa.of("ABC1D23")
    private val clienteAtivo = Cliente.reconstituir(1, "12345678909", "Maria", "maria@email.com", true)
    private val clienteInativo = Cliente.reconstituir(2, "52998224725", "Joao", "joao@email.com", false)
    private val veiculo = Veiculo.reconstituir(10, "ABC1D23", "Fiat", "Uno", 2020, 1, true)

    @Test
    fun `cadastra veiculo para cliente ativo`() {
        every { clientes.buscarPorId(1) } returns clienteAtivo
        every { veiculos.existePorPlaca(placa) } returns false
        every { veiculos.salvar(any()) } answers { firstArg() }

        val resultado = service.cadastrar("abc-1d23", "Fiat", "Uno", 2020, 1)

        assertThat(resultado.placa).isEqualTo(placa)
        assertThat(resultado.clienteId).isEqualTo(1)
        assertThat(resultado.ativo).isTrue()
    }

    @Test
    fun `cadastro com cliente inexistente falha`() {
        every { clientes.buscarPorId(9) } returns null

        assertThatThrownBy { service.cadastrar("ABC1D23", "Fiat", "Uno", 2020, 9) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
            .hasMessage("Cliente não encontrado")
        verify(exactly = 0) { veiculos.salvar(any()) }
    }

    @Test
    fun `cadastro com cliente inativo falha`() {
        every { clientes.buscarPorId(2) } returns clienteInativo

        assertThatThrownBy { service.cadastrar("ABC1D23", "Fiat", "Uno", 2020, 2) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Cliente inativo não pode ter veículos")
    }

    @Test
    fun `cadastro com placa duplicada falha`() {
        every { clientes.buscarPorId(1) } returns clienteAtivo
        every { veiculos.existePorPlaca(placa) } returns true

        assertDuplicidade("placa") { service.cadastrar("ABC1D23", "Fiat", "Uno", 2020, 1) }
        verify(exactly = 0) { veiculos.salvar(any()) }
    }

    @Test
    fun `ano acima do limite derivado do relogio e rejeitado`() {
        assertDadoInvalido("ano", "Ano deve estar entre 1900 e 2027") {
            service.cadastrar("ABC1D23", "Fiat", "Uno", 2028, 1)
        }
        verify(exactly = 0) { clientes.buscarPorId(any()) }
    }

    @Test
    fun `ano do proximo ano e aceito`() {
        every { clientes.buscarPorId(1) } returns clienteAtivo
        every { veiculos.existePorPlaca(placa) } returns false
        every { veiculos.salvar(any()) } answers { firstArg() }

        assertThat(service.cadastrar("ABC1D23", "Fiat", "Uno", 2027, 1).ano).isEqualTo(2027)
    }

    @Test
    fun `altera dados mantendo o dono`() {
        every { veiculos.buscarPorId(10) } returns veiculo
        every { clientes.buscarPorId(1) } returns clienteAtivo
        every { veiculos.salvar(any()) } answers { firstArg() }

        val resultado = service.alterar(10, "VW", "Gol", 2021, 1)

        assertThat(resultado.marca).isEqualTo("VW")
        assertThat(resultado.placa).isEqualTo(placa)
    }

    @Test
    fun `troca o dono para outro cliente ativo`() {
        val outro = Cliente.reconstituir(3, "11144477735", "Ana", "ana@email.com", true)
        every { veiculos.buscarPorId(10) } returns veiculo
        every { clientes.buscarPorId(3) } returns outro
        every { veiculos.salvar(any()) } answers { firstArg() }

        assertThat(service.alterar(10, "Fiat", "Uno", 2020, 3).clienteId).isEqualTo(3)
    }

    @Test
    fun `troca de dono para cliente inativo falha`() {
        every { veiculos.buscarPorId(10) } returns veiculo
        every { clientes.buscarPorId(2) } returns clienteInativo

        assertThatThrownBy { service.alterar(10, "Fiat", "Uno", 2020, 2) }
            .isInstanceOf(ConflitoException::class.java)
        verify(exactly = 0) { veiculos.salvar(any()) }
    }

    @Test
    fun `alterar veiculo inexistente ou inativo falha`() {
        every { veiculos.buscarPorId(9) } returns null
        every { veiculos.buscarPorId(11) } returns veiculo.inativar()

        assertThatThrownBy { service.alterar(9, "Fiat", "Uno", 2020, 1) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
        assertThatThrownBy { service.alterar(11, "Fiat", "Uno", 2020, 1) }
            .isInstanceOf(ConflitoException::class.java)
    }

    @Test
    fun `inativa veiculo`() {
        val salvo = slot<Veiculo>()
        every { veiculos.buscarPorId(10) } returns veiculo
        every { veiculos.salvar(capture(salvo)) } answers { salvo.captured }

        service.inativar(10)

        assertThat(salvo.captured.ativo).isFalse()
    }

    @Test
    fun `inativar veiculo ja inativo nao faz nada`() {
        every { veiculos.buscarPorId(10) } returns veiculo.inativar()

        service.inativar(10)

        verify(exactly = 0) { veiculos.salvar(any()) }
    }

    @Test
    fun `inativar veiculo inexistente falha`() {
        every { veiculos.buscarPorId(9) } returns null

        assertThatThrownBy { service.inativar(9) }.isInstanceOf(RegistroNaoEncontradoException::class.java)
    }
}
