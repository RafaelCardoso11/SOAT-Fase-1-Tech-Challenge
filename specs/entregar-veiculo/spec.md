# Entregar Veículo (UC11)

**Issue:** #46
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc11_entregar_veiculo.feature`
**Fonte:** Event Storming — board Tech01 (frame "UC11 Entregar Veículo" + post-its "6. Entrega")

## Overview / Problema
Na retirada do veículo, o atendente confirma a entrega de uma OS `Finalizada`. A OS vai para `Entregue`,
a OS é encerrada e um comprovante de entrega é disponibilizado.

## Goals & Non-Goals
**Goals:**
- Entregar o veículo de uma OS `Finalizada` (→ `Entregue`), registrando "Veículo entregue" e, em seguida, "OS encerrada".
- Disponibilizar um **comprovante de entrega**.
- Bloquear a entrega de OS que não esteja `Finalizada`.

**Non-Goals:**
- Geração de PDF/assinatura do comprovante — o comprovante é um retorno simples (dados da OS + data/hora).

## User Stories
- Como **atendente**, quero registrar a entrega do veículo, para encerrar a OS e dar o comprovante ao cliente.

Comportamento (BDD):
- Dada OS `Finalizada` / Quando o atendente confirma a entrega / Então "Veículo entregue", status `Entregue`, "OS encerrada" (depois de "Veículo entregue") e o comprovante é disponibilizado.
- Dada OS fora de `Finalizada` / Quando tenta entregar / Então não registra e o status continua.

## Functional Requirements
- `FR-001`: O sistema MUST permitir entregar apenas OS `Finalizada`.
- `FR-002`: Ao entregar, o status MUST ir para `Entregue`, registrando "Veículo entregue" e, em seguida, "OS encerrada".
- `FR-003`: O sistema MUST disponibilizar um comprovante de entrega (nº da OS, veículo, documento, data/hora).
- `FR-004`: O sistema MUST NOT entregar fora de `Finalizada`, mantendo o status.

## Non-Functional Requirements
- Escrita sob transação.

## Edge Cases
- OS inexistente → "não encontrada".
- Entregar OS já `Entregue` → bloqueado (fora de `Finalizada`).

## Acceptance Criteria
- [ ] `Finalizada` → `Entregue` + "Veículo entregue" + "OS encerrada" + comprovante.
- [ ] Fora de `Finalizada` → bloqueado; status inalterado.
- [ ] `./gradlew build` verde.

## Open Questions
- Nenhuma bloqueante.
