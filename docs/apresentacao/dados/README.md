# Vídeo da apresentação

`gerar-video.py` gera o vídeo narrado da entrega. As requisições do `demo.http` rodam contra a aplicação em execução, e os prints do board Tech01, do Swagger e do Mailpit são capturados na hora, então as telas mostram respostas reais.

Requer macOS (`afinfo`), Google Chrome, Python 3 e Docker. Na raiz do repositório:

```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build

mkdir -p build/video/voz
./gradlew build | tee build/video/build.log

python3 -m venv build/video/venv
build/video/venv/bin/pip install imageio-ffmpeg piper-tts playwright

for e in onnx onnx.json; do
  curl -L -o build/video/voz/pt_BR-jeff-medium.$e \
    https://huggingface.co/rhasspy/piper-voices/resolve/main/pt/pt_BR/jeff/medium/pt_BR-jeff-medium.$e
done

build/video/venv/bin/python docs/apresentacao/dados/gerar-video.py <total-de-testes> "<faixa-de-cobertura>"
```

Exemplo dos argumentos: `517 "89,7% e 100%"`. O vídeo fica em `build/video/apresentacao-fase1.mp4`, fora do controle de versão.

Ninguém deve estar com o board aberto durante a geração, senão o cursor aparece nos prints.
