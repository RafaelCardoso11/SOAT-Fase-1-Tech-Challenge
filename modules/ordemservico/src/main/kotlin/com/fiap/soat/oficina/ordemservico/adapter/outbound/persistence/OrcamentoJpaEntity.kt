package com.fiap.soat.oficina.ordemservico.adapter.outbound.persistence

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "orcamento", schema = "ordemservico")
class OrcamentoJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, precision = 12, scale = 2)
    var total: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var validoAte: Instant = Instant.EPOCH,

    @Column(nullable = true)
    var aprovadoEm: Instant? = null,

    @Column(nullable = true)
    var motivoRejeicao: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "orcamento_id", nullable = false)
    var itens: MutableList<ItemOrcamentoJpaEntity> = mutableListOf(),
)
