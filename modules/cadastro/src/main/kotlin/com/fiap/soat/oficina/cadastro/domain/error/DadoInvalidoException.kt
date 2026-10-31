package com.fiap.soat.oficina.cadastro.domain.error

class DadoInvalidoException(val campo: String, mensagem: String) : CadastroException(mensagem)
