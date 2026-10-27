package com.fiap.soat.oficina.autenticacao.adapter.config

import com.fiap.soat.oficina.autenticacao.adapter.outbound.persistence.UsuarioJpaEntity
import com.fiap.soat.oficina.autenticacao.adapter.outbound.persistence.UsuarioSpringDataRepository
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.CodificadorDeSenha
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class SeedUsuariosDemo(
    private val usuarios: UsuarioSpringDataRepository,
    private val codificadorDeSenha: CodificadorDeSenha,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        criarSeAusente("admin", "admin@123")
        criarSeAusente("mecanico", "mecanico@123")
    }

    private fun criarSeAusente(nomeUsuario: String, senha: String) {
        if (usuarios.findByNomeUsuario(nomeUsuario) == null) {
            usuarios.save(
                UsuarioJpaEntity(
                    nomeUsuario = nomeUsuario,
                    senhaHash = codificadorDeSenha.codificar(senha),
                    ativo = true,
                ),
            )
        }
    }
}
