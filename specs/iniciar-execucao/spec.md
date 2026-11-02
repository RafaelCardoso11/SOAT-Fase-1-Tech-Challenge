# Iniciar Execução + Integração real com Estoque (UC09 / #39)

**Issues:** #44 (UC09) · #39 (integração estoque — parte de reserva/baixa)
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc09_iniciar_execucao.feature`
**Fonte:** Event Storming — board Tech01 (Execução: Iniciar Execução, Peças Baixadas)

## Overview / Problema
Com o orçamento aprovado (OS já em `Em execução`), o mecânico inicia os serviços e o sistema dá **baixa
definitiva** nas peças reservadas. Nesta mesma entrega, a integração com o módulo **estoque** passa a ser
**real** (os colegas publicaram reserva/liberação/baixa), substituindo os stubs usados por UC03/06/07/08.

## Goals & Non-Goals
**Goals:**
- UC09: iniciar a execução de uma OS `Em execução`, registrando "Execução iniciada" e "Peças baixadas", dando baixa das peças no estoque; status continua `Em execução`.
- Bloquear iniciar execução sem orçamento aprovado (OS fora de `Em execução`); nenhuma baixa.
- Integração real: `EstoquePort` passa a chamar os casos de uso do módulo `estoque` (reservar/liberar/baixar), chaveados por `osId`. Remove o stub.

**Non-Goals:**
- Catálogo estruturado no orçamento — a peça continua **texto livre**, casada ao item do estoque **pelo nome** (qtd 1) no MVP.
- Tratamento de "peça indisponível" na reserva (o resultado `Indisponivel` é ignorado por ora).

## User Stories
- Como **mecânico**, quero iniciar os serviços aprovados e baixar as peças reservadas, para executar o reparo.

Comportamento (BDD):
- Dada OS `Em execução` com orçamento aprovado e peça reservada / Quando inicio a execução / Então "Execução iniciada", status continua `Em execução`, baixa definitiva das peças, "Peças baixadas".
- Dada OS fora de `Em execução` / Quando tento iniciar / Então não inicia, alerta de "orçamento não aprovado", nenhuma baixa.

## Functional Requirements
- `FR-001`: O sistema MUST permitir iniciar a execução apenas quando a OS estiver `Em execução` (orçamento aprovado).
- `FR-002`: Ao iniciar, o sistema MUST dar **baixa** das peças reservadas da OS no estoque e registrar "Execução iniciada" + "Peças baixadas".
- `FR-003`: O sistema MUST NOT iniciar fora de `Em execução`, sem dar baixa, informando que o orçamento não foi aprovado.
- `FR-004`: `EstoquePort` MUST delegar para os casos de uso reais do módulo `estoque` (reserva/liberação/baixa), chaveados por `osId`.

## Non-Functional Requirements
- Escrita sob transação. Integração in-process via portas de entrada do `estoque` (ACL — ADR-0006).

## Edge Cases
- OS inexistente → "não encontrada".
- Iniciar duas vezes → a baixa do estoque é idempotente (sem reservas ativas, baixa vazia).
- Peça do orçamento sem item correspondente no estoque (por nome) → reserva é no-op (best-effort).

## Acceptance Criteria
- [ ] OS `Em execução` → iniciar: "Execução iniciada" + baixa + "Peças baixadas"; status continua.
- [ ] Fora de `Em execução` → bloqueado, sem baixa.
- [ ] Reserva/liberação/baixa chamam o estoque real (sem stub).
- [ ] `./gradlew build` verde.

## Open Questions
- `[NEEDS CLARIFICATION: peça ↔ item do estoque]` — decidido **casar por nome, qtd 1** (MVP); evoluir para item+quantidade referenciados no orçamento depois. Não bloqueia.
- `[NEEDS CLARIFICATION: idempotência do iniciar]` — apoiada na baixa idempotente do estoque; sem flag própria no MVP. Não bloqueia.
