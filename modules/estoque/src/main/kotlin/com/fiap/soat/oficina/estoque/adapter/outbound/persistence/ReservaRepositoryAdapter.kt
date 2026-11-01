package com.fiap.soat.oficina.estoque.adapter.outbound.persistence

import com.fiap.soat.oficina.estoque.domain.model.Reserva
import com.fiap.soat.oficina.estoque.domain.model.SituacaoReserva
import com.fiap.soat.oficina.estoque.domain.port.outbound.ReservaRepository
import org.springframework.stereotype.Component

@Component
class ReservaRepositoryAdapter(
    private val springData: ReservaSpringDataRepository,
) : ReservaRepository {

    override fun salvar(reserva: Reserva): Reserva =
        traduzindoUnicidade("itemId", "A OS já possui uma reserva ativa para este item") {
            springData.saveAndFlush(reserva.paraEntidade()).paraModelo()
        }

    override fun buscarAtivaPorOsEItem(osId: Long, itemId: Long): Reserva? =
        springData.findByOsIdAndItemIdAndSituacao(osId, itemId, SituacaoReserva.ATIVA.name)?.paraModelo()

    override fun listarAtivasPorOs(osId: Long): List<Reserva> =
        springData.findByOsIdAndSituacaoOrderByItemId(osId, SituacaoReserva.ATIVA.name).map { it.paraModelo() }

    override fun listarPorOs(osId: Long): List<Reserva> =
        springData.findByOsIdOrderById(osId).map { it.paraModelo() }

    private fun Reserva.paraEntidade() = ReservaJpaEntity(id, itemId, osId, quantidade.valor, situacao.name)

    private fun ReservaJpaEntity.paraModelo() =
        Reserva.reconstituir(requireNotNull(id), osId, itemId, quantidade, situacao)
}
