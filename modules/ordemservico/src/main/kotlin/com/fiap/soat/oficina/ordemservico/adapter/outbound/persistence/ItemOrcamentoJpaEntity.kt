package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "item_orcamento", schema = "ordemservico")
class ItemOrcamentoJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var tipo: String = "",

    @Column(nullable = false)
    var descricao: String = "",

    @Column(nullable = false, precision = 12, scale = 2)
    var valor: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = true)
    var itemEstoqueId: Long? = null,

    @Column(nullable = true)
    var quantidade: Int? = null,
)
