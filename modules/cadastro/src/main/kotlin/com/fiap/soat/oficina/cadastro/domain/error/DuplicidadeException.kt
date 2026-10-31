package com.fiap.soat.oficina.cadastro.domain.error

class DuplicidadeException(val campo: String, mensagem: String) : CadastroException(mensagem)
