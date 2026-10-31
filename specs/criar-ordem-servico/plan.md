# Technical Plan: Criar Ordem de Serviço (UC01)

**Spec (BDD):** `docs/bdd/uc01_criar_ordem_de_servico.feature` (o `.feature` é a especificação de comportamento)
**Status:** Implementado — backfill retroativo (PR #36)

## Summary
O atendente abre uma OS com documento do cliente (CPF/CNPJ), veículo (placa, marca, modelo, ano) e relato.
A OS nasce com status `RECEBIDA` e emite o evento `OrdemServicoCriada`. As validações de "obrigatório" vs
"inválido" (mensagens do BDD) usam os VOs do `shared-kernel` (`Documento`, `Placa`). Cliente e veículo são
**embutidos** na OS por ora (sem `CadastroPort` — ver Decisões).

## Requirement Mapping (BDD → componentes)

| Regra do BDD | Componente |
|---|---|
| Criar OS válida → status `Recebida` | `application/usecase/CriarOrdemServicoService` + `domain/model/OrdemServico.criar` |
| Evento "Ordem de serviço criada" | `domain/event/OrdemServicoCriada` + `domain/port/outbound/PublicadorDeEventos` |
| CPF/CNPJ obrigatório / inválido | `CriarOrdemServicoService` (presença) + `shared/domain/vo/Documento.of` (formato) |
| Placa obrigatória / inválida | `CriarOrdemServicoService` (presença) + `shared/domain/vo/Placa.of` (formato) |
| Confirmação da criação | `adapter/inbound/web/OrdemServicoController` (201 + id) |
| Mensagem de erro | `adapter/inbound/web/OrdemServicoExceptionHandler` (400 + mensagem) |

## Entry Point
`POST /api/admin/ordens` (protegido por JWT, via `SecurityConfig` do `bootstrap`). Corpo:
`{ documento, placa, marca, modelo, ano, relato }` → `201 { id, status }` | `400 { erro }` | `401` (sem token).

## Component Plan (`modules/ordemservico/.../`)
- `domain/model/`: `OrdemServico` (raiz), `Veiculo`, `StatusOS` (6 oficiais).
- `domain/event/OrdemServicoCriada`; `domain/port/inbound/CriarOrdemServicoUseCase` (+ comando); `domain/port/outbound/{OrdemServicoRepository, PublicadorDeEventos}`.
- `application/usecase/CriarOrdemServicoService`.
- `adapter/inbound/web/{OrdemServicoController, OrdemServicoExceptionHandler, dto/*}`.
- `adapter/outbound/persistence/{OrdemServicoJpaEntity, OrdemServicoSpringDataRepository, OrdemServicoRepositoryAdapter}`.
- `adapter/outbound/event/PublicadorDeEventosSpring`; `adapter/config/OrdemServicoBeanConfig`.

## Data Model / Persistence
Migração `bootstrap/.../db/migration/V3__create_ordem_servico.sql` → tabela `ordemservico.ordem_servico`
(`id`, `documento`, `placa`, `marca`, `modelo`, `ano`, `relato`, `status`).

## Integrations & Observability
- **Eventos:** `PublicadorDeEventos` → `ApplicationEventPublisher` (in-process).
- **Respostas:** `201` criação, `400` dados inválidos, `401` sem token.

## Test Strategy
- Unit (`CriarOrdemServicoServiceTest`): as 4 regras de validação + status + evento.
- Integração (`OrdemServicoPersistenceIntegrationTest`, Testcontainers): persistência/JPA + migration V3.
- Cucumber: os 5 cenários do UC01 (tag `@implementado`), dirigindo o use case com mocks.

## Decisões
- **Cliente/veículo embutidos na OS**; `CadastroPort` adiado para quando `cadastro`/UC12 existirem.
- Status apenas os 6 oficiais por ora (`DIAGNOSTICO_FINALIZADO` etc. entram com a decisão do modelo de status — issue #37).

## Lacunas identificadas (o backfill revelou)

**Resolvidas neste PR:**
- ✅ **`marca`/`modelo`/`ano` validados** (presença / ano positivo) no `CriarOrdemServicoService` + testes unitários.
- ✅ **Teste HTTP e2e** do endpoint (`CriarOrdemServicoHttpIntegrationTest`: 201 válido, 400 inválido, 401 sem token).

**Adiadas (padrões cross-cutting — decisão do grupo, afetam todos os UCs):**
- ⏳ **Evento publicado mas não persistido** — sem subscriber nem outbox; "registrado" fica parcial. Fazer quando houver consumidor (outbox/event log).
- ⏳ **`@Transactional`** — para o insert único do UC01 é inócuo; definir o boundary quando um UC tiver múltiplas escritas (ex.: UC09 reserva+baixa).
