# Gerar Orçamento (UC03)

**Issue:** #39
**Status:** Ready for Plan
**BDD:** `docs/bdd/uc03_gerar_orcamento.feature`
**Fonte:** Event Storming — board Tech01 (frame "UC03 Gerar Orçamento" + post-its "3. Orçamento")

## Overview / Problema
A partir de um diagnóstico finalizado, o mecânico monta o orçamento (serviços + peças), o sistema calcula
o total, associa o orçamento à OS, muda o status para `Aguardando aprovação`, solicita a reserva das peças
ao estoque e notifica o cliente.

## Goals & Non-Goals
**Goals:**
- Gerar o orçamento de uma OS cujo diagnóstico foi finalizado (liberada para orçamento), com itens (serviço/peça) e **valor total calculado**.
- Associar o orçamento à OS e mudar o status para `Aguardando aprovação`.
- Solicitar a reserva das peças ao estoque e notificar o cliente.
- Registrar o evento "Orçamento gerado".
- Bloquear a geração quando o diagnóstico não estiver finalizado ou a OS estiver fora da etapa de diagnóstico.

**Non-Goals:**
- **Integração real** com `cadastro` (catálogo de serviços) e `estoque` (reserva de peças) e com notificação (e-mail/SMTP) — entram como **portas com stub**; a issue #39 só fecha com a integração real (PR usa `Refs #39`).
- Fluxo de peça indisponível / revisão do orçamento (UC07) e aprovação (UC05).

## User Stories
- Como **mecânico**, quero montar o orçamento a partir do diagnóstico, para que o cliente possa aprovar.
- Como **cliente**, quero ser notificado quando o orçamento for gerado, para decidir sobre a aprovação.

Comportamento (BDD):
- Dada OS `Em diagnóstico` com "Diagnóstico finalizado" registrado / Quando o mecânico monta o orçamento com itens / Então o total é a soma dos itens, o orçamento fica associado à OS, o evento "Orçamento gerado" é registrado, o status vira `Aguardando aprovação`, a reserva da peça é solicitada ao estoque e o cliente é notificado.
- Dada OS `Em diagnóstico` **sem** "Diagnóstico finalizado" / Quando tenta gerar / Então não gera e o status continua `Em diagnóstico`.
- Dada OS fora da etapa de diagnóstico (`Recebida`, `Em execução`, `Finalizada`) / Quando tenta gerar / Então não gera e o status continua.

## Functional Requirements
- `FR-001`: O sistema MUST gerar o orçamento apenas quando a OS estiver `Em diagnóstico` **e** com o diagnóstico finalizado (liberada para orçamento).
- `FR-002`: O orçamento MUST conter itens (tipo serviço/peça, descrição, valor) e MUST ter o **valor total calculado** como a soma dos itens.
- `FR-003`: Ao gerar, o sistema MUST associar o orçamento à OS e mudar o status para `Aguardando aprovação`, registrando o evento "Orçamento gerado".
- `FR-004`: O sistema MUST solicitar a **reserva das peças** do orçamento ao estoque (porta; stub por ora).
- `FR-005`: O sistema MUST **notificar o cliente** da geração do orçamento (porta; stub por ora).
- `FR-006`: O sistema MUST NOT gerar o orçamento fora das condições de `FR-001`, mantendo o status atual.
- `FR-007` (#106): O item de serviço MAY referenciar `servicoId` e o de peça `itemEstoqueId` + `quantidade`; o sistema MUST obter nome e preço do cadastro/estoque (peça = preço × quantidade) pela `CatalogoPort`. `descricao` e `valor` informados MUST prevalecer (o valor é o da linha); item sem id MUST trazer `descricao` e `valor`. Id inexistente ou inativo → 404; item sem preço possível → 400. Vale também para a revisão (UC07).

## Non-Functional Requirements
- Valores monetários MUST usar o VO `Dinheiro` (não-negativo, 2 casas).
- Concorrência: geração sob transação + lock otimista (mesma estratégia do UC02).

## Edge Cases
- OS inexistente → "não encontrada".
- Orçamento sem itens → rejeitado (dados inválidos).
- Gerar duas vezes → bloqueado (após a 1ª, o status já não é `Em diagnóstico`).
- Peça indisponível no estoque → `[NEEDS CLARIFICATION]` (ver Open Questions).

## Acceptance Criteria
- [ ] Orçamento gerado com total = soma dos itens; associado à OS.
- [ ] Status `Em diagnóstico` → `Aguardando aprovação`; evento "Orçamento gerado".
- [ ] Reserva de peça solicitada ao estoque; cliente notificado.
- [ ] Geração bloqueada sem diagnóstico finalizado ou fora da etapa; status inalterado.
- [ ] `./gradlew build` verde.

## Open Questions
- `[NEEDS CLARIFICATION: peça indisponível]` — a política do Event Storming é "revisar o orçamento; sem alternativa, encerrar a OS" (UC07). Assumido **fora do escopo** deste UC; o stub de estoque considera a peça disponível. Não bloqueia.
- `[NEEDS CLARIFICATION: itens vêm do catálogo?]` — assumido **texto livre** por ora (descrição + valor informados); catálogo estruturado quando a integração com `cadastro`/`estoque` existir. Não bloqueia.
