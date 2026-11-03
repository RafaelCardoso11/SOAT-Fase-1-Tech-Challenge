package com.fiap.soat.oficina.ordemservico.domain.port.outbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.Pagina
import com.fiap.soat.oficina.ordemservico.domain.model.ResumoOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import java.time.Instant

interface OrdemServicoRepository {
    fun salvar(ordemServico: OrdemServico): Long
    fun buscarPorId(id: Long): OrdemServico?
    fun buscarPorTokenAprovacao(token: String): OrdemServico?
    fun buscarPorPlaca(placa: String): List<OrdemServico>
    fun buscarPorDocumento(documento: String): List<OrdemServico>
    fun buscarExpiraveis(agora: Instant): List<OrdemServico>

    /** Página de resumos, da OS mais recente para a mais antiga; sem [status] traz todas. */
    fun listar(status: StatusOS?, pagina: Int, tamanho: Int): Pagina<ResumoOrdemServico>

    /** OS com início e fim de execução registrados (para o tempo médio). */
    fun buscarComExecucaoConcluida(): List<OrdemServico>
}
