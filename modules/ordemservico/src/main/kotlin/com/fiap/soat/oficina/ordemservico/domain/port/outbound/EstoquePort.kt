package com.fiap.soat.oficina.ordemservico.domain.port.outbound

interface EstoquePort {
    fun reservar(descricaoPeca: String)
}
