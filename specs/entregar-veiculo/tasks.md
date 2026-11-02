# Tasks: Entregar Veículo (UC11)

**Plan:** specs/entregar-veiculo/plan.md

## Domain

- [ ] T001 - `OrdemServico.entregar()` (guarda `FINALIZADA`; status→`ENTREGUE`)
- [ ] T002 - Evento `domain/event/VeiculoEntregue`
- [ ] T003 - Port `domain/port/inbound/EntregarVeiculoUseCase` (`entregar(id): OrdemServico`)

## Application

- [ ] T004 - `application/usecase/EntregarVeiculoService` (`@Transactional`; entrega → salva → `VeiculoEntregue` → `OsEncerrada`)

## Adapter

- [ ] T005 - `EntregaController` (`POST /api/admin/ordens/{id}/entrega`) + `ComprovanteResponse`
- [ ] T006 - Bean `EntregarVeiculoService` em `OrdemServicoBeanConfig`

## Tests

- [ ] T007 - Unit `EntregarVeiculoServiceTest` (entrega; eventos na ordem; fora de finalizada; não encontrada)
- [ ] T008 - Integração (Testcontainers) + HTTP (MockMvc) do UC11
- [ ] T009 - BDD `uc11_entregar_veiculo.feature` `@implementado` + steps

## Finalize

- [ ] T010 - `./gradlew build` (gate Jacoco ≥ 80%) verde
