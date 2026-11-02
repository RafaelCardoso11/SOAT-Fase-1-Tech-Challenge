package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface EntregarVeiculoUseCase {
    fun entregar(ordemServicoId: Long): OrdemServico
}
