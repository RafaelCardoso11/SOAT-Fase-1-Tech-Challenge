# Finalizar Execução (UC10)

**Issue:** #45
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc10_finalizar_execucao.feature`
**Fonte:** Event Storming — board Tech01 (frame "UC10 Finalizar Execução" + post-its "5. Execução")

## Overview / Problema
O mecânico registra a conclusão dos serviços de uma OS `Em execução`. A OS vai para `Finalizada` e o
cliente é notificado de que o veículo está pronto para retirada.

## Goals & Non-Goals
**Goals:**
- Finalizar a execução de uma OS `Em execução` (→ `Finalizada`), registrando "Execução finalizada".
- Notificar o cliente que o veículo está pronto para retirada.
- Bloquear a finalização com itens pendentes (nem tudo aplicado) ou fora de `Em execução`.

**Non-Goals:**
- Rastreio de aplicação **por item** (marcar cada serviço/peça como aplicado) — ver Open Questions.
- Entrega do veículo (UC11).

## User Stories
- Como **mecânico**, quero registrar a conclusão dos serviços, para liberar o veículo para retirada.

Comportamento (BDD):
- Dada OS `Em execução` com tudo aplicado / Quando registro a conclusão / Então "Execução finalizada", status `Finalizada`, cliente notificado (pronto p/ retirada).
- Dada OS `Em execução` com item pendente / Quando tento concluir / Então status continua `Em execução` e sou informado dos pendentes.
- Dada OS fora de `Em execução` / Quando tento concluir / Então status continua.

## Functional Requirements
- `FR-001`: O sistema MUST permitir finalizar apenas OS `Em execução`.
- `FR-002`: A finalização MUST exigir que todos os itens do orçamento tenham sido aplicados; caso contrário, MUST bloquear informando pendência, sem mudar o status.
- `FR-003`: Ao finalizar, o status MUST ir para `Finalizada`, registrando "Execução finalizada".
- `FR-004`: O sistema MUST notificar o cliente de que o veículo está pronto para retirada.
- `FR-005`: O sistema MUST NOT finalizar fora de `Em execução`.

## Non-Functional Requirements
- Escrita sob transação.

## Edge Cases
- OS inexistente → "não encontrada".
- Finalizar duas vezes → a 2ª cai no bloqueio de status (já `Finalizada`).

## Acceptance Criteria
- [ ] `Em execução` + tudo aplicado → `Finalizada` + evento + notificação.
- [ ] Item pendente → bloqueado; status inalterado; informado.
- [ ] Fora de `Em execução` → bloqueado.
- [ ] `./gradlew build` verde.

## Open Questions
- `[NEEDS CLARIFICATION: itens aplicados]` — no MVP, a conclusão recebe uma **confirmação de que todos os itens foram aplicados** (flag no comando); o rastreio por item (marcar cada serviço/peça aplicado) fica para evolução. Não bloqueia.
