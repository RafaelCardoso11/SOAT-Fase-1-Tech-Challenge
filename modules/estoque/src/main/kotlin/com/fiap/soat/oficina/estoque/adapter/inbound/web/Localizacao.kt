package com.fiap.soat.oficina.estoque.adapter.inbound.web

import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI

internal fun localizacaoDe(id: Long): URI =
    ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()
