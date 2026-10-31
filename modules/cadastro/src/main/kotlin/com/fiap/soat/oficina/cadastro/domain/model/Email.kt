package com.fiap.soat.oficina.cadastro.domain.model

@JvmInline
value class Email private constructor(val valor: String) {

    companion object {
        private const val MAXIMO = 150
        private val PADRAO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun of(entrada: String): Email {
            val normalizado = entrada.trim().lowercase()
            require(PADRAO.matches(normalizado)) { "E-mail inválido" }
            require(normalizado.length <= MAXIMO) { "E-mail deve ter no máximo $MAXIMO caracteres" }
            return Email(normalizado)
        }
    }
}
