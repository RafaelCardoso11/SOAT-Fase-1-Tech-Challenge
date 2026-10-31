package com.fiap.soat.oficina.ordemservico.domain.model

import java.time.Instant

data class TransicaoStatus(val status: StatusOS, val momento: Instant)
