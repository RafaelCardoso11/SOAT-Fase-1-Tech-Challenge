# Realizar Diagnóstico (UC02)

**Issue:** #38
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc02_realizar_diagnostico.feature`
**Fonte:** Event Storming — board Tech01 (frame "UC02 Realizar Diagnóstico" + post-its "2. Diagnóstico")

## Overview / Problema
O mecânico assume uma OS recém-recebida, inspeciona o veículo, registra as observações e conclui o
diagnóstico identificando os serviços/insumos necessários — liberando a OS para a etapa de orçamento.
É o primeiro caso de uso que **altera o status** de uma OS após a criação.

## Goals & Non-Goals
**Goals:**
- Iniciar o diagnóstico de uma OS `Recebida`, mudando o status para `Em diagnóstico`.
- Registrar as observações da inspeção técnica.
- Finalizar o diagnóstico, deixando a OS **liberada para orçamento** (o status **permanece** `Em diagnóstico`).
- Encerrar a OS quando a peça necessária não for identificada (status `Finalizada`).
- Registrar os eventos de domínio e as transições de status na linha do tempo da OS.

**Non-Goals:**
- Geração de orçamento (UC03) e identificação estruturada de serviços/peças do catálogo (depende de `cadastro`/`estoque`).
- Novo status "Diagnóstico finalizado" — decidido: usar flag de liberação, mantendo os 6 status oficiais.
- Atribuição de mecânico específico / autenticação por papel (fora do escopo deste UC).

## User Stories
- Como **mecânico**, quero assumir uma OS recebida e marcá-la em diagnóstico, para começar a inspeção.
- Como **mecânico**, quero registrar as observações da inspeção, para documentar o que encontrei.
- Como **mecânico**, quero concluir o diagnóstico identificando o que é necessário, para liberar a OS ao orçamento.
- Como **mecânico**, quando não identifico a peça necessária, quero encerrar a OS, para não deixá-la pendente.

Comportamento (BDD):
- Dada OS `Recebida` / Quando o mecânico assume a OS / Então status vira `Em diagnóstico` e o evento "Diagnóstico iniciado" é registrado.
- Dada OS `Em diagnóstico` / Quando registra a inspeção / Então o evento "Diagnóstico realizado" é registrado.
- Dada OS `Em diagnóstico` com inspeção registrada / Quando conclui informando serviços e peças / Então o evento "Diagnóstico finalizado" é registrado, o status **continua** `Em diagnóstico` e a OS fica liberada para orçamento.
- Dada OS `Em diagnóstico` / Quando conclui **sem** identificar a peça / Então o evento "OS encerrada" é registrado, o status vira `Finalizada` e **não** há evento "Diagnóstico realizado".
- Dada OS fora de `Recebida` / Quando tenta iniciar o diagnóstico / Então não inicia e o status permanece.

## Functional Requirements
- `FR-001`: O sistema MUST permitir iniciar o diagnóstico de uma OS que esteja em `Recebida`, mudando o status para `Em diagnóstico` e registrando o evento "Diagnóstico iniciado".
- `FR-002`: O sistema MUST permitir registrar as observações da inspeção de uma OS `Em diagnóstico`, registrando o evento "Diagnóstico realizado".
- `FR-003`: O sistema MUST permitir finalizar o diagnóstico de uma OS `Em diagnóstico`: registra o evento "Diagnóstico finalizado", **mantém** o status `Em diagnóstico` e marca a OS como **liberada para orçamento**.
- `FR-004`: Ao finalizar **sem** identificar a peça necessária, o sistema MUST encerrar a OS (status `Finalizada`) e registrar o evento "OS encerrada", **sem** registrar "Diagnóstico realizado".
- `FR-005`: O sistema MUST impedir iniciar o diagnóstico quando a OS não estiver em `Recebida`, mantendo o status atual.
- `FR-006`: Cada mudança de status MUST ser registrada como transição na linha do tempo da OS (com data/hora), reusando o histórico introduzido no UC12.

## Non-Functional Requirements
- A linha do tempo permanece **append-only** (uma transição registrada não é alterada).

## Edge Cases
- Iniciar diagnóstico de OS inexistente → erro de "não encontrada".
- Iniciar/registrar/finalizar sobre OS em status incompatível → operação rejeitada, status inalterado.
- Finalizar sem inspeção registrada → `[NEEDS CLARIFICATION]` (ver Open Questions).
- Concluir sem peça identificada → encerra a OS (não é erro).

## Acceptance Criteria
- [ ] `Recebida` → `Em diagnóstico` ao assumir, com evento "Diagnóstico iniciado".
- [ ] Registro de inspeção gera evento "Diagnóstico realizado".
- [ ] Finalizar: evento "Diagnóstico finalizado", status continua `Em diagnóstico`, OS liberada para orçamento.
- [ ] Sem peça identificada: status `Finalizada`, evento "OS encerrada".
- [ ] Iniciar fora de `Recebida` é bloqueado; status não muda.
- [ ] Transições registradas na linha do tempo; `./gradlew build` verde.

## Open Questions
- `[NEEDS CLARIFICATION: finalizar sem inspeção registrada]` — assumido **permitir** finalizar sem exigir observações prévias (o BDD não impõe a ordem); não bloqueia.
- `[NEEDS CLARIFICATION: conteúdo de "serviços e insumos identificados"]` — assumido um **texto livre** por ora (catálogo estruturado depende de `cadastro`/`estoque`, fora do escopo); não bloqueia.
