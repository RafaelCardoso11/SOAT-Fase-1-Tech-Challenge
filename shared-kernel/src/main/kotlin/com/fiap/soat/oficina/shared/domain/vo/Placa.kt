package com.fiap.soat.oficina.shared.domain.vo

@JvmInline
value class Placa private constructor(val valor: String) {

    companion object {
        private val PADRAO = Regex("^[A-Z]{3}[0-9][0-9A-Z][0-9]{2}$")

        fun of(entrada: String): Placa {
            val normalizada = entrada.filter(Char::isLetterOrDigit).uppercase()
            require(PADRAO.matches(normalizada)) { "Placa inválida" }
            return Placa(normalizada)
        }
    }
}
