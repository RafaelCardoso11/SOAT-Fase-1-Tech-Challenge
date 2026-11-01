# Aprovar Orçamento (UC05)

**Issue:** #40
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc05_aprovar_orcamento.feature`
**Fonte:** Event Storming — board Tech01 (frame "UC05 Aprovar Orçamento" + post-its "4. Aprovação")

## Overview / Problema
O cliente aprova o orçamento de uma OS que está `Aguardando aprovação`, dentro do prazo de validade,
autorizando a execução do serviço. A aprovação registra data/hora, emite evento e leva a OS para
`Em execução`.

## Goals & Non-Goals
**Goals:**
- Aprovar o orçamento de uma OS `Aguardando aprovação`, registrando data/hora e o evento "Orçamento aprovado".
- Transicionar a OS para `Em execução` (status oficial — **sem criar status novo**).
- Bloquear a aprovação de orçamento **expirado** (fora do prazo de validade) e informar o cliente.
- Bloquear a aprovação quando a OS não estiver `Aguardando aprovação`.
- O **prazo de validade do orçamento é configurável** (default 7 dias), contado a partir da geração (UC03).

**Non-Goals:**
- Rejeitar (UC06) e revisar (UC07) orçamento; expirar/encerrar automaticamente (UC08).
- Liberar reserva de peça no estoque (a política "encerrar + liberar reserva" é do UC06/UC08; depende de `estoque`).
- Autenticação específica de cliente — a ação roda nas rotas administrativas por ora (ver Open Questions).

## User Stories
- Como **cliente**, quero aprovar o orçamento dentro da validade, para autorizar a execução.

Comportamento (BDD):
- Dada OS `Aguardando aprovação` com orçamento no prazo / Quando aprovo / Então a aprovação é registrada com data/hora, o evento "Orçamento aprovado" é registrado e o status vira `Em execução`.
- Dada OS cujo orçamento **expirou** / Quando tento aprovar / Então a aprovação **não** é registrada e sou informado de que o orçamento expirou.
- Dada OS fora de `Aguardando aprovação` / Quando tento aprovar / Então a aprovação não é registrada e o status continua.

## Functional Requirements
- `FR-001`: O sistema MUST permitir aprovar o orçamento apenas quando a OS estiver `Aguardando aprovação`.
- `FR-002`: A aprovação MUST registrar **data/hora** e emitir o evento "Orçamento aprovado".
- `FR-003`: Ao aprovar, o status da OS MUST ir para `Em execução`.
- `FR-004`: O sistema MUST bloquear a aprovação de um orçamento **expirado** (agora > validade) e informar o cliente, sem alterar o status.
- `FR-005`: O sistema MUST NOT aprovar fora de `Aguardando aprovação`, mantendo o status atual.
- `FR-006`: O **prazo de validade** do orçamento MUST ser configurável (default 7 dias), aplicado na geração (UC03).

## Non-Functional Requirements
- Data/hora em UTC (`Instant`); validade derivada de config (`orcamento.validade-dias`).
- Concorrência: aprovação sob transação + lock otimista (como nos UCs de escrita).

## Edge Cases
- OS inexistente → "não encontrada".
- OS `Aguardando aprovação` sem orçamento associado → estado inconsistente (não deve ocorrer; tratar como erro).
- Aprovar duas vezes → a 2ª cai no bloqueio de status (já estará `Em execução`).

## Acceptance Criteria
- [ ] Aprovar dentro da validade → aprovação com data/hora + evento + status `Em execução`.
- [ ] Orçamento expirado → aprovação bloqueada + mensagem de expirado; status inalterado.
- [ ] Fora de `Aguardando aprovação` → bloqueado; status inalterado.
- [ ] Prazo de validade configurável (default 7 dias); `./gradlew build` verde.

## Open Questions
- `[NEEDS CLARIFICATION: rota de aprovação do cliente]` — assumido **rota administrativa** (sem auth de cliente ainda); revisar quando houver acesso do cliente. Não bloqueia.
- `[NEEDS CLARIFICATION: encerrar on expiração]` — a política "expirado → encerrar + liberar reserva" é do **UC08**; no UC05 o expirado apenas **bloqueia** a aprovação. Não bloqueia.
