package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Documento
import java.time.Instant

class OrdemServico private constructor(
    val id: Long?,
    val documento: Documento,
    val veiculo: Veiculo,
    val relato: String,
    status: StatusOS,
    transicoes: List<TransicaoStatus>,
    observacoes: String?,
    liberadoParaOrcamento: Boolean,
) {
    var status: StatusOS = status
        private set

    private val _transicoes: MutableList<TransicaoStatus> = transicoes.toMutableList()
    val transicoes: List<TransicaoStatus> get() = _transicoes.toList()

    var observacoes: String? = observacoes
        private set

    var liberadoParaOrcamento: Boolean = liberadoParaOrcamento
        private set

    fun iniciarDiagnostico() {
        check(status == StatusOS.RECEBIDA) { "Diagnóstico só pode ser iniciado em uma OS Recebida" }
        transitarPara(StatusOS.EM_DIAGNOSTICO)
    }

    fun registrarInspecao(observacoes: String) {
        check(status == StatusOS.EM_DIAGNOSTICO) { "A inspeção só pode ser registrada com a OS em diagnóstico" }
        this.observacoes = observacoes
    }

    fun finalizarDiagnostico(pecaIdentificada: Boolean) {
        check(status == StatusOS.EM_DIAGNOSTICO) { "O diagnóstico só pode ser finalizado com a OS em diagnóstico" }
        if (pecaIdentificada) {
            liberadoParaOrcamento = true
        } else {
            transitarPara(StatusOS.FINALIZADA)
        }
    }

    private fun transitarPara(novo: StatusOS) {
        status = novo
        _transicoes.add(TransicaoStatus(novo, Instant.now()))
    }

    companion object {
        fun criar(documento: Documento, veiculo: Veiculo, relato: String): OrdemServico =
            OrdemServico(
                id = null,
                documento = documento,
                veiculo = veiculo,
                relato = relato,
                status = StatusOS.RECEBIDA,
                transicoes = listOf(TransicaoStatus(StatusOS.RECEBIDA, Instant.now())),
                observacoes = null,
                liberadoParaOrcamento = false,
            )

        fun reconstituir(
            id: Long,
            documento: Documento,
            veiculo: Veiculo,
            relato: String,
            status: StatusOS,
            transicoes: List<TransicaoStatus>,
            observacoes: String? = null,
            liberadoParaOrcamento: Boolean = false,
        ): OrdemServico =
            OrdemServico(id, documento, veiculo, relato, status, transicoes, observacoes, liberadoParaOrcamento)
    }
}
