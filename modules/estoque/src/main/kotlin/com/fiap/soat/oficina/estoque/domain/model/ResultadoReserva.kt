package com.fiap.soat.oficina.estoque.domain.model

sealed interface ResultadoReserva {
    data class Reservada(val reserva: Reserva) : ResultadoReserva

    data class Indisponivel(val disponivel: Int) : ResultadoReserva
}
