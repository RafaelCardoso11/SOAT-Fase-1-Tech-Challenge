package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface RevisarOrcamentoUseCase {
    fun revisar(ordemServicoId: Long, itens: List<ItemOrcamento>): OrdemServico
}
