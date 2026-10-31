package com.fiap.soat.oficina.cadastro.domain.port.inbound

import com.fiap.soat.oficina.cadastro.domain.model.Cliente

/** Com [somenteAtivos], um cliente inativo é tratado como não encontrado (FR-020). */
interface ConsultarClienteUseCase {
    fun buscarPorId(id: Long, somenteAtivos: Boolean = false): Cliente
    fun buscarPorDocumento(documento: String, somenteAtivos: Boolean = false): Cliente
    fun listar(ativo: Boolean? = null): List<Cliente>
}
