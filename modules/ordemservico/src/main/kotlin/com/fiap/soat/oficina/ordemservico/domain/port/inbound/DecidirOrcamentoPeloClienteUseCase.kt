package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface DecidirOrcamentoPeloClienteUseCase {
    fun consultar(token: String): OrdemServico
    fun aprovar(token: String): OrdemServico
    fun rejeitar(token: String, motivo: String?): OrdemServico
}
