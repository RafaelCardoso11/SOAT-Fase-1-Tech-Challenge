package com.fiap.soat.oficina.autenticacao.domain.model

class TokenEmitido(
    val token: String,
    val expiraEmSegundos: Long,
)
