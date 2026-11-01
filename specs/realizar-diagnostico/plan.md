# Technical Plan: Realizar Diagnóstico (UC02)

**Spec Ref:** `specs/realizar-diagnostico/spec.md`
**Status:** Draft

## Summary
Primeiro UC que **altera** o estado de uma OS após a criação. O agregado `OrdemServico` ganha
comportamento (iniciar/registrar inspeção/finalizar) e dois campos de diagnóstico (`observacoes`,
`liberadoParaOrcamento`). As únicas transições de status são `Recebida`→`Em diagnóstico` (iniciar) e
`Em diagnóstico`→`Finalizada` (encerrar sem peça) — reusando a linha do tempo do UC12. O `OrdemServicoRepository`
passa a **atualizar** uma OS existente (hoje só insere). **Nenhum status novo** no enum.

## Requirement Mapping
| Spec Ref | Componente / Camada | Nota |
|---|---|---|
| `FR-001` | `domain/model/OrdemServico.iniciarDiagnostico` + `application/usecase/RealizarDiagnosticoService.iniciar` + evento `DiagnosticoIniciado` | `Recebida`→`Em diagnóstico` |
| `FR-002` | `OrdemServico.registrarInspecao` + `...Service.registrarInspecao` + evento `DiagnosticoRealizado` | grava observações |
| `FR-003` | `OrdemServico.finalizarDiagnostico(pecaIdentificada=true)` + `...Service.finalizar` + evento `DiagnosticoFinalizado` | mantém `Em diagnóstico`, `liberadoParaOrcamento=true` |
| `FR-004` | `OrdemServico.finalizarDiagnostico(pecaIdentificada=false)` (ramo encerrar) + evento `OsEncerrada` | status `Finalizada` |
| `FR-005` | `OrdemServico.iniciarDiagnostico` (guarda de status) + `OrdemServicoExceptionHandler` | bloqueia fora de `Recebida` |
| `FR-006` | `OrdemServico` (append de `TransicaoStatus`) + `adapter/outbound/persistence` | transições persistidas |

## Entry Point(s)
Rotas **administrativas** (mecânico autenticado), sob `/api/admin/ordens/{id}` (JWT, como UC01):
- `POST /api/admin/ordens/{id}/diagnostico/iniciar` → `200 { status }`
- `POST /api/admin/ordens/{id}/diagnostico/inspecao` body `{ observacoes }` → `200`
- `POST /api/admin/ordens/{id}/diagnostico/finalizar` body `{ pecaIdentificada }` → `200 { status, liberadoParaOrcamento }`
- Erros: `404` OS inexistente, `409` operação incompatível com o status atual, `400` payload inválido.

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Modificar | `domain/model/OrdemServico` | campos `observacoes: String?`, `liberadoParaOrcamento: Boolean`; métodos `iniciarDiagnostico`, `registrarInspecao`, `finalizarDiagnostico`; `reconstituir` recebe os novos campos |
| Domain (event) | Criar | `domain/event/{DiagnosticoIniciado,DiagnosticoRealizado,DiagnosticoFinalizado,OsEncerrada}` | `: DomainEvent`, carregam `ordemServicoId` |
| Domain (port in) | Criar | `domain/port/inbound/RealizarDiagnosticoUseCase` | `iniciar(id)`, `registrarInspecao(id, observacoes)`, `finalizar(id, pecaIdentificada)` |
| Domain (port out) | Modificar | `domain/port/outbound/OrdemServicoRepository` | `salvar` passa a atualizar quando `id != null`; `buscarPorId` já existe (UC12) |
| Application | Criar | `application/usecase/RealizarDiagnosticoService` | carrega OS, aplica o método do agregado, salva, publica o evento |
| Adapter (in/web) | Criar | `adapter/inbound/web/DiagnosticoController` + `dto/` | 3 rotas acima |
| Adapter (in/web) | Modificar | `adapter/inbound/web/OrdemServicoExceptionHandler` | += `IllegalStateException`→409, `NoSuchElementException`→404 |
| Adapter (out/persist) | Modificar | `adapter/outbound/persistence/{OrdemServicoJpaEntity,OrdemServicoRepositoryAdapter}` | colunas `observacoes`/`liberado_para_orcamento`; `salvar` com caminho de update + append de transições |
| Adapter (config) | Modificar | `adapter/config/OrdemServicoBeanConfig` | bean do novo use case |

## Data Model / Persistence
- `ALTER TABLE ordemservico.ordem_servico` += `observacoes VARCHAR(2000) NULL`, `liberado_para_orcamento BOOLEAN NOT NULL DEFAULT FALSE` — migração **`V6`**.
- `salvar` em update: busca a entidade por id, atualiza escalares e **anexa** apenas as transições novas (histórico append-only), evitando duplicar as já persistidas.

## Integrations & Observability
- **Integrações:** nenhuma externa (serviços/peças como texto livre; catálogo estruturado fica p/ `cadastro`/`estoque`).
- **Observabilidade:** eventos de domínio `DiagnosticoIniciado/Realizado/Finalizado/OsEncerrada` via `PublicadorDeEventos`. Códigos HTTP 200/400/404/409.

## Test Strategy
- **Unit (MockK):** `RealizarDiagnosticoService` — iniciar (ok + bloqueio fora de `Recebida`), registrar inspeção, finalizar com peça (libera, mantém status), finalizar sem peça (encerra → `Finalizada`); eventos corretos.
- **Integração (Testcontainers):** persistência do caminho de **update** — criar OS, iniciar diagnóstico, recarregar e conferir status/transições/flag.
- **Cucumber:** cenários do UC02 (`uc02_realizar_diagnostico.feature`, tag `@implementado`) dirigindo o service com repositório em memória/mock.

## Quality / Lint Risks & Mitigation
- **Agregado deixa de ser imutável** (ganha `var` nos campos que mudam) → encapsular as mudanças **apenas** via métodos com guarda de status; nada de setters públicos soltos.
- **`salvar` com update + append de transições** → risco de duplicar transições; mitigado anexando só as que excedem as já persistidas (append-only). `shortcut:` documentado no adapter.
- **Retrocompat UC12/UC01** → `reconstituir` e `salvar` continuam funcionando; testes existentes devem seguir verdes.

## Alternatives Considered
- **Novo status `DIAGNOSTICO_FINALIZADO`:** descartado — decisão do time/Event Storming é manter os 6 status oficiais e usar flag de liberação.
- **Agregado imutável (métodos retornando nova instância):** descartado por ora — mais cerimônia sem ganho; o repositório persiste o estado final.

## Risks / Open Questions
- Finalizar sem inspeção registrada: assumido permitido (spec Open Questions).
- "Serviços e insumos identificados": texto livre por ora (spec Open Questions).
