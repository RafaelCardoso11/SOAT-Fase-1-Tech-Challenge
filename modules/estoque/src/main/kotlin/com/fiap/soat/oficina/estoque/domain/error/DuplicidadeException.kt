package com.fiap.soat.oficina.estoque.domain.error

class DuplicidadeException(val campo: String, mensagem: String) : EstoqueException(mensagem)
