package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.MontadorDeItensOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.CatalogoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.ItemCatalogo
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MontadorDeItensOrcamentoTest {

    private val catalogo = mockk<CatalogoPort> {
        every { buscarServico(10L) } returns ItemCatalogo("Troca de óleo", Dinheiro.of(BigDecimal("120.00")))
        every { buscarServico(99L) } returns null
        every { buscarPeca(50L) } returns ItemCatalogo("Pastilha de freio", Dinheiro.of(BigDecimal("200.00")))
        every { buscarPeca(98L) } returns null
    }
    private val montador = MontadorDeItensOrcamento(catalogo)

    private fun montar(item: ItemSolicitado) = montador.montar(listOf(item)).single()

    @Test
    fun `servico so com id usa nome e preco do cadastro`() {
        val item = montar(ItemSolicitado(TipoItem.SERVICO, servicoId = 10L))

        assertThat(item.descricao).isEqualTo("Troca de óleo")
        assertThat(item.valor.valor).isEqualByComparingTo("120.00")
    }

    @Test
    fun `peca so com id usa nome e preco do estoque vezes a quantidade`() {
        val item = montar(ItemSolicitado(TipoItem.PECA, itemEstoqueId = 50L, quantidade = 3))

        assertThat(item.descricao).isEqualTo("Pastilha de freio")
        assertThat(item.valor.valor).isEqualByComparingTo("600.00")
        assertThat(item.itemEstoqueId).isEqualTo(50L)
        assertThat(item.quantidade).isEqualTo(3)
    }

    @Test
    fun `valor e descricao informados prevalecem sobre o catalogo`() {
        val servico = montar(ItemSolicitado(TipoItem.SERVICO, "Troca com desconto", BigDecimal("100.00"), servicoId = 10L))
        val peca = montar(
            ItemSolicitado(TipoItem.PECA, "Pastilha promocional", BigDecimal("150.00"), itemEstoqueId = 50L, quantidade = 2),
        )

        assertThat(servico.descricao).isEqualTo("Troca com desconto")
        assertThat(servico.valor.valor).isEqualByComparingTo("100.00")
        assertThat(peca.descricao).isEqualTo("Pastilha promocional")
        assertThat(peca.valor.valor).isEqualByComparingTo("150.00")
    }

    @Test
    fun `descricao em branco cai no nome do catalogo`() {
        assertThat(montar(ItemSolicitado(TipoItem.SERVICO, "  ", servicoId = 10L)).descricao).isEqualTo("Troca de óleo")
    }

    @Test
    fun `servico avulso com descricao e valor continua aceito`() {
        val item = montar(ItemSolicitado(TipoItem.SERVICO, "Lavagem", BigDecimal("40.00")))

        assertThat(item.descricao).isEqualTo("Lavagem")
        assertThat(item.valor.valor).isEqualByComparingTo("40.00")
    }

    @Test
    fun `peca com valor da linha ainda valida o id`() {
        val item = montar(ItemSolicitado(TipoItem.PECA, "Pastilha", BigDecimal("180.00"), itemEstoqueId = 50L, quantidade = 1))
        assertThat(item.valor.valor).isEqualByComparingTo("180.00")

        assertThatThrownBy {
            montar(ItemSolicitado(TipoItem.PECA, "Pastilha", BigDecimal("180.00"), itemEstoqueId = 98L, quantidade = 1))
        }.isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `servico inexistente ou inativo lanca NoSuchElement`() {
        assertThatThrownBy { montar(ItemSolicitado(TipoItem.SERVICO, servicoId = 99L)) }
            .isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("99")
    }

    @Test
    fun `peca inexistente ou inativa lanca NoSuchElement`() {
        assertThatThrownBy { montar(ItemSolicitado(TipoItem.PECA, itemEstoqueId = 98L, quantidade = 1)) }
            .isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("98")
    }

    @Test
    fun `servico avulso sem valor ou sem descricao lanca IllegalArgument`() {
        assertThatThrownBy { montar(ItemSolicitado(TipoItem.SERVICO, "Lavagem")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { montar(ItemSolicitado(TipoItem.SERVICO, valor = BigDecimal("40.00"))) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `peca por id sem valor e sem quantidade positiva lanca IllegalArgument`() {
        assertThatThrownBy { montar(ItemSolicitado(TipoItem.PECA, itemEstoqueId = 50L)) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { montar(ItemSolicitado(TipoItem.PECA, itemEstoqueId = 50L, quantidade = 0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `peca sem itemEstoqueId lanca IllegalArgument`() {
        assertThatThrownBy { montar(ItemSolicitado(TipoItem.PECA, "Pastilha", BigDecimal("10.00"), quantidade = 1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `servicoId em peca lanca IllegalArgument`() {
        assertThatThrownBy { montar(ItemSolicitado(TipoItem.PECA, servicoId = 10L, itemEstoqueId = 50L, quantidade = 1)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `valor negativo lanca IllegalArgument`() {
        assertThatThrownBy { montar(ItemSolicitado(TipoItem.SERVICO, "X", BigDecimal("-1"))) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
