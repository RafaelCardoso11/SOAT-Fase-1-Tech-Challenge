package com.fiap.soat.oficina.autenticacao.adapter.inbound.web

import com.fiap.soat.oficina.autenticacao.domain.port.inbound.AutenticarUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AutenticacaoController(
    private val autenticarUseCase: AutenticarUseCase,
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        val emitido = autenticarUseCase.autenticar(request.nomeUsuario, request.senha)
        return LoginResponse(emitido.token, "Bearer", emitido.expiraEmSegundos)
    }
}

data class LoginRequest(val nomeUsuario: String, val senha: String)

data class LoginResponse(val token: String, val tipo: String, val expiraEmSegundos: Long)
