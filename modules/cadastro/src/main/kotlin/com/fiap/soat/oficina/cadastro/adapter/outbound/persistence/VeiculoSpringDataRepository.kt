package com.fiap.soat.oficina.cadastro.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface VeiculoSpringDataRepository : JpaRepository<VeiculoJpaEntity, Long> {
    fun findByPlaca(placa: String): VeiculoJpaEntity?
    fun existsByPlaca(placa: String): Boolean
    fun existsByClienteIdAndAtivoTrue(clienteId: Long): Boolean
    fun findAllByOrderById(): List<VeiculoJpaEntity>
    fun findByAtivoOrderById(ativo: Boolean): List<VeiculoJpaEntity>
    fun findByClienteIdOrderById(clienteId: Long): List<VeiculoJpaEntity>
    fun findByClienteIdAndAtivoOrderById(clienteId: Long, ativo: Boolean): List<VeiculoJpaEntity>
}
