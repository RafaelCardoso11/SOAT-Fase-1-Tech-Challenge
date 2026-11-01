# Tasks: Expirar Orçamento (UC08)

**Plan:** specs/expirar-orcamento/plan.md

## Domain

- [ ] T001 - Em `domain/model/OrdemServico`: `expirarOrcamento(agora)` (guarda `AGUARDANDO_APROVACAO` + `agora > validoAte`; status→`FINALIZADA`)
- [ ] T002 - Criar evento `domain/event/OrcamentoExpirado` (`: DomainEvent`, `ordemServicoId`)
- [ ] T003 - Criar port `domain/port/inbound/ExpirarOrcamentosUseCase` (`executar(): Int`)
- [ ] T004 - Em `domain/port/outbound/OrdemServicoRepository`: += `buscarExpiraveis(agora): List<OrdemServico>`; em `NotificacaoPort`: += `notificarOrcamentoExpirado(id)`

## Application

- [ ] T005 - Criar `application/usecase/ExpirarOrcamentosService` (`@Transactional`; itera `buscarExpiraveis` → `expirarOrcamento` → salva → libera reservas → publica `OrcamentoExpirado`/`OsEncerrada`/`ReservaLiberada` → notifica; retorna contagem)

## Persistence

- [ ] T006 - Em `OrdemServicoSpringDataRepository`: `findByStatusAndOrcamento_ValidoAteBefore(status, momento)`; em `OrdemServicoRepositoryAdapter`: `buscarExpiraveis(agora)`

## Adapter (in + stubs + config + bootstrap)

- [ ] T007 - Criar `adapter/inbound/scheduler/ExpiracaoScheduler` (`@Scheduled(cron = ${orcamento.expiracao-cron:0 0 * * * *})`) e `adapter/inbound/web/ExpiracaoController` (`POST /api/admin/orcamentos/expirar` → `{ expirados }`)
- [ ] T008 - Implementar `notificarOrcamentoExpirado` em `NotificacaoPortStub` e `NotificacaoPorEmail`
- [ ] T009 - Em `OrdemServicoBeanConfig`: bean `ExpirarOrcamentosService`; em `OficinaApplication`: `@EnableScheduling`

## Tests

- [ ] T010 [P] - Unit `ExpirarOrcamentosServiceTest` (1 vencida → expira + eventos + libera + notifica + retorna 1; vazio → 0) + domínio `OrdemServico.expirarOrcamento` (vencido/ dentro do prazo / fora de aguardando)
- [ ] T011 - Integração (Testcontainers, `orcamento.validade-dias=-1`): gerar orçamento vencido → `executar()` → `Finalizada`
- [ ] T012 - HTTP (MockMvc + JWT): `POST /api/admin/orcamentos/expirar` → 200; 401 sem token
- [ ] T013 - BDD: `uc08_expirar_orcamento.feature` `@implementado` + steps `bootstrap/.../bdd/ExpirarOrcamentoSteps.kt` (fraseado próprio)

## Finalize

- [ ] T014 - `./gradlew build` (gate Jacoco ≥ 80%) verde
- [ ] T015 - Verificar UCs anteriores verdes (mudanças em portas)
