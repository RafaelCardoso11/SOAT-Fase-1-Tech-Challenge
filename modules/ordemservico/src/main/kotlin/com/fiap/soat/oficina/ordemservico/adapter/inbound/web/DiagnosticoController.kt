package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.FinalizarDiagnosticoRequest
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.RegistrarInspecaoRequest
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RealizarDiagnosticoUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens/{id}/diagnostico")
class DiagnosticoController(
    private val realizarDiagnostico: RealizarDiagnosticoUseCase,
) {

    @PostMapping("/iniciar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun iniciar(@PathVariable id: Long) = realizarDiagnostico.iniciar(id)

    @PostMapping("/inspecao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun inspecao(@PathVariable id: Long, @RequestBody request: RegistrarInspecaoRequest) =
        realizarDiagnostico.registrarInspecao(id, request.observacoes)

    @PostMapping("/finalizar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun finalizar(@PathVariable id: Long, @RequestBody request: FinalizarDiagnosticoRequest) =
        realizarDiagnostico.finalizar(id, request.pecaIdentificada)
}
