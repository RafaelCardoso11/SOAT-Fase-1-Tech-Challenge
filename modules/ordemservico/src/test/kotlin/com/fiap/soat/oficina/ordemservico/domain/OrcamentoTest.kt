package com.fiap.soat.oficina.ordemservico.domain

import com.fiap.soat.oficina.ordemservico.domain.model.ItemOrcamento
import com.fiap.soat.oficina.ordemservico.domain.model.Orcamento
import com.fiap.soat.oficina.ordemservico.domain.model.TipoItem
import com.fiap.soat.oficina.shared.domain.vo.Dinheiro
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class OrcamentoTest {

    @Test
    fun `total e a soma dos itens`() {
        val orcamento = Orcamento(
            listOf(
                ItemOrcamento(TipoItem.SERVICO, "Troca de pastilhas", Dinheiro.of(BigDecimal("150.00"))),
                ItemOrcamento(TipoItem.PECA, "Pastilha de freio", Dinheiro.of(BigDecimal("200.00")), itemEstoqueId = 50L, quantidade = 1),
            ),
            validoAte = Instant.now().plusSeconds(3600),
        )

        assertThat(orcamento.total.valor).isEqualByComparingTo("350.00")
    }

    @Test
    fun `orcamento sem itens e invalido`() {
        assertThatThrownBy { Orcamento(emptyList(), validoAte = Instant.now()) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
