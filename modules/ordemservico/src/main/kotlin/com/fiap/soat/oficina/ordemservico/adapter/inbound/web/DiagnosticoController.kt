package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.DiagnosticoResponse
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.FinalizarDiagnosticoRequest
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.RegistrarInspecaoRequest
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RealizarDiagnosticoUseCase
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens/{id}/diagnostico")
class DiagnosticoController(
    private val realizarDiagnostico: RealizarDiagnosticoUseCase,
) {

    @PostMapping("/iniciar")
    fun iniciar(@PathVariable id: Long) = DiagnosticoResponse.de(realizarDiagnostico.iniciar(id))

    @PostMapping("/inspecao")
    fun inspecao(@PathVariable id: Long, @RequestBody request: RegistrarInspecaoRequest) =
        DiagnosticoResponse.de(realizarDiagnostico.registrarInspecao(id, request.observacoes))

    @PostMapping("/finalizar")
    fun finalizar(@PathVariable id: Long, @RequestBody request: FinalizarDiagnosticoRequest) =
        DiagnosticoResponse.de(realizarDiagnostico.finalizar(id, request.pecaIdentificada))
}
