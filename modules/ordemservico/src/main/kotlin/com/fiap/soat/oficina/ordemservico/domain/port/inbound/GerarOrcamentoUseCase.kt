package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.ItemSolicitado
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface GerarOrcamentoUseCase {
    fun gerar(ordemServicoId: Long, itens: List<ItemSolicitado>): OrdemServico
}
