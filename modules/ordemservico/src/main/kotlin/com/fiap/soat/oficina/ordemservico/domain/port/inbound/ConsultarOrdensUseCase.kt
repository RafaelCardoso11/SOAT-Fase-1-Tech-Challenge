package com.fiap.soat.oficina.ordemservico.domain.port.inbound

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

interface ConsultarOrdensUseCase {
    fun porNumero(numero: Long): OrdemServico?
    fun porPlaca(placa: String): List<OrdemServico>
    fun porDocumento(documento: String): List<OrdemServico>
}
