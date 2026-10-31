package com.fiap.soat.oficina.ordemservico.application.usecase

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
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
}
