package com.fiap.soat.oficina.estoque.adapter.inbound.web

import com.fiap.soat.oficina.estoque.adapter.inbound.web.dto.ErroResponse
import com.fiap.soat.oficina.estoque.adapter.inbound.web.dto.ReservaRequest
import com.fiap.soat.oficina.estoque.adapter.inbound.web.dto.ReservaResponse
import com.fiap.soat.oficina.estoque.domain.model.ResultadoReserva
import com.fiap.soat.oficina.estoque.domain.port.inbound.BaixarPecasUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarReservasUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.LiberarReservaUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.ReservarPecaUseCase
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/estoque/ordens/{osId}")
@Tag(name = "Estoque - reservas", description = "Reserva, liberação e baixa de peças para uma OS")
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
            description = "Quantidade indisponível, item inativo ou OS que já reservou o item",
            content = [Content(schema = Schema(implementation = ErroResponse::class))],
        ),
    ],
)
class ReservaController(
    private val reservarPeca: ReservarPecaUseCase,
    private val liberarReserva: LiberarReservaUseCase,
    private val baixarPecas: BaixarPecasUseCase,
    private val consultarReservas: ConsultarReservasUseCase,
) {

    @PostMapping("/reservas")
    @Operation(
        summary = "Reserva um item de estoque para a OS",
        description = "Com disponível suficiente cria a reserva (201). Sem disponível, nada é reservado e responde 409.",
    )
    @ApiResponse(
        responseCode = "201",
        description = "Reserva criada",
        content = [Content(schema = Schema(implementation = ReservaResponse::class))],
    )
    fun reservar(
        @PathVariable("osId") osId: Long,
        @RequestBody request: ReservaRequest,
    ): ResponseEntity<*> =
        when (val resultado = reservarPeca.reservar(osId, request.itemId, request.quantidade)) {
            is ResultadoReserva.Reservada ->
                ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponse.de(resultado.reserva))
            is ResultadoReserva.Indisponivel ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ErroResponse("Quantidade indisponível em estoque (disponível: ${resultado.disponivel})", "quantidade"),
                )
        }

    @GetMapping("/reservas")
    @Operation(summary = "Consulta as reservas da OS (ativas, liberadas e consumidas)")
    fun consultar(@PathVariable("osId") osId: Long): List<ReservaResponse> =
        consultarReservas.porOs(osId).map(ReservaResponse::de)

    @PostMapping("/reservas/liberar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Libera as reservas ativas da OS, ou só a de um item (idempotente)")
    fun liberar(
        @PathVariable("osId") osId: Long,
        @Parameter(description = "Libera só a reserva deste item; omitido libera todas as da OS")
        @RequestParam("itemId", required = false) itemId: Long?,
    ) {
        liberarReserva.liberar(osId, itemId)
    }

    @PostMapping("/baixa")
    @Operation(
        summary = "Dá baixa nas reservas ativas da OS (tudo ou nada, idempotente)",
        description = "Diminui o saldo dos itens e consome as reservas; devolve as reservas consumidas, vazia se não havia ativas.",
    )
    fun baixar(@PathVariable("osId") osId: Long): List<ReservaResponse> =
        baixarPecas.baixar(osId).map(ReservaResponse::de)
}
