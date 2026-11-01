package com.fiap.soat.oficina.estoque.domain.event

import com.fiap.soat.oficina.shared.domain.event.DomainEvent

data class PecasBaixadas(val osId: Long, val itens: List<ItemBaixado>) : DomainEvent {
    data class ItemBaixado(val itemId: Long, val quantidade: Int)
}
