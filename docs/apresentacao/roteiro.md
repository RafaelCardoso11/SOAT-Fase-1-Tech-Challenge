# Apresentação — Tech Challenge Fase 1

Agenda do vídeo, com cada tema ligado aos itens pedidos no enunciado. Duração: 12:30 (máximo de 15 minutos).

- [`slides.html`](slides.html): slides. Abra no navegador; as setas mudam de slide e `F` liga a tela cheia.
- [`demo.http`](demo.http): requisições da demonstração, com dados fictícios. Executadas com a extensão REST Client do VS Code.

## Agenda

| Início | Tema | Duração | Material | Itens do enunciado |
|--------|------|---------|----------|--------------------|
| 00:00 | Integrantes e proposta | 1:00 | Slides 1 e 2 | Identificação dos integrantes; proposta da solução |
| 01:00 | Event Storming | 1:30 | Slide 3 e board Tech01 no Miro | Processo de Event Storming |
| 02:30 | DDD | 1:30 | Slides 4 e 5, [`linguagem-ubiqua.md`](../ddd/linguagem-ubiqua.md) | Principais elementos de DDD |
| 04:00 | Arquitetura e camadas | 1:30 | Slide 6, [`arquitetura.md`](../arquitetura.md) e código | Arquitetura; organização do monólito e camadas; documentação e decisões arquiteturais |
| 05:30 | Banco de dados | 0:30 | Slide 7 | Escolha e justificativa do banco |
| 06:00 | Execução e Swagger | 1:00 | Slide 8, terminal e Swagger UI | Execução da aplicação; Swagger ou OpenAPI |
| 07:00 | Autenticação | 1:00 | Slide 9 e `demo.http`, requisições 01 a 04 | Autenticação das APIs administrativas |
| 08:00 | Ordem de Serviço | 3:00 | Slide 10 e `demo.http`, requisições 05 a 20 | Criação e acompanhamento da OS; serviços, peças e insumos; geração e aprovação do orçamento; alteração dos status |
| 11:00 | Testes automatizados | 1:00 | Slide 11, saída do `./gradlew build` e CI | Testes automatizados |
| 12:00 | Análise de vulnerabilidades | 0:30 | Slide 12 e [`trivy-report.txt`](../security/trivy-report.txt) | Resultado da análise de vulnerabilidades |

## Demonstração

As requisições rodam em ordem, com o banco zerado:

```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build
```

| Requisições | Resultado esperado |
|-------------|--------------------|
| 01 a 04 | Aplicação no ar; `401` sem token; login; `200` com token |
| 05 a 08 | Cliente, serviço, peça e insumo cadastrados |
| 09 e 10 | OS `RECEBIDA`; consulta pública do andamento |
| 11 a 13 | OS `EM_DIAGNOSTICO`; orçamento liberado |
| 14 | Orçamento montado com os preços do catálogo; OS `AGUARDANDO_APROVACAO`; e-mail no Mailpit |
| 15 e 16 | Cliente consulta e aprova o orçamento pelo link do e-mail; OS `EM_EXECUCAO` |
| 17 a 19 | Baixa das peças; OS `FINALIZADA` e depois `ENTREGUE` |
| 20 | Linha do tempo com todos os status |
