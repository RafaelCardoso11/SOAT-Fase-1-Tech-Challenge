# Tasks: Finalizar Execução (UC10)

**Plan:** specs/finalizar-execucao/plan.md

## Domain

- [ ] T001 - `OrdemServico.finalizarExecucao(todosAplicados)` (guarda `EM_EXECUCAO`; `check(todosAplicados)`; status→`FINALIZADA`)
- [ ] T002 - Evento `domain/event/ExecucaoFinalizada`
- [ ] T003 - Port `domain/port/inbound/FinalizarExecucaoUseCase` (`finalizar(id, todosAplicados)`); `NotificacaoPort` += `notificarVeiculoPronto(id)`

## Application

- [ ] T004 - `application/usecase/FinalizarExecucaoService` (`@Transactional`; finaliza → salva → `ExecucaoFinalizada` → notifica)

## Adapter

- [ ] T005 - `ExecucaoController` += `POST /finalizar` (body `{ todosAplicados }`) → `{ status }`
- [ ] T006 - `notificarVeiculoPronto` em `NotificacaoPortStub` e `NotificacaoPorEmail`; bean em `OrdemServicoBeanConfig`

## Tests

- [ ] T007 - Unit `FinalizarExecucaoServiceTest` (finaliza; pendente; fora de execução; não encontrada)
- [ ] T008 - Integração (Testcontainers) + HTTP (MockMvc) do UC10
- [ ] T009 - BDD `uc10_finalizar_execucao.feature` `@implementado` + steps

## Finalize

- [ ] T010 - `./gradlew build` (gate Jacoco ≥ 80%) verde
