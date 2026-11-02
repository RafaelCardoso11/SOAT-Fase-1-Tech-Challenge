# Technical Plan: Finalizar Execução (UC10)

**Spec Ref:** `specs/finalizar-execucao/spec.md`
**Status:** Draft

## Summary
`OrdemServico.finalizarExecucao(todosAplicados)` valida status `Em execução` e exige a confirmação de que
todos os itens foram aplicados; transiciona para `Finalizada` e registra `ExecucaoFinalizada`. O serviço
notifica o cliente (veículo pronto). Sem mudança de schema.

## Requirement Mapping
| Spec Ref | Componente | Nota |
|---|---|---|
| `FR-001`/`FR-005` | `domain/model/OrdemServico.finalizarExecucao` (guarda `EM_EXECUCAO`) | bloqueia fora de execução |
| `FR-002` | `finalizarExecucao` (`check(todosAplicados)`) | bloqueia itens pendentes |
| `FR-003` | `finalizarExecucao` (status→`FINALIZADA`) + evento `ExecucaoFinalizada` | finaliza |
| `FR-004` | `FinalizarExecucaoService` + `NotificacaoPort.notificarVeiculoPronto` | notifica cliente |

## Entry Point(s)
`POST /api/admin/ordens/{id}/execucao/finalizar`, body `{ todosAplicados }` → `200 { status }`.
Erros: `404`, `409` (fora de `Em execução` **ou** itens pendentes).

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Modificar | `domain/model/OrdemServico` | `finalizarExecucao(todosAplicados)` (guarda; status→`FINALIZADA`) |
| Domain (event) | Criar | `domain/event/ExecucaoFinalizada` | `: DomainEvent`, `ordemServicoId` |
| Domain (port in) | Criar | `domain/port/inbound/FinalizarExecucaoUseCase` | `finalizar(id, todosAplicados)` |
| Domain (port out) | Modificar | `domain/port/outbound/NotificacaoPort` | += `notificarVeiculoPronto(id)` |
| Application | Criar | `application/usecase/FinalizarExecucaoService` | `@Transactional`; finaliza → salva → evento → notifica |
| Adapter (in/web) | Modificar | `adapter/inbound/web/ExecucaoController` | += `POST /finalizar` + request `{ todosAplicados }` |
| Adapter (out) | Modificar | `notificacao/{NotificacaoPortStub,NotificacaoPorEmail}` | implementar `notificarVeiculoPronto` |
| Adapter (config) | Modificar | `OrdemServicoBeanConfig` | bean `FinalizarExecucaoService` |

## Data Model / Persistence
Nenhuma mudança de schema (confirmação é parâmetro do comando, não estado persistido).

## Integrations & Observability
- Evento `ExecucaoFinalizada`; notificação ao cliente (stub/e-mail). HTTP 200/404/409.

## Test Strategy
- **Unit (MockK):** `FinalizarExecucaoServiceTest` — finaliza (status `FINALIZADA`, evento, notifica); item pendente (`todosAplicados=false`) bloqueia; fora de `EM_EXECUCAO`; OS inexistente.
- **Integração (Testcontainers):** fluxo criar→diagnóstico→orçamento→aprovar→iniciar→finalizar → `Finalizada`.
- **HTTP (MockMvc + JWT):** `POST /execucao/finalizar` → 200; 409 pendente/fora; 404; 401.
- **Cucumber:** cenários do UC10 (`@implementado`), fraseado próprio.

## Quality / Lint Risks & Mitigation
- **`Finalizada` reusado** (encerramento vs conclusão) → distinguidos pelos eventos (`ExecucaoFinalizada` vs `OsEncerrada`).

## Alternatives Considered
- **Rastreio por item (aplicado):** descartado no MVP — exigiria marcar cada item + persistência; confirmação por flag resolve o comportamento.

## Risks / Open Questions
- Confirmação por flag em vez de rastreio por item (spec Open Questions).
