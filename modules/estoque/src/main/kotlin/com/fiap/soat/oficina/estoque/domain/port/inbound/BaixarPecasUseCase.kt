package com.fiap.soat.oficina.estoque.domain.port.inbound

import com.fiap.soat.oficina.estoque.domain.model.Reserva

/** Baixa tudo ou nada; devolve as reservas consumidas, vazia se a OS não tem reservas ativas. */
interface BaixarPecasUseCase {
    fun baixar(osId: Long): List<Reserva>
}
