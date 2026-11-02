package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.domain.port.inbound.FinalizarExecucaoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.IniciarExecucaoUseCase
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class FinalizarExecucaoRequest(val todosAplicados: Boolean = false)

@RestController
@RequestMapping("/api/admin/ordens/{id}/execucao")
class ExecucaoController(
    private val iniciarExecucao: IniciarExecucaoUseCase,
    private val finalizarExecucao: FinalizarExecucaoUseCase,
) {

    @PostMapping("/iniciar")
    fun iniciar(@PathVariable id: Long) = mapOf("status" to iniciarExecucao.iniciar(id).status.name)

    @PostMapping("/finalizar")
    fun finalizar(@PathVariable id: Long, @RequestBody request: FinalizarExecucaoRequest) =
        mapOf("status" to finalizarExecucao.finalizar(id, request.todosAplicados).status.name)
}
