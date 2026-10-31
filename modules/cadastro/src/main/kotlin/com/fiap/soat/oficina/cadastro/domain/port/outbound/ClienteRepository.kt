package com.fiap.soat.oficina.cadastro.domain.port.outbound

import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import com.fiap.soat.oficina.shared.domain.vo.Documento

interface ClienteRepository {
    fun salvar(cliente: Cliente): Cliente
    fun buscarPorId(id: Long): Cliente?
    fun buscarPorDocumento(documento: Documento): Cliente?
    fun existePorDocumento(documento: Documento): Boolean
    fun listar(ativo: Boolean?): List<Cliente>
}
