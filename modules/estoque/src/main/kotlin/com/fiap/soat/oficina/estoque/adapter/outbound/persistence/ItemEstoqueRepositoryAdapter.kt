package com.fiap.soat.oficina.estoque.adapter.outbound.persistence

import com.fiap.soat.oficina.estoque.domain.model.ItemEstoque
import com.fiap.soat.oficina.estoque.domain.port.outbound.ItemEstoqueRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ItemEstoqueRepositoryAdapter(
    private val springData: ItemEstoqueSpringDataRepository,
) : ItemEstoqueRepository {

    override fun salvar(item: ItemEstoque): ItemEstoque =
        traduzindoUnicidade("nome", "Já existe um item de estoque com este nome") {
            springData.saveAndFlush(item.paraEntidade()).paraModelo()
        }

    override fun buscarPorId(id: Long): ItemEstoque? = springData.findByIdOrNull(id)?.paraModelo()

    override fun buscarPorIdParaAtualizar(id: Long): ItemEstoque? = springData.buscarComLock(id)?.paraModelo()

    override fun existePorNome(nome: String, ignorandoId: Long?): Boolean =
        if (ignorandoId == null) {
            springData.existePorNome(nome)
        } else {
            springData.existePorNomeExcetoId(nome, ignorandoId)
        }

    override fun listar(ativo: Boolean?): List<ItemEstoque> =
        (if (ativo == null) springData.findAllByOrderById() else springData.findByAtivoOrderById(ativo))
            .map { it.paraModelo() }

    private fun ItemEstoque.paraEntidade() =
        ItemEstoqueJpaEntity(id, nome, descricao, tipo.name, preco.valor, saldo, reservado, ativo)

    private fun ItemEstoqueJpaEntity.paraModelo() =
        ItemEstoque.reconstituir(requireNotNull(id), nome, descricao, tipo, preco, saldo, reservado, ativo)
}
