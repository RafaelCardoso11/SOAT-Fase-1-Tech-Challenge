package com.fiap.soat.oficina.estoque.adapter.inbound.web

import com.fiap.soat.oficina.estoque.adapter.inbound.web.dto.EntradaEstoqueRequest
import com.fiap.soat.oficina.estoque.adapter.inbound.web.dto.ErroResponse
import com.fiap.soat.oficina.estoque.adapter.inbound.web.dto.ItemEstoqueAlteracaoRequest
import com.fiap.soat.oficina.estoque.adapter.inbound.web.dto.ItemEstoqueRequest
import com.fiap.soat.oficina.estoque.adapter.inbound.web.dto.ItemEstoqueResponse
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarItemUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.GerenciarItemUseCase
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
@RequestMapping("/api/v1/admin/estoque/itens")
@Tag(name = "Estoque - itens", description = "Peças e insumos, saldo e entrada de estoque")
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
            description = "Item de estoque não encontrado",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "Nome duplicado, item inativo ou com reservas ativas",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
    ],
)
class ItemEstoqueController(
    private val gerenciar: GerenciarItemUseCase,
    private val consultar: ConsultarItemUseCase,
) {

    @PostMapping
    @Operation(summary = "Cadastra uma peça ou insumo")
    fun cadastrar(@RequestBody request: ItemEstoqueRequest): ResponseEntity<ItemEstoqueResponse> {
        val item = gerenciar.cadastrar(request.nome, request.descricao, request.tipo, request.preco, request.saldoInicial)
        val resposta = ItemEstoqueResponse.de(item)
        return ResponseEntity.created(localizacaoDe(resposta.id)).body(resposta)
    }

    @GetMapping
    @Operation(summary = "Lista os itens, com filtro opcional de ativos/inativos")
    fun listar(
        @Parameter(description = "true lista só ativos; false só inativos; omitido lista todos")
        @RequestParam("ativo", required = false) ativo: Boolean?,
    ): List<ItemEstoqueResponse> = consultar.listar(ativo).map(ItemEstoqueResponse::de)

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um item com saldo, reservado e disponível")
    fun buscarPorId(
        @PathVariable("id") id: Long,
        @Parameter(description = "true trata item inativo como não encontrado")
        @RequestParam("ativo", required = false) ativo: Boolean?,
    ): ItemEstoqueResponse = ItemEstoqueResponse.de(consultar.buscarPorId(id, ativo == true))

    @PutMapping("/{id}")
    @Operation(summary = "Altera nome, descrição, tipo e preço de um item ativo")
    fun alterar(
        @PathVariable("id") id: Long,
        @RequestBody request: ItemEstoqueAlteracaoRequest,
    ): ItemEstoqueResponse =
        ItemEstoqueResponse.de(gerenciar.alterar(id, request.nome, request.descricao, request.tipo, request.preco))

    @PostMapping("/{id}/entradas")
    @Operation(summary = "Registra uma entrada (reposição) de estoque")
    fun registrarEntrada(
        @PathVariable("id") id: Long,
        @RequestBody request: EntradaEstoqueRequest,
    ): ItemEstoqueResponse = ItemEstoqueResponse.de(gerenciar.registrarEntrada(id, request.quantidade))

    @PostMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa um item sem reservas ativas (idempotente)")
    fun inativar(@PathVariable("id") id: Long) {
        gerenciar.inativar(id)
    }
}
