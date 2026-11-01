package com.fiap.soat.oficina.estoque.adapter.outbound.event

import com.fiap.soat.oficina.estoque.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.shared.domain.event.DomainEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class PublicadorDeEventosEstoque(
    private val publisher: ApplicationEventPublisher,
) : PublicadorDeEventos {

    override fun publicar(evento: DomainEvent) = publisher.publishEvent(evento)
}
