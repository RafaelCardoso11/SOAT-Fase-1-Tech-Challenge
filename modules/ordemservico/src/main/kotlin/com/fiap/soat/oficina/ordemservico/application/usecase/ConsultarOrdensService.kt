package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.Pagina
import com.fiap.soat.oficina.ordemservico.domain.model.ResumoOrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.port.inbound.ConsultarOrdensUseCase
import com.fiap.soat.oficina.ordemservico.domain.port.outbound.OrdemServicoRepository
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa

class ConsultarOrdensService(
    private val repositorio: OrdemServicoRepository,
) : ConsultarOrdensUseCase {

    override fun porNumero(numero: Long): OrdemServico? = repositorio.buscarPorId(numero)

    override fun porPlaca(placa: String): List<OrdemServico> =
        runCatching { Placa.of(placa).valor }.map(repositorio::buscarPorPlaca).getOrDefault(emptyList())

    override fun porDocumento(documento: String): List<OrdemServico> =
        runCatching { Documento.of(documento).valor }.map(repositorio::buscarPorDocumento).getOrDefault(emptyList())

    override fun listar(status: StatusOS?, pagina: Int, tamanho: Int): Pagina<ResumoOrdemServico> {
        require(pagina >= 0) { "A página deve ser maior ou igual a zero" }
        require(tamanho in 1..TAMANHO_MAXIMO) { "O tamanho da página deve estar entre 1 e $TAMANHO_MAXIMO" }
        return repositorio.listar(status, pagina, tamanho)
    }

    override fun detalhar(numero: Long): OrdemServico =
        repositorio.buscarPorId(numero) ?: throw NoSuchElementException("Ordem de serviço não encontrada")

    private companion object {
        const val TAMANHO_MAXIMO = 100
    }
}
