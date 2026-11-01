package com.fiap.soat.oficina.estoque.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "item", schema = "estoque")
class ItemEstoqueJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var nome: String = "",

    @Column
    var descricao: String? = null,

    @Column(nullable = false)
    var tipo: String = "",

    @Column(nullable = false, precision = 12, scale = 2)
    var preco: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var saldo: Int = 0,

    @Column(nullable = false)
    var reservado: Int = 0,

    @Column(nullable = false)
    var ativo: Boolean = true,
)
