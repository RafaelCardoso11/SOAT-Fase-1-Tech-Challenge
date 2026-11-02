# Linguagem Ubíqua — Oficina Mecânica

Vocabulário comum entre o negócio e a equipe, usado igual nas conversas, no Event Storming, nas specs e no
código. Exportado do glossário do board **Tech01** no Miro e ligado às classes da branch `main`.

Convenção no código: termos do domínio em português, sem acento (`Orcamento`, `Peca`); eventos no passado
(`OrcamentoAprovado`); comandos e casos de uso no imperativo (`AprovarOrcamentoUseCase`).

## Glossário do domínio

### Ordem de Serviço e seu ciclo

| Termo | Definição | No código |
|-------|-----------|-----------|
| Ordem de Serviço (OS) | Registro que acompanha o veículo do início ao fim do atendimento, agrupando diagnóstico, orçamento, execução e entrega sob um único status. | `ordemservico.domain.model.OrdemServico` (raiz do agregado) |
| Número da OS | Identificador que o cliente usa para consultar a OS. | `OrdemServico.id`, `GET /api/public/ordens/{numero}` |
| Relato | Problema descrito pelo cliente na abertura da OS. | `OrdemServico.relato` |
| Status da OS | Estado atual da OS no fluxo. Só seis valores; qualquer outra transição é recusada. | `StatusOS` |
| Recebida | Status inicial, após a criação na recepção. | `StatusOS.RECEBIDA` |
| Em diagnóstico | O mecânico está inspecionando o veículo. | `StatusOS.EM_DIAGNOSTICO` |
| Aguardando aprovação | Orçamento gerado, à espera da decisão do cliente. | `StatusOS.AGUARDANDO_APROVACAO` |
| Em execução | Orçamento aprovado; serviços em andamento. | `StatusOS.EM_EXECUCAO` |
| Finalizada | Serviços executados, veículo ainda na oficina. Também fecha a OS que sai do fluxo normal (peça não identificada ou indisponível, orçamento rejeitado ou expirado). | `StatusOS.FINALIZADA` |
| Entregue | Veículo devolvido ao cliente. | `StatusOS.ENTREGUE` |
| OS Encerrada | Fato de a OS sair do fluxo normal. Não é um status: a OS vai para Finalizada. | `OrdemServico.encerrar()`, evento `OsEncerrada` |
| Linha do tempo | Histórico das mudanças de status com data e hora, exibido na consulta. | `TransicaoStatus`, tabela `ordemservico.transicao_status` |
| Diagnóstico | Inspeção técnica feita pelo mecânico para identificar problemas, peças e serviços necessários. | `OrdemServico.iniciarDiagnostico/registrarInspecao/finalizarDiagnostico`, `RealizarDiagnosticoService` |
| Inspeção | Observações registradas durante o diagnóstico. | `OrdemServico.registrarInspecao(observacoes)` |
| Peça identificada | Resultado do diagnóstico que libera a OS para orçamento. Sem peça identificada, a OS é encerrada. | `finalizarDiagnostico(pecaIdentificada)` |
| Orçamento | Proposta com os valores de serviços e peças, baseada no diagnóstico. Precisa da aprovação do cliente para a execução. | `Orcamento` (entidade dentro do agregado OS) |
| Item do orçamento | Linha do orçamento: um serviço ou uma peça, com descrição e valor; a peça referencia o item de estoque e a quantidade. | `ItemOrcamento`, `TipoItem` (`SERVICO`, `PECA`) |
| Total do orçamento | Soma dos valores dos itens, calculada pelo domínio. | `Orcamento.total` |
| Validade do orçamento | Prazo para o cliente decidir: 7 dias por padrão. | `Orcamento.validoAte`, `ORCAMENTO_VALIDADE_DIAS` |
| Aprovação | Decisão do cliente de aceitar o orçamento. Leva a OS para Em execução. | `OrdemServico.aprovarOrcamento()`, `AprovarOrcamentoService` |
| Rejeição | Decisão do cliente de recusar o orçamento, com motivo opcional. Encerra a OS e libera as reservas. | `rejeitarOrcamento(motivo)`, `Orcamento.motivoRejeicao` |
| Revisão | Novo orçamento a pedido do cliente; refaz as reservas e reinicia a validade. | `revisarOrcamento(itens, validoAte)`, `RevisarOrcamentoService` |
| Orçamento Expirado | Orçamento sem resposta dentro da validade. Encerra a OS e libera as reservas. | `expirarOrcamento(agora)`, `ExpiracaoScheduler` |
| Execução | Realização dos serviços aprovados, com baixa das peças reservadas. | `iniciarExecucao()`, `finalizarExecucao(todosAplicados)` |
| Tempo médio de execução | Média entre o início e o fim da execução das OS concluídas. | `ConsultarTempoMedioExecucaoService`, `GET /api/admin/relatorios/tempo-medio-execucao` |
| Entrega | Devolução do veículo ao cliente com emissão de comprovante. Só com a OS Finalizada. | `OrdemServico.entregar()`, `EntregarVeiculoService` |
| Comprovante | Read model da entrega: número da OS, veículo, documento, status e data. | `ComprovanteResponse` |

### Pessoas

| Termo | Definição | No código |
|-------|-----------|-----------|
| Cliente | Proprietário do veículo. Decide sobre o orçamento, acompanha o status e recebe o veículo. | `cadastro.domain.model.Cliente`; na OS, pelo `Documento` |
| Atendente | Faz a interface com o cliente: abre a OS, faz cadastros, controla o estoque e entrega o veículo. | Usuário autenticado (`admin`) |
| Mecânico | Diagnostica, orça e executa os serviços. | Usuário autenticado (`mecanico`) |
| Usuário | Quem acessa as APIs administrativas com login e senha. | `autenticacao.domain.model.Usuario` |

### Cadastro

| Termo | Definição | No código |
|-------|-----------|-----------|
| CPF/CNPJ | Documento que identifica o cliente, validado pelos dígitos verificadores e único no cadastro. | VO `shared.domain.vo.Documento` |
| Veículo | Automóvel do cliente, identificado pela placa, com marca, modelo e ano. | `cadastro.domain.model.Veiculo`; na OS, VO `ordemservico.domain.model.Veiculo` |
| Placa | Identificação do veículo no padrão antigo ou Mercosul, validada e única. | VO `shared.domain.vo.Placa` |
| Serviço | Trabalho do catálogo da oficina, como troca de óleo ou alinhamento, com preço. | `cadastro.domain.model.Servico` |
| Inativar | Retirar um cadastro de uso sem apagá-lo; mantém o histórico. | `inativar()` em `Cliente`, `Veiculo`, `Servico`, `ItemEstoque` |
| Dinheiro | Valor monetário em reais, nunca negativo. | VO `shared.domain.vo.Dinheiro` |

### Estoque

| Termo | Definição | No código |
|-------|-----------|-----------|
| Peça | Componente físico usado no reparo. | `ItemEstoque` com `TipoItem.PECA` |
| Insumo | Material consumido no serviço e controlado em estoque, como óleo. | `ItemEstoque` com `TipoItem.INSUMO` |
| Item de estoque | Peça ou insumo com nome único, preço e saldo. | `estoque.domain.model.ItemEstoque` (raiz do agregado) |
| Saldo | Quantidade física em estoque. | `ItemEstoque.saldo` |
| Reservado | Parte do saldo separada para OS. | `ItemEstoque.reservado` |
| Disponível | Saldo menos reservado: o que pode ser prometido a uma nova OS. | `ItemEstoque.disponivel` |
| Entrada de estoque | Aumento do saldo por recebimento; única forma de o saldo subir. | `ItemEstoque.registrarEntrada(quantidade)` |
| Reserva de peça | Separar a peça para uma OS, garantindo a disponibilidade até a execução. Tudo ou nada. | `Reserva`, `ReservarPecaService`, evento `PecaReservada` |
| Peça Indisponível | Não há disponível suficiente para a reserva. | Evento `PecaIndisponivel` |
| Liberação | Devolver a reserva ao disponível quando a OS é encerrada sem execução. | `Reserva.liberar()`, evento `ReservaLiberada` |
| Baixa de peça | Retirada definitiva das peças reservadas do saldo, no início da execução. Distinta da reserva. | `ItemEstoque.baixar`, `Reserva.consumir()`, evento `PecasBaixadas` |
| Situação da reserva | Ativa, liberada ou consumida. | `SituacaoReserva` |
| Quantidade | Inteiro maior que zero. | VO `estoque.domain.model.Quantidade` |

### Integrações

| Termo | Definição | No código |
|-------|-----------|-----------|
| Notificação | Aviso ao cliente por e-mail: orçamento gerado, rejeitado, expirado e veículo pronto. | `NotificacaoPort`, `NotificacaoPorEmail` |
| Consulta pública | Acompanhamento da OS pelo cliente, sem login. | `ConsultarOrdensController`, `/api/public/ordens` |
| Token JWT | Credencial das APIs administrativas, obtida no login. | `TokenEmitido`, `JwtTokenProvider`, `POST /api/auth/login` |

## Termos de DDD usados no projeto

| Termo | Definição | Exemplo no projeto |
|-------|-----------|--------------------|
| Agregado | Cluster de objetos tratado como unidade de consistência, alterado só pela raiz. | `OrdemServico` (com `Orcamento`, `ItemOrcamento`, `TransicaoStatus`), `ItemEstoque`, `Reserva` |
| Entidade | Objeto com identidade própria que muda ao longo do tempo. | `Orcamento`, `Cliente`, `Reserva` |
| Objeto de valor | Objeto definido só pelos seus valores, imutável. | `Documento`, `Placa`, `Dinheiro`, `Quantidade`, `ItemOrcamento` |
| Evento de domínio | Fato relevante ocorrido no negócio, nomeado no passado. | `OrcamentoAprovado`, `PecaReservada` (implementam `DomainEvent`) |
| Comando | Pedido para que algo aconteça, no imperativo; pode ser recusado. | `AprovarOrcamentoUseCase`, `POST …/orcamento/aprovar` |
| Política | Regra reativa "quando X acontece, então Y". | Rejeitado ou expirado: liberar reservas e encerrar a OS |
| Read model | Modelo de leitura para consulta. | Status e linha do tempo da OS, Comprovante, Saldo de estoque |
| Bounded context | Fronteira em que cada termo tem um só significado. | Módulos `ordemservico`, `cadastro`, `estoque`, `autenticacao` |
| Subdomínio | Parte do negócio: principal, suporte ou genérico. | OS é principal; Cadastro e Estoque, suporte; Autenticação, genérico |
| Shared kernel | Modelo compartilhado entre contextos, mantido pequeno. | `shared-kernel`: `Documento`, `Placa`, `Dinheiro`, `DomainEvent` |
| Camada anticorrupção (ACL) | Tradução entre o modelo de um contexto e o de outro. | `EstoqueAdapter` traduz a `EstoquePort` da OS para os casos de uso do Estoque |
| Porta | Interface do domínio para entrada (caso de uso) ou saída (repositório, integração). | `domain/port/inbound`, `domain/port/outbound` |
| Hotspot | Dúvida ou risco levantado no workshop. | "Encerrada é status?" — resolvido em [`event-storming.md`](fontes/event-storming.md#hotspots-e-decisões) |

## Mesmo termo, contextos diferentes

| Termo | Em Ordem de Serviço | Em Cadastro | Em Estoque |
|-------|---------------------|-------------|------------|
| Veículo | VO com placa, marca, modelo e ano copiados na abertura da OS | Entidade com dono, ciclo de vida e inativação | — |
| Peça | Item do orçamento com valor e quantidade | — | Item de estoque com saldo e reservas |
| `TipoItem` | `SERVICO` ou `PECA` (linha do orçamento) | — | `PECA` ou `INSUMO` (natureza do item) |
| `PecasBaixadas`, `ReservaLiberada` | Eventos publicados pela OS ao iniciar a execução ou encerrar | — | Eventos publicados pelo Estoque ao mudar o saldo |

## Relação entre documentação e código

| Artefato de documentação | Onde vira código |
|--------------------------|------------------|
| Eventos do Event Storming | `modules/*/src/main/kotlin/**/domain/event/` |
| Comandos | `domain/port/inbound/*UseCase.kt` e `application/usecase/*Service.kt`; endpoints em `adapter/inbound/web/` |
| Políticas | Serviços de aplicação (`GerarOrcamentoService`, `RejeitarOrcamentoService`, `ExpirarOrcamentosService`, `IniciarExecucaoService`) e `ExpiracaoScheduler` |
| Agregados e regras de transição | `ordemservico/domain/model/OrdemServico.kt`, `estoque/domain/model/ItemEstoque.kt`, `Reserva.kt` |
| Objetos de valor | `shared-kernel/src/main/kotlin/**/vo/` e `domain/model/` de cada módulo |
| Bounded contexts | Um módulo Gradle por contexto em `modules/`, um schema por módulo nas migrations em `bootstrap/src/main/resources/db/migration/` |
| Casos de uso UC01–UC13 | Specs em `specs/<feature>/` e cenários BDD em `docs/bdd/*.feature`, executados pelo Cucumber |
