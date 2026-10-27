package com.fiap.soat.oficina.autenticacao.domain.port.outbound

import com.fiap.soat.oficina.autenticacao.domain.model.TokenEmitido
import com.fiap.soat.oficina.autenticacao.domain.model.Usuario

interface TokenProvider {
    fun emitir(usuario: Usuario): TokenEmitido
    fun validar(token: String): String?
}
