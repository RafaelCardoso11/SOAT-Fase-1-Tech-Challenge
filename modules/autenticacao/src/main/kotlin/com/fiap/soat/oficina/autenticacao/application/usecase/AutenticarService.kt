package com.fiap.soat.oficina.autenticacao.application.usecase

import com.fiap.soat.oficina.autenticacao.domain.error.CredenciaisInvalidasException
import com.fiap.soat.oficina.autenticacao.domain.model.TokenEmitido
import com.fiap.soat.oficina.autenticacao.domain.port.inbound.AutenticarUseCase
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.CodificadorDeSenha
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.TokenProvider
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.UsuarioRepository

class AutenticarService(
    private val usuarioRepository: UsuarioRepository,
    private val codificadorDeSenha: CodificadorDeSenha,
    private val tokenProvider: TokenProvider,
) : AutenticarUseCase {

    override fun autenticar(nomeUsuario: String, senha: String): TokenEmitido {
        val usuario = usuarioRepository.buscarPorNomeUsuario(nomeUsuario)
        if (usuario == null || !usuario.ativo || !codificadorDeSenha.confere(senha, usuario.senhaHash)) {
            throw CredenciaisInvalidasException()
        }
        return tokenProvider.emitir(usuario)
    }
}
