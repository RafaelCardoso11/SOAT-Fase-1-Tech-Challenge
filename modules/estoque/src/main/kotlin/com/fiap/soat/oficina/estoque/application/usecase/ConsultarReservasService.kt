package com.fiap.soat.oficina.estoque.application.usecase

import com.fiap.soat.oficina.estoque.domain.model.Reserva
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarReservasUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository

class ConsultarReservasService(
    private val reservas: ReservaRepository,
) : ConsultarReservasUseCase {

    override fun porOs(osId: Long): List<Reserva> = reservas.listarPorOs(osId)
}
