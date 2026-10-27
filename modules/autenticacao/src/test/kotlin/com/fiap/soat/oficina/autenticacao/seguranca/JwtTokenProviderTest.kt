package com.fiap.soat.oficina.autenticacao.seguranca

import com.fiap.soat.oficina.autenticacao.adapter.outbound.seguranca.JwtTokenProvider
import com.fiap.soat.oficina.autenticacao.domain.model.Usuario
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {

    private val secret = "um-segredo-de-teste-bem-longo-para-hmac-sha-256!!"
    private val usuario = Usuario(1L, "admin", "hash", true)

    @Test
    fun `emitir e validar faz round-trip`() {
        val provider = JwtTokenProvider(secret, 60)

        val emitido = provider.emitir(usuario)

        assertThat(provider.validar(emitido.token)).isEqualTo("admin")
    }

    @Test
    fun `token expirado e invalido`() {
        val provider = JwtTokenProvider(secret, -1)

        val emitido = provider.emitir(usuario)

        assertThat(provider.validar(emitido.token)).isNull()
    }

    @Test
    fun `token assinado com outra chave e invalido`() {
        val provider = JwtTokenProvider(secret, 60)
        val outro = JwtTokenProvider("outro-segredo-de-teste-bem-longo-para-hmac-256!!", 60)

        val emitido = provider.emitir(usuario)

        assertThat(outro.validar(emitido.token)).isNull()
    }
}
