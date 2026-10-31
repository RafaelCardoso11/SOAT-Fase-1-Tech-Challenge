package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "ordem_servico", schema = "ordemservico")
class OrdemServicoJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var documento: String = "",

    @Column(nullable = false)
    var placa: String = "",

    @Column(nullable = false)
    var marca: String = "",

    @Column(nullable = false)
    var modelo: String = "",

    @Column(nullable = false)
    var ano: Int = 0,

    @Column(nullable = false)
    var relato: String = "",

    @Column(nullable = false)
    var status: String = "",
)
