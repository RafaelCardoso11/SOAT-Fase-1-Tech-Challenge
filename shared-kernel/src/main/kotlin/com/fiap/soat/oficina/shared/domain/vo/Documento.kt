package com.fiap.soat.oficina.shared.domain.vo

@JvmInline
value class Documento private constructor(val valor: String) {

    companion object {
        fun of(entrada: String): Documento {
            val digitos = entrada.filter(Char::isDigit)
            val valido = when (digitos.length) {
                11 -> cpfValido(digitos)
                14 -> cnpjValido(digitos)
                else -> false
            }
            require(valido) { "CPF/CNPJ inválido" }
            return Documento(digitos)
        }

        private fun cpfValido(d: String): Boolean {
            if (d.all { it == d[0] }) return false
            val dv1 = dv(d.substring(0, 9), intArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2))
            val dv2 = dv(d.substring(0, 9) + dv1, intArrayOf(11, 10, 9, 8, 7, 6, 5, 4, 3, 2))
            return d == d.substring(0, 9) + dv1 + dv2
        }

        private fun cnpjValido(d: String): Boolean {
            if (d.all { it == d[0] }) return false
            val dv1 = dv(d.substring(0, 12), intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
            val dv2 = dv(d.substring(0, 12) + dv1, intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
            return d == d.substring(0, 12) + dv1 + dv2
        }

        private fun dv(base: String, pesos: IntArray): Int {
            val resto = base.mapIndexed { i, c -> (c - '0') * pesos[i] }.sum() % 11
            return if (resto < 2) 0 else 11 - resto
        }
    }
}
