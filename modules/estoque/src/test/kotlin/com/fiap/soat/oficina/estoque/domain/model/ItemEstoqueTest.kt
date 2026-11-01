package com.fiap.soat.oficina.estoque.domain.model

import com.fiap.soat.oficina.estoque.assertDadoInvalido
import com.fiap.soat.oficina.estoque.domain.error.ConflitoException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

class ItemEstoqueTest {

    private fun item() = ItemEstoque.criar("Pastilha de freio", "Dianteira", "PECA", BigDecimal("200"), 10)

    @Test
    fun `cria item ativo normalizando nome, descricao, tipo e preco`() {
        val item = ItemEstoque.criar(" Pastilha de freio ", " Dianteira ", "peca", BigDecimal("200"), 10)

        assertThat(item.id).isNull()
        assertThat(item.nome).isEqualTo("Pastilha de freio")
        assertThat(item.descricao).isEqualTo("Dianteira")
        assertThat(item.tipo).isEqualTo(TipoItem.PECA)
        assertThat(item.preco.valor).isEqualByComparingTo("200.00")
        assertThat(item.preco.valor.scale()).isEqualTo(2)
        assertThat(item.saldo).isEqualTo(10)
        assertThat(item.reservado).isZero()
        assertThat(item.disponivel).isEqualTo(10)
        assertThat(item.ativo).isTrue()
    }

    @Test
    fun `saldo inicial e opcional e comeca em zero`() {
        assertThat(ItemEstoque.criar("Oleo", null, "INSUMO", BigDecimal.TEN, null).saldo).isZero()
    }

    @Test
    fun `descricao vazia vira nula`() {
        assertThat(ItemEstoque.criar("Oleo", "  ", "INSUMO", BigDecimal.TEN, 0).descricao).isNull()
    }

    @Test
    fun `preco zero e aceito`() {
        assertThat(ItemEstoque.criar("Brinde", null, "INSUMO", BigDecimal.ZERO, 0).preco.valor).isEqualByComparingTo("0")
    }

    @Test
    fun `nome ausente ou em branco e obrigatorio`() {
        assertDadoInvalido("nome", "Nome é obrigatório") { ItemEstoque.criar(null, null, "PECA", BigDecimal.TEN, 0) }
        assertDadoInvalido("nome", "Nome é obrigatório") { ItemEstoque.criar("  ", null, "PECA", BigDecimal.TEN, 0) }
    }

    @Test
    fun `nome acima de 120 caracteres e rejeitado`() {
        assertDadoInvalido("nome", "Nome deve ter no máximo 120 caracteres") {
            ItemEstoque.criar("a".repeat(121), null, "PECA", BigDecimal.TEN, 0)
        }
    }

    @Test
    fun `descricao acima de 500 caracteres e rejeitada`() {
        assertDadoInvalido("descricao", "Descrição deve ter no máximo 500 caracteres") {
            ItemEstoque.criar("Oleo", "a".repeat(501), "INSUMO", BigDecimal.TEN, 0)
        }
    }

    @Test
    fun `tipo ausente e obrigatorio`() {
        assertDadoInvalido("tipo", "Tipo é obrigatório") { ItemEstoque.criar("Oleo", null, null, BigDecimal.TEN, 0) }
        assertDadoInvalido("tipo", "Tipo é obrigatório") { ItemEstoque.criar("Oleo", null, " ", BigDecimal.TEN, 0) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["SERVICO", "peça", "x"])
    fun `tipo invalido e rejeitado`(tipo: String) {
        assertDadoInvalido("tipo", "Tipo inválido: use PECA ou INSUMO") {
            ItemEstoque.criar("Oleo", null, tipo, BigDecimal.TEN, 0)
        }
    }

    @Test
    fun `preco ausente e obrigatorio`() {
        assertDadoInvalido("preco", "Preço é obrigatório") { ItemEstoque.criar("Oleo", null, "INSUMO", null, 0) }
    }

    @Test
    fun `preco negativo e rejeitado`() {
        assertDadoInvalido("preco", "Preço não pode ser negativo") {
            ItemEstoque.criar("Oleo", null, "INSUMO", BigDecimal("-0.01"), 0)
        }
    }

    @Test
    fun `saldo inicial negativo e rejeitado`() {
        assertDadoInvalido("saldo", "Saldo inicial não pode ser negativo") {
            ItemEstoque.criar("Oleo", null, "INSUMO", BigDecimal.TEN, -1)
        }
    }

    @Test
    fun `alterar muda os dados cadastrais sem tocar em saldo e reservado`() {
        val original = ItemEstoque.reconstituir(4, "Oleo", null, "INSUMO", BigDecimal("30.00"), 10, 3, true)

        val alterado = original.alterar("Oleo sintetico", "5W30", "insumo", BigDecimal("45.5"))

        assertThat(alterado.id).isEqualTo(4)
        assertThat(alterado.nome).isEqualTo("Oleo sintetico")
        assertThat(alterado.descricao).isEqualTo("5W30")
        assertThat(alterado.preco.valor).isEqualByComparingTo("45.50")
        assertThat(alterado.saldo).isEqualTo(10)
        assertThat(alterado.reservado).isEqualTo(3)
        assertThat(original.nome).isEqualTo("Oleo")
    }

    @Test
    fun `alteracao valida os campos`() {
        assertDadoInvalido("nome", "Nome é obrigatório") { item().alterar(null, null, "PECA", BigDecimal.TEN) }
        assertDadoInvalido("tipo", "Tipo inválido: use PECA ou INSUMO") { item().alterar("X", null, "Y", BigDecimal.TEN) }
        assertDadoInvalido("preco", "Preço é obrigatório") { item().alterar("X", null, "PECA", null) }
    }

    @Test
    fun `entrada soma ao saldo e ao disponivel`() {
        val original = ItemEstoque.reconstituir(1, "Pastilha", null, "PECA", BigDecimal.TEN, 5, 2, true)

        val depois = original.registrarEntrada(Quantidade.of(10))

        assertThat(depois.saldo).isEqualTo(15)
        assertThat(depois.reservado).isEqualTo(2)
        assertThat(depois.disponivel).isEqualTo(13)
        assertThat(original.saldo).isEqualTo(5)
    }

    @Test
    fun `item inativo nao pode ser alterado nem receber entrada`() {
        val inativo = item().inativar()

        assertThatThrownBy { inativo.alterar("Outro", null, "PECA", BigDecimal.TEN) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Item inativo não pode ser alterado")
        assertThatThrownBy { inativo.registrarEntrada(Quantidade.of(1)) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Item inativo não pode receber entrada de estoque")
    }

    @Test
    fun `inativar marca como inativo e e idempotente`() {
        val inativo = item().inativar()

        assertThat(inativo.ativo).isFalse()
        assertThat(inativo.inativar()).isSameAs(inativo)
    }

    @Test
    fun `item com reservas ativas nao pode ser inativado`() {
        val reservado = ItemEstoque.reconstituir(1, "Pastilha", null, "PECA", BigDecimal.TEN, 5, 1, true)

        assertThatThrownBy { reservado.inativar() }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Item possui reservas ativas e não pode ser inativado")
    }

    @Test
    fun `reconstitui um item persistido`() {
        val item = ItemEstoque.reconstituir(9, "Oleo", "5W30", "INSUMO", BigDecimal("30.00"), 8, 2, false)

        assertThat(item.id).isEqualTo(9)
        assertThat(item.tipo).isEqualTo(TipoItem.INSUMO)
        assertThat(item.disponivel).isEqualTo(6)
        assertThat(item.ativo).isFalse()
    }
}
