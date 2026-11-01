package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface AprovarOrcamentoUseCase {
    fun aprovar(ordemServicoId: Long): OrdemServico
}
