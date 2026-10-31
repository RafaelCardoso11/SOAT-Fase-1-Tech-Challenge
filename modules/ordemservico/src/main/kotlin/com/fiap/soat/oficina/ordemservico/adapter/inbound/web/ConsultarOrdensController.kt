package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrdemServicoResponse
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ConsultarOrdensUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public/ordens")
class ConsultarOrdensController(
    private val consultarOrdens: ConsultarOrdensUseCase,
) {

    @GetMapping("/{numero}")
    fun porNumero(@PathVariable numero: Long): ResponseEntity<OrdemServicoResponse> =
        consultarOrdens.porNumero(numero)
            ?.let { ResponseEntity.ok(OrdemServicoResponse.de(it)) }
            ?: ResponseEntity.notFound().build()

    @GetMapping
    fun porIdentificador(
        @RequestParam(required = false) placa: String?,
        @RequestParam(required = false) documento: String?,
    ): List<OrdemServicoResponse> = when {
        placa != null -> consultarOrdens.porPlaca(placa)
        documento != null -> consultarOrdens.porDocumento(documento)
        else -> emptyList()
    }.map(OrdemServicoResponse::de)
}
