package com.fiap.soat.oficina.estoque.domain.port.inbound

import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque

/** Com [somenteAtivos], um item inativo é tratado como não encontrado (FR-009). */
interface ConsultarItemUseCase {
    fun buscarPorId(id: Long, somenteAtivos: Boolean = false): ItemEstoque
    fun listar(ativo: Boolean? = null): List<ItemEstoque>
}
