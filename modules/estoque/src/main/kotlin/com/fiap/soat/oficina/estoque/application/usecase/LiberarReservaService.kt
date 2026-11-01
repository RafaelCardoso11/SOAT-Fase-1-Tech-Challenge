package com.fiap.soat.oficina.estoque.application.usecase

import com.fiap.soat.oficina.estoque.domain.event.ReservaLiberada
import com.fiap.soat.oficina.estoque.domain.port.inbound.LiberarReservaUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import com.fiap.soat.oficina.estoque.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository
import org.springframework.transaction.annotation.Transactional

@Transactional
class LiberarReservaService(
    private val itens: ItemEstoqueRepository,
    private val reservas: ReservaRepository,
    private val eventos: PublicadorDeEventos,
) : LiberarReservaUseCase {

    override fun liberar(osId: Long, itemId: Long?) {
        reservas.listarAtivasPorOs(osId)
            .filter { itemId == null || it.itemId == itemId }
            .emOrdemDeBloqueio()
            .forEach { reserva ->
                itens.salvar(itens.obterParaAtualizar(reserva.itemId).liberar(reserva.quantidade))
                reservas.salvar(reserva.liberar())
                eventos.publicar(ReservaLiberada(osId, reserva.itemId, reserva.quantidade.valor))
            }
    }
}
