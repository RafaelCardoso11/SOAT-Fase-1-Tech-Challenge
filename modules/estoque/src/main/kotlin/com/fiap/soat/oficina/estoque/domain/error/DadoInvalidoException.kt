package com.fiap.soat.oficina.estoque.domain.error

class DadoInvalidoException(val campo: String, mensagem: String) : EstoqueException(mensagem)
