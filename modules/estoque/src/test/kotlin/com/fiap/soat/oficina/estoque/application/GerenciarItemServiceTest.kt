package com.fiap.soat.oficina.estoque.application

import com.fiap.soat.oficina.estoque.application.usecase.GerenciarItemService
import com.fiap.soat.oficina.estoque.assertDadoInvalido
import com.fiap.soat.oficina.estoque.assertDuplicidade
import com.fiap.soat.oficina.estoque.domain.error.ConflitoException
import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GerenciarItemServiceTest {

    private val itens = mockk<ItemEstoqueRepository>()
    private val service = GerenciarItemService(itens)

    private val ativo = ItemEstoque.reconstituir(1, "Pastilha", null, "PECA", BigDecimal("200.00"), 10, 0, true)

    @Test
    fun `cadastra item novo`() {
        every { itens.existePorNome("Pastilha de freio", null) } returns false
        every { itens.salvar(any()) } answers { firstArg() }

        val resultado = service.cadastrar("Pastilha de freio", "Dianteira", "PECA", BigDecimal("200"), 5)

        assertThat(resultado.nome).isEqualTo("Pastilha de freio")
        assertThat(resultado.saldo).isEqualTo(5)
        assertThat(resultado.ativo).isTrue()
        verify(exactly = 1) { itens.salvar(any()) }
    }

    @Test
    fun `rejeita nome ja existente`() {
        every { itens.existePorNome("Pastilha de freio", null) } returns true

        assertDuplicidade("nome") { service.cadastrar("Pastilha de freio", null, "PECA", BigDecimal.TEN, 0) }
        verify(exactly = 0) { itens.salvar(any()) }
    }

    @Test
    fun `rejeita dado invalido sem consultar o repositorio`() {
        assertDadoInvalido("tipo", "Tipo inválido: use PECA ou INSUMO") {
            service.cadastrar("Pastilha", null, "SERVICO", BigDecimal.TEN, 0)
        }
        verify(exactly = 0) { itens.existePorNome(any(), any()) }
    }

    @Test
    fun `altera item ignorando o proprio registro na checagem de nome`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns ativo
        every { itens.existePorNome("Pastilha ceramica", 1L) } returns false
        every { itens.salvar(any()) } answers { firstArg() }

        val resultado = service.alterar(1, "Pastilha ceramica", "Premium", "PECA", BigDecimal("250"))

        assertThat(resultado.nome).isEqualTo("Pastilha ceramica")
        assertThat(resultado.saldo).isEqualTo(10)
    }

    @Test
    fun `renomear para nome de outro item falha`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns ativo
        every { itens.existePorNome("Oleo", 1L) } returns true

        assertDuplicidade("nome") { service.alterar(1, "Oleo", null, "INSUMO", BigDecimal.TEN) }
        verify(exactly = 0) { itens.salvar(any()) }
    }

    @Test
    fun `alterar item inexistente ou inativo falha`() {
        every { itens.buscarPorIdParaAtualizar(9) } returns null
        every { itens.buscarPorIdParaAtualizar(2) } returns ativo.inativar()

        assertThatThrownBy { service.alterar(9, "X", null, "PECA", BigDecimal.TEN) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
            .hasMessage("Item de estoque não encontrado")
        assertThatThrownBy { service.alterar(2, "X", null, "PECA", BigDecimal.TEN) }
            .isInstanceOf(ConflitoException::class.java)
    }

    @Test
    fun `registra entrada somando ao saldo`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns ativo
        every { itens.salvar(any()) } answers { firstArg() }

        val resultado = service.registrarEntrada(1, 5)

        assertThat(resultado.saldo).isEqualTo(15)
    }

    @Test
    fun `entrada com quantidade invalida falha sem consultar o repositorio`() {
        assertDadoInvalido("quantidade", "Quantidade deve ser maior que zero") { service.registrarEntrada(1, 0) }
        assertDadoInvalido("quantidade", "Quantidade é obrigatória") { service.registrarEntrada(1, null) }
        verify(exactly = 0) { itens.buscarPorIdParaAtualizar(any()) }
    }

    @Test
    fun `entrada em item inexistente ou inativo falha`() {
        every { itens.buscarPorIdParaAtualizar(9) } returns null
        every { itens.buscarPorIdParaAtualizar(2) } returns ativo.inativar()

        assertThatThrownBy { service.registrarEntrada(9, 1) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
        assertThatThrownBy { service.registrarEntrada(2, 1) }.isInstanceOf(ConflitoException::class.java)
    }

    @Test
    fun `inativa item sem reservas`() {
        val salvo = slot<ItemEstoque>()
        every { itens.buscarPorIdParaAtualizar(1) } returns ativo
        every { itens.salvar(capture(salvo)) } answers { salvo.captured }

        service.inativar(1)

        assertThat(salvo.captured.ativo).isFalse()
    }

    @Test
    fun `nao inativa item com reservas ativas`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns
            ItemEstoque.reconstituir(1, "Pastilha", null, "PECA", BigDecimal.TEN, 10, 2, true)

        assertThatThrownBy { service.inativar(1) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Item possui reservas ativas e não pode ser inativado")
        verify(exactly = 0) { itens.salvar(any()) }
    }

    @Test
    fun `inativar item ja inativo nao faz nada`() {
        every { itens.buscarPorIdParaAtualizar(1) } returns ativo.inativar()

        service.inativar(1)

        verify(exactly = 0) { itens.salvar(any()) }
    }

    @Test
    fun `inativar item inexistente falha`() {
        every { itens.buscarPorIdParaAtualizar(9) } returns null

        assertThatThrownBy { service.inativar(9) }.isInstanceOf(RegistroNaoEncontradoException::class.java)
    }
}
