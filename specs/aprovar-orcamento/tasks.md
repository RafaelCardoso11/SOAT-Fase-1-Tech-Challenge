# Tasks: Aprovar Orçamento (UC05)

**Plan:** specs/aprovar-orcamento/plan.md

## Domain

- [ ] T001 - Em `domain/model/Orcamento`: `validoAte: Instant` + `aprovadoEm: Instant?` (private set); `aprovar(momento)` (check `momento <= validoAte` → `IllegalStateException("Orçamento expirado")`); remover constante de dias do domínio
- [ ] T002 - Em `domain/model/OrdemServico`: `gerarOrcamento(itens, validoAte)` (recebe a validade); `aprovarOrcamento()` (guarda `AGUARDANDO_APROVACAO`; `orcamento.aprovar(now)`; status→`EM_EXECUCAO`); `reconstituir` já carrega o orçamento
- [ ] T003 - Criar evento `domain/event/OrcamentoAprovado` (`: DomainEvent`, `ordemServicoId`)
- [ ] T004 - Criar port `domain/port/inbound/AprovarOrcamentoUseCase` (`aprovar(id)`)

## Application

- [ ] T005 - Criar `application/usecase/AprovarOrcamentoService` (`@Transactional`; carrega → `aprovarOrcamento` → salva → publica `OrcamentoAprovado`)
- [ ] T006 - Em `GerarOrcamentoService`: receber `validadeDias`; calcular `validoAte = now + dias`; chamar `os.gerarOrcamento(itens, validoAte)`

## Data / persistence

- [ ] T007 - Migration `bootstrap/.../db/migration/V8__orcamento_validade.sql` (colunas `valido_ate`, `aprovado_em`)
- [ ] T008 - Em `OrcamentoJpaEntity`: colunas `validoAte` + `aprovadoEm`; em `OrdemServicoRepositoryAdapter` `salvar` sincroniza `aprovadoEm` (orçamento já existente) e `toJpa`/`toDomain` mapeiam validade/aprovação

## Adapter (web + config)

- [ ] T009 - Criar `adapter/inbound/web/AprovarOrcamentoController` (POST `/api/admin/ordens/{id}/orcamento/aprovar`) + response `{ status }`
- [ ] T010 - Em `OrdemServicoBeanConfig`: bean `AprovarOrcamentoService`; injetar `@Value("\${orcamento.validade-dias}")` no `GerarOrcamentoService`
- [ ] T011 - `application.yml`: `orcamento.validade-dias: ${ORCAMENTO_VALIDADE_DIAS:7}`; `.env.example`: `ORCAMENTO_VALIDADE_DIAS=7`

## Tests

- [ ] T012 [P] - Unit `AprovarOrcamentoServiceTest` (aprovar ok; expirado; fora de aguardando; não encontrada) + `Orcamento.aprovar` (válido/expirado)
- [ ] T013 - Atualizar testes/BDD do UC03 ao novo `GerarOrcamentoService`/`Orcamento` (construção com validade)
- [ ] T014 - Integração (Testcontainers): fluxo até aprovar (`EM_EXECUCAO` + `aprovadoEm`); orçamento expirado → bloqueio
- [ ] T015 - HTTP (MockMvc + JWT): `POST /orcamento/aprovar` → 200; 409 fora de aguardando; 404; 401
- [ ] T016 - BDD: `uc05_aprovar_orcamento.feature` `@implementado` + steps `bootstrap/.../bdd/AprovarOrcamentoSteps.kt` (fraseado próprio)

## Finalize

- [ ] T017 - `./gradlew build` (gate Jacoco ≥ 80%) verde
- [ ] T018 - Verificar UC01/UC02/UC03/UC12 verdes (retrofit do `Orcamento`)
