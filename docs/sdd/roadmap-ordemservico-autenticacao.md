# Roadmap SDD — módulos `ordemservico` + `autenticacao`

Documento de trabalho (local) para conduzir o Spec-Driven Development das duas frentes sob minha
responsabilidade. Cada linha vira o ciclo: **issue → branch → spec → plan → tasks → implement → PR**
(ver `CLAUDE.md` e as skills `/spec-specify`, `/spec-plan`, `/spec-tasks`).

## Divisão de trabalho

| Módulo | Dono | Subdomínio |
|--------|------|------------|
| `ordemservico` | **eu** | principal (core) |
| `autenticacao` | **eu** | genérico |
| `cadastro` | outro(a) integrante | suporte |
| `estoque` | outro(a) integrante | suporte |
| `shared-kernel` | **compartilhado** (combinar antes de mexer) | — |

> ⚠️ Vários UCs de `ordemservico` dependem de `cadastro`/`estoque` (de outra pessoa). Onde houver
> dependência, a comunicação é por **porta + ACL** (`CadastroPort`, `EstoquePort`) — dá para começar
> com a porta + um stub e integrar depois, sem travar.

## Backlog (features e status SDD)

Legenda: ⬜ a fazer · 🟦 em andamento · ✅ feito

### `autenticacao`

| Feature (slug) | Escopo | Depende de | spec | plan | tasks | impl |
|----------------|--------|-----------|:----:|:----:|:-----:|:----:|
| `auth-jwt` | Login (emite JWT) + filtro que protege as rotas administrativas; `Usuario` | — (autocontido) | ✅ | ✅ | ✅ | ✅ (PR #18) |

### `ordemservico`

| Feature (slug) | UC | Resumo | Depende de | spec | plan | tasks | impl |
|----------------|----|--------|-----------|:----:|:----:|:-----:|:----:|
| `criar-ordem-servico` | UC01 | Abrir OS (cliente + veículo + relato) → status `Recebida` | cadastro | ⬜ | ⬜ | ⬜ | ⬜ |
| `realizar-diagnostico` | UC02 | Iniciar/registrar/finalizar diagnóstico | — | ⬜ | ⬜ | ⬜ | ⬜ |
| `gerar-orcamento` | UC03 | Montar orçamento a partir do diagnóstico → `Aguardando aprovação` | cadastro (serviços), estoque (peças) | ⬜ | ⬜ | ⬜ | ⬜ |
| `aprovar-orcamento` | UC05 | Cliente aprova dentro da validade → `Aguardando execução` | — | ⬜ | ⬜ | ⬜ | ⬜ |
| `rejeitar-orcamento` | UC06 | Cliente rejeita (com/sem motivo) → `Rejeitado` | — | ⬜ | ⬜ | ⬜ | ⬜ |
| `revisar-orcamento` | UC07 | Ajustar orçamento rejeitado e reenviar | — | ⬜ | ⬜ | ⬜ | ⬜ |
| `expirar-orcamento` | UC08 | Expirar orçamento sem resposta no prazo | — | ⬜ | ⬜ | ⬜ | ⬜ |
| `iniciar-execucao` | UC09 | Iniciar serviços aprovados + baixa de peças | estoque (baixa via porta) | ⬜ | ⬜ | ⬜ | ⬜ |
| `finalizar-execucao` | UC10 | Registrar conclusão → `Finalizada` | — | ⬜ | ⬜ | ⬜ | ⬜ |
| `entregar-veiculo` | UC11 | Registrar entrega → `Encerrada` | — | ⬜ | ⬜ | ⬜ | ⬜ |
| `consultar-status` | UC12 | Consulta pública do andamento da OS | cadastro (lookup por placa) | ⬜ | ⬜ | ⬜ | ⬜ |
| `tempo-medio-execucao` | UC13 | Tempo médio entre início e fim da execução (admin) | — | ⬜ | ⬜ | ⬜ | ⬜ |

## Pendências de domínio a resolver na fase `spec` (hotspots do Event Storming)

- **Prazo de validade do orçamento** (UC08) — ainda não definido.
- **Peça não identificada no diagnóstico** (UC02) — regra em aberto.
- **Status da OS:** reconciliar os 6 oficiais do enunciado (`Recebida → Em diagnóstico → Aguardando
  aprovação → Em execução → Finalizada → Entregue`) com os estados extras usados nos BDDs
  (`Diagnóstico finalizado`, `Aguardando execução`, `Encerrada`). Decidir e documentar no `spec`/`data-model`.

## Ordem recomendada

1. **`auth-jwt`** — autocontido (não depende de ninguém) e protege as rotas administrativas dos demais
   UCs. Bom primeiro slice vertical: exercita `domain/application/adapter` + segurança + testes.
2. **`shared-kernel` (combinar com o grupo):** VOs que vou precisar — `Dinheiro` (orçamento) e, se não
   ficarem com `cadastro`, `Documento`/`Placa`. Alinhar dono antes de implementar.
3. **`criar-ordem-servico` (UC01)** — primeira fatia do core. Depende de `cadastro`; começar pela
   `CadastroPort` + stub se o módulo do outro ainda não existir.
4. **Ciclo do orçamento:** `realizar-diagnostico` (UC02) → `gerar-orcamento` (UC03) → `aprovar`/`rejeitar`/
   `revisar`/`expirar` (UC05–08).
5. **Execução e entrega:** `iniciar-execucao` (UC09, integra com `estoque`) → `finalizar-execucao` (UC10)
   → `entregar-veiculo` (UC11).
6. **Consultas:** `consultar-status` (UC12, pública) e `tempo-medio-execucao` (UC13).

## Passo a passo por feature

Para cada linha do backlog:

1. **Issue** (`gh issue create`): `## Contexto`, `## Escopo` (checklist), `## Critérios de aceite`, label `enhancement`.
2. **Branch** de `origin/main`: `feat/<nº-issue>-<slug>`.
3. **`/spec-specify <slug>`** → `specs/<slug>/spec.md` (requisitos, agnóstico de tecnologia; resolver hotspots).
4. **`/spec-plan <slug>`** → `specs/<slug>/plan.md` (mapear FR → `domain/application/adapter`, portas, migrations Flyway).
5. **`/spec-tasks <slug>`** → `specs/<slug>/tasks.md` (checklist T001.., ordem por dependência, testes explícitos).
6. **Implement:** seguir o `tasks.md`; testes unit (JUnit5/MockK) + integração (Testcontainers) + o `.feature` via Cucumber; gate `./gradlew build` (cobertura ≥ 80%).
7. **PR** com `## Objetivo / O que entra / Como validar / Fora de escopo` + `Closes #<nº>`.

## Definição de pronto (por feature)

- [ ] `spec.md`, `plan.md`, `tasks.md` em `specs/<slug>/`.
- [ ] Testes cobrindo happy path + ramos de erro; cobertura ≥ 80% no domínio/aplicação.
- [ ] `.feature` correspondente passando (Cucumber).
- [ ] `./gradlew build` verde (local e CI).
- [ ] PR revisado e mesclado, fechando a issue.
