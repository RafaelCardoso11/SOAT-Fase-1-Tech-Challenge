package com.fiap.soat.oficina.cadastro.domain.model

import com.fiap.soat.oficina.cadastro.assertDadoInvalido
import com.fiap.soat.oficina.cadastro.domain.error.ConflitoException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ClienteTest {

    private fun cliente() = Cliente.criar("123.456.789-09", "Maria Silva", "maria@email.com")

    @Test
    fun `cria cliente ativo normalizando documento, nome e e-mail`() {
        val cliente = Cliente.criar("123.456.789-09", "  Maria Silva ", " Maria@Email.com ")

        assertThat(cliente.id).isNull()
        assertThat(cliente.documento.valor).isEqualTo("12345678909")
        assertThat(cliente.nome).isEqualTo("Maria Silva")
        assertThat(cliente.email.valor).isEqualTo("maria@email.com")
        assertThat(cliente.ativo).isTrue()
    }

    @Test
    fun `aceita CNPJ`() {
        assertThat(Cliente.criar("11.222.333/0001-81", "Oficina Ltda", "contato@oficina.com").documento.valor)
            .isEqualTo("11222333000181")
    }

    @Test
    fun `documento ausente e obrigatorio`() {
        assertDadoInvalido("documento", "CPF/CNPJ é obrigatório") { Cliente.criar(null, "Maria", "maria@email.com") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `documento em branco e obrigatorio`(documento: String) {
        assertDadoInvalido("documento", "CPF/CNPJ é obrigatório") { Cliente.criar(documento, "Maria", "m@e.com") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["111.111.111-11", "123.456.789-00", "123"])
    fun `documento invalido e rejeitado`(documento: String) {
        assertDadoInvalido("documento", "CPF/CNPJ inválido") { Cliente.criar(documento, "Maria", "m@e.com") }
    }

    @Test
    fun `nome ausente ou em branco e obrigatorio`() {
        assertDadoInvalido("nome", "Nome é obrigatório") { Cliente.criar("123.456.789-09", null, "m@e.com") }
        assertDadoInvalido("nome", "Nome é obrigatório") { Cliente.criar("123.456.789-09", "  ", "m@e.com") }
    }

    @Test
    fun `nome acima de 150 caracteres e rejeitado`() {
        assertDadoInvalido("nome", "Nome deve ter no máximo 150 caracteres") {
            Cliente.criar("123.456.789-09", "a".repeat(151), "m@e.com")
        }
    }

    @Test
    fun `e-mail ausente e obrigatorio`() {
        assertDadoInvalido("email", "E-mail é obrigatório") { Cliente.criar("123.456.789-09", "Maria", null) }
    }

    @Test
    fun `e-mail invalido e rejeitado`() {
        assertDadoInvalido("email", "E-mail inválido") { Cliente.criar("123.456.789-09", "Maria", "invalido") }
    }

    @Test
    fun `altera nome e e-mail sem mudar documento nem identificador`() {
        val original = Cliente.reconstituir(7, "12345678909", "Maria", "maria@email.com", true)

        val alterado = original.alterar("Maria Souza", "souza@email.com")

        assertThat(alterado.id).isEqualTo(7)
        assertThat(alterado.documento).isEqualTo(original.documento)
        assertThat(alterado.nome).isEqualTo("Maria Souza")
        assertThat(alterado.email.valor).isEqualTo("souza@email.com")
        assertThat(original.nome).isEqualTo("Maria")
    }

    @Test
    fun `alteracao valida os campos`() {
        assertDadoInvalido("nome", "Nome é obrigatório") { cliente().alterar(null, "m@e.com") }
        assertDadoInvalido("email", "E-mail inválido") { cliente().alterar("Maria", "x") }
    }

    @Test
    fun `cliente inativo nao pode ser alterado`() {
        val inativo = cliente().inativar()

        assertThatThrownBy { inativo.alterar("Outro", "outro@email.com") }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Cliente inativo não pode ser alterado")
    }

    @Test
    fun `inativar marca como inativo e e idempotente`() {
        val inativo = cliente().inativar()

        assertThat(inativo.ativo).isFalse()
        assertThat(inativo.inativar()).isSameAs(inativo)
    }

    @Test
    fun `reconstitui um cliente persistido`() {
        val cliente = Cliente.reconstituir(3, "12345678909", "Maria", "maria@email.com", false)

        assertThat(cliente.id).isEqualTo(3)
        assertThat(cliente.ativo).isFalse()
    }
}
