package com.fiap.soat.oficina.ordemservico.domain.port.outbound

import com.fiap.soat.oficina.shared.domain.vo.Dinheiro

data class ItemCatalogo(val nome: String, val preco: Dinheiro)

interface CatalogoPort {
    fun servico(id: Long): ItemCatalogo
    fun itemEstoque(id: Long): ItemCatalogo
}
