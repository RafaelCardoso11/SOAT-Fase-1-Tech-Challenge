package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface OrdemServicoSpringDataRepository : JpaRepository<OrdemServicoJpaEntity, Long> {
    fun findByPlacaOrderByIdDesc(placa: String): List<OrdemServicoJpaEntity>
    fun findByDocumentoOrderByIdDesc(documento: String): List<OrdemServicoJpaEntity>
}
