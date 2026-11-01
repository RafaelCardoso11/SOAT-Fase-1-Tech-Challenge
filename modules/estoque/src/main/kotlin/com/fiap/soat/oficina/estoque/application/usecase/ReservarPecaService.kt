package com.fiap.soat.oficina.estoque.application.usecase

import com.fiap.soat.oficina.estoque.domain.error.ConflitoException
import com.fiap.soat.oficina.estoque.domain.error.DadoInvalidoException
import com.fiap.soat.oficina.estoque.domain.event.PecaIndisponivel
import com.fiap.soat.oficina.estoque.domain.event.PecaReservada
import com.fiap.soat.oficina.estoque.domain.model.Quantidade
import com.fiap.soat.oficina.estoque.domain.model.Reserva
import com.fiap.soat.oficina.estoque.domain.model.ResultadoReserva
import com.fiap.soat.oficina.estoque.domain.port.inbound.ReservarPecaUseCase
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import com.fiap.soat.oficina.estoque.domain.port.outbound.PublicadorDeEventos
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository
import org.springframework.transaction.annotation.Transactional

@Transactional
class ReservarPecaService(
    private val itens: ItemEstoqueRepository,
    private val reservas: ReservaRepository,
    private val eventos: PublicadorDeEventos,
) : ReservarPecaUseCase {

    override fun reservar(osId: Long, itemId: Long?, quantidade: Int?): ResultadoReserva {
        val id = itemId ?: throw DadoInvalidoException("itemId", "Item é obrigatório")
        val solicitada = Quantidade.of(quantidade)
        val item = itens.obterParaAtualizar(id)
        if (!item.ativo) throw ConflitoException("Item inativo não pode ser reservado")
        if (reservas.buscarAtivaPorOsEItem(osId, id) != null) {
            throw ConflitoException("A OS já possui uma reserva ativa para este item")
        }
        if (!item.comporta(solicitada)) {
            eventos.publicar(PecaIndisponivel(osId, id, solicitada.valor, item.disponivel))
            return ResultadoReserva.Indisponivel(item.disponivel)
        }
        itens.salvar(item.reservar(solicitada))
        val reserva = reservas.salvar(Reserva.criar(osId, id, solicitada))
        eventos.publicar(PecaReservada(osId, id, solicitada.valor))
        return ResultadoReserva.Reservada(reserva)
    }
}
