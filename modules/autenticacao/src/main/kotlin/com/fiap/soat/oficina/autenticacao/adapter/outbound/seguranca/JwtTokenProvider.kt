package com.fiap.soat.oficina.autenticacao.adapter.outbound.seguranca

import com.fiap.soat.oficina.autenticacao.domain.model.TokenEmitido
import com.fiap.soat.oficina.autenticacao.domain.model.Usuario
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.TokenProvider
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-minutes}") private val expiracaoMinutos: Long,
) : TokenProvider {

    private val chave: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    override fun emitir(usuario: Usuario): TokenEmitido {
        val agora = Instant.now()
        val expiraEm = agora.plusSeconds(expiracaoMinutos * 60)
        val token = Jwts.builder()
            .subject(usuario.nomeUsuario)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(expiraEm))
            .signWith(chave)
            .compact()
        return TokenEmitido(token, expiracaoMinutos * 60)
    }

    override fun validar(token: String): String? =
        try {
            Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}
