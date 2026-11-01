package com.fiap.soat.oficina.estoque.domain.event

import com.fiap.soat.oficina.shared.domain.event.DomainEvent

data class ReservaLiberada(val osId: Long, val itemId: Long, val quantidade: Int) : DomainEvent
