package com.fiap.soat.oficina.autenticacao.adapter.config

import com.fiap.soat.oficina.autenticacao.application.usecase.AutenticarService
import com.fiap.soat.oficina.autenticacao.domain.port.inbound.AutenticarUseCase
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.CodificadorDeSenha
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.TokenProvider
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.UsuarioRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AutenticacaoBeanConfig {

    @Bean
    fun autenticarUseCase(
        usuarioRepository: UsuarioRepository,
        codificadorDeSenha: CodificadorDeSenha,
        tokenProvider: TokenProvider,
    ): AutenticarUseCase = AutenticarService(usuarioRepository, codificadorDeSenha, tokenProvider)
}
