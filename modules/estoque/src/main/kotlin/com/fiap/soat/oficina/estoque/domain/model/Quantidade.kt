package com.fiap.soat.oficina.estoque.domain.model

import com.fiap.soat.oficina.estoque.domain.error.DadoInvalidoException

@JvmInline
value class Quantidade private constructor(val valor: Int) {

    companion object {
        fun of(entrada: Int?): Quantidade {
            if (entrada == null) throw DadoInvalidoException("quantidade", "Quantidade é obrigatória")
            if (entrada <= 0) throw DadoInvalidoException("quantidade", "Quantidade deve ser maior que zero")
            return Quantidade(entrada)
        }
    }
}
