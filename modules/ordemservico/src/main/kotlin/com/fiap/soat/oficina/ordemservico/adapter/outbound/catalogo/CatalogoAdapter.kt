package com.fiap.soat.oficina.ordemservico.adapter.outbound.catalogo

import com.fiap.soat.oficina.cadastro.domain.port.inbound.ConsultarServicoUseCase
import com.fiap.soat.oficina.estoque.domain.port.inbound.ConsultarItemUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.CatalogoPort
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.ItemCatalogo
import org.springframework.stereotype.Component
import com.fiap.soat.oficina.cadastro.domain.error.RegistroNaoEncontradoException as ServicoNaoEncontrado
import com.fiap.soat.oficina.estoque.domain.error.RegistroNaoEncontradoException as ItemNaoEncontrado

@Component
class CatalogoAdapter(
    private val servicos: ConsultarServicoUseCase,
    private val itensEstoque: ConsultarItemUseCase,
) : CatalogoPort {

    override fun buscarServico(id: Long): ItemCatalogo? = try {
        servicos.buscarPorId(id, somenteAtivos = true).let { ItemCatalogo(it.nome, it.preco) }
    } catch (_: ServicoNaoEncontrado) {
        null
    }

    override fun buscarPeca(id: Long): ItemCatalogo? = try {
        itensEstoque.buscarPorId(id, somenteAtivos = true).let { ItemCatalogo(it.nome, it.preco) }
    } catch (_: ItemNaoEncontrado) {
        null
    }
}
