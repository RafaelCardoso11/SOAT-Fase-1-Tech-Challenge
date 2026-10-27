---
name: "spec-plan"
description: >
  Fase de planejamento técnico do SDD. Usada quando o `spec.md` já existe e o usuário
  precisa do plano técnico, mapeamento de arquitetura ou design de "como construir"
  antes de criar as tarefas (Tasks) ou escrever o código.
---

## Goal

Traduzir o `spec.md` aprovado em um **plano técnico concreto para este repositório** — garantindo que a fase de Tasks gere um checklist inequívoco e a Implementação não precise tomar decisões de arquitetura sob pressão.

> 🛠️ **Fase Plan:** Use a taxonomia e estrutura de pastas real do repositório. Não invente uma arquitetura paralela. Consulte `docs/arquitetura.md`.

---

## Entradas Obrigatórias

1. **`specs/<feature-slug>/spec.md` (Obrigatório):** Se não existir, interrompa e retorne para a fase `spec-specify`.
2. **Requisitos e critérios de aceite:** Leia todos os `FR-xxx` e critérios de aceite definidos no `spec.md`.
3. **Analogia no código:** Identifique features ou módulos parecidos já existentes no projeto e reutilize padrões de pastas, *entry points* e testes.

> ⚠️ Se o `spec.md` possuir pontos de `[NEEDS CLARIFICATION]` que afetem a abordagem técnica, resolva com o usuário ou registre como uma suposição na seção **Riscos / Open Questions**.

---

## Passos do Planejamento

1. **Entry Point(s):** Como a funcionalidade é acionada (HTTP controller, mensagem de fila, cron job, CLI, etc.).
2. **Fluxo por Requisito:** Mapeie o caminho: `Entry Point ──> Camadas Intermediárias ──> Persistência/Integrações`, usando nomes reais das pastas e módulos do projeto.
3. **Modelo de Dados:** Defina alterações em tabelas, schemas, entidades ou migrações (ou declare "nenhum").
4. **Integrações Externas:** Apontar APIs de terceiros, filas, serviços externos ou bibliotecas (ou declare "nenhum").
5. **Observabilidade:** Definir o que logar, métricas e códigos de erro a serem tratados.
6. **Estratégia de Testes:** Identificar quais camadas receberão testes unitários, de integração ou end-to-end.
7. **Riscos de Qualidade:** Mapear complexidade, potenciais gargalos e mitigações técnicas.
8. **Alternativas:** Breve resumo de outras abordagens consideradas e por que foram descartadas.

---

## Estrutura do `plan.md`

Modelo de estrutura para o arquivo `plan.md` que o agente deverá gerar. Os exemplos usam a
estrutura hexagonal real deste projeto (`domain` / `application` / `adapter`):

```markdown
# Technical Plan: <Nome da Feature>

**Spec Ref:** `specs/<feature-slug>/spec.md`
**Status:** Draft | Approved

## Summary
Um parágrafo descrevendo a abordagem técnica escolhida em linguagem clara e objetiva.

## Requirement Mapping
Mapeamento direto dos requisitos funcionais para os componentes do projeto:

| Spec Ref | Componente / Camada | Nota de Implementação |
|---|---|---|
| `FR-001` | `modules/ordemservico/.../adapter/inbound/web/OrdemServicoController.kt` | Validação e recepção do payload |
| `FR-002` | `modules/ordemservico/.../application/usecase/CriarOrdemServicoService.kt` | Regra de negócio e orquestração |

*(Todo requisito numerado da spec deve ter uma linha ou justificativa explícita).*

## Entry Point(s)
Gatilhos de entrada, contratos conceituais e payloads envolvidos.

## Component Plan
Onde o código novo/alterado vai morar:

| Camada / Área | Ação | Caminho ou Módulo | Notas |
|---|---|---|---|
| Adapter (inbound/web) | Criar | `modules/ordemservico/.../adapter/inbound/web/` | Rota POST `/api/admin/ordens` |
| Application (use case) | Criar | `modules/ordemservico/.../application/usecase/` | Orquestra o agregado |
| Domain (model) | Modificar | `modules/ordemservico/.../domain/model/` | Nova regra de transição de status |

## Data Model / Persistence
Novas tabelas, alterações de colunas, índices, migrations do Flyway (ou "N/A").

## Integrations & Observability
- **Integrações:** Portas/ACL para outros contextos (ex: `EstoquePort`) ou sistemas externos.
- **Observabilidade:** Logs estruturados, eventos de domínio emitidos ou métricas capturadas.

## Test Strategy
Plano de cobertura por tipo de teste:
- **Unitários:** Mapear funções/métodos com regras de negócio críticas (JUnit5 + MockK).
- **Integração:** Endpoints HTTP ou comunicação com o banco (Testcontainers/Postgres).

## Quality / Lint Risks & Mitigation
Pontos que podem gerar complexidade ou ferir os padrões do repositório e como serão mitigados.

## Alternatives Considered
- **Opção A:** Descrição breve de por que não foi escolhida.

## Risks / Open Questions
Suposições ou incertezas mantidas durante o planejamento técnico.
```

---

## Diretrizes de Código vs Plano

* Defina **responsabilidades e assinaturas conceituais** (nomes de métodos, parâmetros principais, entradas e saídas), mas **não escreva blocos longos de código/implementação completa** no `plan.md`.
* Aponte caminhos reais do repositório onde o código viverá.
* Sempre que possível, oriente o agente a "seguir o padrão do arquivo `X` do repositório".

---

## Checklist para Concluir o Planejamento

- [ ] Cada requisito funcional (`FR-xxx`) da especificação aparece no mapeamento técnico?
- [ ] A separação de responsabilidades entre as camadas/pastas está clara?
- [ ] Mudanças de dados, tabelas e integrações foram explicitadas (ou declaradas como "nenhum")?
- [ ] A estratégia de testes nomeia os cenários e camadas a cobrir?
- [ ] Riscos de qualidade e complexidade foram documentados?
