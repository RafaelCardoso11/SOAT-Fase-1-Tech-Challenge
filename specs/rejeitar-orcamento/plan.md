# Technical Plan: Rejeitar Orçamento (UC06)

**Spec Ref:** `specs/rejeitar-orcamento/spec.md`
**Status:** Draft

## Summary
`OrdemServico.rejeitarOrcamento(motivo)` valida status `Aguardando aprovação`, registra o motivo no
`Orcamento` e encerra a OS (`Finalizada`). O serviço libera as reservas de peça (`EstoquePort.liberarReserva`,
stub) e notifica a oficina (`NotificacaoPort.notificarOrcamentoRejeitado`), publicando `OrcamentoRejeitado`,
`OsEncerrada` (reusado do UC02) e `ReservaLiberada`. Escrita sob `@Transactional` + lock otimista.

## Requirement Mapping
| Spec Ref | Componente / Camada | Nota |
|---|---|---|
| `FR-001`/`FR-006` | `domain/model/OrdemServico.rejeitarOrcamento` (guarda de status) | bloqueia fora de `AGUARDANDO_APROVACAO` |
| `FR-002` | `domain/model/Orcamento.rejeitar(motivo)` (`motivoRejeicao`) + evento `domain/event/OrcamentoRejeitado` | motivo opcional + evento |
| `FR-003` | `OrdemServico.rejeitarOrcamento` (status→`FINALIZADA`) + evento `OsEncerrada` (reuso UC02) | encerra |
| `FR-004` | `domain/port/outbound/EstoquePort.liberarReserva` (stub) + evento `domain/event/ReservaLiberada` | libera reserva |
| `FR-005` | `domain/port/outbound/NotificacaoPort.notificarOrcamentoRejeitado` (stub/e-mail) | notifica oficina |

## Entry Point(s)
Rota administrativa (JWT): `POST /api/admin/ordens/{id}/orcamento/rejeitar`, body `{ motivo? }`
→ `200 { status }`. Erros: `404` inexistente, `409` fora de `Aguardando aprovação`.

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Modificar | `domain/model/Orcamento` | `motivoRejeicao: String?` (private set); `rejeitar(motivo)` |
| Domain (model) | Modificar | `domain/model/OrdemServico` | `rejeitarOrcamento(motivo)` (guarda; status→`FINALIZADA`) |
| Domain (event) | Criar | `domain/event/{OrcamentoRejeitado,ReservaLiberada}` | `: DomainEvent`, `ordemServicoId` |
| Domain (port in) | Criar | `domain/port/inbound/RejeitarOrcamentoUseCase` | `rejeitar(id, motivo)` |
| Domain (port out) | Modificar | `domain/port/outbound/{EstoquePort,NotificacaoPort}` | += `liberarReserva(descricao)` / `notificarOrcamentoRejeitado(id, motivo)` |
| Application | Criar | `application/usecase/RejeitarOrcamentoService` | `@Transactional`; rejeita → salva → libera reservas → eventos → notifica |
| Adapter (in/web) | Criar | `adapter/inbound/web/RejeitarOrcamentoController` + dto request `{ motivo? }` | rota POST |
| Adapter (out) | Modificar | `estoque/EstoquePortStub` + `notificacao/{NotificacaoPortStub,NotificacaoPorEmail}` | implementar os novos métodos |
| Adapter (out/persist) | Modificar | `persistence/OrcamentoJpaEntity` + `OrdemServicoRepositoryAdapter` | coluna `motivo_rejeicao`; `salvar` sincroniza; `toJpa`/`toDomain` mapeiam |
| Adapter (config) | Modificar | `adapter/config/OrdemServicoBeanConfig` | bean `RejeitarOrcamentoService` |

## Data Model / Persistence
Migração **`V9`**: `ALTER TABLE ordemservico.orcamento ADD COLUMN motivo_rejeicao VARCHAR(500) NULL`.

## Integrations & Observability
- **Portas (stub):** `EstoquePort.liberarReserva` (integração real = estoque), `NotificacaoPort.notificarOrcamentoRejeitado` (stub/e-mail à oficina).
- **Eventos:** `OrcamentoRejeitado`, `OsEncerrada`, `ReservaLiberada`. HTTP 200/404/409.

## Test Strategy
- **Unit (MockK):** `RejeitarOrcamentoServiceTest` — rejeitar com motivo (status `FINALIZADA`, motivo, 3 eventos, `liberarReserva` por peça, notifica); sem motivo; fora de `AGUARDANDO_APROVACAO`; OS inexistente.
- **Integração (Testcontainers):** fluxo criar→diagnóstico→orçamento→rejeitar → recarregar (`FINALIZADA`, `motivoRejeicao`).
- **HTTP (MockMvc + JWT):** `POST /orcamento/rejeitar` → 200; 409 fora de aguardando; 404; 401.
- **Cucumber:** cenários do UC06 (`@implementado`), com fraseado próprio (evitar colisão de passos).

## Quality / Lint Risks & Mitigation
- **Novos métodos nas portas** → implementar no stub **e** no adapter de e-mail (não quebrar o wiring condicional).
- **`salvar` sincroniza `motivoRejeicao`** (orçamento já existe) → tratar junto do `aprovadoEm`.

## Alternatives Considered
- **Não encerrar na rejeição:** descartado — a política do Event Storming encerra a OS e libera a reserva.

## Risks / Open Questions
- Liberação de reserva é stub (estoque real pendente — #39).
- Notificação à oficina com destinatário de MVP (spec Open Questions).
