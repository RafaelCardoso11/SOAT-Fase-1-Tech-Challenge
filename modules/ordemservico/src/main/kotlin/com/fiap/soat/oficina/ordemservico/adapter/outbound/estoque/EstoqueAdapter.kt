package com.fiap.soat.oficina.ordemservico.adapter.outbound.estoque

import com.fiap.soat.oficina.estoque.domain.model.ResultadoReserva
import com.fiap.soat.oficina.estoque.domain.port.inbound.BaixarPecasUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.LiberarReservaUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.ReservarPecaUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import org.springframework.stereotype.Component

@Component
class EstoqueAdapter(
    private val reservarPeca: ReservarPecaUseCase,
    private val liberarReserva: LiberarReservaUseCase,
    private val baixarPecas: BaixarPecasUseCase,
) : EstoquePort {

    override fun reservar(ordemServicoId: Long, itemEstoqueId: Long, quantidade: Int): Boolean =
        reservarPeca.reservar(ordemServicoId, itemEstoqueId, quantidade) is ResultadoReserva.Reservada

    override fun liberarReservas(ordemServicoId: Long) = liberarReserva.liberar(ordemServicoId)

    override fun baixarPecas(ordemServicoId: Long) {
        baixarPecas.baixar(ordemServicoId)
    }
}
