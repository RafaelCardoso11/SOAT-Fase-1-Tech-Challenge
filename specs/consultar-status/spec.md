# Consultar Status da Ordem de Serviço (UC12)

**Issue:** #47
**Status:** Ready for Plan

## Overview / Problema
Cliente e atendente precisam acompanhar o andamento de uma Ordem de Serviço. Esta feature permite
consultar as OS por diferentes identificadores e ver o status atual e a linha do tempo do atendimento.

## Goals & Non-Goals
**Goals:**
- Consultar OS por número da OS, por placa e por documento (CPF/CNPJ).
- Exibir, para cada OS, o status atual, a **linha do tempo** (histórico de transições de status, com data/hora) e os detalhes (cliente, veículo, relato; serviços e orçamento quando existirem).
- Registrar cada **transição de status** da OS ao longo do ciclo de vida (incluindo a criação), compondo a linha do tempo.
- Consulta **pública** (sem autenticação).

**Non-Goals:**
- Autenticação/restrição de acesso (a consulta é pública).
- Serviços e orçamento ainda não existem (UC02/UC03) — serão exibidos quando passarem a existir.
- A consulta não altera estado nem gera evento de domínio.

## User Stories
- Como **cliente**, quero consultar minhas OS pela placa ou pelo número, para acompanhar o andamento.
- Como **atendente**, quero consultar OS pelo CPF/CNPJ do cliente, para informá-lo.
- Como **cliente/atendente**, quero ver a linha do tempo da OS, para saber por quais etapas passou e quando.

Comportamento (BDD):
- Dada uma OS existente / Quando consulto por número, placa ou CPF / Então vejo o status, a linha do tempo e os detalhes; **nenhum evento de domínio** é registrado.
- Dado que não há OS para o identificador / Quando consulto / Então sou informado que **nenhuma OS foi encontrada**.

## Functional Requirements
- `FR-001`: O sistema MUST permitir consultar OS por **número da OS**, por **placa** e por **documento (CPF/CNPJ)**.
- `FR-002`: A consulta por placa ou documento MUST retornar a **lista** de todas as OS correspondentes.
- `FR-003`: Cada OS retornada MUST exibir o **status atual**, a **linha do tempo** (transições de status, cada uma com data/hora) e os **detalhes** (cliente, veículo, relato; serviços/orçamento quando existirem).
- `FR-004`: O sistema MUST registrar cada **transição de status** da OS (incluindo a criação → `Recebida`) com data/hora.
- `FR-005`: Quando não houver OS para o identificador, o sistema MUST informar que **nenhuma OS foi encontrada** (não é erro).
- `FR-006`: A consulta MUST ser **pública** (sem autenticação) e MUST NOT alterar estado nem gerar evento de domínio.

## Non-Functional Requirements
- A linha do tempo é **append-only**: uma transição registrada não é alterada depois.

## Edge Cases
- Identificador sem OS → mensagem "nenhuma OS encontrada" (resposta vazia, não erro).
- Placa/documento com várias OS → lista, ordenada da mais recente para a mais antiga.
- Identificador malformado (placa/CPF inválido) → tratado como "nenhuma encontrada" (sem erro de validação).

## Acceptance Criteria
- [ ] Consulta por número, placa e CPF retorna a(s) OS com status + linha do tempo + detalhes.
- [ ] Placa/CPF com múltiplas OS → lista.
- [ ] Sem OS → mensagem de não encontrado.
- [ ] Consulta pública, sem efeito colateral nem evento de domínio.
- [ ] Transições de status são registradas, começando pela criação.

## Open Questions
- `[NEEDS CLARIFICATION: ordenação da lista]` — assumido **mais recente primeiro** (não bloqueia).
- `[NEEDS CLARIFICATION: identificador inválido]` — assumido tratar como "não encontrado" (não bloqueia).
