package com.fiap.soat.oficina.cadastro.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "veiculo", schema = "cadastro")
class VeiculoJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var placa: String = "",

    @Column(nullable = false)
    var marca: String = "",

    @Column(nullable = false)
    var modelo: String = "",

    @Column(nullable = false)
    var ano: Int = 0,

    @Column(name = "cliente_id", nullable = false)
    var clienteId: Long = 0,

    @Column(nullable = false)
    var ativo: Boolean = true,
)
