package com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico

data class RegistrarInspecaoRequest(val observacoes: String)

data class FinalizarDiagnosticoRequest(val pecaIdentificada: Boolean)

data class DiagnosticoResponse(val status: String, val liberadoParaOrcamento: Boolean) {
    companion object {
        fun de(os: OrdemServico) = DiagnosticoResponse(os.status.name, os.liberadoParaOrcamento)
    }
}
