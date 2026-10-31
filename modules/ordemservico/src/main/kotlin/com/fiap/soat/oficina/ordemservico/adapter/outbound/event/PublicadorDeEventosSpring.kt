package com.fiap.soat.oficina.ordemservico.adapter.outbound.event

import com.fiap.soat.oficina.ordemservico.domain.port.outbound.PublicadorDeEventos
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class PublicadorDeEventosSpring(
    private val publisher: ApplicationEventPublisher,
) : PublicadorDeEventos {

    override fun publicar(evento: Any) = publisher.publishEvent(evento)
}
