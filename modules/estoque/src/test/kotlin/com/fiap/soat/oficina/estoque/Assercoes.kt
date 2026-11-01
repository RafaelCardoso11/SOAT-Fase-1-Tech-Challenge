package com.fiap.soat.oficina.estoque

import com.fiap.soat.oficina.estoque.domain.error.DadoInvalidoException
import com.fiap.soat.oficina.estoque.domain.error.DuplicidadeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

internal fun assertDadoInvalido(campo: String, mensagem: String, bloco: () -> Unit) {
    assertThatThrownBy { bloco() }.isInstanceOfSatisfying(DadoInvalidoException::class.java) {
        assertThat(it.campo).isEqualTo(campo)
        assertThat(it.message).isEqualTo(mensagem)
    }
}

internal fun assertDuplicidade(campo: String, bloco: () -> Unit) {
    assertThatThrownBy { bloco() }.isInstanceOfSatisfying(DuplicidadeException::class.java) {
        assertThat(it.campo).isEqualTo(campo)
    }
}
