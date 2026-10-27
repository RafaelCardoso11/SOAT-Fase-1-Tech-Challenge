package com.fiap.soat.oficina.autenticacao.adapter.outbound.seguranca

import com.fiap.soat.oficina.autenticacao.domain.port.outbound.CodificadorDeSenha
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptCodificadorDeSenha : CodificadorDeSenha {

    private val encoder = BCryptPasswordEncoder()

    override fun codificar(senhaPura: String): String = encoder.encode(senhaPura)

    override fun confere(senhaPura: String, hash: String): Boolean = encoder.matches(senhaPura, hash)
}
