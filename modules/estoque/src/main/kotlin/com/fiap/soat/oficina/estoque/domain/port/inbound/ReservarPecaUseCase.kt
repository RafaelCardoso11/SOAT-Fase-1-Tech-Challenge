package com.fiap.soat.oficina.estoque.domain.port.inbound

import com.fiap.soat.oficina.estoque.domain.model.ResultadoReserva

/** Sem disponibilidade, devolve `Indisponivel` em vez de lançar exceção: é um desfecho esperado do fluxo da OS. */
interface ReservarPecaUseCase {
    fun reservar(osId: Long, itemId: Long?, quantidade: Int?): ResultadoReserva
}
