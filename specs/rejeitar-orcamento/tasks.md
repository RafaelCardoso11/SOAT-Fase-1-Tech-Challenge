# Tasks: Rejeitar Orçamento (UC06)

**Plan:** specs/rejeitar-orcamento/plan.md

## Domain

- [ ] T001 - Em `domain/model/Orcamento`: `motivoRejeicao: String?` (private set) + `rejeitar(motivo)`
- [ ] T002 - Em `domain/model/OrdemServico`: `rejeitarOrcamento(motivo)` (guarda `AGUARDANDO_APROVACAO`; `orcamento.rejeitar`; status→`FINALIZADA`)
- [ ] T003 - Criar eventos `domain/event/{OrcamentoRejeitado,ReservaLiberada}` (`: DomainEvent`, `ordemServicoId`)
- [ ] T004 - Criar port `domain/port/inbound/RejeitarOrcamentoUseCase` (`rejeitar(id, motivo)`)
- [ ] T005 - Em `domain/port/outbound/EstoquePort`: += `liberarReserva(descricaoPeca)`; em `NotificacaoPort`: += `notificarOrcamentoRejeitado(id, motivo)`

## Application

- [ ] T006 - Criar `application/usecase/RejeitarOrcamentoService` (`@Transactional`; rejeita → salva → libera reservas das peças → publica `OrcamentoRejeitado`/`OsEncerrada`/`ReservaLiberada` → notifica)

## Data / persistence

- [ ] T007 - Migration `bootstrap/.../db/migration/V9__orcamento_rejeicao.sql` (coluna `motivo_rejeicao`)
- [ ] T008 - Em `OrcamentoJpaEntity`: coluna `motivoRejeicao`; em `OrdemServicoRepositoryAdapter` `salvar` sincroniza `motivoRejeicao` e `toJpa`/`toDomain` mapeiam

## Adapter (web + stubs + config)

- [ ] T009 - Criar `adapter/inbound/web/RejeitarOrcamentoController` (POST `/api/admin/ordens/{id}/orcamento/rejeitar`, body `{ motivo? }`) + response `{ status }`
- [ ] T010 - Implementar os novos métodos em `EstoquePortStub`, `NotificacaoPortStub` e `NotificacaoPorEmail`
- [ ] T011 - Em `OrdemServicoBeanConfig`: bean `RejeitarOrcamentoService`

## Tests

- [ ] T012 [P] - Unit `RejeitarOrcamentoServiceTest` (com motivo; sem motivo; fora de aguardando; não encontrada; `liberarReserva`/notificação acionados)
- [ ] T013 - Integração (Testcontainers): fluxo até rejeitar (`FINALIZADA` + `motivoRejeicao`)
- [ ] T014 - HTTP (MockMvc + JWT): `POST /orcamento/rejeitar` → 200; 409 fora de aguardando; 404; 401
- [ ] T015 - BDD: `uc06_rejeitar_orcamento.feature` `@implementado` + steps `bootstrap/.../bdd/RejeitarOrcamentoSteps.kt` (fraseado próprio)

## Finalize

- [ ] T016 - `./gradlew build` (gate Jacoco ≥ 80%) verde
- [ ] T017 - Verificar UCs anteriores verdes (mudanças em portas/`Orcamento`)
