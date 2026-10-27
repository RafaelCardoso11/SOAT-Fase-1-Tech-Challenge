package com.fiap.soat.oficina.autenticacao.seguranca

import com.fiap.soat.oficina.autenticacao.adapter.outbound.seguranca.BCryptCodificadorDeSenha
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BCryptCodificadorDeSenhaTest {

    private val codificador = BCryptCodificadorDeSenha()

    @Test
    fun `confere verdadeiro para a propria senha`() {
        val hash = codificador.codificar("segredo")

        assertThat(codificador.confere("segredo", hash)).isTrue()
    }

    @Test
    fun `confere falso para senha diferente`() {
        val hash = codificador.codificar("segredo")

        assertThat(codificador.confere("outra", hash)).isFalse()
    }
}
