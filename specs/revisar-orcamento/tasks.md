# Tasks: Revisar Orçamento (UC07)

**Plan:** specs/revisar-orcamento/plan.md

## Domain

- [ ] T001 - Em `domain/model/OrdemServico`: `revisarOrcamento(itens, validoAte)` (guarda `AGUARDANDO_APROVACAO`; substitui `orcamento`; sem transição de status)
- [ ] T002 - Criar evento `domain/event/OrcamentoRevisado` (`: DomainEvent`, `ordemServicoId`)
- [ ] T003 - Criar port `domain/port/inbound/RevisarOrcamentoUseCase` (`revisar(id, itens): OrdemServico`)

## Application

- [ ] T004 - Criar `application/usecase/RevisarOrcamentoService` (`@Transactional`; `validadeDias`; calcula `validoAte`; revisa → salva → reserva peças → publica `OrcamentoRevisado` → reenvia via `notificarOrcamentoGerado`)

## Persistence

- [ ] T005 - Em `OrdemServicoRepositoryAdapter.salvar`: atualizar o orçamento existente (total/validoAte/aprovadoEm/motivoRejeicao sempre; substituir itens quando diferem dos persistidos)

## Adapter (web + config)

- [ ] T006 - Criar `adapter/inbound/web/RevisarOrcamentoController` (POST `/api/admin/ordens/{id}/orcamento/revisar`, reusa `GerarOrcamentoRequest`) → `{ status, total }`
- [ ] T007 - Em `OrdemServicoBeanConfig`: bean `RevisarOrcamentoService` (+ `@Value("\${orcamento.validade-dias}")`)

## Tests

- [ ] T008 [P] - Unit `RevisarOrcamentoServiceTest` (ajustar valor; trocar peça → reserva; fora de aguardando; não encontrada)
- [ ] T009 - Integração (Testcontainers): revisar → novos itens/total persistidos, status `AGUARDANDO_APROVACAO`
- [ ] T010 - HTTP (MockMvc + JWT): `POST /orcamento/revisar` → 200; 409 fora de aguardando; 404; 401
- [ ] T011 - BDD: `uc07_revisar_orcamento.feature` — cenários 1/2/4 com `@implementado` (cenário "resposta pendente" sem tag) + steps `bootstrap/.../bdd/RevisarOrcamentoSteps.kt`

## Finalize

- [ ] T012 - `./gradlew build` (gate Jacoco ≥ 80%) verde
- [ ] T013 - Verificar UCs anteriores verdes (mudança no `salvar` do orçamento)
