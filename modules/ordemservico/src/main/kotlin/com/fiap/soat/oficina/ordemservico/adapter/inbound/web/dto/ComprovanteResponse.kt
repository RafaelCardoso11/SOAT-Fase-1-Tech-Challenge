package com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import java.time.Instant

data class ComprovanteResponse(
    val numero: Long?,
    val placa: String,
    val marca: String,
    val modelo: String,
    val documento: String,
    val status: String,
    val entregueEm: Instant?,
) {
    companion object {
        fun de(os: OrdemServico) = ComprovanteResponse(
            numero = os.id,
            placa = os.veiculo.placa.valor,
            marca = os.veiculo.marca,
            modelo = os.veiculo.modelo,
            documento = os.documento.valor,
            status = os.status.name,
            entregueEm = os.transicoes.lastOrNull()?.momento,
        )
    }
}
