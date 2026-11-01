package com.fiap.soat.oficina.estoque.domain.event

import com.fiap.soat.oficina.shared.domain.event.DomainEvent

data class PecaIndisponivel(
    val osId: Long,
    val itemId: Long,
    val quantidadeSolicitada: Int,
    val disponivel: Int,
) : DomainEvent
