package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface RevisarOrcamentoUseCase {
    fun revisar(ordemServicoId: Long, itens: List<ItemSolicitado>): OrdemServico
}
