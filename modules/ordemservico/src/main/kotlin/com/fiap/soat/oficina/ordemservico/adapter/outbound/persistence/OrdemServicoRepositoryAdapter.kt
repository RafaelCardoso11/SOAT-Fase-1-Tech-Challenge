package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.Orcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import org.springframework.stereotype.Component

@Component
class OrdemServicoRepositoryAdapter(
    private val springData: OrdemServicoSpringDataRepository,
) : OrdemServicoRepository {

    override fun salvar(ordemServico: OrdemServico): Long {
        val entidade = ordemServico.id?.let { springData.findById(it).orElseThrow() } ?: OrdemServicoJpaEntity()
        entidade.documento = ordemServico.documento.valor
        entidade.placa = ordemServico.veiculo.placa.valor
        entidade.marca = ordemServico.veiculo.marca
        entidade.modelo = ordemServico.veiculo.modelo
        entidade.ano = ordemServico.veiculo.ano
        entidade.relato = ordemServico.relato
        entidade.status = ordemServico.status.name
        entidade.observacoes = ordemServico.observacoes
        entidade.liberadoParaOrcamento = ordemServico.liberadoParaOrcamento
        val domOrcamento = ordemServico.orcamento
        if (domOrcamento != null) {
            val orcamentoEntity = entidade.orcamento
            if (orcamentoEntity == null) {
                entidade.orcamento = domOrcamento.toJpa()
            } else {
                orcamentoEntity.aprovadoEm = domOrcamento.aprovadoEm
            }
        }
        // shortcut: histórico append-only — anexa só as transições além das já persistidas; revisar se transições puderem ser editadas/removidas.
        val novas = ordemServico.transicoes.drop(entidade.transicoes.size)
        entidade.transicoes.addAll(
            novas.map { TransicaoStatusJpaEntity(status = it.status.name, momento = it.momento) },
        )
        return springData.save(entidade).id!!
    }

    override fun buscarPorId(id: Long): OrdemServico? =
        springData.findById(id).map(OrdemServicoJpaEntity::toDomain).orElse(null)

    override fun buscarPorPlaca(placa: String): List<OrdemServico> =
        springData.findByPlacaOrderByIdDesc(placa).map(OrdemServicoJpaEntity::toDomain)

    override fun buscarPorDocumento(documento: String): List<OrdemServico> =
        springData.findByDocumentoOrderByIdDesc(documento).map(OrdemServicoJpaEntity::toDomain)
}

private fun OrdemServicoJpaEntity.toDomain(): OrdemServico =
    OrdemServico.reconstituir(
        id = id!!,
        documento = Documento.of(documento),
        veiculo = Veiculo(Placa.of(placa), marca, modelo, ano),
        relato = relato,
        status = StatusOS.valueOf(status),
        transicoes = transicoes
            .map { TransicaoStatus(StatusOS.valueOf(it.status), it.momento) }
            .sortedBy { it.momento },
        observacoes = observacoes,
        liberadoParaOrcamento = liberadoParaOrcamento,
        orcamento = orcamento?.toDomain(),
    )

private fun Orcamento.toJpa(): OrcamentoJpaEntity = OrcamentoJpaEntity(
    total = total.valor,
    validoAte = validoAte,
    aprovadoEm = aprovadoEm,
    itens = itens.map {
        ItemOrcamentoJpaEntity(tipo = it.tipo.name, descricao = it.descricao, valor = it.valor.valor)
    }.toMutableList(),
)

private fun OrcamentoJpaEntity.toDomain(): Orcamento = Orcamento(
    itens = itens.map { ItemOrcamento(TipoItem.valueOf(it.tipo), it.descricao, Dinheiro.of(it.valor)) },
    validoAte = validoAte,
    aprovadoEm = aprovadoEm,
)
