package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Documento

data class ResumoOrdemServico(
    val numero: Long,
    val status: StatusOS,
    val documento: Documento,
    val veiculo: Veiculo,
    val relato: String,
)
