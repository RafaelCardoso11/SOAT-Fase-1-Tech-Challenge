package com.fiap.soat.oficina.autenticacao.domain.model

class Usuario(
    val id: Long?,
    val nomeUsuario: String,
    val senhaHash: String,
    val ativo: Boolean,
)
