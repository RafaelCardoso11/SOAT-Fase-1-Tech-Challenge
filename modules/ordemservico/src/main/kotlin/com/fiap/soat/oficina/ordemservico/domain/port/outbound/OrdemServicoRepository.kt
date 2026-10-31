package com.fiap.soat.oficina.ordemservico.domain.port.outbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface OrdemServicoRepository {
    fun salvar(ordemServico: OrdemServico): Long
    fun buscarPorId(id: Long): OrdemServico?
    fun buscarPorPlaca(placa: String): List<OrdemServico>
    fun buscarPorDocumento(documento: String): List<OrdemServico>
}
