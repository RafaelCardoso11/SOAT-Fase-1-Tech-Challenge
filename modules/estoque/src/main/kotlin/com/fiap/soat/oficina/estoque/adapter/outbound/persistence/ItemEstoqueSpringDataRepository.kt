package com.fiap.soat.oficina.estoque.adapter.outbound.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ItemEstoqueSpringDataRepository : JpaRepository<ItemEstoqueJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemEstoqueJpaEntity i where i.id = :id")
    fun buscarComLock(@Param("id") id: Long): ItemEstoqueJpaEntity?

    @Query("select count(i) > 0 from ItemEstoqueJpaEntity i where lower(i.nome) = lower(:nome)")
    fun existePorNome(@Param("nome") nome: String): Boolean

    @Query("select count(i) > 0 from ItemEstoqueJpaEntity i where lower(i.nome) = lower(:nome) and i.id <> :id")
    fun existePorNomeExcetoId(@Param("nome") nome: String, @Param("id") id: Long): Boolean

    fun findAllByOrderById(): List<ItemEstoqueJpaEntity>
    fun findByAtivoOrderById(ativo: Boolean): List<ItemEstoqueJpaEntity>
}
