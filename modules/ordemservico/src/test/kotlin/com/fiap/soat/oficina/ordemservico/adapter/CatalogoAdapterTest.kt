package com.fiap.soat.oficina.ordemservico.adapter

import com.fiap.soat.oficina.cadastro.domain.model.Servico
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarServicoUseCase
import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarItemUseCase
import com.fiap.soat.oficina.ordemservico.adapter.outbound.catalogo.CatalogoAdapter
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException as ServicoNaoEncontrado
import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException as ItemNaoEncontrado

class CatalogoAdapterTest {

    private val servicos = mockk<ConsultarServicoUseCase>()
    private val itens = mockk<ConsultarItemUseCase>()
    private val adapter = CatalogoAdapter(servicos, itens)

    @Test
    fun `traduz servico ativo do cadastro`() {
        every { servicos.buscarPorId(1L, true) } returns
            Servico.reconstituir(1L, "Alinhamento", null, BigDecimal("90.00"), true)

        val item = adapter.buscarServico(1L)!!

        assertThat(item.nome).isEqualTo("Alinhamento")
        assertThat(item.preco.valor).isEqualByComparingTo("90.00")
    }

    @Test
    fun `servico inexistente ou inativo vira null`() {
        every { servicos.buscarPorId(2L, true) } throws ServicoNaoEncontrado("Serviço não encontrado")

        assertThat(adapter.buscarServico(2L)).isNull()
    }

    @Test
    fun `traduz item ativo do estoque`() {
        every { itens.buscarPorId(3L, true) } returns
            ItemEstoque.reconstituir(3L, "Filtro de óleo", null, "PECA", BigDecimal("35.50"), 10, 0, true)

        val item = adapter.buscarPeca(3L)!!

        assertThat(item.nome).isEqualTo("Filtro de óleo")
        assertThat(item.preco.valor).isEqualByComparingTo("35.50")
    }

    @Test
    fun `item inexistente ou inativo vira null`() {
        every { itens.buscarPorId(4L, true) } throws ItemNaoEncontrado("Item de estoque não encontrado")

        assertThat(adapter.buscarPeca(4L)).isNull()
    }
}
