# Technical Plan: Entregar Veículo (UC11)

**Spec Ref:** `specs/entregar-veiculo/spec.md`
**Status:** Draft

## Summary
`OrdemServico.entregar()` valida status `Finalizada` e transiciona para `Entregue`. O serviço publica
`VeiculoEntregue` e, em seguida, `OsEncerrada`, e devolve um **comprovante** (dados da OS + data/hora da
entrega). Sem mudança de schema (a entrega é uma transição de status já persistida na linha do tempo).

## Requirement Mapping
| Spec Ref | Componente | Nota |
|---|---|---|
| `FR-001`/`FR-004` | `domain/model/OrdemServico.entregar` (guarda `FINALIZADA`) | bloqueia fora de finalizada |
| `FR-002` | `entregar` (status→`ENTREGUE`) + eventos `VeiculoEntregue`, `OsEncerrada` (nessa ordem) | entrega + encerra |
| `FR-003` | `adapter/inbound/web/dto/ComprovanteResponse` | comprovante no retorno |

## Entry Point(s)
`POST /api/admin/ordens/{id}/entrega` → `200` com o comprovante. Erros: `404`, `409` (fora de `Finalizada`).

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Modificar | `domain/model/OrdemServico` | `entregar()` (guarda `FINALIZADA`; status→`ENTREGUE`) |
| Domain (event) | Criar | `domain/event/VeiculoEntregue` | `: DomainEvent`, `ordemServicoId` |
| Domain (port in) | Criar | `domain/port/inbound/EntregarVeiculoUseCase` | `entregar(id): OrdemServico` |
| Application | Criar | `application/usecase/EntregarVeiculoService` | `@Transactional`; entrega → salva → `VeiculoEntregue` → `OsEncerrada` |
| Adapter (in/web) | Criar | `adapter/inbound/web/EntregaController` + `dto/ComprovanteResponse` | rota POST; monta o comprovante |
| Adapter (config) | Modificar | `OrdemServicoBeanConfig` | bean `EntregarVeiculoService` |

## Data Model / Persistence
Nenhuma mudança de schema. Data/hora da entrega = momento da transição para `ENTREGUE` (linha do tempo).

## Integrations & Observability
- Eventos `VeiculoEntregue` → `OsEncerrada`. HTTP 200/404/409.

## Test Strategy
- **Unit (MockK):** `EntregarVeiculoServiceTest` — entrega (status `ENTREGUE`, eventos na ordem); fora de `FINALIZADA`; OS inexistente.
- **Integração (Testcontainers):** fluxo até finalizar → entregar → `Entregue`.
- **HTTP (MockMvc + JWT):** `POST /entrega` → 200 (comprovante); 409 fora de finalizada; 404; 401.
- **Cucumber:** cenários do UC11 (`@implementado`), fraseado próprio.

## Quality / Lint Risks & Mitigation
- **Ordem dos eventos** (`VeiculoEntregue` antes de `OsEncerrada`) → publicar na ordem no service; teste verifica a sequência.

## Alternatives Considered
- **Comprovante como PDF/arquivo:** fora do MVP — retorno simples com os dados.

## Risks / Open Questions
- Nenhuma bloqueante.
