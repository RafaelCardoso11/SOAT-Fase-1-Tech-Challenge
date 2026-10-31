package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface OrdemServicoSpringDataRepository : JpaRepository<OrdemServicoJpaEntity, Long>
