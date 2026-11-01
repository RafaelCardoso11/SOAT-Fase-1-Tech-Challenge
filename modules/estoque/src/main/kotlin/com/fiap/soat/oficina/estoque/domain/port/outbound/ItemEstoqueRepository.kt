package com.fiap.soat.oficina.estoque.domain.port.outbound

import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque

interface ItemEstoqueRepository {
    fun salvar(item: ItemEstoque): ItemEstoque
    fun buscarPorId(id: Long): ItemEstoque?

    /** Carrega o item com lock de escrita, para serializar entradas e reservas concorrentes. */
    fun buscarPorIdParaAtualizar(id: Long): ItemEstoque?
    fun existePorNome(nome: String, ignorandoId: Long? = null): Boolean
    fun listar(ativo: Boolean?): List<ItemEstoque>
}
