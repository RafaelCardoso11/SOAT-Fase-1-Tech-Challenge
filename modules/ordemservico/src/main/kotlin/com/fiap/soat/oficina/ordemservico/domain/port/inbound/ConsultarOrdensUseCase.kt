package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.Pagina
import com.fiap.soat.oficina.ordemservico.domain.model.ResumoOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS

interface ConsultarOrdensUseCase {
    fun porNumero(numero: Long): OrdemServico?
    fun porPlaca(placa: String): List<OrdemServico>
    fun porDocumento(documento: String): List<OrdemServico>
    fun listar(status: StatusOS?, pagina: Int, tamanho: Int): Pagina<ResumoOrdemServico>
    fun detalhar(numero: Long): OrdemServico
}
