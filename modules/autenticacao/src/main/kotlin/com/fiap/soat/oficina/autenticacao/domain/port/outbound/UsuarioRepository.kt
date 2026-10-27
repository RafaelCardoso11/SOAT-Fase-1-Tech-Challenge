package com.fiap.soat.oficina.autenticacao.domain.port.outbound

import com.fiap.soat.oficina.autenticacao.domain.model.Usuario

interface UsuarioRepository {
    fun buscarPorNomeUsuario(nomeUsuario: String): Usuario?
}
