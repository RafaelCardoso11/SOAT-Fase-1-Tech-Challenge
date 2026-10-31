package com.fiap.soat.oficina.cadastro.domain.port.inbound

import com.fiap.soat.oficina.cadastro.domain.model.Cliente

interface GerenciarClienteUseCase {
    fun cadastrar(documento: String?, nome: String?, email: String?): Cliente
    fun alterar(id: Long, nome: String?, email: String?): Cliente
    fun inativar(id: Long)
}
