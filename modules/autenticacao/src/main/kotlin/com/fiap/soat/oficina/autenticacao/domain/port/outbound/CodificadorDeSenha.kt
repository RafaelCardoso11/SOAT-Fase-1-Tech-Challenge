package com.fiap.soat.oficina.autenticacao.domain.port.outbound

interface CodificadorDeSenha {
    fun codificar(senhaPura: String): String
    fun confere(senhaPura: String, hash: String): Boolean
}
