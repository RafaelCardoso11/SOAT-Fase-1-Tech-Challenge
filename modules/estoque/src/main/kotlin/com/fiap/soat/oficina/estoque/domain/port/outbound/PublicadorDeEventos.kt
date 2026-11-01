package com.fiap.soat.oficina.estoque.domain.port.outbound

import com.fiap.soat.oficina.shared.domain.event.DomainEvent

interface PublicadorDeEventos {
    fun publicar(evento: DomainEvent)
}
