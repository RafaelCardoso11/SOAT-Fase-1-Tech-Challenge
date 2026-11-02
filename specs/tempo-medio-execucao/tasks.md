# Tasks: Tempo Médio de Execução (UC13)

**Plan:** specs/tempo-medio-execucao/plan.md

## Domain / modelagem

- [ ] T001 - `OrdemServico` += `execucaoIniciadaEm` (set no `iniciarExecucao`) e `execucaoFinalizadaEm` (set no `finalizarExecucao`)
- [ ] T002 - Port `domain/port/inbound/ConsultarTempoMedioExecucaoUseCase` (`consultar(): Duration?`)
- [ ] T003 - `OrdemServicoRepository` += `buscarComExecucaoConcluida()`

## Application

- [ ] T004 - `application/usecase/ConsultarTempoMedioExecucaoService` (média das OS com início e fim; null se vazio)

## Persistence

- [ ] T005 - Migration `V12__ordem_servico_execucao_tempos.sql` (colunas); `OrdemServicoJpaEntity` + query `findByExecucaoIniciadaEmIsNotNullAndExecucaoFinalizadaEmIsNotNull`; adapter mapeia + `IniciarExecucaoService` salva

## Adapter (web + config)

- [ ] T006 - `RelatorioController` (`GET /api/admin/relatorios/tempo-medio-execucao`) + `TempoMedioExecucaoResponse`
- [ ] T007 - Bean do use case em `OrdemServicoBeanConfig`

## Tests

- [ ] T008 - Unit `ConsultarTempoMedioExecucaoServiceTest` (média; ignora incompleta; sem dados)
- [ ] T009 - Integração (Testcontainers) + HTTP (MockMvc) do UC13
- [ ] T010 - BDD `uc13_tempo_medio_execucao.feature` `@implementado` + steps

## Finalize

- [ ] T011 - `./gradlew build` (gate Jacoco ≥ 80%) verde
- [ ] T012 - Verificar UC09/UC10 verdes (iniciar/finalizar agora gravam instantes)
