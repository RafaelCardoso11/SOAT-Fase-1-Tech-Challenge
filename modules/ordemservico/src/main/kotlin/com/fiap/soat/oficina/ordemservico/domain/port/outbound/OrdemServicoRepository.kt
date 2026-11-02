package com.fiap.soat.oficina.ordemservico.domain.port.outbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import java.time.Instant

interface OrdemServicoRepository {
    fun salvar(ordemServico: OrdemServico): Long
    fun buscarPorId(id: Long): OrdemServico?
    fun buscarPorPlaca(placa: String): List<OrdemServico>
    fun buscarPorDocumento(documento: String): List<OrdemServico>
    fun buscarExpiraveis(agora: Instant): List<OrdemServico>

    /** OS com início e fim de execução registrados (para o tempo médio). */
    fun buscarComExecucaoConcluida(): List<OrdemServico>
}
