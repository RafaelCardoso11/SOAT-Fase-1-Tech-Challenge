package com.fiap.soat.oficina.cadastro.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "servico", schema = "cadastro")
class ServicoJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var nome: String = "",

    @Column
    var descricao: String? = null,

    @Column(nullable = false, precision = 12, scale = 2)
    var preco: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var ativo: Boolean = true,
)
