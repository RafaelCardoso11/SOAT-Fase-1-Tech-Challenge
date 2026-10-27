package com.fiap.soat.oficina.autenticacao.application

import com.fiap.soat.oficina.autenticacao.application.usecase.AutenticarService
import com.fiap.soat.oficina.autenticacao.domain.error.CredenciaisInvalidasException
import com.fiap.soat.oficina.autenticacao.domain.model.TokenEmitido
import com.fiap.soat.oficina.autenticacao.domain.model.Usuario
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.CodificadorDeSenha
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.TokenProvider
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.UsuarioRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AutenticarServiceTest {

    private val usuarios = mockk<UsuarioRepository>()
    private val codificador = mockk<CodificadorDeSenha>()
    private val tokens = mockk<TokenProvider>()
    private val service = AutenticarService(usuarios, codificador, tokens)

    private val usuarioAtivo = Usuario(1L, "admin", "hash", true)

    @Test
    fun `credenciais validas emitem token`() {
        every { usuarios.buscarPorNomeUsuario("admin") } returns usuarioAtivo
        every { codificador.confere("senha", "hash") } returns true
        every { tokens.emitir(usuarioAtivo) } returns TokenEmitido("jwt", 3600)

        val resultado = service.autenticar("admin", "senha")

        assertThat(resultado.token).isEqualTo("jwt")
        assertThat(resultado.expiraEmSegundos).isEqualTo(3600)
    }

    @Test
    fun `senha incorreta e rejeitada`() {
        every { usuarios.buscarPorNomeUsuario("admin") } returns usuarioAtivo
        every { codificador.confere("errada", "hash") } returns false

        assertThatThrownBy { service.autenticar("admin", "errada") }
            .isInstanceOf(CredenciaisInvalidasException::class.java)
    }

    @Test
    fun `usuario inexistente e rejeitado com a mesma falha`() {
        every { usuarios.buscarPorNomeUsuario("fantasma") } returns null

        assertThatThrownBy { service.autenticar("fantasma", "qualquer") }
            .isInstanceOf(CredenciaisInvalidasException::class.java)
    }

    @Test
    fun `usuario inativo e rejeitado`() {
        every { usuarios.buscarPorNomeUsuario("inativo") } returns Usuario(2L, "inativo", "hash", false)

        assertThatThrownBy { service.autenticar("inativo", "senha") }
            .isInstanceOf(CredenciaisInvalidasException::class.java)
    }
}
