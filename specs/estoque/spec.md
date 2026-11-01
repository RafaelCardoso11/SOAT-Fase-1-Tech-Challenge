# Estoque de peças e insumos

**Issue:** #58
**Status:** Ready for Plan

## Overview / Problema

A oficina perde o controle de peças e insumos: não sabe o que tem disponível, uma mesma peça é prometida a
mais de um cliente e a baixa depende de anotações. O orçamento e a execução da Ordem de Serviço (OS)
dependem de saber se a peça existe, de garantir que ela fique separada para aquela OS até a execução e de
dar baixa quando o serviço é feito. Precisamos cadastrar peças e insumos, controlar o saldo e oferecer
reserva, liberação e baixa de forma confiável, mesmo com várias OS disputando o mesmo item.

## Goals & Non-Goals

**Goals:**
- Cadastrar, consultar, listar, alterar e inativar itens de estoque (peças e insumos).
- Registrar a entrada (reposição) de estoque e informar saldo, quantidade reservada e quantidade disponível.
- Reservar itens para uma OS, garantindo que um item reservado não seja prometido a outra OS.
- Liberar reservas (OS rejeitada, expirada, encerrada ou orçamento revisado) e dar baixa definitiva dos itens reservados quando a execução começa.
- Registrar os fatos de negócio do Event Storming: peça reservada, peça indisponível, reserva liberada e peças baixadas.

**Non-Goals:**
- Histórico de movimentações (kardex), estoque mínimo e alertas de reposição.
- Compras, fornecedores, lotes, múltiplos depósitos, preço de custo e unidades de medida (as quantidades são inteiras).
- Escolha automática de peça alternativa quando um item está indisponível (decisão do mecânico, no fluxo da OS).
- Notificar atendente, mecânico ou cliente (responsabilidade do fluxo da OS).
- Regras de orçamento, aprovação e status da OS.
- Reativação de itens inativos e exclusão definitiva.
- Autorização por perfil (qualquer usuário autenticado acessa o estoque).

## User Stories

- Como **atendente**, quero cadastrar uma peça ou insumo com nome, descrição, tipo e preço, para que ele possa entrar em orçamentos.
- Como **atendente**, quero registrar a entrada de estoque de um item, para refletir as reposições.
- Como **atendente ou mecânico**, quero consultar o saldo, o reservado e o disponível de um item, para saber o que posso prometer.
- Como **atendente**, quero listar e detalhar itens e alterar nome, descrição e preço, para manter o cadastro correto.
- Como **atendente**, quero inativar um item que não é mais usado, sem perder o histórico.
- Como **sistema (módulo de OS)**, quero reservar um item para uma OS, para garantir a disponibilidade até a execução.
- Como **sistema (módulo de OS)**, quero liberar as reservas de uma OS (ou de um item dela), para devolver o item ao estoque disponível.
- Como **sistema (módulo de OS)**, quero dar baixa nos itens reservados de uma OS ao iniciar a execução, para refletir o consumo real.
- Como **mecânico**, quero consultar as reservas de uma OS, para conferir o que foi separado.

Comportamento (BDD):
- Dado um item com 3 unidades disponíveis / Quando reservo 1 para uma OS / Então a reserva é criada, vinculada à OS, e restam 2 disponíveis.
- Dado um item com 0 unidades disponíveis / Quando tento reservar 1 / Então nenhuma reserva é criada e o fato "peça indisponível" é registrado.
- Dado 1 unidade já reservada para uma OS / Quando outra OS tenta reservar o mesmo item / Então a reserva da outra OS não é criada e "peça indisponível" é registrado.
- Dada uma OS com itens reservados / Quando a reserva é liberada / Então as quantidades voltam ao disponível e "reserva liberada" é registrado.
- Dada uma OS com itens reservados / Quando a execução inicia e a baixa é solicitada / Então o saldo diminui, as reservas são consumidas e "peças baixadas" é registrado.
- Dada uma entrada de 10 unidades / Quando registrada / Então o saldo do item aumenta em 10.

## Functional Requirements

**Item de estoque (peça ou insumo)**
- `FR-001`: O sistema MUST permitir cadastrar um item com nome, descrição (opcional), tipo (peça ou insumo), preço de venda e saldo inicial (opcional, padrão zero).
- `FR-002`: O sistema MUST rejeitar nome ausente, tipo inválido, preço ausente ou negativo e saldo inicial negativo; o preço é em reais, com duas casas decimais.
- `FR-003`: O sistema MUST impedir dois itens com o mesmo nome (sem diferenciar maiúsculas/minúsculas), inclusive se o existente estiver inativo.
- `FR-004`: O sistema MUST permitir alterar nome, descrição, tipo e preço; a alteração MUST NOT mudar o saldo nem as reservas.
- `FR-005`: O sistema MUST permitir consultar um item por identificador e listar itens, com filtro por ativos/inativos.
- `FR-006`: Toda consulta de item MUST informar o saldo, a quantidade reservada e a quantidade disponível (saldo menos reservado).
- `FR-007`: O sistema MUST permitir inativar um item; o item inativo permanece consultável e preserva o histórico.
- `FR-008`: O sistema MUST negar a inativação de um item que possua reservas ativas.
- `FR-009`: O sistema MUST NOT permitir alterar, receber entrada ou ser reservado um item inativo; as buscas MUST permitir restringir a itens ativos.

**Entrada de estoque**
- `FR-010`: O sistema MUST permitir registrar uma entrada de estoque para um item ativo, com quantidade inteira maior que zero, aumentando o saldo.
- `FR-011`: O saldo MUST NOT diminuir por nenhuma operação além da baixa (`FR-020`).

**Reserva**
- `FR-012`: O sistema MUST permitir reservar uma quantidade inteira maior que zero de um item ativo para uma OS, quando a quantidade disponível for suficiente.
- `FR-013`: Ao reservar, o sistema MUST vincular a reserva à OS, reduzir a quantidade disponível (sem alterar o saldo) e registrar "peça reservada".
- `FR-014`: Quando a quantidade disponível for insuficiente, o sistema MUST NOT criar reserva (nem parcial), MUST registrar "peça indisponível" e MUST devolver ao chamador um resultado distinguível de sucesso.
- `FR-015`: Uma unidade reservada para uma OS MUST NOT poder ser reservada por outra OS; com disputa simultânea pelo último item, apenas uma reserva MUST ser criada.
- `FR-016`: Uma OS MUST ter, no máximo, uma reserva ativa por item; uma nova solicitação para a mesma OS e o mesmo item MUST ser rejeitada como conflito (para mudar a quantidade, libera-se e reserva-se de novo).
- `FR-017`: O sistema MUST rejeitar reserva de item inexistente ou inativo.

**Liberação**
- `FR-018`: O sistema MUST permitir liberar a reserva de um item específico de uma OS e liberar todas as reservas ativas de uma OS; a quantidade volta ao disponível e "reserva liberada" é registrado.
- `FR-019`: Liberar uma reserva inexistente ou já liberada MUST NOT gerar erro nem efeito (resultado idêntico).

**Baixa**
- `FR-020`: O sistema MUST permitir dar baixa em todas as reservas ativas de uma OS, diminuindo o saldo de cada item pela quantidade reservada, consumindo as reservas e registrando "peças baixadas"; a baixa é tudo ou nada.
- `FR-021`: Dar baixa em uma OS sem reservas ativas, ou já baixada, MUST NOT gerar erro nem alterar o saldo (idempotente).

**Consulta de reservas**
- `FR-022`: O sistema MUST permitir consultar as reservas de uma OS, com item, quantidade e situação (ativa, liberada ou consumida).

## Non-Functional Requirements

- **Segurança:** todas as operações exigem usuário autenticado.
- **Integridade:** o saldo nunca fica negativo e a quantidade reservada nunca excede o saldo, mesmo sob requisições simultâneas; nome único garantido sob concorrência.
- **Consistência:** reservar, liberar e dar baixa são operações atômicas por chamada (ou aplicam tudo, ou nada).
- **Qualidade:** cobertura mínima de 80% no domínio e na aplicação.
- **Respostas de erro:** mensagens claras e em português, indicando o campo inválido.

## Edge Cases

- Nome com espaços nas pontas ou em caixa diferente de um nome existente: tratado como duplicado.
- Quantidade zero, negativa ou ausente em entrada ou reserva: dado inválido.
- Reservar mais do que o disponível, ou exatamente o disponível: o primeiro falha sem reserva; o segundo funciona e zera o disponível.
- Duas OS disputando a última unidade ao mesmo tempo: apenas uma reserva é criada, a outra recebe "peça indisponível".
- Reserva já liberada ou consumida não conta no reservado nem pode ser liberada ou baixada de novo.
- Baixa quando uma OS reservou vários itens: todos baixam juntos ou nenhum baixa.
- Item inexistente em qualquer operação: resposta de não encontrado.
- Inativar item com reserva ativa: negado; depois de liberar ou baixar tudo, permitido.
- Preço zero: aceito.

## Acceptance Criteria

- [ ] Itens (peça ou insumo) podem ser cadastrados, consultados, listados, alterados e inativados, com nome único.
- [ ] Entrada de estoque aumenta o saldo; o saldo, o reservado e o disponível aparecem na consulta.
- [ ] Reservar com saldo disponível cria a reserva vinculada à OS; sem disponível, não cria e registra "peça indisponível".
- [ ] Uma unidade reservada não pode ser reservada por outra OS, inclusive em disputa simultânea.
- [ ] Liberar devolve ao disponível; baixar diminui o saldo e consome as reservas; ambos são idempotentes.
- [ ] Item inativo não é reservado, não recebe entrada e não é alterado; item com reserva ativa não é inativado.
- [ ] Os fatos "peça reservada", "peça indisponível", "reserva liberada" e "peças baixadas" são registrados.
- [ ] Todas as operações exigem autenticação.
- [ ] Testes unitários e de integração cobrem os fluxos acima, com cobertura mínima de 80%.

## Open Questions

- `[NEEDS CLARIFICATION: nova reserva para a mesma OS e o mesmo item deve somar, substituir ou ser rejeitada?]` (assumindo **rejeitada** como conflito, `FR-016`; para o fluxo de revisão do orçamento, libera e reserva de novo; não bloqueia).
- `[NEEDS CLARIFICATION: tamanho máximo do nome e da descrição?]` (assumindo os mesmos do serviço: nome 120 e descrição 500; não bloqueia).
- `[NEEDS CLARIFICATION: quantidades fracionárias, como litros de óleo?]` (assumindo **apenas inteiras** no MVP; não bloqueia).
- `[NEEDS CLARIFICATION: o que fazer com a peça indisponível: aguardar reposição, trocar ou encerrar?]` (hotspot do Event Storming, decidido no fluxo da OS; aqui só registramos "peça indisponível").
