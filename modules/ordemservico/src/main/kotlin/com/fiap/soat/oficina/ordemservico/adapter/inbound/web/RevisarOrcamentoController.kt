package com.fiap.soat.oficina.ordemservico.adapter.inbound.web

import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.GerarOrcamentoRequest
import com.fiap.soat.oficina.ordemservico.adapter.inbound.web.dto.OrcamentoResponse
import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RevisarOrcamentoUseCase
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ordens/{id}/orcamento")
class RevisarOrcamentoController(
    private val revisarOrcamento: RevisarOrcamentoUseCase,
) {

    @PostMapping("/revisar")
    fun revisar(@PathVariable id: Long, @RequestBody request: GerarOrcamentoRequest): OrcamentoResponse {
        val itens = request.itens.map {
            ItemOrcamento(tipoDe(it.tipo), it.descricao, Dinheiro.of(it.valor), it.itemEstoqueId, it.quantidade)
        }
        return OrcamentoResponse.de(revisarOrcamento.revisar(id, itens))
    }

    private fun tipoDe(valor: String): TipoItem = when (valor.trim().lowercase()) {
        "servico", "serviço" -> TipoItem.SERVICO
        "peca", "peça" -> TipoItem.PECA
        else -> throw IllegalArgumentException("Tipo de item inválido: $valor")
    }
}
