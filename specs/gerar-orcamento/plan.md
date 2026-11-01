# Technical Plan: Gerar Orçamento (UC03)

**Spec Ref:** `specs/gerar-orcamento/spec.md`
**Status:** Draft

## Summary
Estende o agregado `OrdemServico` com a geração de orçamento: cria um `Orcamento` (itens + total em
`Dinheiro`), transiciona `Em diagnóstico`→`Aguardando aprovação` e registra o evento `OrcamentoGerado`.
Reserva de peça (`EstoquePort`) e notificação (`NotificacaoPort`) entram como **portas com stub** (integração
real adiada — PR usa `Refs #39`). Concorrência via `@Transactional` + `@Version` (como no UC02).

## Requirement Mapping
| Spec Ref | Componente / Camada | Nota |
|---|---|---|
| `FR-001`/`FR-006` | `domain/model/OrdemServico.gerarOrcamento` (guarda: `EM_DIAGNOSTICO` + `liberadoParaOrcamento`) | bloqueia fora das condições |
| `FR-002` | `domain/model/{Orcamento,ItemOrcamento,TipoItem}` (+ `Dinheiro`) | total = soma dos itens |
| `FR-003` | `OrdemServico.gerarOrcamento` (status→`AGUARDANDO_APROVACAO`) + evento `domain/event/OrcamentoGerado` + `application/usecase/GerarOrcamentoService` | associa + transita + evento |
| `FR-004` | `domain/port/outbound/EstoquePort` (+ stub) | reserva de peças |
| `FR-005` | `domain/port/outbound/NotificacaoPort` (+ stub) | notifica cliente |

## Entry Point(s)
Rota administrativa (JWT): `POST /api/admin/ordens/{id}/orcamento`, body `{ itens: [{ tipo, descricao, valor }] }`
→ `200 { status, total }`. Erros: `404` OS inexistente, `409` fora das condições, `400` itens inválidos.

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Criar | `domain/model/{TipoItem,ItemOrcamento,Orcamento}` | `Orcamento.total` = fold com `Dinheiro.plus` |
| Domain (model) | Modificar | `domain/model/OrdemServico` | `orcamento: Orcamento?`; `gerarOrcamento(itens): Orcamento`; `reconstituir` recebe `orcamento` (default null) |
| Domain (event) | Criar | `domain/event/OrcamentoGerado` | `: DomainEvent`, `ordemServicoId` |
| Domain (port in) | Criar | `domain/port/inbound/GerarOrcamentoUseCase` | `gerar(id, itens): OrdemServico` |
| Domain (port out) | Criar | `domain/port/outbound/{EstoquePort,NotificacaoPort}` | `reservar(descricaoPeca)` / `notificarOrcamentoGerado(id, total)` |
| Application | Criar | `application/usecase/GerarOrcamentoService` | `@Transactional`; gera → salva → reserva peças → evento → notifica |
| Adapter (in/web) | Criar | `adapter/inbound/web/OrcamentoController` + `dto/` | rota POST + mapeia request→`ItemOrcamento` |
| Adapter (out) | Criar (stub) | `adapter/outbound/estoque/EstoquePortStub` + `adapter/outbound/notificacao/NotificacaoPortStub` | `@Component` no-op; `shortcut:` documentado |
| Adapter (out/persist) | Modificar | `adapter/outbound/persistence/{OrdemServicoJpaEntity,OrdemServicoRepositoryAdapter}` + `OrcamentoJpaEntity`/`ItemOrcamentoJpaEntity` | `@OneToOne` orçamento; `salvar`/`toDomain` mapeiam o orçamento |
| Adapter (config) | Modificar | `adapter/config/OrdemServicoBeanConfig` | bean `GerarOrcamentoService` |

## Data Model / Persistence
Migração **`V7`**:
- `ordemservico.orcamento (id, total NUMERIC(12,2))`
- `ordemservico.ordem_servico ADD COLUMN orcamento_id BIGINT REFERENCES orcamento(id)`
- `ordemservico.item_orcamento (id, orcamento_id FK, tipo, descricao, valor NUMERIC(12,2))`

## Integrations & Observability
- **Portas (stub):** `EstoquePort` (reserva — integração real = UC04/estoque), `NotificacaoPort` (e-mail/SMTP — externo). PR com `Refs #39`.
- **Eventos:** `OrcamentoGerado` via `PublicadorDeEventos`. HTTP 200/400/404/409.

## Test Strategy
- **Unit (MockK):** `GerarOrcamentoService` — total correto, status→`AGUARDANDO_APROVACAO`, evento, `EstoquePort.reservar` chamado por peça, `NotificacaoPort` chamado; bloqueio sem diagnóstico finalizado; bloqueio fora da etapa; OS inexistente. Domínio: `OrdemServico.gerarOrcamento` (total/guarda).
- **Integração (Testcontainers):** criar OS → diagnóstico → gerar orçamento → recarregar: total + itens + status persistidos.
- **HTTP (MockMvc + JWT):** `POST /orcamento` → 200; 409 sem diagnóstico finalizado; 404; 401. (Padrão do repo — UC01/UC02/cadastro.)
- **Cucumber:** cenários do UC03 (`@implementado`) dirigindo o service com portas mockadas.

## Quality / Lint Risks & Mitigation
- **Agregado cresce (orçamento)** → encapsular via `gerarOrcamento`; sem setters públicos.
- **Persistência do orçamento (1-1 + itens)** → cascade a partir da OS; `salvar` cria o orçamento no caminho de update quando ainda não existe.
- **Stubs** → `shortcut:` nos adapters apontando a integração real (estoque/notificação).

## Alternatives Considered
- **`Orcamento` como agregado próprio:** descartado — Event Storming tem a OS como raiz; o orçamento vive sob a OS (consistência no mesmo agregado).

## Risks / Open Questions
- Peça indisponível / revisão (UC07): fora do escopo; stub considera disponível.
- Itens como texto livre até a integração com catálogo/estoque.
