package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface OrdemServicoSpringDataRepository : JpaRepository<OrdemServicoJpaEntity, Long> {
    fun findByPlacaOrderByIdDesc(placa: String): List<OrdemServicoJpaEntity>
    fun findByDocumentoOrderByIdDesc(documento: String): List<OrdemServicoJpaEntity>
    fun findByStatusAndOrcamento_ValidoAteBefore(status: String, momento: Instant): List<OrdemServicoJpaEntity>
    fun findByExecucaoIniciadaEmIsNotNullAndExecucaoFinalizadaEmIsNotNull(): List<OrdemServicoJpaEntity>
}
