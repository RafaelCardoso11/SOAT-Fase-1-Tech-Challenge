---
name: "spec-tasks"
description: >
  Fase de Tasks do desenvolvimento orientado a especificação (SDD) do sistema da Oficina
  Mecânica (SOAT Fase 1). Usada quando o `plan.md` já existe e o usuário quer a lista de
  tarefas, o `tasks.md`, um checklist ordenado ou "o que fazer primeiro" antes de codificar.
  Produz `specs/<feature-slug>/tasks.md`: tarefas pequenas em ordem de dependência, com IDs
  estáveis, caminhos/módulos concretos, tarefas de teste explícitas e um gate final de
  qualidade. Não introduz novo design — decisões voltam para `spec-plan`.
---

# Goal

Converter `plan.md` em um **checklist executável item a item**, na ordem certa, para que a fase de implementação (`Implement`) não precise re-descobrir "o que falta".

Esta é a fase **Tasks** de `spec-driven-development`. **Não** introduz decisões de design – volte ao Plan (`spec-plan`) se algo precisar ser decidido.

---

## Entradas

1. `specs/<slug>/plan.md` **obrigatório**. Caso contrário → `spec-plan`.
2. Releia a tabela de mapeamento / component plan; cada linha relevante deve gerar pelo menos uma task.

---

## Estrutura de `tasks.md`

```markdown
# Tasks: <feature name>

**Plan:** specs/<slug>/plan.md

## Setup / scaffolding

- [ ] T001 - <descrição curta e concreta>

## Data / persistence (omita a seção se N/A)

- [ ] T002 - <migration/schema/entidade>

## <Camada ou componente - nomes vindos do plan.md>

- [ ] T003 - <implementação>
- [ ] T004 [P] - <tarefa independente que pode rodar em paralelo>

## Tests

- [ ] T00N - <testes explícitos por camada (ex: unit, integration, e2e)>

## Finalize

- [ ] T0XX Rodar `./gradlew build` (inclui o gate de cobertura Jacoco ≥ 80%) e corrigir falhas
- [ ] T0YY Atualizar o CHANGELOG (omita — ainda não há changelog no projeto)
```

Adapte as seções ao `plan.md` real – o template é flexível, não uma lista fixa de camadas.

---

## Regras de escrita de tasks

- **Uma task = uma unidade revisável:** Idealmente um arquivo ou par estrito (impl + mock/test).
- **ID estável (`T001`, `T002`, ...):** Para commits, PRs e relatório de verificação.
- **Caminho ou módulo concreto:** Copiado do plan – evite "atualizar o serviço".
- **Ordem por dependência:** Persistência/schema antes do domínio que lê dados; núcleo antes do ponto de entrada; implementação antes ou junto do teste que depende dela.
- **`[P]`:** Só quando tasks são independentes (arquivos distintos, sem dependência de task anterior aberta).
- **Testes explícitos:** Não esconda "add tests" dentro de task de produção; siga os padrões de teste do projeto (JUnit5 + MockK + Testcontainers), cobrindo happy path + ramos de erro relevantes.
- **Mitigações do plan:** Extraia tarefas próprias quando o plan pediu.
- **Finalize:** Sempre inclui o gate `./gradlew build` (cobertura Jacoco ≥ 80%); changelog só se o projeto passar a ter um.

---

## Durante a implementação

- Marque `- [x]` assim que concluir – `tasks.md` é um documento vivo.
- Trabalho novo → novo ID; não faça trabalho fora da lista.
- Task errada → corrija `tasks.md` (e `plan.md` se o plano estava errado).

---

## Checklist antes de Implement

1. Cada linha do mapeamento do plan tem task correspondente?
2. Pares impl/test cobertos conforme estratégia do plan?
3. Ordem de dependência correta?
4. `[P]` só onde faz sentido?
5. Seção Finalize com o quality gate (`./gradlew build`)?

---

## Relacionamento

- **Orquestrador:** `spec-driven-development`
- **Anterior:** `spec-plan`
- **Execução e Verificação:** fases `Implement` e `Verify` do orquestrador (ainda sem skill dedicada;
  seguem os padrões de código e teste do projeto e o gate `./gradlew build`).
