package com.fiap.soat.oficina.estoque.domain.model

import com.fiap.soat.oficina.estoque.assertDadoInvalido
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class QuantidadeTest {

    @Test
    fun `aceita inteiro positivo`() {
        assertThat(Quantidade.of(1).valor).isEqualTo(1)
        assertThat(Quantidade.of(250).valor).isEqualTo(250)
    }

    @Test
    fun `quantidade ausente e obrigatoria`() {
        assertDadoInvalido("quantidade", "Quantidade é obrigatória") { Quantidade.of(null) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, -100])
    fun `zero e negativo sao rejeitados`(entrada: Int) {
        assertDadoInvalido("quantidade", "Quantidade deve ser maior que zero") { Quantidade.of(entrada) }
    }
}
