package com.fiap.soat.oficina.autenticacao.adapter.outbound.persistence

import com.fiap.soat.oficina.autenticacao.domain.model.Usuario
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.UsuarioRepository
import org.springframework.stereotype.Component

@Component
class UsuarioRepositoryAdapter(
    private val springData: UsuarioSpringDataRepository,
) : UsuarioRepository {

    override fun buscarPorNomeUsuario(nomeUsuario: String): Usuario? =
        springData.findByNomeUsuario(nomeUsuario)?.let {
            Usuario(it.id, it.nomeUsuario, it.senhaHash, it.ativo)
        }
}
