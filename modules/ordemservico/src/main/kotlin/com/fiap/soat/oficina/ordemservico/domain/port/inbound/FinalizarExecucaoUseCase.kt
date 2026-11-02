package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface FinalizarExecucaoUseCase {
    fun finalizar(ordemServicoId: Long, todosAplicados: Boolean): OrdemServico
}
