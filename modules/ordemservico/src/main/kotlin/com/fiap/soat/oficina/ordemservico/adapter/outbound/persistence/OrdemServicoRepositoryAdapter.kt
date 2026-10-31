package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import org.springframework.stereotype.Component

@Component
class OrdemServicoRepositoryAdapter(
    private val springData: OrdemServicoSpringDataRepository,
) : OrdemServicoRepository {

    override fun salvar(ordemServico: OrdemServico): Long {
        val entidade = OrdemServicoJpaEntity(
            documento = ordemServico.documento.valor,
            placa = ordemServico.veiculo.placa.valor,
            marca = ordemServico.veiculo.marca,
            modelo = ordemServico.veiculo.modelo,
            ano = ordemServico.veiculo.ano,
            relato = ordemServico.relato,
            status = ordemServico.status.name,
            transicoes = ordemServico.transicoes.map {
                TransicaoStatusJpaEntity(status = it.status.name, momento = it.momento)
            }.toMutableList(),
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
    )
