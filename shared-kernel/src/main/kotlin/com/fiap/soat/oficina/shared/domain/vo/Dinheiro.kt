package com.fiap.soat.oficina.shared.domain.vo

import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class Dinheiro private constructor(val valor: BigDecimal) {

    operator fun plus(outro: Dinheiro): Dinheiro = Dinheiro(valor + outro.valor)

    companion object {
        fun of(valor: BigDecimal): Dinheiro {
            require(valor.signum() >= 0) { "Dinheiro não pode ser negativo" }
            return Dinheiro(valor.setScale(2, RoundingMode.HALF_UP))
        }
    }
}
