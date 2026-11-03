# Rejeitar Orçamento (UC06)

**Issue:** #41
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc06_rejeitar_orcamento.feature`
**Fonte:** Event Storming — board Tech01 (post-its "4. Aprovação": comando Rejeitar Orçamento; política "se rejeitado ou expirado, encerrar a OS e liberar a reserva")

## Overview / Problema
O cliente rejeita o orçamento de uma OS `Aguardando aprovação` (com ou sem motivo). A rejeição encerra a
OS (`Finalizada`), libera a reserva das peças no estoque e notifica a oficina/mecânico.

## Goals & Non-Goals
**Goals:**
- Rejeitar o orçamento de uma OS `Aguardando aprovação`, registrando o **motivo (opcional)** e o evento "Orçamento rejeitado".
- Encerrar a OS (`Finalizada`) e registrar o evento "OS encerrada".
- Liberar a reserva das peças no estoque e registrar "Reserva liberada".
- Notificar a oficina/mecânico.
- Bloquear a rejeição fora de `Aguardando aprovação`.

**Non-Goals:**
- Revisar orçamento (UC07) e expirar (UC08).
- Integração real com `estoque`/notificação — portas com **stub** (liberar reserva = no-op; a integração real segue pendente no #39).

## User Stories
- Como **cliente**, quero rejeitar o orçamento (com ou sem motivo), para encerrar o atendimento sem execução.

Comportamento (BDD):
- Dada OS `Aguardando aprovação` com peça reservada / Quando rejeito com motivo / Então o motivo é registrado, eventos "Orçamento rejeitado" + "OS encerrada", status `Finalizada`, a reserva da peça é liberada + evento "Reserva liberada", e oficina/mecânico são notificados.
- Dada OS `Aguardando aprovação` / Quando rejeito sem motivo / Então evento "Orçamento rejeitado" e status `Finalizada`.
- Dada OS fora de `Aguardando aprovação` / Quando tento rejeitar / Então a rejeição não é registrada e o status continua.

## Functional Requirements
- `FR-001`: O sistema MUST permitir rejeitar o orçamento apenas quando a OS estiver `Aguardando aprovação`.
- `FR-002`: A rejeição MUST registrar o **motivo (opcional)** e emitir o evento "Orçamento rejeitado".
- `FR-003`: Ao rejeitar, o sistema MUST encerrar a OS (`Finalizada`) e emitir "OS encerrada".
- `FR-004`: O sistema MUST solicitar a **liberação da reserva** das peças ao estoque (porta; stub) e emitir "Reserva liberada".
- `FR-005`: O sistema MUST notificar a oficina/mecânico da rejeição (porta; stub/e-mail).
- `FR-006`: O sistema MUST NOT rejeitar fora de `Aguardando aprovação`, mantendo o status atual.

## Non-Functional Requirements
- Escrita sob transação + lock otimista (como nos demais UCs de escrita).

## Edge Cases
- OS inexistente → "não encontrada".
- Rejeitar duas vezes → a 2ª cai no bloqueio de status (já `Finalizada`).
- Orçamento sem peças → não há reserva a liberar (nenhuma chamada ao estoque).

## Acceptance Criteria
- [ ] Rejeitar com motivo → motivo registrado + eventos + `Finalizada` + reserva liberada + notificação.
- [ ] Rejeitar sem motivo → evento + `Finalizada`.
- [ ] Fora de `Aguardando aprovação` → bloqueado; status inalterado.
- [ ] `./gradlew build` verde.

## Open Questions
- `[NEEDS CLARIFICATION: destinatário da notificação]` — assumido notificar o endereço da **oficina** (MVP); refinar com acesso real de oficina/mecânico. Não bloqueia.
- `[NEEDS CLARIFICATION: um evento "Reserva liberada" por peça ou por OS]` — assumido **um por OS** (BDD cita no singular). Não bloqueia.
- **Rejeição pelo cliente (#113):** além da rota administrativa, o cliente rejeita sem login em `POST /api/public/orcamentos/{token}/rejeitar` (motivo opcional), com o token do e-mail do orçamento.
