package com.fiap.soat.oficina.cadastro.adapter.inbound.web

import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.ClienteAlteracaoRequest
import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.ClienteRequest
import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.ClienteResponse
import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.ErroResponse
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarClienteUseCase
import com.fiap.soat.oficina.cadastro.domain.port.inbound.GerenciarClienteUseCase
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
@RequestMapping("/api/v1/admin/clientes")
@Tag(name = "Clientes", description = "Cadastro de clientes")
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
            description = "Cliente não encontrado",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "Documento duplicado, cliente inativo ou com veículos ativos",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
    ],
)
class ClienteController(
    private val gerenciar: GerenciarClienteUseCase,
    private val consultar: ConsultarClienteUseCase,
) {

    @PostMapping
    @Operation(summary = "Cadastra um cliente")
    fun cadastrar(@RequestBody request: ClienteRequest): ResponseEntity<ClienteResponse> {
        val cliente = gerenciar.cadastrar(request.documento, request.nome, request.email)
        val resposta = ClienteResponse.de(cliente)
        return ResponseEntity.created(localizacaoDe(resposta.id)).body(resposta)
    }

    @GetMapping
    @Operation(summary = "Lista os clientes, com filtro opcional de ativos/inativos")
    fun listar(
        @Parameter(description = "true lista só ativos; false só inativos; omitido lista todos")
        @RequestParam("ativo", required = false) ativo: Boolean?,
    ): List<ClienteResponse> = consultar.listar(ativo).map(ClienteResponse::de)

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um cliente pelo identificador")
    fun buscarPorId(
        @PathVariable("id") id: Long,
        @Parameter(description = "true trata cliente inativo como não encontrado")
        @RequestParam("ativo", required = false) ativo: Boolean?,
    ): ClienteResponse = ClienteResponse.de(consultar.buscarPorId(id, ativo == true))

    @GetMapping("/documento/{documento}")
    @Operation(summary = "Busca um cliente pelo CPF/CNPJ")
    fun buscarPorDocumento(
        @PathVariable("documento") documento: String,
        @Parameter(description = "true trata cliente inativo como não encontrado")
        @RequestParam("ativo", required = false) ativo: Boolean?,
    ): ClienteResponse = ClienteResponse.de(consultar.buscarPorDocumento(documento, ativo == true))

    @PutMapping("/{id}")
    @Operation(summary = "Altera nome e e-mail de um cliente ativo")
    fun alterar(
        @PathVariable("id") id: Long,
        @RequestBody request: ClienteAlteracaoRequest,
    ): ClienteResponse = ClienteResponse.de(gerenciar.alterar(id, request.nome, request.email))

    @PostMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa um cliente sem veículos ativos (idempotente)")
    fun inativar(@PathVariable("id") id: Long) {
        gerenciar.inativar(id)
    }
}
