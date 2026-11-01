package com.fiap.soat.oficina.estoque.domain.error

sealed class EstoqueException(mensagem: String) : RuntimeException(mensagem)
