# Diagramas DDD — Oficina Mecânica

Diagramas do domínio e da solução, alinhados ao código da branch `main`. O Event Storming está em
[`event-storming.md`](event-storming.md) e a linguagem ubíqua em [`linguagem-ubiqua.md`](../linguagem-ubiqua.md).

## 1. Mapa de contexto

Mapa original do workshop:

![Mapa de contexto](mapa-de-contexto.jpg)

Relações implementadas. A OS é sempre o contexto cliente (downstream) e protege seu modelo com uma camada
anticorrupção (ACL) em cada porta de saída:

```mermaid
flowchart LR
    OS["Ordem de Serviço<br/>(principal)"]
    CAD["Cadastro<br/>(suporte)"]
    EST["Estoque<br/>(suporte)"]
    AUT["Autenticação<br/>(genérico)"]
    NOT["Servidor de E-mail<br/>(externo)"]

    AUT -- "JWT" --> OS
    AUT -. "JWT" .-> CAD
    AUT -. "JWT" .-> EST
    CAD -- "U → D · preço dos serviços<br/>ACL: CatalogoPort / CatalogoAdapter" --> OS
    EST -- "U → D · reserva, libera, baixa e preço das peças<br/>ACL: EstoquePort, CatalogoPort" --> OS
    NOT -- "U → D · avisa o cliente<br/>ACL: NotificacaoPort / NotificacaoPorEmail" --> OS

    classDef core fill:#FFB77B,stroke:#C97A3A,color:#000
    classDef suporte fill:#8AB6F9,stroke:#3F74C4,color:#000
    classDef generico fill:#d9d9d9,stroke:#888,color:#000
    classDef externo fill:#F59BE6,stroke:#B8509F,color:#000
    class OS core
    class CAD,EST suporte
    class AUT generico
    class NOT externo
```

Os contextos compartilham só o `shared-kernel` (`Documento`, `Placa`, `Dinheiro`, `DomainEvent`). No MVP a OS guarda documento, placa, marca, modelo e ano informados na abertura e não consulta o Cadastro nesse momento; o orçamento busca nome e preço pela `CatalogoPort` (ACL `CatalogoAdapter`).

## 2. Subdomínios e bounded contexts

| Bounded context | Módulo Gradle | Subdomínio | Schema no banco |
|-----------------|---------------|------------|-----------------|
| Ordem de Serviço | `modules/ordemservico` | Principal | `ordemservico` |
| Cadastro | `modules/cadastro` | Suporte | `cadastro` |
| Estoque | `modules/estoque` | Suporte | `estoque` |
| Autenticação | `modules/autenticacao` | Genérico | `autenticacao` |
| — | `shared-kernel` | Kernel compartilhado | — |

## 3. Agregados, entidades e objetos de valor

### Ordem de Serviço

```mermaid
classDiagram
    direction LR
    class OrdemServico {
        <<Aggregate Root>>
        Long id
        Documento documento
        StatusOS status
        String relato
        Instant execucaoIniciadaEm
        Instant execucaoFinalizadaEm
        iniciarDiagnostico()
        registrarInspecao(observacoes)
        finalizarDiagnostico(pecaIdentificada)
        gerarOrcamento(itens, validoAte)
        aprovarOrcamento()
        rejeitarOrcamento(motivo)
        revisarOrcamento(itens, validoAte)
        expirarOrcamento(agora)
        iniciarExecucao()
        finalizarExecucao(todosAplicados)
        entregar()
        encerrar()
    }
    class Orcamento {
        <<Entity>>
        Instant validoAte
        Instant aprovadoEm
        String motivoRejeicao
        Dinheiro total
    }
    class ItemOrcamento {
        <<Value Object>>
        TipoItem tipo
        String descricao
        Dinheiro valor
        Long itemEstoqueId
        Int quantidade
    }
    class Veiculo {
        <<Value Object>>
        Placa placa
        String marca
        String modelo
        Int ano
    }
    class TransicaoStatus {
        <<Value Object>>
        StatusOS status
        Instant momento
    }
    class StatusOS {
        <<enumeration>>
        RECEBIDA
        EM_DIAGNOSTICO
        AGUARDANDO_APROVACAO
        EM_EXECUCAO
        FINALIZADA
        ENTREGUE
    }
    OrdemServico "1" *-- "1" Veiculo
    OrdemServico "1" *-- "0..1" Orcamento
    Orcamento "1" *-- "1..*" ItemOrcamento
    OrdemServico "1" *-- "1..*" TransicaoStatus
    OrdemServico --> StatusOS
```

### Estoque

```mermaid
classDiagram
    direction LR
    class ItemEstoque {
        <<Aggregate Root>>
        Long id
        String nome
        TipoItem tipo
        Dinheiro preco
        Int saldo
        Int reservado
        Boolean ativo
        disponivel()
        registrarEntrada(quantidade)
        reservar(quantidade)
        liberar(quantidade)
        baixar(quantidade)
        inativar()
    }
    class Reserva {
        <<Aggregate Root>>
        Long id
        Long osId
        Long itemId
        Quantidade quantidade
        SituacaoReserva situacao
        liberar()
        consumir()
    }
    class Quantidade {
        <<Value Object>>
        Int valor
    }
    class SituacaoReserva {
        <<enumeration>>
        ATIVA
        LIBERADA
        CONSUMIDA
    }
    Reserva --> ItemEstoque : itemId
    Reserva --> Quantidade
    Reserva --> SituacaoReserva
```

### Cadastro e Autenticação

```mermaid
classDiagram
    direction LR
    class Cliente {
        <<Aggregate Root>>
        Long id
        Documento documento
        String nome
        Email email
        Boolean ativo
    }
    class Veiculo {
        <<Aggregate Root>>
        Long id
        Placa placa
        String marca
        String modelo
        Int ano
        Long clienteId
        Boolean ativo
    }
    class Servico {
        <<Aggregate Root>>
        Long id
        String nome
        Dinheiro preco
        Boolean ativo
    }
    class Usuario {
        <<Aggregate Root>>
        Long id
        String nomeUsuario
        String senhaHash
        Boolean ativo
    }
    class Documento {
        <<Value Object · shared-kernel>>
        CPF ou CNPJ com dígitos verificadores
    }
    class Placa {
        <<Value Object · shared-kernel>>
        Antiga ou Mercosul
    }
    class Dinheiro {
        <<Value Object · shared-kernel>>
        BigDecimal valor
    }
    Veiculo --> Cliente : clienteId
    Cliente --> Documento
    Veiculo --> Placa
    Servico --> Dinheiro
```

## 4. Ciclo de vida da Ordem de Serviço

Os 6 status do enunciado e as transições que o agregado `OrdemServico` aceita. Qualquer outra transição é
recusada com `409`.

```mermaid
stateDiagram-v2
    [*] --> Recebida : Criar Ordem de Serviço
    Recebida --> EmDiagnostico : Iniciar Diagnóstico
    EmDiagnostico --> AguardandoAprovacao : Gerar Orçamento (peças reservadas)
    EmDiagnostico --> Finalizada : Peça não identificada
    EmDiagnostico --> Finalizada : Peça indisponível
    AguardandoAprovacao --> AguardandoAprovacao : Revisar Orçamento
    AguardandoAprovacao --> EmExecucao : Aprovar Orçamento
    AguardandoAprovacao --> Finalizada : Rejeitar Orçamento
    AguardandoAprovacao --> Finalizada : Orçamento Expirado (7 dias)
    EmExecucao --> Finalizada : Finalizar Execução
    Finalizada --> Entregue : Entregar Veículo
    Entregue --> [*]

    EmDiagnostico : Em diagnóstico
    AguardandoAprovacao : Aguardando aprovação
    EmExecucao : Em execução
```

## 5. Gerar Orçamento com reserva de peças

O fluxo que cruza dois bounded contexts numa só transação.

```mermaid
sequenceDiagram
    actor M as Mecânico
    participant C as OrcamentoController
    participant S as GerarOrcamentoService
    participant OS as OrdemServico
    participant E as EstoqueAdapter (ACL)
    participant R as ReservarPecaService (estoque)
    participant N as NotificacaoPort

    M->>C: POST /api/admin/ordens/{id}/orcamento
    C->>S: gerar(id, itens)
    S->>OS: gerarOrcamento(itens, validoAte)
    OS-->>S: orçamento (total calculado), status Aguardando aprovação
    loop cada peça do orçamento
        S->>E: reservar(osId, itemEstoqueId, quantidade)
        E->>R: reservar
        R-->>E: Peça Reservada ou Peça Indisponível
    end
    alt todas reservadas
        S->>S: salva a OS e publica Orçamento Gerado
        S->>N: envia orçamento ao cliente
    else alguma indisponível
        S->>E: liberarReservas(osId)
        S->>OS: encerrar()
        OS-->>S: OS Encerrada, status Finalizada
    end
    S-->>C: status e total
```

## 6. C4

Mantidos em [`docs/c4/`](../../c4/).

[C4 Nível 1 — Contexto](../../c4/c4-nivel1-contexto.mmd)

[C4 Nível 2 — Contêineres](../../c4/c4-nivel2-conteineres.mmd)

[C4 Nível 3 — Componentes](../../c4/c4-nivel3-componentes.mmd)
