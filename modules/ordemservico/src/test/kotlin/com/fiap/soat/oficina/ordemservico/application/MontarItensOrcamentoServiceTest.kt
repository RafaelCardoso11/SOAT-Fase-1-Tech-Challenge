package com.fiap.soat.oficina.ordemservico.application

import com.fiap.soat.oficina.ordemservico.application.usecase.MontarItensOrcamentoService
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.CatalogoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.ItemCatalogo
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MontarItensOrcamentoServiceTest {

    private val catalogo = mockk<CatalogoPort>()
    private val servico = MontarItensOrcamentoService(catalogo)

    private fun dinheiro(valor: String) = Dinheiro.of(BigDecimal(valor))

    @Test
    fun `servico cadastrado usa nome e preco do catalogo`() {
        every { catalogo.servico(1) } returns ItemCatalogo("Alinhamento", dinheiro("120.00"))

        val item = servico.montar(listOf(ItemSolicitado(TipoItem.SERVICO, servicoId = 1))).single()

        assertThat(item.descricao).isEqualTo("Alinhamento")
        assertThat(item.valor).isEqualTo(dinheiro("120.00"))
    }

    @Test
    fun `peca cadastrada usa preco vezes quantidade`() {
        every { catalogo.itemEstoque(7) } returns ItemCatalogo("Pastilha", dinheiro("30.50"))

        val item = servico.montar(
            listOf(ItemSolicitado(TipoItem.PECA, itemEstoqueId = 7, quantidade = 3)),
        ).single()

        assertThat(item.descricao).isEqualTo("Pastilha")
        assertThat(item.valor).isEqualTo(dinheiro("91.50"))
        assertThat(item.itemEstoqueId).isEqualTo(7)
        assertThat(item.quantidade).isEqualTo(3)
    }

    @Test
    fun `valor e descricao informados tem prioridade sobre o catalogo`() {
        every { catalogo.itemEstoque(7) } returns ItemCatalogo("Pastilha", dinheiro("30.50"))

        val item = servico.montar(
            listOf(
                ItemSolicitado(
                    TipoItem.PECA,
                    itemEstoqueId = 7,
                    quantidade = 3,
                    descricao = "Pastilha com desconto",
                    valor = dinheiro("80.00"),
                ),
            ),
        ).single()

        assertThat(item.descricao).isEqualTo("Pastilha com desconto")
        assertThat(item.valor).isEqualTo(dinheiro("80.00"))
    }

    @Test
    fun `descricao em branco usa o nome do catalogo`() {
        every { catalogo.servico(1) } returns ItemCatalogo("Alinhamento", dinheiro("120.00"))

        val item = servico.montar(listOf(ItemSolicitado(TipoItem.SERVICO, servicoId = 1, descricao = " "))).single()

        assertThat(item.descricao).isEqualTo("Alinhamento")
    }

    @Test
    fun `item avulso com descricao e valor nao consulta o catalogo`() {
        val item = servico.montar(
            listOf(ItemSolicitado(TipoItem.SERVICO, descricao = "Revisão extra", valor = dinheiro("50.00"))),
        ).single()

        assertThat(item.valor).isEqualTo(dinheiro("50.00"))
        verify(exactly = 0) { catalogo.servico(any()) }
    }

    @Test
    fun `item avulso sem valor e recusado`() {
        assertThatThrownBy {
            servico.montar(listOf(ItemSolicitado(TipoItem.SERVICO, descricao = "Revisão extra")))
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("valor")
    }

    @Test
    fun `item avulso sem descricao e recusado`() {
        assertThatThrownBy {
            servico.montar(listOf(ItemSolicitado(TipoItem.SERVICO, valor = dinheiro("50.00"))))
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("descrição")
    }

    @Test
    fun `peca cadastrada sem quantidade e recusada`() {
        every { catalogo.itemEstoque(7) } returns ItemCatalogo("Pastilha", dinheiro("30.50"))

        assertThatThrownBy {
            servico.montar(listOf(ItemSolicitado(TipoItem.PECA, itemEstoqueId = 7)))
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("quantidade")
    }

    @Test
    fun `peca sem item de estoque e recusada`() {
        assertThatThrownBy {
            servico.montar(listOf(ItemSolicitado(TipoItem.PECA, quantidade = 1, descricao = "x", valor = dinheiro("1"))))
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("item do estoque")
    }

    @Test
    fun `cadastro inexistente propaga nao encontrado`() {
        every { catalogo.servico(9) } throws NoSuchElementException("Serviço 9 não encontrado")

        assertThatThrownBy {
            servico.montar(listOf(ItemSolicitado(TipoItem.SERVICO, servicoId = 9)))
        }.isInstanceOf(NoSuchElementException::class.java)
    }
}
