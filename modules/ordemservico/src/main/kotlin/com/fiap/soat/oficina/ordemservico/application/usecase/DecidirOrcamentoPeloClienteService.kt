package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.AprovarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.DecidirOrcamentoPeloClienteUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.RejeitarOrcamentoUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository

class DecidirOrcamentoPeloClienteService(
    private val repositorio: OrdemServicoRepository,
    private val aprovarOrcamento: AprovarOrcamentoUseCase,
    private val rejeitarOrcamento: RejeitarOrcamentoUseCase,
) : DecidirOrcamentoPeloClienteUseCase {

    override fun consultar(token: String): OrdemServico =
        repositorio.buscarPorTokenAprovacao(token) ?: throw NoSuchElementException("Orçamento não encontrado")

    override fun aprovar(token: String): OrdemServico = aprovarOrcamento.aprovar(idDa(token))

    override fun rejeitar(token: String, motivo: String?): OrdemServico =
        rejeitarOrcamento.rejeitar(idDa(token), motivo)

    private fun idDa(token: String): Long = consultar(token).id!!
}
