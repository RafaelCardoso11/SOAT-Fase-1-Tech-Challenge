package com.fiap.soat.oficina.estoque.application

import com.fiap.soat.oficina.estoque.application.usecase.ConsultarItemService
import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ConsultarItemServiceTest {

    private val itens = mockk<ItemEstoqueRepository>()
    private val service = ConsultarItemService(itens)

    private val ativo = ItemEstoque.reconstituir(1, "Pastilha", null, "PECA", BigDecimal("200.00"), 10, 3, true)
    private val inativo = ItemEstoque.reconstituir(2, "Oleo", null, "INSUMO", BigDecimal("30.00"), 8, 0, false)

    @Test
    fun `busca por identificador com saldo, reservado e disponivel`() {
        every { itens.buscarPorId(1) } returns ativo

        val item = service.buscarPorId(1)

        assertThat(item).isSameAs(ativo)
        assertThat(item.disponivel).isEqualTo(7)
    }

    @Test
    fun `identificador inexistente falha`() {
        every { itens.buscarPorId(9) } returns null

        assertThatThrownBy { service.buscarPorId(9) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
            .hasMessage("Item de estoque não encontrado")
    }

    @Test
    fun `item inativo e devolvido sem filtro e nao encontrado com somenteAtivos`() {
        every { itens.buscarPorId(2) } returns inativo

        assertThat(service.buscarPorId(2)).isSameAs(inativo)
        assertThatThrownBy { service.buscarPorId(2, somenteAtivos = true) }
            .isInstanceOf(RegistroNaoEncontradoException::class.java)
    }

    @Test
    fun `lista com e sem filtro`() {
        every { itens.listar(null) } returns listOf(ativo, inativo)
        every { itens.listar(true) } returns listOf(ativo)

        assertThat(service.listar()).hasSize(2)
        assertThat(service.listar(true)).containsExactly(ativo)
    }
}
