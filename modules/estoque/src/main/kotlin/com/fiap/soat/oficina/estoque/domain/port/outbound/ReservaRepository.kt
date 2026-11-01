package com.fiap.soat.oficina.estoque.domain.port.outbound

import com.fiap.soat.oficina.estoque.domain.model.Reserva

interface ReservaRepository {
    fun salvar(reserva: Reserva): Reserva
    fun buscarAtivaPorOsEItem(osId: Long, itemId: Long): Reserva?

    fun listarAtivasPorOs(osId: Long): List<Reserva>
    fun listarPorOs(osId: Long): List<Reserva>
}
