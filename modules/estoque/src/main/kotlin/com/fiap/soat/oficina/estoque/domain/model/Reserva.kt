package com.fiap.soat.oficina.estoque.domain.model

import com.fiap.soat.oficina.estoque.domain.error.ConflitoException

class Reserva private constructor(
    val id: Long?,
    val osId: Long,
    val itemId: Long,
    val quantidade: Quantidade,
    val situacao: SituacaoReserva,
) {

    val ativa: Boolean get() = situacao == SituacaoReserva.ATIVA

    fun liberar(): Reserva = mudarPara(SituacaoReserva.LIBERADA)

    fun consumir(): Reserva = mudarPara(SituacaoReserva.CONSUMIDA)

    private fun mudarPara(nova: SituacaoReserva): Reserva {
        if (!ativa) throw ConflitoException("Reserva não está ativa e não pode ser alterada")
        return Reserva(id, osId, itemId, quantidade, nova)
    }

    companion object {
        fun criar(osId: Long, itemId: Long, quantidade: Quantidade): Reserva =
            Reserva(null, osId, itemId, quantidade, SituacaoReserva.ATIVA)

        fun reconstituir(id: Long, osId: Long, itemId: Long, quantidade: Int, situacao: String): Reserva =
            Reserva(id, osId, itemId, Quantidade.of(quantidade), SituacaoReserva.valueOf(situacao))
    }
}
