package com.fiap.soat.oficina.ordemservico.domain.model

import com.fiap.soat.oficina.shared.domain.vo.Placa

class Veiculo(
    val placa: Placa,
    val marca: String,
    val modelo: String,
    val ano: Int,
)
