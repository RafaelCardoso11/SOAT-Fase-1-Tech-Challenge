package com.fiap.soat.oficina.shared.domain.vo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DinheiroTest {

    @Test
    fun `cria com duas casas decimais`() {
        assertThat(Dinheiro.of(BigDecimal("10.5")).valor).isEqualByComparingTo("10.50")
    }

    @Test
    fun `soma dois valores`() {
        val total = Dinheiro.of(BigDecimal("150.00")) + Dinheiro.of(BigDecimal("200.00"))
        assertThat(total.valor).isEqualByComparingTo("350.00")
    }

    @Test
    fun `multiplica pela quantidade`() {
        assertThat((Dinheiro.of(BigDecimal("33.33")) * 3).valor).isEqualByComparingTo("99.99")
    }

    @Test
    fun `rejeita quantidade negativa`() {
        assertThatThrownBy { Dinheiro.of(BigDecimal("10")) * -1 }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejeita valor negativo`() {
        assertThatThrownBy { Dinheiro.of(BigDecimal("-1")) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
