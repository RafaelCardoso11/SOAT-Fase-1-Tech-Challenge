# Technical Plan: Iniciar Execução + Integração Estoque (UC09 / #39)

**Spec Ref:** `specs/iniciar-execucao/spec.md`
**Status:** Draft

## Summary
Integração **real** com o módulo `estoque` via ACL: o `EstoquePort` do ordemservico é implementado por
`EstoqueAdapter`, que chama os casos de uso do `estoque` (`ReservarPecaUseCase`/`LiberarReservaUseCase`/
`BaixarPecasUseCase`), tudo chaveado por `osId`. A peça do orçamento passa a **referenciar o item do estoque**
(`itemEstoqueId` + `quantidade`); serviço segue texto livre. UC09 inicia a execução de uma OS `Em execução`,
dando **baixa** das peças e publicando `Execução iniciada`/`Peças baixadas`. **Caminho não-feliz** (peça
indisponível na geração/revisão) **encerra a OS** (`Finalizada`), conforme política do Event Storming.

## Requirement Mapping
| Spec Ref | Componente | Nota |
|---|---|---|
| `FR-001`/`FR-003` | `domain/model/OrdemServico.iniciarExecucao` (guarda `EM_EXECUCAO`) + `IniciarExecucaoService` | inicia / bloqueia |
| `FR-002` | `IniciarExecucaoService` + `EstoquePort.baixarPecas` + eventos `ExecucaoIniciada`/`PecasBaixadas` | baixa + eventos |
| `FR-004` | `adapter/outbound/estoque/EstoqueAdapter` (ACL) | delega aos use cases do estoque |
| (integração) | `ItemOrcamento` (`itemEstoqueId`+`quantidade`) + `EstoquePort.reservar` (Boolean) | reserva real; indisponível → `OrdemServico.encerrar()` |

## Entry Point(s)
`POST /api/admin/ordens/{id}/execucao/iniciar` → `200 { status }`. Erros: `404`, `409` (fora de `Em execução`).

## Component Plan
- Domain: `OrdemServico.iniciarExecucao()`, `OrdemServico.encerrar()`; `ItemOrcamento` ganha `itemEstoqueId`/`quantidade` (validados p/ PEÇA); eventos `ExecucaoIniciada`, `PecasBaixadas`.
- Port in: `IniciarExecucaoUseCase`. Port out: `EstoquePort` (reservar→Boolean, liberarReservas, baixarPecas) por `osId`.
- Application: `IniciarExecucaoService` (`@Transactional`); `GerarOrcamentoService`/`RevisarOrcamentoService` reservam e **encerram** se indisponível.
- Adapter: `EstoqueAdapter` (ACL, `@Component`) — substitui o stub; `ExecucaoController`.
- Persistência: colunas `item_estoque_id`/`quantidade` em `item_orcamento` (migration `V11`).
- Build: `ordemservico` passa a depender de `:modules:estoque`.

## Data Model / Persistence
Migração **`V11`**: `item_orcamento` += `item_estoque_id BIGINT NULL`, `quantidade INT NULL`.

## Integrations & Observability
- **ACL síncrono** para o `estoque` (ADR-0006). Eventos `ExecucaoIniciada`/`PecasBaixadas`/`OsEncerrada`. HTTP 200/404/409.
- Discussão sobre migrar para integração por eventos: issue **#80**.

## Test Strategy
- **Unit:** `IniciarExecucaoServiceTest` (inicia/baixa/eventos; bloqueio; não encontrada); `GerarOrcamentoServiceTest` ganha caso "peça indisponível → encerra".
- **Integração (Testcontainers):** fluxo criar→diagnóstico→orçamento→aprovar→iniciar execução → `Em execução`.
- **HTTP (MockMvc + JWT):** `POST /execucao/iniciar` → 200; 409 sem aprovação; 401.
- **Cucumber:** UC09 (`@implementado`). Testes de integração/HTTP de orçamento usam **serviço** (reserva real coberta por unit).

## Quality / Lint Risks & Mitigation
- **Mudança de contrato (UC03/UC07) + migration** → aditivo; testes atualizados.
- **`EstoquePort` agora é ACL real** → domínio intacto; trocar p/ eventos (B) = trocar adapter.

## Alternatives Considered
- **Casar peça por nome (qtd 1):** descartado — frágil e silencioso; optou-se por referência explícita.
- **Integração por eventos (B):** adiada p/ decisão do time (#80).

## Risks / Open Questions
- Indisponível encerra a OS (política MVP do Event Storming).
- Idempotência do iniciar apoiada na baixa idempotente do estoque.
