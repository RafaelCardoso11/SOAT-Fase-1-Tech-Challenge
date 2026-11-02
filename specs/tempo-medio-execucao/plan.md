# Technical Plan: Tempo Médio de Execução (UC13)

**Spec Ref:** `specs/tempo-medio-execucao/spec.md`
**Status:** Draft

## Summary
A OS passa a persistir `execucaoIniciadaEm` (set no `iniciarExecucao`, UC09) e `execucaoFinalizadaEm`
(set no `finalizarExecucao`, UC10). `ConsultarTempoMedioExecucaoService` busca as OS com ambos e devolve a
média das durações (`Duration?`; null quando não há dados). Consulta read-only, administrativa.

## Requirement Mapping
| Spec Ref | Componente | Nota |
|---|---|---|
| `FR-001`/`FR-002` | `OrdemServicoRepository.buscarComExecucaoConcluida` + `application/usecase/ConsultarTempoMedioExecucaoService` | média das OS com início e fim |
| `FR-003` | `ConsultarTempoMedioExecucaoService` (null) + `RelatorioController` (mensagem) | sem dados |
| `FR-004` | rota `/api/admin/relatorios/**` (JWT); sem evento | read-only |
| (modelagem) | `OrdemServico.{execucaoIniciadaEm,execucaoFinalizadaEm}` + migration `V12` | instantes persistidos |

## Entry Point(s)
`GET /api/admin/relatorios/tempo-medio-execucao` → `200 { temDados, mediaMinutos?, mensagem? }`. `401` sem token.

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Modificar | `domain/model/OrdemServico` | `execucaoIniciadaEm`/`execucaoFinalizadaEm` (set em iniciar/finalizar) |
| Domain (port in) | Criar | `domain/port/inbound/ConsultarTempoMedioExecucaoUseCase` | `consultar(): Duration?` |
| Domain (port out) | Modificar | `domain/port/outbound/OrdemServicoRepository` | += `buscarComExecucaoConcluida()` |
| Application | Criar | `application/usecase/ConsultarTempoMedioExecucaoService` | média das durações |
| Adapter (in/web) | Criar | `adapter/inbound/web/RelatorioController` + `TempoMedioExecucaoResponse` | rota GET |
| Adapter (out/persist) | Modificar | `OrdemServicoJpaEntity` + `OrdemServicoSpringDataRepository` + `OrdemServicoRepositoryAdapter` | colunas + query `findBy...IsNotNull...` |
| Adapter (config) | Modificar | `OrdemServicoBeanConfig` | bean do use case |

## Data Model / Persistence
Migração **`V12`**: `ordem_servico` += `execucao_iniciada_em TIMESTAMP NULL`, `execucao_finalizada_em TIMESTAMP NULL`.
Query: `findByExecucaoIniciadaEmIsNotNullAndExecucaoFinalizadaEmIsNotNull`.

## Integrations & Observability
- Read-only; sem evento. HTTP 200/401.

## Test Strategy
- **Unit (MockK):** `ConsultarTempoMedioExecucaoServiceTest` — média de 2; ignora incompleta; sem dados → null.
- **Integração (Testcontainers):** fluxo até finalizar execução → consultar devolve média não-nula.
- **HTTP (MockMvc + JWT):** `GET /relatorios/tempo-medio-execucao` → 200 com dados; 401.
- **Cucumber:** cenários do UC13 (`@implementado`), dirigindo o service com repositório mockado (instantes fixos).

## Quality / Lint Risks & Mitigation
- **`iniciarExecucao` volta a persistir** (grava o instante) → `IniciarExecucaoService` salva; mudança aditiva.
- **Média com timestamps de teste** — Cucumber/unit usam instantes fixos; integração/HTTP só verificam presença (tempos reais).

## Alternatives Considered
- **Derivar da linha de status (aprovação→fim):** descartado — mediria a espera até o mecânico iniciar, não a execução real.
- **Event store:** fora do escopo (lacuna #1); os instantes na OS resolvem o cálculo.

## Risks / Open Questions
- Nenhuma bloqueante.
