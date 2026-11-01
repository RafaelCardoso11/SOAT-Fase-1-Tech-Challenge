package com.fiap.soat.oficina.estoque.domain.port.inbound

import com.fiap.soat.oficina.estoque.domain.model.Reserva

interface ConsultarReservasUseCase {
    fun porOs(osId: Long): List<Reserva>
}
