package com.fiap.soat.oficina.ordemservico.domain.port.outbound

interface EstoquePort {
    /** Reserva a peça para a OS; devolve false quando o item está indisponível (sem saldo). */
    fun reservar(ordemServicoId: Long, itemEstoqueId: Long, quantidade: Int): Boolean
    fun liberarReservas(ordemServicoId: Long)
    fun baixarPecas(ordemServicoId: Long)
}
