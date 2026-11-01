# Expirar Orçamento (UC08)

**Issue:** #43
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc08_expirar_orcamento.feature`
**Fonte:** Event Storming — board Tech01 (post-its "4. Aprovação": evento Orçamento Expirado; política "se rejeitado ou expirado, encerrar a OS e liberar a reserva")

## Overview / Problema
Orçamentos de OS `Aguardando aprovação` que passam do prazo de validade sem resposta do cliente devem
**expirar**: o sistema, numa verificação periódica, encerra a OS (`Finalizada`), libera as peças reservadas
e notifica o atendente.

## Goals & Non-Goals
**Goals:**
- Verificação periódica que encontra orçamentos vencidos (`Aguardando aprovação` + prazo passado) e os expira.
- Expirar → encerrar a OS (`Finalizada`), liberar reserva, registrar "Orçamento expirado" + "OS encerrada", notificar atendente.
- Não expirar dentro do prazo nem OS já respondida (aprovada/rejeitada).

**Non-Goals:**
- Integração real com `estoque`/notificação — portas com **stub** (segue no #39).
- Reabertura/renegociação após expirar (encerramento é terminal aqui).

## User Stories
- Como **sistema**, quero expirar orçamentos sem resposta após o prazo, para encerrar a OS e liberar as peças.

Comportamento (BDD):
- Dada OS `Aguardando aprovação` com peça reservada, prazo vencido e sem resposta / Quando roda a verificação / Então "Orçamento expirado" + "OS encerrada", status `Finalizada`, reserva liberada, atendente notificado.
- Dada OS `Aguardando aprovação` dentro do prazo / Quando roda a verificação / Então **não** expira; status continua `Aguardando aprovação`.
- Dado prazo vencido mas orçamento já aprovado (OS `Em execução`) / Quando roda a verificação / Então **não** expira; status continua `Em execução`.

## Functional Requirements
- `FR-001`: O sistema MUST executar uma **verificação periódica** de validade dos orçamentos.
- `FR-002`: A verificação MUST expirar apenas OS `Aguardando aprovação` cujo orçamento passou do `validoAte`.
- `FR-003`: Ao expirar, o sistema MUST encerrar a OS (`Finalizada`), emitir "Orçamento expirado" + "OS encerrada", liberar a reserva das peças (porta; stub) e notificar o atendente (porta; stub).
- `FR-004`: O sistema MUST NOT expirar orçamentos dentro do prazo nem OS já respondidas (status ≠ `Aguardando aprovação`).

## Non-Functional Requirements
- Escrita sob transação + lock otimista.
- Periodicidade e disparo **configuráveis** (cron); verificação idempotente (reexecutar não afeta OS já encerradas).

## Edge Cases
- Nenhuma OS vencida → verificação não faz nada.
- Várias OS vencidas → todas expiradas na mesma execução.
- Reexecução após expirar → OS já `Finalizada` não é tocada (fora do filtro `Aguardando aprovação`).

## Acceptance Criteria
- [ ] OS aguardando + vencida → `Finalizada` + eventos + reserva liberada + atendente notificado.
- [ ] Dentro do prazo → não expira.
- [ ] Já aprovada/rejeitada → não expira.
- [ ] `./gradlew build` verde.

## Open Questions
- `[NEEDS CLARIFICATION: periodicidade]` — assumido **cron configurável, default de hora em hora** (`orcamento.expiracao-cron`). Não bloqueia.
- `[NEEDS CLARIFICATION: disparo manual]` — adicionado um endpoint administrativo para rodar a verificação sob demanda (ops/teste), além do agendamento. Não bloqueia.
