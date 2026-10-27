package com.fiap.soat.oficina.autenticacao.adapter.inbound.web

import com.fiap.soat.oficina.autenticacao.domain.port.outbound.TokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val tokenProvider: TokenProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            val nomeUsuario = tokenProvider.validar(token)
            if (nomeUsuario != null && SecurityContextHolder.getContext().authentication == null) {
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(nomeUsuario, null, emptyList())
            }
        }
        filterChain.doFilter(request, response)
    }
}
