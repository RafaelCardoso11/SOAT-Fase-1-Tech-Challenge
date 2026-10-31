package com.fiap.soat.oficina.cadastro.domain.error

sealed class CadastroException(mensagem: String) : RuntimeException(mensagem)
