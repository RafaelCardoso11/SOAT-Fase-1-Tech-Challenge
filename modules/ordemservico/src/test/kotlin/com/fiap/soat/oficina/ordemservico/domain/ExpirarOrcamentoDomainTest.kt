package com.fiap.soat.oficina.ordemservico.domain

import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.Orcamento
import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.ordemservico.domain.model.TransicaoStatus
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class ExpirarOrcamentoDomainTest {

    private fun os(status: StatusOS, validoAte: Instant) = OrdemServico.reconstituir(
        id = 1L,
        documento = Documento.of("123.456.789-09"),
        veiculo = Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
        relato = "Barulho ao frear",
        status = status,
        transicoes = listOf(TransicaoStatus(status, Instant.now())),
        liberadoParaOrcamento = true,
        orcamento = Orcamento(
            listOf(ItemOrcamento(TipoItem.PECA, "Pastilha de freio", Dinheiro.of(BigDecimal("200.00")), itemEstoqueId = 50L, quantidade = 1)),
            validoAte = validoAte,
        ),
    )

    @Test
    fun `expira vencido aguardando aprovacao`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO, Instant.now().minusSeconds(3600))

        ordem.expirarOrcamento(Instant.now())

        assertThat(ordem.status).isEqualTo(StatusOS.FINALIZADA)
    }

    @Test
    fun `nao expira dentro do prazo`() {
        val ordem = os(StatusOS.AGUARDANDO_APROVACAO, Instant.now().plusSeconds(3600))

        assertThatThrownBy { ordem.expirarOrcamento(Instant.now()) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(ordem.status).isEqualTo(StatusOS.AGUARDANDO_APROVACAO)
    }

    @Test
    fun `nao expira fora de aguardando aprovacao`() {
        val ordem = os(StatusOS.EM_EXECUCAO, Instant.now().minusSeconds(3600))

        assertThatThrownBy { ordem.expirarOrcamento(Instant.now()) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(ordem.status).isEqualTo(StatusOS.EM_EXECUCAO)
    }
}
