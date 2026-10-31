package com.fiap.soat.oficina.cadastro.adapter.inbound.web

import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.ErroResponse
import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.ServicoRequest
import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.ServicoResponse
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarServicoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.inbound.GerenciarServicoUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/servicos")
@Tag(name = "Serviços", description = "Catálogo de serviços da oficina")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "400",
            description = "Dado inválido",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
        ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado", content = [Content()]),
        ApiResponse(
            responseCode = "404",
            description = "Serviço não encontrado",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "Nome duplicado ou serviço inativo",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
    ],
)
class ServicoController(
    private val gerenciar: GerenciarServicoUseCase,
    private val consultar: ConsultarServicoUseCase,
) {

    @PostMapping
    @Operation(summary = "Cadastra um serviço no catálogo")
    fun cadastrar(@RequestBody request: ServicoRequest): ResponseEntity<ServicoResponse> {
        val servico = gerenciar.cadastrar(request.nome, request.descricao, request.preco)
        val resposta = ServicoResponse.de(servico)
        return ResponseEntity.created(localizacaoDe(resposta.id)).body(resposta)
    }

    @GetMapping
    @Operation(summary = "Lista os serviços, com filtro opcional de ativos/inativos")
    fun listar(
        @Parameter(description = "true lista só ativos; false só inativos; omitido lista todos")
        @RequestParam("ativo", required = false) ativo: Boolean?,
    ): List<ServicoResponse> = consultar.listar(ativo).map(ServicoResponse::de)

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um serviço pelo identificador")
    fun buscarPorId(
        @PathVariable("id") id: Long,
        @Parameter(description = "true trata serviço inativo como não encontrado")
        @RequestParam("ativo", required = false) ativo: Boolean?,
    ): ServicoResponse = ServicoResponse.de(consultar.buscarPorId(id, ativo == true))

    @PutMapping("/{id}")
    @Operation(summary = "Altera nome, descrição e preço de um serviço ativo")
    fun alterar(
        @PathVariable("id") id: Long,
        @RequestBody request: ServicoRequest,
    ): ServicoResponse = ServicoResponse.de(gerenciar.alterar(id, request.nome, request.descricao, request.preco))

    @PostMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa um serviço (idempotente)")
    fun inativar(@PathVariable("id") id: Long) {
        gerenciar.inativar(id)
    }
}
