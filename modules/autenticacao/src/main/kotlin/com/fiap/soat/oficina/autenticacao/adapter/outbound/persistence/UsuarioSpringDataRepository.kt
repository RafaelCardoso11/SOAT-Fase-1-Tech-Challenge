package com.fiap.soat.oficina.autenticacao.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioSpringDataRepository : JpaRepository<UsuarioJpaEntity, Long> {
    fun findByNomeUsuario(nomeUsuario: String): UsuarioJpaEntity?
}
