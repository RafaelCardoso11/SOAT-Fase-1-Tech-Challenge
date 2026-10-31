package com.fiap.soat.oficina.ordemservico.domain.port.outbound

import com.fiap.soat.oficina.shared.domain.event.DomainEvent

interface PublicadorDeEventos {
    fun publicar(evento: DomainEvent)
}
