package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
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
        )
        return springData.save(entidade).id!!
    }
}
