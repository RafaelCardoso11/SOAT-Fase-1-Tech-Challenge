package com.fiap.soat.oficina.cadastro.application

import com.fiap.soat.oficina.cadastro.application.usecase.GerenciarServicoService
import com.fiap.soat.oficina.cadastro.assertDadoInvalido
import com.fiap.soat.oficina.cadastro.assertDuplicidade
import com.fiap.soat.oficina.cadastro.domain.error.ConflitoException
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.cadastro.domain.model.Servico
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ServicoRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GerenciarServicoServiceTest {

    private val servicos = mockk<ServicoRepository>()
    private val service = GerenciarServicoService(servicos)

    private val ativo = Servico.reconstituir(5, "Alinhamento", null, BigDecimal("80.00"), true)

    @Test
    fun `cadastra servico novo`() {
        every { servicos.existePorNome("Troca de óleo", null) } returns false
        every { servicos.salvar(any()) } answers { firstArg() }

        val resultado = service.cadastrar("Troca de óleo", "Óleo e filtro", BigDecimal("150"))

        assertThat(resultado.nome).isEqualTo("Troca de óleo")
        assertThat(resultado.preco.valor).isEqualByComparingTo("150.00")
        assertThat(resultado.ativo).isTrue()
    }

    @Test
    fun `rejeita nome ja existente`() {
        every { servicos.existePorNome("Troca de óleo", null) } returns true

        assertDuplicidade("nome") { service.cadastrar("Troca de óleo", null, BigDecimal.TEN) }
        verify(exactly = 0) { servicos.salvar(any()) }
    }

    @Test
    fun `rejeita dado invalido sem consultar o repositorio`() {
        assertDadoInvalido("preco", "Preço não pode ser negativo") {
            service.cadastrar("Troca de óleo", null, BigDecimal("-1"))
        }
        verify(exactly = 0) { servicos.existePorNome(any(), any()) }
    }

    @Test
    fun `altera servico ignorando o proprio registro na checagem de nome`() {
        every { servicos.buscarPorId(5) } returns ativo
        every { servicos.existePorNome("Alinhamento 3D", 5L) } returns false
        every { servicos.salvar(any()) } answers { firstArg() }

        val resultado = service.alterar(5, "Alinhamento 3D", "Computadorizado", BigDecimal("120"))

        assertThat(resultado.nome).isEqualTo("Alinhamento 3D")
        assertThat(resultado.preco.valor).isEqualByComparingTo("120.00")
    }

    @Test
    fun `renomear para nome de outro servico falha`() {
        every { servicos.buscarPorId(5) } returns ativo
        every { servicos.existePorNome("Troca de óleo", 5L) } returns true

        assertDuplicidade("nome") { service.alterar(5, "Troca de óleo", null, BigDecimal.TEN) }
        verify(exactly = 0) { servicos.salvar(any()) }
    }

    @Test
    fun `alterar servico inexistente ou inativo falha`() {
        every { servicos.buscarPorId(9) } returns null
        every { servicos.buscarPorId(6) } returns ativo.inativar()

        assertThatThrownBy { service.alterar(9, "X", null, BigDecimal.TEN) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
            .hasMessage("Serviço não encontrado")
        assertThatThrownBy { service.alterar(6, "X", null, BigDecimal.TEN) }
            .isInstanceOf(ConflitoException::class.java)
    }

    @Test
    fun `inativa servico`() {
        val salvo = slot<Servico>()
        every { servicos.buscarPorId(5) } returns ativo
        every { servicos.salvar(capture(salvo)) } answers { salvo.captured }

        service.inativar(5)

        assertThat(salvo.captured.ativo).isFalse()
    }

    @Test
    fun `inativar servico ja inativo nao faz nada`() {
        every { servicos.buscarPorId(5) } returns ativo.inativar()

        service.inativar(5)

        verify(exactly = 0) { servicos.salvar(any()) }
    }

    @Test
    fun `inativar servico inexistente falha`() {
        every { servicos.buscarPorId(9) } returns null

        assertThatThrownBy { service.inativar(9) }.isInstanceOf(RegistroNaoEncontradoException::class.java)
    }
}
