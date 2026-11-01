package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface RejeitarOrcamentoUseCase {
    fun rejeitar(ordemServicoId: Long, motivo: String?): OrdemServico
}
