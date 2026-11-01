package com.fiap.soat.oficina.estoque.domain.model

import com.fiap.soat.oficina.estoque.domain.error.ConflitoException
import com.fiap.soat.oficina.estoque.domain.error.DadoInvalidoException

internal inline fun <T> validando(campo: String, mensagem: String? = null, bloco: () -> T): T =
    try {
        bloco()
    } catch (e: IllegalArgumentException) {
        throw DadoInvalidoException(campo, mensagem ?: e.message ?: "Valor inválido")
    }

internal fun textoObrigatorio(
    campo: String,
    rotulo: String,
    valor: String?,
    maximo: Int,
    feminino: Boolean = false,
): String {
    val texto = valor?.trim().orEmpty()
    if (texto.isEmpty()) {
        throw DadoInvalidoException(campo, "$rotulo é obrigatór${if (feminino) "ia" else "io"}")
    }
    if (texto.length > maximo) {
        throw DadoInvalidoException(campo, "$rotulo deve ter no máximo $maximo caracteres")
    }
    return texto
}

internal fun exigirAtivo(ativo: Boolean, mensagem: String) {
    if (!ativo) throw ConflitoException(mensagem)
}
