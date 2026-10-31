# Tasks: Consultar Status da OS (UC12)

**Plan:** specs/consultar-status/plan.md

## Domain

- [x] T001 - Criar `TransicaoStatus` (VO: `status: StatusOS`, `momento: Instant`) em `modules/ordemservico/.../domain/model/`
- [x] T002 - Em `domain/model/OrdemServico`: adicionar `id: Long?` + `transicoes: List<TransicaoStatus>`; `criar(...)` registra transição inicial `RECEBIDA`; adicionar factory `reconstituir(...)` para carga
- [x] T003 - Criar port de entrada `domain/port/inbound/ConsultarOrdensUseCase` (`porNumero`, `porPlaca`, `porDocumento`)
- [x] T004 - Estender `domain/port/outbound/OrdemServicoRepository` com `buscarPorId`, `buscarPorPlaca` (lista), `buscarPorDocumento` (lista)

## Application

- [x] T005 - Criar `application/usecase/ConsultarOrdensService` (orquestra as três consultas; lista vazia quando nada encontrado)

## Data / persistence

- [x] T006 - Criar `adapter/outbound/persistence/TransicaoStatusJpaEntity` + mapeamento 1-N em `OrdemServicoJpaEntity`
- [x] T007 - Migration `bootstrap/.../db/migration/V4__create_transicao_status.sql` (tabela `ordemservico.transicao_status`: id, ordem_servico_id FK, status, momento)
- [x] T008 - Em `OrdemServicoRepositoryAdapter`: `salvar` persiste as transições; implementar `buscarPorId/Placa/Documento` reconstituindo a OS com `reconstituir(...)` (transições ordenadas por `momento`)

## Adapter (inbound/web)

- [x] T009 - Criar `adapter/inbound/web/ConsultarOrdensController` (GET `/api/public/ordens/{numero}`, `?placa=`, `?documento=`) + DTOs de resposta (status + linha do tempo + detalhes) em `adapter/inbound/web/dto/`
- [x] T010 - Registrar bean de `ConsultarOrdensService` em `adapter/config/OrdemServicoBeanConfig`

## Tests

- [x] T011 [P] - Unit `ConsultarOrdensServiceTest` (por número/placa/documento; lista vazia) com MockK
- [x] T012 [P] - Unit: `OrdemServico.criar` registra transição `RECEBIDA` (ampliar `CriarOrdemServicoServiceTest` ou teste de domínio)
- [x] T013 - Integração (Testcontainers): salvar OS com transição e consultar por número/placa/documento → lista com timeline; identificador sem OS → vazio
- [x] T014 - BDD: `docs/bdd/uc12_consultar_status.feature` (tag `@implementado`) + step defs `bootstrap/.../bdd/ConsultarStatusSteps.kt` dirigindo o use case

## Finalize

- [x] T015 - Rodar `./gradlew build` (inclui gate Jacoco ≥ 80% em domain+application) e corrigir falhas
- [x] T016 - Verificar que os testes existentes do UC01 continuam verdes (retrofit da transição inicial)
