package com.fiap.soat.oficina.estoque.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "reserva", schema = "estoque")
class ReservaJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "item_id", nullable = false)
    var itemId: Long = 0,

    @Column(name = "os_id", nullable = false)
    var osId: Long = 0,

    @Column(nullable = false)
    var quantidade: Int = 0,

    @Column(nullable = false)
    var situacao: String = "",
)
