# Tasks: Iniciar Execução + Integração Estoque (UC09 / #39)

**Plan:** specs/iniciar-execucao/plan.md

## Integração estoque (ACL)

- [x] T001 - `ordemservico` depende de `:modules:estoque`; `EstoquePort` chaveado por `osId` (reservar→Boolean, liberarReservas, baixarPecas)
- [x] T002 - `EstoqueAdapter` (ACL, `@Component`) delega aos use cases do estoque; remove `EstoquePortStub`
- [x] T003 - `ItemOrcamento` ganha `itemEstoqueId`+`quantidade` (validados p/ PEÇA); contrato UC03/UC07 + `item_orcamento` (migration `V11`)
- [x] T004 - `GerarOrcamentoService`/`RevisarOrcamentoService`: reservam; se indisponível → `OrdemServico.encerrar()` (Finalizada) + liberar + `OsEncerrada`
- [x] T005 - Ajustar os serviços de rejeitar/expirar para `liberarReservas(osId)`

## UC09

- [x] T006 - `OrdemServico.iniciarExecucao()` (guarda `EM_EXECUCAO`); eventos `ExecucaoIniciada`/`PecasBaixadas`
- [x] T007 - Port `IniciarExecucaoUseCase`; `IniciarExecucaoService` (`@Transactional`; baixa + eventos)
- [x] T008 - `ExecucaoController` (`POST /api/admin/ordens/{id}/execucao/iniciar`); bean no `OrdemServicoBeanConfig`

## Tests

- [x] T009 - Unit `IniciarExecucaoServiceTest`; `GerarOrcamentoServiceTest` caso "peça indisponível → encerra"
- [x] T010 - Atualizar testes/BDD afetados pela nova assinatura do `EstoquePort` e pelo `itemEstoqueId`
- [x] T011 - Integração (Testcontainers) + HTTP (MockMvc) do UC09
- [x] T012 - BDD `uc09_iniciar_execucao.feature` `@implementado` + steps

## Finalize

- [ ] T013 - `./gradlew build` (gate Jacoco ≥ 80%) verde
- [ ] T014 - Issue #80 aberta (discussão ACL vs eventos)
