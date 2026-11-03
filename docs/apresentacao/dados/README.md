# Vídeo de ensaio

`gerar-ensaio.py` gera um vídeo narrado com as telas da apresentação, para conferir a ordem, o ritmo e a duração. As requisições do `demo.http` rodam contra a aplicação em execução, e as respostas no vídeo são reais.

Requer macOS (comando `say`), Google Chrome, Python 3 e Docker. Na raiz do repositório:

```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build

mkdir -p build/ensaio
./gradlew build | tee build/ensaio/build.log

python3 -m venv build/ensaio/venv
build/ensaio/venv/bin/pip install imageio-ffmpeg
build/ensaio/venv/bin/python docs/apresentacao/dados/gerar-ensaio.py <total-de-testes> "<faixa-de-cobertura>"
```

Exemplo dos argumentos: `503 "89,7% e 100%"`. O vídeo fica em `build/ensaio/ensaio-apresentacao.mp4`, fora do controle de versão. A voz muda com a variável `VOZ`, e o caminho do navegador com `CHROME`.

A tela do board Tech01 é provisória: no vídeo final, a navegação no Miro é feita ao vivo.
