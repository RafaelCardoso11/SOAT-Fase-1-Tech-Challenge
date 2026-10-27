package com.fiap.soat.oficina.autenticacao.domain.port.inbound

import com.fiap.soat.oficina.autenticacao.domain.model.TokenEmitido

interface AutenticarUseCase {
    fun autenticar(nomeUsuario: String, senha: String): TokenEmitido
}
