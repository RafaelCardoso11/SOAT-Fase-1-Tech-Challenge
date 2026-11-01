package com.fiap.soat.oficina.estoque.application.usecase

import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException
import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository

class ConsultarItemService(
    private val itens: ItemEstoqueRepository,
) : ConsultarItemUseCase {

    override fun buscarPorId(id: Long, somenteAtivos: Boolean): ItemEstoque =
        itens.buscarPorId(id)?.takeIf { !somenteAtivos || it.ativo }
            ?: throw RegistroNaoEncontradoException("Item de estoque não encontrado")

    override fun listar(ativo: Boolean?): List<ItemEstoque> = itens.listar(ativo)
}
