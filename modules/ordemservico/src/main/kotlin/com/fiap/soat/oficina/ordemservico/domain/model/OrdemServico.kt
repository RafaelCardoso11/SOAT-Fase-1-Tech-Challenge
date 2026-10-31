package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Documento

class OrdemServico private constructor(
    val documento: Documento,
    val veiculo: Veiculo,
    val relato: String,
    val status: StatusOS,
) {
    companion object {
        fun criar(documento: Documento, veiculo: Veiculo, relato: String): OrdemServico =
            OrdemServico(documento, veiculo, relato, StatusOS.RECEBIDA)
    }
}
