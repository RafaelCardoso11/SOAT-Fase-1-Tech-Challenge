package com.fiap.soat.oficina.cadastro.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "cliente", schema = "cadastro")
class ClienteJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var documento: String = "",

    @Column(nullable = false)
    var nome: String = "",

    @Column(nullable = false)
    var email: String = "",

    @Column(nullable = false)
    var ativo: Boolean = true,
)
