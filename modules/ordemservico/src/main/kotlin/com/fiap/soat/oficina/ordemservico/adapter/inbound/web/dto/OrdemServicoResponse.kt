package com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import java.time.Instant

data class OrdemServicoResponse(
    val numero: Long?,
    val status: String,
    val documento: String,
    val placa: String,
    val marca: String,
    val modelo: String,
    val ano: Int,
    val relato: String,
    val linhaDoTempo: List<TransicaoResponse>,
) {
    companion object {
        fun de(os: OrdemServico) = OrdemServicoResponse(
            numero = os.id,
            status = os.status.name,
            documento = mascararDocumento(os.documento.valor),
            placa = os.veiculo.placa.valor,
            marca = os.veiculo.marca,
            modelo = os.veiculo.modelo,
            ano = os.veiculo.ano,
            relato = os.relato,
            linhaDoTempo = os.transicoes.map { TransicaoResponse(it.status.name, it.momento) },
        )
    }
}

data class TransicaoResponse(val status: String, val momento: Instant)

fun mascararDocumento(valor: String): String =
    if (valor.length <= 2) "*".repeat(valor.length)
    else "*".repeat(valor.length - 2) + valor.takeLast(2)
