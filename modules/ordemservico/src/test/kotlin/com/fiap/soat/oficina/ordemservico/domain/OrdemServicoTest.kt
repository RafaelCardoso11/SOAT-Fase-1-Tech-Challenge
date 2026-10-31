package com.fiap.soat.oficina.ordemservico.domain

import com.fiap.soat.oficina.ordemservico.domain.model.OrdemServico
import com.fiap.soat.oficina.ordemservico.domain.model.StatusOS
import com.fiap.soat.oficina.ordemservico.domain.model.Veiculo
import com.fiap.soat.oficina.shared.domain.vo.Documento
import com.fiap.soat.oficina.shared.domain.vo.Placa
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrdemServicoTest {

    @Test
    fun `criar registra a transicao inicial RECEBIDA na linha do tempo`() {
        val os = OrdemServico.criar(
            Documento.of("123.456.789-09"),
            Veiculo(Placa.of("ABC1D23"), "Fiat", "Uno", 2020),
            "Barulho ao frear",
        )

        assertThat(os.status).isEqualTo(StatusOS.RECEBIDA)
        assertThat(os.transicoes).singleElement()
            .satisfies({ assertThat(it.status).isEqualTo(StatusOS.RECEBIDA) })
    }
}
