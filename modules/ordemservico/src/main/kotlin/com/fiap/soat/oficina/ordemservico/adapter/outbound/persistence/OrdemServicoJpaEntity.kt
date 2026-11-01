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
import jakarta.persistence.Version

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

    @Column(nullable = true)
    var observacoes: String? = null,

    @Column(nullable = false)
    var liberadoParaOrcamento: Boolean = false,

    @Version
    var versao: Long? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    var transicoes: MutableList<TransicaoStatusJpaEntity> = mutableListOf(),
)
