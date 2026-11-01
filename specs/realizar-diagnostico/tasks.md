# Tasks: Realizar Diagnóstico (UC02)

**Plan:** specs/realizar-diagnostico/plan.md

## Domain

- [ ] T001 - Criar eventos `domain/event/{DiagnosticoIniciado,DiagnosticoRealizado,DiagnosticoFinalizado,OsEncerrada}` (`: DomainEvent`, com `ordemServicoId: Long`)
- [ ] T002 - Em `domain/model/OrdemServico`: campos `observacoes: String?` + `liberadoParaOrcamento: Boolean`; `reconstituir(...)` recebe os dois; métodos `iniciarDiagnostico()`, `registrarInspecao(observacoes)`, `finalizarDiagnostico(pecaIdentificada)` com guarda de status e append de `TransicaoStatus`
- [ ] T003 - Criar port `domain/port/inbound/RealizarDiagnosticoUseCase` (`iniciar`, `registrarInspecao`, `finalizar`)
- [ ] T004 - Em `domain/port/outbound/OrdemServicoRepository`: documentar que `salvar` atualiza quando `id != null` (assinatura inalterada; `buscarPorId` já existe)

## Application

- [ ] T005 - Criar `application/usecase/RealizarDiagnosticoService` (carrega por id → aplica método do agregado → salva → publica evento; OS inexistente → `NoSuchElementException`)

## Data / persistence

- [ ] T006 - Migration `bootstrap/.../db/migration/V6__ordem_servico_diagnostico.sql` (ALTER TABLE: `observacoes`, `liberado_para_orcamento`)
- [ ] T007 - Em `OrdemServicoJpaEntity`: colunas `observacoes` (nullable) + `liberadoParaOrcamento`
- [ ] T008 - Em `OrdemServicoRepositoryAdapter`: `salvar` com caminho de update (busca por id, atualiza escalares, anexa só transições novas); `toDomain` mapeia os novos campos

## Adapter (inbound/web)

- [ ] T009 - Criar `adapter/inbound/web/DiagnosticoController` (3 rotas `/api/admin/ordens/{id}/diagnostico/...`) + DTOs de request/response
- [ ] T010 - Em `OrdemServicoExceptionHandler`: `IllegalStateException`→409, `NoSuchElementException`→404
- [ ] T011 - Registrar bean de `RealizarDiagnosticoService` em `OrdemServicoBeanConfig`

## Tests

- [ ] T012 [P] - Unit `RealizarDiagnosticoServiceTest` (iniciar ok/bloqueio fora de `Recebida`; registrar inspeção; finalizar com peça = libera + mantém status; finalizar sem peça = `Finalizada`; eventos)
- [ ] T013 - Integração (Testcontainers): criar OS → iniciar diagnóstico → recarregar e conferir status `EM_DIAGNOSTICO` + transição extra (caminho de update)
- [ ] T014 - BDD: marcar `uc02_realizar_diagnostico.feature` com `@implementado` + step defs `bootstrap/.../bdd/RealizarDiagnosticoSteps.kt`

## Finalize

- [ ] T015 - Rodar `./gradlew build` (gate Jacoco ≥ 80% em domain+application) e corrigir falhas
- [ ] T016 - Verificar que os testes de UC01/UC12 continuam verdes (mudanças em `OrdemServico`/`salvar`)
