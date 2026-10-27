package com.fiap.soat.oficina.shared.domain.vo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PlacaTest {

    @Test
    fun `aceita placa Mercosul e normaliza`() {
        assertThat(Placa.of("abc1d23").valor).isEqualTo("ABC1D23")
    }

    @Test
    fun `aceita placa no formato antigo`() {
        assertThat(Placa.of("ABC-1234").valor).isEqualTo("ABC1234")
    }

    @Test
    fun `rejeita placa invalida`() {
        assertThatThrownBy { Placa.of("12345") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
