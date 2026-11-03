package com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto

import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.Orcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.Pagina
import com.fiap.soat.oficina.ordemservico.domain.model.ResumoOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import java.math.BigDecimal
import java.time.Instant

data class PaginaResponse<T>(
    val conteudo: List<T>,
    val pagina: Int,
    val tamanho: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
) {
    companion object {
        fun <T, R> de(pagina: Pagina<T>, mapear: (T) -> R) = PaginaResponse(
            conteudo = pagina.conteudo.map(mapear),
            pagina = pagina.pagina,
            tamanho = pagina.tamanho,
            totalElementos = pagina.totalElementos,
            totalPaginas = pagina.totalPaginas,
        )
    }
}

data class ResumoOrdemServicoResponse(
    val numero: Long,
    val status: String,
    val documento: String,
    val placa: String,
    val marca: String,
    val modelo: String,
    val ano: Int,
    val relato: String,
) {
    companion object {
        fun de(resumo: ResumoOrdemServico) = ResumoOrdemServicoResponse(
            numero = resumo.numero,
            status = resumo.status.name,
            documento = resumo.documento.valor,
            placa = resumo.veiculo.placa.valor,
            marca = resumo.veiculo.marca,
            modelo = resumo.veiculo.modelo,
            ano = resumo.veiculo.ano,
            relato = resumo.relato,
        )
    }
}

data class OrdemServicoDetalheResponse(
    val numero: Long?,
    val status: String,
    val documento: String,
    val placa: String,
    val marca: String,
    val modelo: String,
    val ano: Int,
    val relato: String,
    val observacoes: String?,
    val liberadoParaOrcamento: Boolean,
    val orcamento: OrcamentoDetalheResponse?,
    val execucao: ExecucaoResponse,
    val historico: List<TransicaoResponse>,
) {
    companion object {
        fun de(os: OrdemServico) = OrdemServicoDetalheResponse(
            numero = os.id,
            status = os.status.name,
            documento = os.documento.valor,
            placa = os.veiculo.placa.valor,
            marca = os.veiculo.marca,
            modelo = os.veiculo.modelo,
            ano = os.veiculo.ano,
            relato = os.relato,
            observacoes = os.observacoes,
            liberadoParaOrcamento = os.liberadoParaOrcamento,
            orcamento = os.orcamento?.let(OrcamentoDetalheResponse::de),
            execucao = ExecucaoResponse(os.execucaoIniciadaEm, os.execucaoFinalizadaEm),
            historico = os.transicoes.map { TransicaoResponse(it.status.name, it.momento) },
        )
    }
}

data class OrcamentoDetalheResponse(
    val total: BigDecimal,
    val validoAte: Instant,
    val aprovadoEm: Instant?,
    val motivoRejeicao: String?,
    val servicos: List<ServicoOrcamentoResponse>,
    val pecas: List<PecaOrcamentoResponse>,
) {
    companion object {
        fun de(orcamento: Orcamento) = OrcamentoDetalheResponse(
            total = orcamento.total.valor,
            validoAte = orcamento.validoAte,
            aprovadoEm = orcamento.aprovadoEm,
            motivoRejeicao = orcamento.motivoRejeicao,
            servicos = orcamento.itens.filter { it.tipo == TipoItem.SERVICO }.map(ServicoOrcamentoResponse::de),
            pecas = orcamento.itens.filter { it.tipo == TipoItem.PECA }.map(PecaOrcamentoResponse::de),
        )
    }
}

data class ServicoOrcamentoResponse(val descricao: String, val valor: BigDecimal) {
    companion object {
        fun de(item: ItemOrcamento) = ServicoOrcamentoResponse(item.descricao, item.valor.valor)
    }
}

data class PecaOrcamentoResponse(
    val descricao: String,
    val valor: BigDecimal,
    val itemEstoqueId: Long?,
    val quantidade: Int?,
) {
    companion object {
        fun de(item: ItemOrcamento) =
            PecaOrcamentoResponse(item.descricao, item.valor.valor, item.itemEstoqueId, item.quantidade)
    }
}

data class ExecucaoResponse(val iniciadaEm: Instant?, val finalizadaEm: Instant?)
