package com.fiap.soat.oficina.estoque.domain.port.inbound

/** Com [itemId] libera só a reserva desse item; sem ele, todas as reservas ativas da OS. Idempotente. */
interface LiberarReservaUseCase {
    fun liberar(osId: Long, itemId: Long? = null)
}
