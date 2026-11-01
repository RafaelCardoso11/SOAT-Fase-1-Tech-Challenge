# Tasks: Gerar Orçamento (UC03)

**Plan:** specs/gerar-orcamento/plan.md

## Domain

- [ ] T001 - Criar `domain/model/TipoItem` (SERVICO, PECA)
- [ ] T002 - Criar `domain/model/ItemOrcamento` (tipo, descricao, valor: `Dinheiro`)
- [ ] T003 - Criar `domain/model/Orcamento` (itens; `total` = soma dos itens via `Dinheiro.plus`; exige ≥ 1 item)
- [ ] T004 - Criar evento `domain/event/OrcamentoGerado` (`: DomainEvent`, `ordemServicoId`)
- [ ] T005 - Em `domain/model/OrdemServico`: `orcamento: Orcamento?`; `gerarOrcamento(itens): Orcamento` (guarda `EM_DIAGNOSTICO` + `liberadoParaOrcamento`; status→`AGUARDANDO_APROVACAO`); `reconstituir(... orcamento = null)`
- [ ] T006 - Criar port in `domain/port/inbound/GerarOrcamentoUseCase` (`gerar(id, itens): OrdemServico`)
- [ ] T007 - Criar ports out `domain/port/outbound/{EstoquePort,NotificacaoPort}`

## Application

- [ ] T008 - Criar `application/usecase/GerarOrcamentoService` (`@Transactional`; gera → salva → reserva peças → publica `OrcamentoGerado` → notifica)

## Data / persistence

- [ ] T009 - Migration `bootstrap/.../db/migration/V7__create_orcamento.sql` (tabelas `orcamento`, `item_orcamento` + coluna `orcamento_id` em `ordem_servico`)
- [ ] T010 - Criar `adapter/outbound/persistence/{OrcamentoJpaEntity,ItemOrcamentoJpaEntity}`; em `OrdemServicoJpaEntity` `@OneToOne orcamento`
- [ ] T011 - Em `OrdemServicoRepositoryAdapter`: `salvar` persiste o orçamento (cria no update quando ausente); `toDomain` reconstitui o orçamento

## Adapter (inbound/web + stubs + config)

- [ ] T012 - Criar `adapter/inbound/web/OrcamentoController` (POST `/api/admin/ordens/{id}/orcamento`) + DTOs (request itens / response status+total)
- [ ] T013 - Criar stubs `adapter/outbound/estoque/EstoquePortStub` e `adapter/outbound/notificacao/NotificacaoPortStub` (`@Component`, no-op, `shortcut:`)
- [ ] T014 - Registrar bean de `GerarOrcamentoService` em `OrdemServicoBeanConfig`

## Tests

- [ ] T015 [P] - Unit `GerarOrcamentoServiceTest` (total; status→aprovação; evento; estoque/notificação chamados; bloqueios; não encontrada)
- [ ] T016 - Integração (Testcontainers): OS → diagnóstico → gerar orçamento → recarregar (total/itens/status)
- [ ] T017 - HTTP (MockMvc + JWT) `GerarOrcamentoHttpIntegrationTest`: 200; 409 sem diagnóstico finalizado; 404; 401
- [ ] T018 - BDD: `uc03_gerar_orcamento.feature` com `@implementado` + steps `bootstrap/.../bdd/GerarOrcamentoSteps.kt`

## Finalize

- [ ] T019 - Rodar `./gradlew build` (gate Jacoco ≥ 80% em domain+application) e corrigir falhas
- [ ] T020 - Verificar que UC01/UC02/UC12 continuam verdes (mudanças em `OrdemServico`/persistência)
