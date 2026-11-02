# Consultar Tempo Médio de Execução (UC13)

**Issue:** #48
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc13_tempo_medio_execucao.feature`
**Fonte:** Requisito do enunciado (não derivado do Event Storming) — consulta administrativa.

## Overview / Problema
O atendente consulta o **tempo médio de execução** dos serviços (entre o início e o fim da execução),
para monitorar a eficiência da oficina. É uma consulta read-only.

## Goals & Non-Goals
**Goals:**
- Calcular o tempo médio entre o **início** (UC09) e o **fim** (UC10) da execução.
- Considerar apenas OS com **início e fim** registrados.
- Informar quando não há dados suficientes.

**Non-Goals:**
- Recortes por período/mecânico; exportação; gráficos.
- Não altera estado nem gera evento de domínio.

## Decisão de modelagem
Como os eventos de domínio **não são persistidos** (sem event store — lacuna #1) e o `iniciarExecucao`
não muda status, o início/fim da execução passam a ser **persistidos na OS** (`execucaoIniciadaEm` no UC09,
`execucaoFinalizadaEm` no UC10). O tempo médio é calculado sobre as OS que têm ambos.

## User Stories
- Como **atendente**, quero ver o tempo médio de execução, para acompanhar a eficiência.

Comportamento (BDD):
- Dadas OS com início/fim de execução / Quando consulto / Então vejo o tempo médio; nenhum evento é registrado.
- OS sem o fim registrado é **ignorada** no cálculo.
- Sem OS elegível / Então sou informado de que não há dados suficientes.

## Functional Requirements
- `FR-001`: O sistema MUST calcular o tempo médio entre `execucaoIniciadaEm` e `execucaoFinalizadaEm` das OS que têm ambos.
- `FR-002`: O sistema MUST ignorar OS sem início ou sem fim.
- `FR-003`: Sem OS elegível, o sistema MUST informar que não há dados suficientes (não é erro).
- `FR-004`: A consulta MUST ser administrativa (JWT) e MUST NOT alterar estado nem gerar evento.

## Acceptance Criteria
- [ ] Média correta sobre OS com início e fim.
- [ ] OS incompleta ignorada.
- [ ] Sem dados → mensagem.
- [ ] `./gradlew build` verde.

## Open Questions
- Nenhuma bloqueante.
