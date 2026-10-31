package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Documento
import java.time.Instant

class OrdemServico private constructor(
    val id: Long?,
    val documento: Documento,
    val veiculo: Veiculo,
    val relato: String,
    val status: StatusOS,
    val transicoes: List<TransicaoStatus>,
) {
    companion object {
        fun criar(documento: Documento, veiculo: Veiculo, relato: String): OrdemServico =
            OrdemServico(
                id = null,
                documento = documento,
                veiculo = veiculo,
                relato = relato,
                status = StatusOS.RECEBIDA,
                transicoes = listOf(TransicaoStatus(StatusOS.RECEBIDA, Instant.now())),
            )

        fun reconstituir(
            id: Long,
            documento: Documento,
            veiculo: Veiculo,
            relato: String,
            status: StatusOS,
            transicoes: List<TransicaoStatus>,
        ): OrdemServico = OrdemServico(id, documento, veiculo, relato, status, transicoes)
    }
}
