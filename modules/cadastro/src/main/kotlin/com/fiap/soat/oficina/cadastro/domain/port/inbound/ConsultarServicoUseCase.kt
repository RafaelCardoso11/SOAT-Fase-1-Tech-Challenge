package com.fiap.soat.oficina.cadastro.domain.port.inbound

import com.fiap.soat.oficina.cadastro.domain.model.Servico

/** Com [somenteAtivos], um serviço inativo é tratado como não encontrado (FR-020). */
interface ConsultarServicoUseCase {
    fun buscarPorId(id: Long, somenteAtivos: Boolean = false): Servico
    fun listar(ativo: Boolean? = null): List<Servico>
}
