package com.fiap.soat.oficina.ordemservico.domain.port.outbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface OrdemServicoRepository {
    fun salvar(ordemServico: OrdemServico): Long
}
