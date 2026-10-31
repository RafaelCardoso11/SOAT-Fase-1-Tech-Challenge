package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.CriarOrdemServicoRequest
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrdemServicoCriadaResponse
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.CriarOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.CriarOrdemServicoUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens")
class OrdemServicoController(
    private val criarOrdemServico: CriarOrdemServicoUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun criar(@RequestBody request: CriarOrdemServicoRequest): OrdemServicoCriadaResponse {
        val id = criarOrdemServico.criar(
            CriarOrdemServico(
                documento = request.documento,
                placa = request.placa,
                marca = request.marca,
                modelo = request.modelo,
                ano = request.ano,
                relato = request.relato,
            ),
        )
        return OrdemServicoCriadaResponse(id, StatusOS.RECEBIDA.name)
    }
}
