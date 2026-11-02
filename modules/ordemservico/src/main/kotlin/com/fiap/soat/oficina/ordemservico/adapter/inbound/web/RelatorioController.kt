package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ConsultarTempoMedioExecucaoUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class TempoMedioExecucaoResponse(
    val temDados: Boolean,
    val mediaMinutos: Long?,
    val mensagem: String?,
)

@RestController
@RequestMapping("/api/admin/relatorios")
class RelatorioController(
    private val consultarTempoMedio: ConsultarTempoMedioExecucaoUseCase,
) {

    @GetMapping("/tempo-medio-execucao")
    fun tempoMedioExecucao(): TempoMedioExecucaoResponse {
        val media = consultarTempoMedio.consultar()
        return if (media == null) {
            TempoMedioExecucaoResponse(false, null, "Não há dados suficientes para calcular o tempo médio")
        } else {
            TempoMedioExecucaoResponse(true, media.toMinutes(), null)
        }
    }
}
