package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.Orcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.Pagina
import com.fiap.soat.oficina.ordemservico.domain.model.ResumoOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
        entidade.execucaoIniciadaEm = ordemServico.execucaoIniciadaEm
        entidade.execucaoFinalizadaEm = ordemServico.execucaoFinalizadaEm
        val domOrcamento = ordemServico.orcamento
        if (domOrcamento != null) {
            val orcamentoEntity = entidade.orcamento
            if (orcamentoEntity == null) {
                entidade.orcamento = domOrcamento.toJpa()
            } else {
                orcamentoEntity.total = domOrcamento.total.valor
                orcamentoEntity.validoAte = domOrcamento.validoAte
                orcamentoEntity.aprovadoEm = domOrcamento.aprovadoEm
                orcamentoEntity.motivoRejeicao = domOrcamento.motivoRejeicao
                if (orcamentoEntity.itens.diferemDe(domOrcamento.itens)) {
                    orcamentoEntity.itens.clear()
                    orcamentoEntity.itens.addAll(domOrcamento.toJpa().itens)
                }
            }
        }
        // shortcut: histórico append-only — anexa só as transições além das já persistidas; revisar se transições puderem ser editadas/removidas.
        val novas = ordemServico.transicoes.drop(entidade.transicoes.size)
        entidade.transicoes.addAll(
            novas.map { TransicaoStatusJpaEntity(status = it.status.name, momento = it.momento) },
        )
        return springData.save(entidade).id!!
    }

    @Transactional(readOnly = true)
    override fun buscarPorId(id: Long): OrdemServico? =
        springData.findById(id).map(OrdemServicoJpaEntity::toDomain).orElse(null)

    @Transactional(readOnly = true)
    override fun buscarPorPlaca(placa: String): List<OrdemServico> =
        springData.findByPlacaOrderByIdDesc(placa).map(OrdemServicoJpaEntity::toDomain)

    @Transactional(readOnly = true)
    override fun buscarPorDocumento(documento: String): List<OrdemServico> =
        springData.findByDocumentoOrderByIdDesc(documento).map(OrdemServicoJpaEntity::toDomain)

    @Transactional(readOnly = true)
    override fun buscarExpiraveis(agora: java.time.Instant): List<OrdemServico> =
        springData.findByStatusAndOrcamento_ValidoAteBefore(StatusOS.AGUARDANDO_APROVACAO.name, agora)
            .map(OrdemServicoJpaEntity::toDomain)

    @Transactional(readOnly = true)
    override fun buscarComExecucaoConcluida(): List<OrdemServico> =
        springData.findByExecucaoIniciadaEmIsNotNullAndExecucaoFinalizadaEmIsNotNull()
            .map(OrdemServicoJpaEntity::toDomain)

    @Transactional(readOnly = true)
    override fun listar(status: StatusOS?, pagina: Int, tamanho: Int): Pagina<ResumoOrdemServico> {
        val ordenacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id"))
        val resultado = if (status == null) {
            springData.findAllBy(ordenacao)
        } else {
            springData.findByStatus(status.name, ordenacao)
        }
        return Pagina(resultado.content.map { it.toResumo() }, pagina, tamanho, resultado.totalElements)
    }
}

private fun OrdemServicoResumoView.toResumo() = ResumoOrdemServico(
    numero = id,
    status = StatusOS.valueOf(status),
    documento = Documento.of(documento),
    veiculo = Veiculo(Placa.of(placa), marca, modelo, ano),
    relato = relato,
)

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
        execucaoIniciadaEm = execucaoIniciadaEm,
        execucaoFinalizadaEm = execucaoFinalizadaEm,
    )

private fun List<ItemOrcamentoJpaEntity>.diferemDe(itens: List<ItemOrcamento>): Boolean =
    map { listOf(it.tipo, it.descricao, it.valor.stripTrailingZeros(), it.itemEstoqueId, it.quantidade) } !=
        itens.map { listOf(it.tipo.name, it.descricao, it.valor.valor.stripTrailingZeros(), it.itemEstoqueId, it.quantidade) }

private fun Orcamento.toJpa(): OrcamentoJpaEntity = OrcamentoJpaEntity(
    total = total.valor,
    validoAte = validoAte,
    aprovadoEm = aprovadoEm,
    motivoRejeicao = motivoRejeicao,
    itens = itens.map {
        ItemOrcamentoJpaEntity(
            tipo = it.tipo.name,
            descricao = it.descricao,
            valor = it.valor.valor,
            itemEstoqueId = it.itemEstoqueId,
            quantidade = it.quantidade,
        )
    }.toMutableList(),
)

private fun OrcamentoJpaEntity.toDomain(): Orcamento = Orcamento(
    itens = itens.map {
        ItemOrcamento(TipoItem.valueOf(it.tipo), it.descricao, Dinheiro.of(it.valor), it.itemEstoqueId, it.quantidade)
    },
    validoAte = validoAte,
    aprovadoEm = aprovadoEm,
    motivoRejeicao = motivoRejeicao,
)
