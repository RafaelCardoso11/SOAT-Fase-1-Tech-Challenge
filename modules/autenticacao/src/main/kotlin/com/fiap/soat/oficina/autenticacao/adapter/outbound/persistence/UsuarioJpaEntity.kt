package com.fiap.soat.oficina.autenticacao.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "usuario", schema = "autenticacao")
class UsuarioJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "nome_usuario", nullable = false, unique = true)
    var nomeUsuario: String = "",

    @Column(name = "senha_hash", nullable = false)
    var senhaHash: String = "",

    @Column(nullable = false)
    var ativo: Boolean = true,
)
