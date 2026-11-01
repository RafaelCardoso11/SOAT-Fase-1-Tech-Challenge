# Technical Plan: Aprovar Orçamento (UC05)

**Spec Ref:** `specs/aprovar-orcamento/spec.md`
**Status:** Draft

## Summary
O `Orcamento` passa a carregar **validade** (`validoAte`) e **data de aprovação** (`aprovadoEm`) — retrofit
do UC03, onde a validade é calculada na geração a partir de config (`orcamento.validade-dias`, default 7).
`OrdemServico.aprovarOrcamento()` valida status `Aguardando aprovação` + não-expirado, registra a aprovação
e transiciona para `Em execução` (status oficial; **nenhum status novo**). Escrita sob `@Transactional` +
lock otimista.

## Requirement Mapping
| Spec Ref | Componente / Camada | Nota |
|---|---|---|
| `FR-001`/`FR-005` | `domain/model/OrdemServico.aprovarOrcamento` (guarda de status) | bloqueia fora de `AGUARDANDO_APROVACAO` |
| `FR-002` | `domain/model/Orcamento.aprovar(momento)` (seta `aprovadoEm`) + evento `domain/event/OrcamentoAprovado` | data/hora + evento |
| `FR-003` | `OrdemServico.aprovarOrcamento` (status→`EM_EXECUCAO`) | transição |
| `FR-004` | `Orcamento.aprovar` (check `momento <= validoAte`) → `IllegalStateException("Orçamento expirado")` | bloqueio expirado |
| `FR-006` | `Orcamento.validoAte` + `application/usecase/GerarOrcamentoService` (calcula `validoAte` a partir de `orcamento.validade-dias`) | validade configurável |

## Entry Point(s)
Rota administrativa (JWT): `POST /api/admin/ordens/{id}/orcamento/aprovar` → `200 { status }`.
Erros: `404` OS inexistente, `409` fora de `Aguardando aprovação` **ou** orçamento expirado.

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Modificar | `domain/model/Orcamento` | `validoAte: Instant`; `aprovadoEm: Instant?` (private set); `aprovar(momento)` (check validade); construtor sem constante de dias |
| Domain (model) | Modificar | `domain/model/OrdemServico` | `aprovarOrcamento()`; `gerarOrcamento(itens, validoAte)` passa a receber a validade |
| Domain (event) | Criar | `domain/event/OrcamentoAprovado` | `: DomainEvent`, `ordemServicoId` |
| Domain (port in) | Criar | `domain/port/inbound/AprovarOrcamentoUseCase` | `aprovar(id)` |
| Application | Criar | `application/usecase/AprovarOrcamentoService` | `@Transactional`; carrega → `aprovarOrcamento` → salva → evento |
| Application | Modificar | `application/usecase/GerarOrcamentoService` | recebe `validadeDias`; calcula `validoAte = now + dias`; `os.gerarOrcamento(itens, validoAte)` |
| Adapter (in/web) | Criar | `adapter/inbound/web/AprovarOrcamentoController` | rota POST; resposta `{ status }` |
| Adapter (out/persist) | Modificar | `persistence/OrcamentoJpaEntity` + `OrdemServicoRepositoryAdapter` | colunas `valido_ate`/`aprovado_em`; `salvar` sincroniza `aprovadoEm`; `toDomain`/`toJpa` mapeiam validade/aprovação |
| Adapter (config) | Modificar | `adapter/config/OrdemServicoBeanConfig` | bean `AprovarOrcamentoService`; injeta `@Value orcamento.validade-dias` no `GerarOrcamentoService` |
| Config | Modificar | `bootstrap/.../application.yml` + `.env.example` | `orcamento.validade-dias` (default 7, env `ORCAMENTO_VALIDADE_DIAS`) |

## Data Model / Persistence
Migração **`V8`**: `ALTER TABLE ordemservico.orcamento ADD COLUMN valido_ate TIMESTAMP NOT NULL`,
`ADD COLUMN aprovado_em TIMESTAMP NULL`.

## Integrations & Observability
- **Eventos:** `OrcamentoAprovado` via `PublicadorDeEventos`. HTTP 200/404/409.
- Sem integração externa (liberar reserva no estoque fica para UC06/UC08).

## Test Strategy
- **Unit (MockK):** `AprovarOrcamentoServiceTest` — aprovar válido (status `EM_EXECUCAO`, `aprovadoEm` setado, evento); expirado → `IllegalStateException`, status inalterado; fora de `AGUARDANDO_APROVACAO`; OS inexistente. Domínio: `Orcamento.aprovar` (válido/expirado).
- **Integração (Testcontainers):** criar → diagnóstico → gerar orçamento → aprovar → recarregar (`EM_EXECUCAO`, `aprovadoEm`). Orçamento com validade no passado → aprovação falha.
- **HTTP (MockMvc + JWT):** `POST /orcamento/aprovar` → 200; 409 fora de aguardando; 404; 401.
- **Cucumber:** cenários do UC05 (`@implementado`), com fraseado próprio (evitar colisão de passos), dirigindo o service com mocks.

## Quality / Lint Risks & Mitigation
- **Retrofit do UC03** (`Orcamento`/`gerarOrcamento`/`GerarOrcamentoService` + testes/BDD do UC03) → aditivo; atualizar os pontos de construção de `Orcamento`.
- **`salvar` precisa sincronizar `aprovadoEm`** (orçamento já existe no update) → tratar além do caso "orçamento novo".
- **Validade configurável sem sujar o domínio** → `validoAte` calculado na aplicação e passado ao agregado.

## Alternatives Considered
- **Constante de dias no domínio:** descartado — o usuário pediu prazo **configurável** (env/config).
- **Status `Aguardando execução`:** descartado — não é status oficial; board e BDD dizem `Em execução`.

## Risks / Open Questions
- Rota de aprovação em `/api/admin` até existir acesso do cliente (spec Open Questions).
- Encerramento on expiração é UC08; aqui expirado apenas bloqueia.
