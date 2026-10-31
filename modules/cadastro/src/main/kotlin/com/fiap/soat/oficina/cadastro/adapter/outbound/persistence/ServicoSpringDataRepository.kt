package com.fiap.soat.oficina.cadastro.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ServicoSpringDataRepository : JpaRepository<ServicoJpaEntity, Long> {

    @Query("select count(s) > 0 from ServicoJpaEntity s where lower(s.nome) = lower(:nome)")
    fun existePorNome(@Param("nome") nome: String): Boolean

    @Query("select count(s) > 0 from ServicoJpaEntity s where lower(s.nome) = lower(:nome) and s.id <> :id")
    fun existePorNomeExcetoId(@Param("nome") nome: String, @Param("id") id: Long): Boolean

    fun findAllByOrderById(): List<ServicoJpaEntity>
    fun findByAtivoOrderById(ativo: Boolean): List<ServicoJpaEntity>
}
