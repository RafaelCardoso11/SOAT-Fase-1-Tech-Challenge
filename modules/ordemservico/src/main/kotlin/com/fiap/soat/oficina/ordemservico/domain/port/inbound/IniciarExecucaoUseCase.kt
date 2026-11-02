package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface IniciarExecucaoUseCase {
    fun iniciar(ordemServicoId: Long): OrdemServico
}
