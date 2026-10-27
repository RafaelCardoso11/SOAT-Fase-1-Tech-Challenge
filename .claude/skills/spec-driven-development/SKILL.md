---
name: "spec-driven-development"
description: >
  Orquestrador de Desenvolvimento Orientado a Especificação (SDD). Usado sempre que
  for necessário planejar uma nova funcionalidade, API, integração ou refatoração
  complexa antes de codificar, ou para orientar o agente em um fluxo estruturado
  (Specify -> Plan -> Tasks -> Implement -> Verify).
---

## Goal

Garantir que todo trabalho não trivial seja **especificado e planejado antes de ser codificado**, produzindo artefatos Markdown duráveis e revisáveis no repositório.

Esta skill é o **guarda-chuva principal**: ela explica o fluxo e delega a execução para skills ou fases especializadas.

---

## Fases do Fluxo

| # | Fase | Pergunta Core | Artefato Gerado |
|---|---|---|---|
| **1** | **Specify** | O que estamos construindo e por quê? | `specs/<feature-slug>/spec.md` |
| **2** | **Plan** | Como isso se encaixa na arquitetura deste projeto? | `specs/<feature-slug>/plan.md` |
| **3** | **Tasks** | Qual é o checklist ordenado de trabalho concreto? | `specs/<feature-slug>/tasks.md` |
| **4** | **Implement** | Escrever o código seguindo as tarefas ativas | Código atualizado + `tasks.md` atualizado |
| **5** | **Verify** | O código atende a todos os requisitos e passa nos testes? | Relatório de validação |

### Fluxo visual:
```text
Specify ──> Plan ──> Tasks ──> Implement ──> Verify
   ▲                                            │
   └───────────────── lacunas encontradas ──────┘
```

---

## Layout e Organização dos Artefatos

Cada feature/funcionalidade deve ter sua própria pasta **commitada no repositório**:

```text
specs/<feature-slug>/
├── spec.md      # O que e por quê (Requisitos & Escopo)
├── plan.md      # Como (Arquitetura, Componentes, Riscos)
└── tasks.md     # Checklist ordenado de tarefas
```

> **Nota:** Use nomes em `kebab-case` curto para `<feature-slug>` (ex: `specs/auth-jwt-login/`).

---

## Onde Entrar no Fluxo

1. **Nada existe ainda:** Iniciar na fase `Specify`.
2. **`spec.md` existe:** Avançar para `Plan`.
3. **`spec.md` e `plan.md` existem, mas não `tasks.md`:** Avançar para `Tasks`.
4. **Três arquivos existem e há tasks abertas:** Ir para `Implement`.
5. **Implementação concluída:** Executar `Verify`.

> ⚠️ **Atenção:** Nunca reescreva um artefato que já existe só porque o usuário pediu a fase seguinte. Leia o que já foi criado e apenas preencha lacunas.

---

## Quando Usar o Fluxo Completo

### ✅ **Usar SDD para:**
* Novas capacidades/endpoints (HTTP, CLI, tarefas agendadas, etc.).
* Novas integrações ou alterações de contrato com sistemas externos.
* Refatoração não trivial que afete múltiplos módulos ou comportamentos.
* Requisitos vagos que precisam ser esclarecidos antes do código.

### 🚫 **Pular SDD (Ir direto para o código) para:**
* Ajustes de ortografia, comentários ou formatação.
* Correções simples de bugs (bugfix de linha única com causa clara).
* Alterações superficiais em configurações.

---

## Regras de Gate (Transição entre Fases)

1. **Não avance para Plan** enquanto houver dúvidas pendentes (`NEEDS CLARIFICATION`) em `spec.md` que mudem *como* a funcionalidade será desenvolvida.
2. **Suficiente, não perfeita:** Cada fase precisa ser completa o bastante para dar segurança, sem perder tempo com polimento excessivo de texto.
3. **Voltar atrás é normal:** Se na fase de Plan ou Tasks você notar que a especificação ficou incompleta, atualize `spec.md`.
4. **Acompanhamento de progresso:** Novas tarefas descobertas no meio do desenvolvimento devem ser adicionadas ao `tasks.md` e marcadas à medida que forem concluídas.

---

## Checklist de Qualidade do Fluxo

- [ ] `spec.md` existe e não há dúvidas que bloqueiem a arquitetura.
- [ ] `plan.md` mapeia cada requisito para componentes/arquivos concretos.
- [ ] `tasks.md` possui checkboxes claros por unidade de trabalho.
- [ ] O código foi implementado estritamente conforme o `tasks.md`.
- [ ] A fase final de verificação confirmou que tudo funciona conforme o especificado.
