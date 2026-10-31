package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "transicao_status", schema = "ordemservico")
class TransicaoStatusJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var status: String = "",

    @Column(nullable = false)
    var momento: Instant = Instant.EPOCH,
)
