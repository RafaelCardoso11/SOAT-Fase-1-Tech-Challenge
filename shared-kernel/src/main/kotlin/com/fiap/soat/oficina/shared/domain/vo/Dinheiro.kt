package com.fiap.soat.oficina.shared.domain.vo

import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class Dinheiro private constructor(val valor: BigDecimal) {

    operator fun plus(outro: Dinheiro): Dinheiro = Dinheiro(valor + outro.valor)

    operator fun times(quantidade: Int): Dinheiro {
        require(quantidade >= 0) { "Quantidade não pode ser negativa" }
        return Dinheiro(valor.multiply(BigDecimal(quantidade)).setScale(2, RoundingMode.HALF_UP))
    }

    companion object {
        fun of(valor: BigDecimal): Dinheiro {
            require(valor.signum() >= 0) { "Dinheiro não pode ser negativo" }
            return Dinheiro(valor.setScale(2, RoundingMode.HALF_UP))
        }
    }
}
