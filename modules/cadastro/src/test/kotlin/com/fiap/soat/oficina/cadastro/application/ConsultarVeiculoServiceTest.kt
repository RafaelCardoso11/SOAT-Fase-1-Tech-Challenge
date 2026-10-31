package com.fiap.soat.oficina.cadastro.application

import com.fiap.soat.oficina.cadastro.application.usecase.ConsultarVeiculoService
import com.fiap.soat.oficina.cadastro.assertDadoInvalido
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Veiculo
import com.fiap.soat.oficina.cadastro.domain.port.outbound.VeiculoRepository
import com.fiap.soat.oficina.shared.domain.vo.Placa
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ConsultarVeiculoServiceTest {

    private val veiculos = mockk<VeiculoRepository>()
    private val service = ConsultarVeiculoService(veiculos)

    private val ativo = Veiculo.reconstituir(10, "ABC1D23", "Fiat", "Uno", 2020, 1, true)
    private val inativo = ativo.inativar()

    @Test
    fun `busca por identificador`() {
        every { veiculos.buscarPorId(10) } returns ativo

        assertThat(service.buscarPorId(10)).isSameAs(ativo)
    }

    @Test
    fun `identificador inexistente falha`() {
        every { veiculos.buscarPorId(9) } returns null

        assertThatThrownBy { service.buscarPorId(9) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
            .hasMessage("Veículo não encontrado")
    }

    @Test
    fun `veiculo inativo e devolvido sem filtro e nao encontrado com somenteAtivos`() {
        every { veiculos.buscarPorId(10) } returns inativo

        assertThat(service.buscarPorId(10)).isSameAs(inativo)
        assertThatThrownBy { service.buscarPorId(10, somenteAtivos = true) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
    }

    @Test
    fun `busca por placa em minusculas e com hifen normaliza antes de consultar`() {
        every { veiculos.buscarPorPlaca(Placa.of("ABC1D23")) } returns ativo

        assertThat(service.buscarPorPlaca("abc-1d23")).isSameAs(ativo)
    }

    @Test
    fun `placa invalida e erro de dado e nao de busca`() {
        assertDadoInvalido("placa", "Placa inválida") { service.buscarPorPlaca("12345") }
    }

    @Test
    fun `placa de veiculo inativo nao e encontrada com somenteAtivos`() {
        every { veiculos.buscarPorPlaca(Placa.of("ABC1D23")) } returns inativo

        assertThatThrownBy { service.buscarPorPlaca("ABC1D23", somenteAtivos = true) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
    }

    @Test
    fun `lista repassando os filtros`() {
        every { veiculos.listar(null, null) } returns listOf(ativo, inativo)
        every { veiculos.listar(true, 1L) } returns listOf(ativo)

        assertThat(service.listar()).hasSize(2)
        assertThat(service.listar(true, 1L)).containsExactly(ativo)
    }
}
