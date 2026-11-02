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
    orcamento: Orcamento?,
    execucaoIniciadaEm: Instant?,
    execucaoFinalizadaEm: Instant?,
) {
    var status: StatusOS = status
        private set

    private val _transicoes: MutableList<TransicaoStatus> = transicoes.toMutableList()
    val transicoes: List<TransicaoStatus> get() = _transicoes.toList()

    var observacoes: String? = observacoes
        private set

    var liberadoParaOrcamento: Boolean = liberadoParaOrcamento
        private set

    var orcamento: Orcamento? = orcamento
        private set

    var execucaoIniciadaEm: Instant? = execucaoIniciadaEm
        private set

    var execucaoFinalizadaEm: Instant? = execucaoFinalizadaEm
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

    fun gerarOrcamento(itens: List<ItemOrcamento>, validoAte: Instant): Orcamento {
        check(status == StatusOS.EM_DIAGNOSTICO && liberadoParaOrcamento) {
            "Orçamento só pode ser gerado a partir de um diagnóstico finalizado"
        }
        val novo = Orcamento(itens, validoAte)
        orcamento = novo
        transitarPara(StatusOS.AGUARDANDO_APROVACAO)
        return novo
    }

    fun aprovarOrcamento() {
        check(status == StatusOS.AGUARDANDO_APROVACAO) {
            "A aprovação só é possível com a OS aguardando aprovação"
        }
        checkNotNull(orcamento) { "Orçamento inexistente" }.aprovar(Instant.now())
        transitarPara(StatusOS.EM_EXECUCAO)
    }

    fun rejeitarOrcamento(motivo: String?) {
        check(status == StatusOS.AGUARDANDO_APROVACAO) {
            "A rejeição só é possível com a OS aguardando aprovação"
        }
        checkNotNull(orcamento) { "Orçamento inexistente" }.rejeitar(motivo)
        transitarPara(StatusOS.FINALIZADA)
    }

    fun revisarOrcamento(itens: List<ItemOrcamento>, validoAte: Instant): Orcamento {
        check(status == StatusOS.AGUARDANDO_APROVACAO) {
            "A revisão só é possível com a OS aguardando aprovação"
        }
        val novo = Orcamento(itens, validoAte)
        orcamento = novo
        return novo
    }

    fun encerrar() {
        check(status != StatusOS.FINALIZADA && status != StatusOS.ENTREGUE) { "OS já encerrada" }
        transitarPara(StatusOS.FINALIZADA)
    }

    fun iniciarExecucao() {
        check(status == StatusOS.EM_EXECUCAO) {
            "A execução só pode ser iniciada com o orçamento aprovado"
        }
        check(execucaoIniciadaEm == null) { "Execução já iniciada" }
        execucaoIniciadaEm = Instant.now()
    }

    fun finalizarExecucao(todosAplicados: Boolean) {
        check(status == StatusOS.EM_EXECUCAO) { "A execução só pode ser finalizada com a OS em execução" }
        check(todosAplicados) { "Há itens pendentes de aplicação" }
        execucaoFinalizadaEm = Instant.now()
        transitarPara(StatusOS.FINALIZADA)
    }

    fun entregar() {
        check(status == StatusOS.FINALIZADA) { "O veículo só pode ser entregue com a OS finalizada" }
        transitarPara(StatusOS.ENTREGUE)
    }

    fun expirarOrcamento(agora: Instant) {
        check(status == StatusOS.AGUARDANDO_APROVACAO) {
            "Só expira orçamento de OS aguardando aprovação"
        }
        val orc = checkNotNull(orcamento) { "Orçamento inexistente" }
        check(agora > orc.validoAte) { "Orçamento ainda dentro do prazo" }
        transitarPara(StatusOS.FINALIZADA)
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
                orcamento = null,
                execucaoIniciadaEm = null,
                execucaoFinalizadaEm = null,
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
            orcamento: Orcamento? = null,
            execucaoIniciadaEm: Instant? = null,
            execucaoFinalizadaEm: Instant? = null,
        ): OrdemServico =
            OrdemServico(
                id, documento, veiculo, relato, status, transicoes, observacoes, liberadoParaOrcamento, orcamento,
                execucaoIniciadaEm, execucaoFinalizadaEm,
            )
    }
}
