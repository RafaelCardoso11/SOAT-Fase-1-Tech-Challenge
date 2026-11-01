package com.fiap.soat.oficina.estoque.application.usecase

import com.fiap.soat.oficina.estoque.domain.event.PecasBaixadas
import com.fiap.soat.oficina.estoque.domain.model.Reserva
import com.fiap.soat.oficina.estoque.domain.port.inbound.BaixarPecasUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import com.fiap.soat.oficina.estoque.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository
import org.springframework.transaction.annotation.Transactional

@Transactional
class BaixarPecasService(
    private val itens: ItemEstoqueRepository,
    private val reservas: ReservaRepository,
    private val eventos: PublicadorDeEventos,
) : BaixarPecasUseCase {

    override fun baixar(osId: Long): List<Reserva> {
        val ativas = reservas.listarAtivasPorOs(osId).emOrdemDeBloqueio()
        if (ativas.isEmpty()) return emptyList()

        val consumidas = ativas.map { reserva ->
            itens.salvar(itens.obterParaAtualizar(reserva.itemId).baixar(reserva.quantidade))
            reservas.salvar(reserva.consumir())
        }
        eventos.publicar(
            PecasBaixadas(osId, consumidas.map { PecasBaixadas.ItemBaixado(it.itemId, it.quantidade.valor) }),
        )
        return consumidas
    }
}
