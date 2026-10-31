package com.fiap.soat.oficina.ordemservico.domain.port.outbound

interface PublicadorDeEventos {
    fun publicar(evento: Any)
}
