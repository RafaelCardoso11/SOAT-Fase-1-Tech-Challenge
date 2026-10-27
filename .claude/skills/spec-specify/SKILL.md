---
name: "spec-specify"
description: >
  Fase de especificação do SDD. Usada quando o usuário quer escrever uma especificação,
  definir requisitos, capturar user stories ou mapear o escopo funcional antes da
  arquitetura ou do código. Transfere a responsabilidade para a fase Plan quando pronta.
---

## Goal

Transformar um pedido vago em um **`spec.md` claro, testável e agnóstico de tecnologia** — para que o plano e a implementação se apoiem em requisitos acordados, e não em suposições.

> 🛑 **Regra de Ouro:** Esta é a fase **Specify**. Pare aqui antes da arquitetura: **sem rotas, pacotes, tabelas, frameworks ou detalhes de código.**

---

## Antes de Escrever

1. Escolha um `<feature-slug>` curto em `kebab-case` e crie o arquivo `specs/<feature-slug>/spec.md`.
2. Leia a issue ou discussão existente e referencie no cabeçalho (se houver).
3. **Se `spec.md` já existir, leia e estenda** — não sobrescreva conteúdo válido.
4. **Pergunte antes de adivinhar:** Quando uma dúvida mudar o escopo ou comportamento (atores, limites, falhas, sync vs async), pergunte diretamente ao usuário.

---

## Estrutura do `spec.md`

Sugestão de estrutura para o arquivo `spec.md` gerado:

```markdown
# <Nome da Feature>

**Issue:** <chave ou "none">
**Status:** Draft | Ready for Plan

## Overview / Problema
2 a 4 frases explicando: qual é o problema, para quem é e por que agora. 
*(Sem detalhes de implementação).*

## Goals & Non-Goals
* **Goals:** O que esta entrega resolve explicitamente.
* **Non-Goals:** O que esta entrega **NÃO** inclui/cobre.

## User Stories
Histórias de usuário focadas em comportamento. Exemplos:
* **BDD:** Given [cenário] / When [ação] / Then [resultado]
* **Tradicional:** As a [ator] I want [ação] so that [benefício]

## Functional Requirements
Requisitos numerados e testáveis:
* `FR-001`: O sistema MUST (deve) permitir...
* `FR-002`: O sistema SHOULD (pode) exibir...
*(Use MUST para obrigatório e SHOULD para negociável).*

## Non-Functional Requirements (Opcional)
Apenas o que for crítico de negócio: performance, segurança/permissões, auditoria, limites.

## Edge Cases
Mapeamento de exceções: entrada inválida, registro não encontrado, dados vazios, concorrência, timeouts.

## Acceptance Criteria
Checklist curto do ponto de vista de negócio para considerar a feature "pronta".

## Open Questions
Consolide aqui qualquer `[NEEDS CLARIFICATION: pergunta]` pendente.
```

---

## Lidar com Ambiguidades

Quando houver lacunas que mudem o escopo ou as regras de negócio:
* Pergunte diretamente ao usuário **OU**
* Marque no texto como `[NEEDS CLARIFICATION: pergunta concreta]` e liste na seção **Open Questions**.
* **Não invente regras de negócio** (limites, permissões, retries) para preencher buracos por conta própria.

> ⚠️ **Regra do Orquestrador:** Clarificações que mudam *como* construir devem estar resolvidas (ou assumidas explicitamente no `plan.md`) antes de avançar.

---

## Permaneça Agnóstico de Tecnologia

O arquivo `spec.md` **NÃO deve mencionar**:
* Caminhos de arquivo, nomes de pacotes, módulos ou classes.
* Verbos HTTP, tópicos de fila, cron expressions, queries SQL ou DDL.
* Nomes de bibliotecas, bancos de dados ou frameworks.

*(Se surgirem esses detalhes durante o rascunho, guarde-os para a fase de `plan.md`).*

---

## Checklist para Concluir a Especificação

Antes de ir para a próxima fase, verifique:
- [ ] Cada user story descreve um comportamento claro?
- [ ] Cada requisito funcional é numerado e testável isoladamente?
- [ ] Edge cases (casos limite e erros) foram mapeados?
- [ ] Não há vazamento de arquitetura ou tecnologias no texto?
- [ ] Não restaram dúvidas bloqueantes em `[NEEDS CLARIFICATION]`?
