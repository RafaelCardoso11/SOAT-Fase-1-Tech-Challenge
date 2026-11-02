package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.domain.port.inbound.IniciarExecucaoUseCase
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens/{id}/execucao")
class ExecucaoController(
    private val iniciarExecucao: IniciarExecucaoUseCase,
) {

    @PostMapping("/iniciar")
    fun iniciar(@PathVariable id: Long) = mapOf("status" to iniciarExecucao.iniciar(id).status.name)
}
