package com.fiap.soat.oficina.estoque.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ReservaSpringDataRepository : JpaRepository<ReservaJpaEntity, Long> {
    fun findByOsIdAndItemIdAndSituacao(osId: Long, itemId: Long, situacao: String): ReservaJpaEntity?
    fun findByOsIdAndSituacaoOrderByItemId(osId: Long, situacao: String): List<ReservaJpaEntity>
    fun findByOsIdOrderById(osId: Long): List<ReservaJpaEntity>
}
