package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrdemServicoDetalheResponse
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.PaginaResponse
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.ResumoOrdemServicoResponse
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ConsultarOrdensUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens")
class ConsultarOrdensAdminController(
    private val consultarOrdens: ConsultarOrdensUseCase,
) {

    @GetMapping
    fun listar(
        @RequestParam("status", required = false) status: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int,
    ): PaginaResponse<ResumoOrdemServicoResponse> =
        PaginaResponse.de(consultarOrdens.listar(status?.let(::statusDe), page, size), ResumoOrdemServicoResponse::de)

    @GetMapping("/{id}")
    fun detalhar(@PathVariable("id") id: Long): OrdemServicoDetalheResponse =
        OrdemServicoDetalheResponse.de(consultarOrdens.detalhar(id))

    private fun statusDe(valor: String): StatusOS =
        StatusOS.entries.firstOrNull { it.name == valor.trim().uppercase() }
            ?: throw IllegalArgumentException("Status inválido: use ${StatusOS.entries.joinToString(", ")}")
}
