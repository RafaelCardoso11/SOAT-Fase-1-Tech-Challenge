package com.fiap.soat.oficina.cadastro.adapter.inbound.web

import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.ErroResponse
import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.VeiculoAlteracaoRequest
import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.VeiculoRequest
import com.fiap.soat.oficina.cadastro.adapter.inbound.web.dto.VeiculoResponse
import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarVeiculoUseCase
import com.fiap.soat.oficina.cadastro.domain.port.inbound.GerenciarVeiculoUseCase
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
@RequestMapping("/api/v1/admin/veiculos")
@Tag(name = "Veículos", description = "Cadastro de veículos")
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
            description = "Veículo ou cliente não encontrado",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "Placa duplicada, veículo inativo ou cliente inativo",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
    ],
)
class VeiculoController(
    private val gerenciar: GerenciarVeiculoUseCase,
    private val consultar: ConsultarVeiculoUseCase,
) {

    @PostMapping
    @Operation(summary = "Cadastra um veículo para um cliente ativo")
    fun cadastrar(@RequestBody request: VeiculoRequest): ResponseEntity<VeiculoResponse> {
        val veiculo = gerenciar.cadastrar(request.placa, request.marca, request.modelo, request.ano, request.clienteId)
        val resposta = VeiculoResponse.de(veiculo)
        return ResponseEntity.created(localizacaoDe(resposta.id)).body(resposta)
    }

    @GetMapping
    @Operation(summary = "Lista os veículos, com filtros opcionais de cliente e de ativos/inativos")
    fun listar(
        @Parameter(description = "true lista só ativos; false só inativos; omitido lista todos")
        @RequestParam("ativo", required = false) ativo: Boolean?,
        @Parameter(description = "Restringe aos veículos deste cliente")
        @RequestParam("clienteId", required = false) clienteId: Long?,
    ): List<VeiculoResponse> = consultar.listar(ativo, clienteId).map(VeiculoResponse::de)

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um veículo pelo identificador")
    fun buscarPorId(
        @PathVariable("id") id: Long,
        @Parameter(description = "true trata veículo inativo como não encontrado")
        @RequestParam("ativo", required = false) ativo: Boolean?,
    ): VeiculoResponse = VeiculoResponse.de(consultar.buscarPorId(id, ativo == true))

    @GetMapping("/placa/{placa}")
    @Operation(summary = "Busca um veículo pela placa")
    fun buscarPorPlaca(
        @PathVariable("placa") placa: String,
        @Parameter(description = "true trata veículo inativo como não encontrado")
        @RequestParam("ativo", required = false) ativo: Boolean?,
    ): VeiculoResponse = VeiculoResponse.de(consultar.buscarPorPlaca(placa, ativo == true))

    @PutMapping("/{id}")
    @Operation(summary = "Altera marca, modelo, ano e dono de um veículo ativo")
    fun alterar(
        @PathVariable("id") id: Long,
        @RequestBody request: VeiculoAlteracaoRequest,
    ): VeiculoResponse =
        VeiculoResponse.de(gerenciar.alterar(id, request.marca, request.modelo, request.ano, request.clienteId))

    @PostMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa um veículo (idempotente)")
    fun inativar(@PathVariable("id") id: Long) {
        gerenciar.inativar(id)
    }
}
