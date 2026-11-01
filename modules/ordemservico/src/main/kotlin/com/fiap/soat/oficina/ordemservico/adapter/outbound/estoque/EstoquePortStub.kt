package com.fiap.soat.oficina.ordemservico.adapter.outbound.estoque

import com.fiap.soat.oficina.ordemservico.domain.port.outbound.EstoquePort
import org.springframework.stereotype.Component

@Component
class EstoquePortStub : EstoquePort {
    // shortcut: integração real com o módulo estoque (reserva de peça, UC04) ainda não existe; no-op por ora.
    override fun reservar(descricaoPeca: String) = Unit
}
