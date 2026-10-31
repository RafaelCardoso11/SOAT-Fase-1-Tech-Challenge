package com.fiap.soat.oficina.cadastro.adapter.outbound.persistence

import com.fiap.soat.oficina.cadastro.domain.model.Servico
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ServicoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ServicoRepositoryAdapter(
    private val springData: ServicoSpringDataRepository,
) : ServicoRepository {

    override fun salvar(servico: Servico): Servico =
        traduzindoUnicidade("nome", "Já existe um serviço com este nome") {
            springData.saveAndFlush(servico.paraEntidade()).paraModelo()
        }

    override fun buscarPorId(id: Long): Servico? = springData.findByIdOrNull(id)?.paraModelo()

    override fun existePorNome(nome: String, ignorandoId: Long?): Boolean =
        if (ignorandoId == null) {
            springData.existePorNome(nome)
        } else {
            springData.existePorNomeExcetoId(nome, ignorandoId)
        }

    override fun listar(ativo: Boolean?): List<Servico> =
        (if (ativo == null) springData.findAllByOrderById() else springData.findByAtivoOrderById(ativo))
            .map { it.paraModelo() }

    private fun Servico.paraEntidade() = ServicoJpaEntity(id, nome, descricao, preco.valor, ativo)

    private fun ServicoJpaEntity.paraModelo() = Servico.reconstituir(requireNotNull(id), nome, descricao, preco, ativo)
}
