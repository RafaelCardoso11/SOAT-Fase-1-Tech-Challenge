# Apresentação — Tech Challenge Fase 1

Agenda do vídeo, com cada tema ligado aos itens pedidos no enunciado. Duração: 12:30 (máximo de 15 minutos).

- [`slides.html`](slides.html): slides. Abra no navegador; as setas mudam de slide e `F` liga a tela cheia.
- [`demo.http`](demo.http): requisições da demonstração, com dados fictícios. Executadas com a extensão REST Client do VS Code.

## Agenda

| Início | Tema | Duração | Material | Itens do enunciado |
|--------|------|---------|----------|--------------------|
| 00:00 | Integrantes e proposta | 1:00 | Slides 1 e 2 | Identificação dos integrantes; proposta da solução |
| 01:00 | Event Storming | 1:30 | Board Tech01 no Miro | Processo de Event Storming |
| 02:30 | DDD | 1:30 | Slides 3 e 4, [`linguagem-ubiqua.md`](../ddd/linguagem-ubiqua.md) | Principais elementos de DDD |
| 04:00 | Arquitetura e camadas | 1:30 | Slide 5, [`arquitetura.md`](../arquitetura.md) e código | Arquitetura; organização do monólito e camadas; documentação e decisões arquiteturais |
| 05:30 | Banco de dados | 0:30 | Slide 6 | Escolha e justificativa do banco |
| 06:00 | Execução e Swagger | 1:00 | Slide 7, terminal e Swagger UI | Execução da aplicação; Swagger ou OpenAPI |
| 07:00 | Autenticação | 1:00 | `demo.http`, requisições 01 a 04 | Autenticação das APIs administrativas |
| 08:00 | Ordem de Serviço | 3:00 | Slide 8 e `demo.http`, requisições 05 a 17 | Criação e acompanhamento da OS; serviços, peças e insumos; geração e aprovação do orçamento; alteração dos status |
| 11:00 | Testes automatizados | 1:00 | Slide 9, saída do `./gradlew build` e CI | Testes automatizados |
| 12:00 | Análise de vulnerabilidades | 0:30 | Slide 10 e [`trivy-report.txt`](../security/trivy-report.txt) | Resultado da análise de vulnerabilidades |

## Demonstração

As requisições rodam em ordem, com o banco zerado:

```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build
```

| Requisições | Resultado esperado |
|-------------|--------------------|
| 01 a 04 | Aplicação no ar; `401` sem token; login; `200` com token |
| 05 e 06 | Peça e insumo cadastrados |
| 07 e 08 | OS `RECEBIDA`; consulta pública do andamento |
| 09 a 11 | OS `EM_DIAGNOSTICO`; orçamento liberado |
| 12 | Orçamento com serviço, peça e insumo; OS `AGUARDANDO_APROVACAO` |
| 13 e 14 | Orçamento aprovado; OS `EM_EXECUCAO` |
| 15 e 16 | OS `FINALIZADA` e depois `ENTREGUE` |
| 17 | Linha do tempo com todos os status |
