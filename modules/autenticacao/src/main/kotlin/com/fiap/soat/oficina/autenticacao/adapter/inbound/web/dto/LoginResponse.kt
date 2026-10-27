package com.fiap.soat.oficina.autenticacao.adapter.inbound.web.dto

data class LoginResponse(val token: String, val tipo: String, val expiraEmSegundos: Long)
