package com.fiap.soat.oficina.cadastro.domain.port.outbound

import com.fiap.soat.oficina.cadastro.domain.model.Servico

interface ServicoRepository {
    fun salvar(servico: Servico): Servico
    fun buscarPorId(id: Long): Servico?

    /** Compara o nome sem diferenciar maiúsculas/minúsculas; [ignorandoId] exclui o próprio serviço ao renomear. */
    fun existePorNome(nome: String, ignorandoId: Long? = null): Boolean
    fun listar(ativo: Boolean?): List<Servico>
}
