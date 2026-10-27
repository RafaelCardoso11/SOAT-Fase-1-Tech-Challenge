# SOAT Fase 1 — Tech Challenge (Oficina Mecânica)

## Fluxo de trabalho: issue → branch → PR

Todo trabalho começa por uma issue. Nada vai para a `main` sem PR vinculado a uma issue.

1. **Issue primeiro.** Antes de criar branch ou escrever código, abra uma issue (`gh issue create`) com:
   - `## Contexto`: o problema ou a necessidade
   - `## Escopo`: checklist (`- [ ]`) do que entra
   - `## Critérios de aceite`: como saber que está pronto
   - Labels existentes: `enhancement`, `documentation`, `bug`
   Se já existir uma issue para o trabalho, use a existente.
2. **Branch a partir do `origin/main`.** Sempre `git fetch` antes e crie a branch a partir de `origin/main`, nunca da `main` local, para não arrastar commits ainda não enviados para o PR.
   - Nome: `<tipo>/<numero-da-issue>-<slug>` (ex.: `feat/12-criar-ordem-servico`)
   - Prefira uma worktree separada (`git worktree add`) para não misturar com alterações locais.
3. **Commits** em Conventional Commits, em português: `feat:`, `fix:`, `docs:`, `test:`, `ci:`, `chore:`, `refactor:` (escopo opcional, ex.: `docs(bdd): ...`).
4. **PR** com a descrição em:
   - `## Objetivo`
   - `## O que entra`
   - `## Como validar`
   - `## Escopo` / `## Fora de escopo`
   - Última linha: `Closes #<numero-da-issue>`, para a issue fechar no merge.
5. **Um assunto por PR.** Antes de abrir, confira `git log --oneline origin/main..HEAD` e `git diff --stat origin/main...HEAD`: só podem aparecer os commits e arquivos daquela issue.

## Referências

- Specs e BDD: `specs/001-mvp-gestao-os/` (BDD em `bdd/*.feature`)
- Arquitetura: `docs/arquitetura.md`; C4 em `docs/c4/`; DDD em `docs/ddd/`
- Event Storming e casos de uso: board **Tech01** no Miro, acessível pelo plugin `miro@claude-plugins-official`
- Fluxo SDD: skills `/spec-driven-development`, `/spec-specify`, `/spec-plan`, `/spec-tasks`
- Gate de qualidade: `./gradlew build` (testes + cobertura Jacoco ≥ 80%)

## Ferramentas de agente

- **rtk**: proxy de CLI que corta tokens da saída de `git`, `gradle` etc. Instale (`brew install rtk`) e rode `rtk init -g`; o hook reescreve os comandos sozinho. `rtk gain` mostra a economia.
- **codegraph**: grafo do código local (SQLite em `.codegraph/`, fora do git) exposto via MCP em `.mcp.json`. Instale o CLI (`npm i -g @colbymchenry/codegraph`) e rode `codegraph init` na raiz uma vez; o índice se atualiza sozinho. Prefira as ferramentas do codegraph a ler arquivo por arquivo para achar símbolos, chamadores e impacto de mudanças.
- **ponytail**: plugin habilitado em `.claude/settings.json`; o Claude Code oferece instalar ao abrir o projeto. Escreva o mínimo que resolve. `/ponytail-review` revisa o diff antes do PR.
