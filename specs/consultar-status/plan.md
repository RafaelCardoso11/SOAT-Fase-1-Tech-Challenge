# Technical Plan: Consultar Status da OS (UC12)

**Spec Ref:** `specs/consultar-status/spec.md`
**Status:** Draft

## Summary
Lado de **leitura** (consulta pública) + registro de **transições de status** (linha do tempo).
Introduz um histórico de transições persistido (append-only), o **carregamento/reconstrução** da OS
(até aqui só salvávamos) e um **retrofit do UC01** para registrar a transição inicial (`RECEBIDA`) na
criação. Consulta por número/placa/documento retorna uma **lista** de OS com status, timeline e detalhes.

## Requirement Mapping
| Spec Ref | Componente / Camada | Nota |
|---|---|---|
| `FR-001`/`FR-002` | `adapter/inbound/web/ConsultarOrdensController` + `application/usecase/ConsultarOrdensService` + `domain/port/outbound/OrdemServicoRepository` (queries) | Consulta por número/placa/documento → lista |
| `FR-003` | DTOs de resposta em `adapter/inbound/web/dto/` (status + timeline + detalhes) | Monta a visão de leitura |
| `FR-004` | `domain/model/OrdemServico` + `domain/model/TransicaoStatus` + persistência | Registra transição (começa na criação) |
| `FR-005` | `ConsultarOrdensService` (lista vazia) + controller | Sem OS → vazio, não erro |
| `FR-006` | `SecurityConfig` (bootstrap) já libera `/api/public/**`; sem evento | Rota pública, read-only |

## Entry Point(s)
- `GET /api/public/ordens/{numero}` → uma OS (ou 404/vazio).
- `GET /api/public/ordens?placa=...` e `?documento=...` → **lista** de OS.
- Resposta: `{ numero, status, veiculo, documento, relato, linhaDoTempo: [{ status, momento }], ... }`. Lista vazia quando nada encontrado (200).
- **Público** (sem token) — já contemplado pela convenção `/api/public/**`.

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Criar | `domain/model/TransicaoStatus` | VO: `status: StatusOS`, `momento: Instant` |
| Domain (model) | Modificar | `domain/model/OrdemServico` | ganha `id: Long?` + `transicoes: List<TransicaoStatus>`; `criar(...)` registra `RECEBIDA`; `reconstituir(...)` p/ load |
| Domain (port in) | Criar | `domain/port/inbound/ConsultarOrdensUseCase` | `porNumero(id)`, `porPlaca(placa)`, `porDocumento(doc)` |
| Domain (port out) | Modificar | `domain/port/outbound/OrdemServicoRepository` | += `buscarPorId`, `buscarPorPlaca` (lista), `buscarPorDocumento` (lista) |
| Application | Criar | `application/usecase/ConsultarOrdensService` | orquestra as consultas |
| Adapter (in/web) | Criar | `adapter/inbound/web/ConsultarOrdensController` + `dto/` | rota pública, mapeia OS → resposta |
| Adapter (out/persist) | Criar | `adapter/outbound/persistence/TransicaoStatusJpaEntity` | tabela de transições |
| Adapter (out/persist) | Modificar | `adapter/outbound/persistence/OrdemServicoRepositoryAdapter` | queries + mapeia/reconstitui OS com transições; `salvar` persiste as transições |

**Retrofit UC01:** `CriarOrdemServicoService`/`OrdemServico.criar` passam a registrar a transição inicial
(`RECEBIDA`, `Instant.now()` via um relógio injetável simples). Os testes existentes do UC01 continuam válidos
(o id e as transições são detalhes a mais na OS criada).

## Data Model / Persistence
- Nova tabela **`ordemservico.transicao_status`** (`id`, `ordem_servico_id` FK, `status`, `momento`) — migração **`V4`**.
- `ordem_servico` inalterada; a OS carrega suas transições (ordenadas por `momento`).

## Integrations & Observability
- **Integrações:** nenhuma externa. Sem `cadastro` (identificadores já embutidos na OS).
- **Observabilidade:** read-only, **sem evento de domínio**. Respostas `200` (lista possivelmente vazia).

## Test Strategy
- **Unit (MockK):** `ConsultarOrdensService` (por número/placa/documento; vazio). `OrdemServico.criar` registra transição `RECEBIDA`.
- **Integração (Testcontainers):** salvar OS (com transição) e consultar por número/placa/documento → lista com timeline; identificador sem OS → vazio.
- **Cucumber:** cenários do UC12 em `docs/bdd/uc12_*.feature` (tag `@implementado`), dirigindo o use case com dados em memória/mocks.

## Quality / Lint Risks & Mitigation
- **Retrofit do UC01** (toca código mergeado: `OrdemServico`, `CriarOrdemServicoService`, adapter de persistência) → mitigado pelos testes existentes + novos; mudança aditiva (id/transições).
- **Reconstrução da OS** (private constructor) → adicionar `reconstituir(...)` no agregado.

## Alternatives Considered
- **Timeline derivada de um event store/outbox** (lacuna #1): descartado por ora — exigiria consumidor de eventos; o histórico de transições (tabela própria, append-only) resolve a linha do tempo com menos acoplamento.
- **Consulta retornando uma única OS**: descartado — placa/documento podem ter várias OS (decisão: lista).

## Risks / Open Questions
- Ordenação da lista: **mais recente primeiro** (assumido).
- Identificador malformado: tratado como **"não encontrado"** (sem erro) — assumido.
