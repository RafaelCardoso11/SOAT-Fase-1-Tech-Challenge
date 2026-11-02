# Revisar Orçamento (UC07)

**Issue:** #42
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc07_revisar_orcamento.feature`
**Fonte:** Event Storming — board Tech01 (post-its "4. Aprovação": comando Revisar Orçamento; evento Orçamento Revisado)

## Overview / Problema
Quando o cliente pede ajustes no orçamento (valor alto, peça indisponível), o mecânico/atendente revisa o
orçamento de uma OS `Aguardando aprovação` e reenvia a nova proposta — a OS **permanece** `Aguardando
aprovação` para uma nova decisão.

> Nota: o texto da issue cita "orçamento rejeitado", mas no UC06 a rejeição **encerra** a OS (`Finalizada`,
> terminal). O comportamento acordado (e executável no `.feature`) é revisar **sem** sair de `Aguardando aprovação`.

## Goals & Non-Goals
**Goals:**
- Revisar o orçamento de uma OS `Aguardando aprovação`: ajustar itens (valores e/ou troca de peça), gerando uma nova proposta.
- Registrar o evento "Orçamento revisado" e **reenviar** a proposta ao cliente; o status **continua** `Aguardando aprovação`.
- Ao trocar peça, solicitar a reserva da nova peça ao estoque (porta; stub).
- Bloquear revisão fora de `Aguardando aprovação`.

**Non-Goals:**
- Histórico/versões do orçamento — a revisão **substitui** a proposta atual (sem versionamento no MVP).
- Tratamento de "resposta do cliente pendente" (hotspot — ver Open Questions): fora do escopo.
- Integração real com `estoque`/notificação (stub).

## User Stories
- Como **mecânico/atendente**, quero ajustar o orçamento a pedido do cliente, para reenviar uma nova proposta.

Comportamento (BDD):
- Dada OS `Aguardando aprovação` / Quando ajusto o valor de um serviço / Então evento "Orçamento revisado", nova proposta reenviada, status continua `Aguardando aprovação`.
- Dada OS `Aguardando aprovação` com peça indisponível / Quando substituo a peça por alternativa / Então evento "Orçamento revisado", reserva da nova peça solicitada, status continua.
- Dada OS fora de `Aguardando aprovação` / Quando tento revisar / Então a revisão não é registrada.

## Functional Requirements
- `FR-001`: O sistema MUST permitir revisar o orçamento apenas quando a OS estiver `Aguardando aprovação`.
- `FR-002`: A revisão MUST substituir os itens do orçamento (recalculando o total) e renovar o prazo de validade.
- `FR-003`: A revisão MUST registrar o evento "Orçamento revisado" e **reenviar** a proposta ao cliente; o status MUST continuar `Aguardando aprovação`.
- `FR-004`: Ao incluir peça na revisão, o sistema MUST solicitar a reserva ao estoque (porta; stub).
- `FR-005`: O sistema MUST NOT revisar fora de `Aguardando aprovação`.

## Non-Functional Requirements
- Escrita sob transação + lock otimista.

## Edge Cases
- OS inexistente → "não encontrada".
- Revisão sem itens → inválida (orçamento exige ≥ 1 item).

## Acceptance Criteria
- [ ] Revisar ajustando valor → "Orçamento revisado" + reenvio + status continua `Aguardando aprovação`.
- [ ] Revisar trocando peça → reserva da nova peça solicitada.
- [ ] Fora de `Aguardando aprovação` → bloqueado.
- [ ] `./gradlew build` verde.

## Open Questions
- `[NEEDS CLARIFICATION: "resposta do cliente pendente de tratamento"]` — conceito não modelado (exigiria fila/estado de respostas); **descopado** do MVP (cenário fica sem `@implementado`). Não bloqueia.
