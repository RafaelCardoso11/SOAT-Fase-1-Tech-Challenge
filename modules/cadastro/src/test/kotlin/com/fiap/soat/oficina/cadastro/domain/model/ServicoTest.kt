package com.fiap.soat.oficina.cadastro.domain.model

import com.fiap.soat.oficina.cadastro.assertDadoInvalido
import com.fiap.soat.oficina.cadastro.domain.error.ConflitoException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ServicoTest {

    private fun servico() = Servico.criar("Troca de óleo", "Óleo e filtro", BigDecimal("150"))

    @Test
    fun `cria servico ativo com preco de duas casas`() {
        val servico = Servico.criar(" Troca de óleo ", " Óleo e filtro ", BigDecimal("150"))

        assertThat(servico.id).isNull()
        assertThat(servico.nome).isEqualTo("Troca de óleo")
        assertThat(servico.descricao).isEqualTo("Óleo e filtro")
        assertThat(servico.preco.valor).isEqualByComparingTo("150.00")
        assertThat(servico.preco.valor.scale()).isEqualTo(2)
        assertThat(servico.ativo).isTrue()
    }

    @Test
    fun `descricao e opcional e vazia vira nula`() {
        assertThat(Servico.criar("Alinhamento", null, BigDecimal.TEN).descricao).isNull()
        assertThat(Servico.criar("Alinhamento", "   ", BigDecimal.TEN).descricao).isNull()
    }

    @Test
    fun `preco zero e aceito`() {
        assertThat(Servico.criar("Cortesia", null, BigDecimal.ZERO).preco.valor).isEqualByComparingTo("0")
    }

    @Test
    fun `nome ausente ou em branco e obrigatorio`() {
        assertDadoInvalido("nome", "Nome é obrigatório") { Servico.criar(null, null, BigDecimal.TEN) }
        assertDadoInvalido("nome", "Nome é obrigatório") { Servico.criar("  ", null, BigDecimal.TEN) }
    }

    @Test
    fun `nome acima de 120 caracteres e rejeitado`() {
        assertDadoInvalido("nome", "Nome deve ter no máximo 120 caracteres") {
            Servico.criar("a".repeat(121), null, BigDecimal.TEN)
        }
    }

    @Test
    fun `descricao acima de 500 caracteres e rejeitada`() {
        assertDadoInvalido("descricao", "Descrição deve ter no máximo 500 caracteres") {
            Servico.criar("Alinhamento", "a".repeat(501), BigDecimal.TEN)
        }
    }

    @Test
    fun `preco ausente e obrigatorio`() {
        assertDadoInvalido("preco", "Preço é obrigatório") { Servico.criar("Alinhamento", null, null) }
    }

    @Test
    fun `preco negativo e rejeitado`() {
        assertDadoInvalido("preco", "Preço não pode ser negativo") {
            Servico.criar("Alinhamento", null, BigDecimal("-0.01"))
        }
    }

    @Test
    fun `altera nome, descricao e preco`() {
        val original = Servico.reconstituir(4, "Alinhamento", null, BigDecimal("80.00"), true)

        val alterado = original.alterar("Alinhamento 3D", "Computadorizado", BigDecimal("120.5"))

        assertThat(alterado.id).isEqualTo(4)
        assertThat(alterado.nome).isEqualTo("Alinhamento 3D")
        assertThat(alterado.descricao).isEqualTo("Computadorizado")
        assertThat(alterado.preco.valor).isEqualByComparingTo("120.50")
        assertThat(original.preco.valor).isEqualByComparingTo("80.00")
    }

    @Test
    fun `alteracao valida os campos`() {
        assertDadoInvalido("nome", "Nome é obrigatório") { servico().alterar(null, null, BigDecimal.TEN) }
        assertDadoInvalido("preco", "Preço é obrigatório") { servico().alterar("Alinhamento", null, null) }
    }

    @Test
    fun `servico inativo nao pode ser alterado`() {
        val inativo = servico().inativar()

        assertThatThrownBy { inativo.alterar("Outro", null, BigDecimal.TEN) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Serviço inativo não pode ser alterado")
    }

    @Test
    fun `inativar marca como inativo e e idempotente`() {
        val inativo = servico().inativar()

        assertThat(inativo.ativo).isFalse()
        assertThat(inativo.inativar()).isSameAs(inativo)
    }
}
