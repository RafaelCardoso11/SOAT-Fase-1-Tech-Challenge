package com.fiap.soat.oficina.cadastro.domain.model

import com.fiap.soat.oficina.cadastro.assertDadoInvalido
import com.fiap.soat.oficina.cadastro.domain.error.ConflitoException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VeiculoTest {

    private val limite = 2027

    private fun veiculo() = Veiculo.criar("ABC1D23", "Fiat", "Uno", 2020, 1L, limite)

    @Test
    fun `cria veiculo ativo normalizando a placa`() {
        val veiculo = Veiculo.criar("abc-1d23", " Fiat ", " Uno ", 2020, 1L, limite)

        assertThat(veiculo.id).isNull()
        assertThat(veiculo.placa.valor).isEqualTo("ABC1D23")
        assertThat(veiculo.marca).isEqualTo("Fiat")
        assertThat(veiculo.modelo).isEqualTo("Uno")
        assertThat(veiculo.ano).isEqualTo(2020)
        assertThat(veiculo.clienteId).isEqualTo(1L)
        assertThat(veiculo.ativo).isTrue()
    }

    @Test
    fun `aceita placa no formato antigo`() {
        assertThat(Veiculo.criar("ABC-1234", "Fiat", "Uno", 2010, 1L, limite).placa.valor).isEqualTo("ABC1234")
    }

    @Test
    fun `placa ausente e obrigatoria`() {
        assertDadoInvalido("placa", "Placa é obrigatória") { Veiculo.criar(null, "Fiat", "Uno", 2020, 1L, limite) }
        assertDadoInvalido("placa", "Placa é obrigatória") { Veiculo.criar(" ", "Fiat", "Uno", 2020, 1L, limite) }
    }

    @Test
    fun `placa invalida e rejeitada`() {
        assertDadoInvalido("placa", "Placa inválida") { Veiculo.criar("12345", "Fiat", "Uno", 2020, 1L, limite) }
    }

    @Test
    fun `marca e modelo sao obrigatorios`() {
        assertDadoInvalido("marca", "Marca é obrigatória") { Veiculo.criar("ABC1D23", null, "Uno", 2020, 1L, limite) }
        assertDadoInvalido("modelo", "Modelo é obrigatório") { Veiculo.criar("ABC1D23", "Fiat", " ", 2020, 1L, limite) }
    }

    @Test
    fun `marca acima de 60 caracteres e rejeitada`() {
        assertDadoInvalido("marca", "Marca deve ter no máximo 60 caracteres") {
            Veiculo.criar("ABC1D23", "a".repeat(61), "Uno", 2020, 1L, limite)
        }
    }

    @Test
    fun `ano ausente e obrigatorio`() {
        assertDadoInvalido("ano", "Ano é obrigatório") { Veiculo.criar("ABC1D23", "Fiat", "Uno", null, 1L, limite) }
    }

    @Test
    fun `ano aceita os limites 1900 e ano limite`() {
        assertThat(Veiculo.criar("ABC1D23", "Fiat", "Uno", 1900, 1L, limite).ano).isEqualTo(1900)
        assertThat(Veiculo.criar("ABC1D23", "Fiat", "Uno", limite, 1L, limite).ano).isEqualTo(limite)
    }

    @Test
    fun `ano fora do intervalo e rejeitado`() {
        val mensagem = "Ano deve estar entre 1900 e 2027"
        assertDadoInvalido("ano", mensagem) { Veiculo.criar("ABC1D23", "Fiat", "Uno", 1899, 1L, limite) }
        assertDadoInvalido("ano", mensagem) { Veiculo.criar("ABC1D23", "Fiat", "Uno", 2028, 1L, limite) }
    }

    @Test
    fun `cliente e obrigatorio`() {
        assertDadoInvalido("clienteId", "Cliente é obrigatório") {
            Veiculo.criar("ABC1D23", "Fiat", "Uno", 2020, null, limite)
        }
    }

    @Test
    fun `altera dados e dono sem mudar a placa`() {
        val original = Veiculo.reconstituir(5, "ABC1D23", "Fiat", "Uno", 2020, 1L, true)

        val alterado = original.alterar("VW", "Gol", 2021, 2L, limite)

        assertThat(alterado.id).isEqualTo(5)
        assertThat(alterado.placa).isEqualTo(original.placa)
        assertThat(alterado.marca).isEqualTo("VW")
        assertThat(alterado.modelo).isEqualTo("Gol")
        assertThat(alterado.ano).isEqualTo(2021)
        assertThat(alterado.clienteId).isEqualTo(2L)
    }

    @Test
    fun `alteracao valida os campos`() {
        assertDadoInvalido("ano", "Ano deve estar entre 1900 e 2027") { veiculo().alterar("Fiat", "Uno", 2030, 1L, limite) }
        assertDadoInvalido("clienteId", "Cliente é obrigatório") { veiculo().alterar("Fiat", "Uno", 2020, null, limite) }
    }

    @Test
    fun `veiculo inativo nao pode ser alterado`() {
        val inativo = veiculo().inativar()

        assertThatThrownBy { inativo.alterar("VW", "Gol", 2021, 2L, limite) }
            .isInstanceOf(ConflitoException::class.java)
            .hasMessage("Veículo inativo não pode ser alterado")
    }

    @Test
    fun `inativar marca como inativo e e idempotente`() {
        val inativo = veiculo().inativar()

        assertThat(inativo.ativo).isFalse()
        assertThat(inativo.inativar()).isSameAs(inativo)
    }
}
