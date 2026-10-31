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
Candidatas a follow-up (issues próprias), **não** corrigidas neste PR de documentação:
1. **Evento publicado mas não persistido** — não há subscriber nem outbox; "registrado" fica parcial (o evento não sobrevive). → outbox/event log quando houver consumidor.
2. **Sem `@Transactional` explícito** — hoje confia no commit por chamada do Spring Data; o enunciado pede tratamento de transações. Definir o boundary (ex.: no adapter/aplicação).
3. **`marca`/`modelo`/`ano` sem validação de presença** (aceita vazio/0) — o enunciado lista esses campos no cadastro do veículo.
4. **Sem teste HTTP e2e** do endpoint (`controller`/`handler` não cobertos por teste automatizado; só unit + persistência + Cucumber via use case).
