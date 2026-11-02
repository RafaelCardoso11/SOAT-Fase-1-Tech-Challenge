# Technical Plan: Revisar Orçamento (UC07)

**Spec Ref:** `specs/revisar-orcamento/spec.md`
**Status:** Draft

## Summary
`OrdemServico.revisarOrcamento(itens, validoAte)` valida status `Aguardando aprovação` e **substitui** o
orçamento por uma nova proposta (novos itens, total recalculado, validade renovada, aprovação/rejeição
zeradas), **sem** mudar o status. O serviço reserva as peças da nova proposta (`EstoquePort`, stub), publica
`OrcamentoRevisado` e reenvia ao cliente (`NotificacaoPort.notificarOrcamentoGerado`). A validade reusa o
`orcamento.validade-dias` (como no UC03). Escrita sob `@Transactional`.

## Requirement Mapping
| Spec Ref | Componente / Camada | Nota |
|---|---|---|
| `FR-001`/`FR-005` | `domain/model/OrdemServico.revisarOrcamento` (guarda de status) | bloqueia fora de `AGUARDANDO_APROVACAO` |
| `FR-002` | `OrdemServico.revisarOrcamento` (novo `Orcamento`) + `GerarOrcamentoService`-like cálculo de `validoAte` | substitui itens + renova validade |
| `FR-003` | evento `domain/event/OrcamentoRevisado` + `NotificacaoPort.notificarOrcamentoGerado` (reenvio) | status continua `Aguardando aprovação` |
| `FR-004` | `EstoquePort.reservar` (stub) para as peças da nova proposta | reserva |

## Entry Point(s)
Rota administrativa (JWT): `POST /api/admin/ordens/{id}/orcamento/revisar`, body `{ itens: [{ tipo, descricao, valor }] }`
→ `200 { status, total }`. Erros: `404` inexistente, `409` fora de `Aguardando aprovação`, `400` itens inválidos.

## Component Plan (`modules/ordemservico/.../`)
| Camada | Ação | Caminho | Notas |
|---|---|---|---|
| Domain (model) | Modificar | `domain/model/OrdemServico` | `revisarOrcamento(itens, validoAte)` (guarda; novo `Orcamento`; sem transição) |
| Domain (event) | Criar | `domain/event/OrcamentoRevisado` | `: DomainEvent`, `ordemServicoId` |
| Domain (port in) | Criar | `domain/port/inbound/RevisarOrcamentoUseCase` | `revisar(id, itens): OrdemServico` |
| Application | Criar | `application/usecase/RevisarOrcamentoService` | `@Transactional`; `validadeDias`; revisa → salva → reserva peças → `OrcamentoRevisado` → reenvia |
| Adapter (in/web) | Criar | `adapter/inbound/web/RevisarOrcamentoController` | reusa `GerarOrcamentoRequest` |
| Adapter (out/persist) | Modificar | `OrdemServicoRepositoryAdapter.salvar` | sincroniza o orçamento existente (escalares sempre; itens quando mudam) |
| Adapter (config) | Modificar | `OrdemServicoBeanConfig` | bean `RevisarOrcamentoService` (+ `@Value validade-dias`) |

## Data Model / Persistence
Sem migração. `salvar` passa a **atualizar** o orçamento existente (total/validoAte/aprovadoEm/motivoRejeicao
sempre; itens substituídos quando diferentes — detecta a revisão sem versionamento).

## Integrations & Observability
- **Portas (stub):** `EstoquePort.reservar`, `NotificacaoPort.notificarOrcamentoGerado` (reenvio).
- **Eventos:** `OrcamentoRevisado`. HTTP 200/400/404/409.

## Test Strategy
- **Unit (MockK):** `RevisarOrcamentoServiceTest` — revisar ajustando valor (novo total, status continua, evento, reenvio); revisar trocando peça (reserva solicitada); fora de `AGUARDANDO_APROVACAO`; OS inexistente.
- **Integração (Testcontainers):** criar→diagnóstico→orçamento→revisar → recarregar (novos itens/total, status `AGUARDANDO_APROVACAO`).
- **HTTP (MockMvc + JWT):** `POST /orcamento/revisar` → 200; 409 fora de aguardando; 404; 401.
- **Cucumber:** cenários 1/2/4 do UC07 marcados `@implementado` (cenário "resposta pendente" fica **sem** a tag). Fraseado próprio.

## Quality / Lint Risks & Mitigation
- **`salvar` passa a substituir itens do orçamento** → só quando diferem dos persistidos (evita churn em aprovar/rejeitar/expirar, que não mudam itens).
- **Reset de aprovação/rejeição na revisão** → o novo `Orcamento` nasce com `aprovadoEm=null`/`motivoRejeicao=null`; `salvar` propaga.

## Alternatives Considered
- **Versionar orçamentos (histórico):** descartado no MVP — a revisão substitui a proposta atual.
- **Novo método de notificação "revisado":** descartado — reusa `notificarOrcamentoGerado` (reenvio da proposta ao cliente).

## Risks / Open Questions
- "Resposta do cliente pendente" descopada (spec Open Questions).
- Reserva/notificação são stub.
