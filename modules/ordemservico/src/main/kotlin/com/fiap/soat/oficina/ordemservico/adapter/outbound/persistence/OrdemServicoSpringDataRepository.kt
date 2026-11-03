package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface OrdemServicoSpringDataRepository : JpaRepository<OrdemServicoJpaEntity, Long> {
    fun findByOrcamento_TokenAprovacao(token: String): OrdemServicoJpaEntity?
    fun findByPlacaOrderByIdDesc(placa: String): List<OrdemServicoJpaEntity>
    fun findByDocumentoOrderByIdDesc(documento: String): List<OrdemServicoJpaEntity>
    fun findByStatusAndOrcamento_ValidoAteBefore(status: String, momento: Instant): List<OrdemServicoJpaEntity>
    fun findByExecucaoIniciadaEmIsNotNullAndExecucaoFinalizadaEmIsNotNull(): List<OrdemServicoJpaEntity>

    fun findAllBy(pageable: Pageable): Page<OrdemServicoResumoView>
    fun findByStatus(status: String, pageable: Pageable): Page<OrdemServicoResumoView>
}
