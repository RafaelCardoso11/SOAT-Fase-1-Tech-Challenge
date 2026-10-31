package com.fiap.soat.oficina.cadastro.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ClienteSpringDataRepository : JpaRepository<ClienteJpaEntity, Long> {
    fun findByDocumento(documento: String): ClienteJpaEntity?
    fun existsByDocumento(documento: String): Boolean
    fun findAllByOrderById(): List<ClienteJpaEntity>
    fun findByAtivoOrderById(ativo: Boolean): List<ClienteJpaEntity>
}
