# Technical Plan: Expirar Orçamento (UC08)

**Spec Ref:** `specs/expirar-orcamento/spec.md`
**Status:** Draft

## Summary
Uma verificação periódica (`@Scheduled`, cron configurável) + um endpoint administrativo disparam o
`ExpirarOrcamentosUseCase`, que busca as OS `Aguardando aprovação` com orçamento vencido
(`validoAte < agora`) e expira cada uma: `OrdemServico.expirarOrcamento(agora)` encerra a OS
(`Finalizada`); o serviço libera as reservas (`EstoquePort`, stub), publica `OrcamentoExpirado` +
`OsEncerrada` + `ReservaLiberada` e notifica o atendente. Escrita sob `@Transactional`.

## Requirement Mapping
| Spec Ref | Componente / Camada | Nota |
|---|---|---|
| `FR-001` | `adapter/inbound/scheduler/ExpiracaoScheduler` (`@Scheduled`) + `adapter/inbound/web/ExpiracaoController` | gatilhos periódico/manual |
| `FR-002` | `domain/port/outbound/OrdemServicoRepository.buscarExpiraveis(agora)` + `application/usecase/ExpirarOrcamentosService` | seleciona vencidos |
| `FR-003` | `domain/model/OrdemServico.expirarOrcamento` (status→`FINALIZADA`) + eventos `OrcamentoExpirado`/`OsEncerrada`/`ReservaLiberada` + `EstoquePort.liberarReserva` + `NotificacaoPort.notificarOrcamentoExpirado` | expira + encerra + libera + notifica |
| `FR-004` | guarda em `expirarOrcamento` + filtro da query (`status=AGUARDANDO_APROVACAO`, `validoAte<agora`) | não expira fora das condições |

## Entry Point(s)
- **Agendado:** `@Scheduled(cron = "${orcamento.expiracao-cron:0 0 * * * *}")` → use case.
- **Manual (admin, JWT):** `POST /api/admin/orcamentos/expirar` → `200 { expirados: n }`.

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Modificar | `domain/model/OrdemServico` | `expirarOrcamento(agora)` (guarda `AGUARDANDO_APROVACAO` + `agora > validoAte`; status→`FINALIZADA`) |
| Domain (event) | Criar | `domain/event/OrcamentoExpirado` | `: DomainEvent`, `ordemServicoId` |
| Domain (port in) | Criar | `domain/port/inbound/ExpirarOrcamentosUseCase` | `executar(): Int` (quantidade expirada) |
| Domain (port out) | Modificar | `domain/port/outbound/OrdemServicoRepository` | += `buscarExpiraveis(agora): List<OrdemServico>` |
| Domain (port out) | Modificar | `domain/port/outbound/NotificacaoPort` | += `notificarOrcamentoExpirado(id)` |
| Application | Criar | `application/usecase/ExpirarOrcamentosService` | `@Transactional`; itera vencidos → expira → libera → eventos → notifica |
| Adapter (in) | Criar | `adapter/inbound/scheduler/ExpiracaoScheduler` + `adapter/inbound/web/ExpiracaoController` | gatilhos |
| Adapter (out/persist) | Modificar | `OrdemServicoSpringDataRepository` + `OrdemServicoRepositoryAdapter` | query derivada `findByStatusAndOrcamento_ValidoAteBefore` |
| Adapter (out) | Modificar | `notificacao/{NotificacaoPortStub,NotificacaoPorEmail}` | implementar `notificarOrcamentoExpirado` |
| Adapter (config) | Modificar | `OrdemServicoBeanConfig` | bean `ExpirarOrcamentosService` |
| Bootstrap | Modificar | `OficinaApplication` | `@EnableScheduling` |

## Data Model / Persistence
Nenhuma mudança de schema. Query: `findByStatusAndOrcamento_ValidoAteBefore(status, agora)`.

## Integrations & Observability
- **Portas (stub):** `EstoquePort.liberarReserva`, `NotificacaoPort.notificarOrcamentoExpirado`.
- **Eventos:** `OrcamentoExpirado`, `OsEncerrada`, `ReservaLiberada`. Endpoint 200/401.
- Agendamento via cron configurável (`orcamento.expiracao-cron`, default horário).

## Test Strategy
- **Unit (MockK):** `ExpirarOrcamentosServiceTest` — com `buscarExpiraveis` retornando 1 vencida → expira (status `FINALIZADA`, eventos, `liberarReserva`, notifica, retorna 1); lista vazia → 0, nada acionado. Domínio: `OrdemServico.expirarOrcamento` (vencido aguardando → `FINALIZADA`; dentro do prazo → erro; fora de aguardando → erro).
- **Integração (Testcontainers):** `@SpringBootTest(properties=["orcamento.validade-dias=-1"])` — gerar orçamento já vencido → `executar()` → OS `Finalizada`.
- **HTTP (MockMvc + JWT):** `POST /api/admin/orcamentos/expirar` → 200 (com validade negativa, expira a OS gerada); 401 sem token.
- **Cucumber:** cenários do UC08 (`@implementado`), com fraseado próprio, dirigindo o service com `buscarExpiraveis` mockado conforme o cenário.

## Quality / Lint Risks & Mitigation
- **`@EnableScheduling` ativo em testes** → cron horário não dispara durante os testes curtos; disparo real coberto pelo endpoint/uso direto.
- **Query em propriedade aninhada (`Orcamento.validoAte`)** → usar nome derivado com underscore; `@SpringBootTest` de persistência valida.
- **Novos métodos de porta** → implementar no stub e no adapter de e-mail.

## Alternatives Considered
- **Só agendado, sem endpoint:** descartado — endpoint facilita ops e teste determinístico.
- **Marcar `expiradoEm` no `Orcamento`:** descartado p/ MVP — status `Finalizada` + ausência de aprovação/rejeição já distingue.

## Risks / Open Questions
- Liberação de reserva e notificação são stub (estoque/SMTP reais pendentes).
- Periodicidade default horária (configurável).
