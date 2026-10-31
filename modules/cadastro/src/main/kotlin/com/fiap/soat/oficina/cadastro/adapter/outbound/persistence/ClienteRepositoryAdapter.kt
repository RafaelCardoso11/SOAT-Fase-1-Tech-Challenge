package com.fiap.soat.oficina.cadastro.adapter.outbound.persistence

import com.fiap.soat.oficina.cadastro.domain.model.Cliente
import com.fiap.soat.oficina.cadastro.domain.port.outbound.ClienteRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ClienteRepositoryAdapter(
    private val springData: ClienteSpringDataRepository,
) : ClienteRepository {

    override fun salvar(cliente: Cliente): Cliente =
        traduzindoUnicidade("documento", "Já existe um cliente com este CPF/CNPJ") {
            springData.saveAndFlush(cliente.paraEntidade()).paraModelo()
        }

    override fun buscarPorId(id: Long): Cliente? = springData.findByIdOrNull(id)?.paraModelo()

    override fun buscarPorDocumento(documento: Documento): Cliente? =
        springData.findByDocumento(documento.valor)?.paraModelo()

    override fun existePorDocumento(documento: Documento): Boolean = springData.existsByDocumento(documento.valor)

    override fun listar(ativo: Boolean?): List<Cliente> =
        (if (ativo == null) springData.findAllByOrderById() else springData.findByAtivoOrderById(ativo))
            .map { it.paraModelo() }

    private fun Cliente.paraEntidade() = ClienteJpaEntity(id, documento.valor, nome, email.valor, ativo)

    private fun ClienteJpaEntity.paraModelo() = Cliente.reconstituir(requireNotNull(id), documento, nome, email, ativo)
}
