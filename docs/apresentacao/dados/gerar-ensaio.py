import html, json, os, re, subprocess, sys, urllib.error, urllib.request
from pathlib import Path

import imageio_ffmpeg

REPO = Path(__file__).resolve().parents[3]
W = REPO / 'build' / 'ensaio'
SLIDES = REPO / 'docs/apresentacao/slides.html'
DEMO = REPO / 'docs/apresentacao/demo.http'
COMPOSE = REPO / 'docker/docker-compose.yml'
SAIDA = W / 'ensaio-apresentacao.mp4'
CHROME = os.environ.get('CHROME', '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome')
VOZ = os.environ.get('VOZ', 'Luciana')
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
TESTES, FAIXA = sys.argv[1], sys.argv[2]

for d in ('img', 'aud', 'seg', 'html'):
    os.makedirs(W / d, exist_ok=True)

CSS = '''<style>
body{margin:0;width:1600px;height:900px;background:#edeff1;color:#1b2128;font-family:"Helvetica Neue",Arial,sans-serif;padding:56px 104px;box-sizing:border-box;overflow:hidden}
h1{font:700 52px "Arial Narrow",Arial,sans-serif;margin:0 0 12px}
.sub{font-size:22px;color:#5a636d;margin-bottom:24px}
.met{font:700 24px Menlo,monospace;color:#0b3c8c;margin-bottom:16px;word-break:break-all}
.st{display:inline-block;font:700 26px Menlo,monospace;padding:4px 14px;border-radius:5px;color:#fff;margin-bottom:12px}
pre{font:17px/1.35 Menlo,monospace;background:#fff;border:2px solid #1b2128;border-radius:6px;padding:16px 20px;margin:0 0 16px;white-space:pre-wrap;word-break:break-all;overflow:hidden}
.aviso{position:absolute;right:104px;top:40px;font-size:18px;color:#5a636d}
</style>'''


def foto(url, destino, espera=3000):
    subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars', f'--user-data-dir={W}/chrome',
                    f'--virtual-time-budget={espera}', '--window-size=1600,900', f'--screenshot={destino}', url],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)


def cartao(nome, corpo):
    caminho = f'{W}/html/{nome}.html'
    with open(caminho, 'w', encoding='utf-8') as f:
        f.write(f'<!doctype html><meta charset="utf-8">{CSS}<div class="aviso">Vídeo de ensaio</div>{corpo}')
    destino = f'{W}/img/{nome}.png'
    foto(f'file://{caminho}', destino, 500)
    return destino


def slide(n):
    destino = f'{W}/img/slide{n}.png'
    foto(f'file://{SLIDES}#{n}', destino)
    return destino


def requisicoes():
    txt = open(DEMO, encoding='utf-8').read()
    vs, resp, saida = {}, {}, []

    def sub(s):
        def f(m):
            k = m.group(1)
            if k in vs:
                return sub(vs[k])
            p = k.split('.')
            v = resp[p[0]]
            for x in p[3:]:
                v = v[x]
            return str(v)
        return re.sub(r'\{\{([^}]+)\}\}', f, s)

    for b in re.split(r'(?m)^###', txt):
        ls = b.split('\n')
        titulo, nome, req, hdr, dados, st = ls[0].strip(), None, None, {}, [], 0
        for l in ls[1:]:
            if st == 0:
                m = re.match(r'# @name (\w+)', l)
                if m:
                    nome = m.group(1)
                    continue
                if not l.strip() or l.startswith('#'):
                    continue
                m = re.match(r'@(\w+) = (.*)', l)
                if m:
                    vs[m.group(1)] = m.group(2)
                    continue
                req, st = sub(l).split(' ', 1), 1
            elif st == 1:
                if not l.strip():
                    st = 2
                    continue
                k, v = l.split(':', 1)
                hdr[k] = sub(v.strip())
            else:
                dados.append(l)
        if not req:
            continue
        corpo = sub('\n'.join(dados).strip())
        r = urllib.request.Request(req[1], method=req[0], headers=hdr, data=corpo.encode() if corpo else None)
        try:
            x = urllib.request.urlopen(r, timeout=30)
            c, t = x.status, x.read().decode()
        except urllib.error.HTTPError as e:
            c, t = e.code, e.read().decode()
        try:
            j = json.loads(t)
        except ValueError:
            j = t
        if nome:
            resp[nome] = j
        saida.append((titulo, req[0], req[1], corpo, c, j))
    return saida


def cartao_req(i, r):
    titulo, met, url, corpo, c, j = r
    tela = dict(j) if isinstance(j, dict) else j
    if isinstance(tela, dict) and 'token' in tela:
        tela['token'] = tela['token'][:24] + '…'
    resposta = json.dumps(tela, ensure_ascii=False, indent=2)
    resposta = re.sub(r'\{\s+"status": ("\w+"),\s+"momento": ("[^"]+")\s+\}', r'{ "status": \1, "momento": \2 }', resposta)
    linhas = resposta.split('\n')
    if len(linhas) > 24:
        resposta = '\n'.join(linhas[:24]) + '\n…'
    cor = '#1d7a46' if c < 400 else '#b3261e'
    bloco = f'<pre>{html.escape(corpo)}</pre>' if corpo else ''
    return cartao(f'req{i:02d}', f'<h1>{html.escape(titulo)}</h1><div class="met">{met} {html.escape(url)}</div>{bloco}'
                                  f'<div class="st" style="background:{cor}">{c}</div><pre>{html.escape(resposta)}</pre>')


FALAS_REQ = [
    'A aplicação está no ar.',
    'Sem token, a rota administrativa recusa o acesso.',
    'O login devolve o token JWT, que vale uma hora.',
    'Com o token, a mesma rota responde.',
    'Cadastro uma peça no estoque, o filtro de óleo.',
    'E um insumo, o óleo, vendido por litro.',
    'Abro a ordem de serviço com o CPF do cliente e a placa, marca, modelo e ano do veículo. Ela nasce Recebida.',
    'O cliente já acompanha a ordem pela API, sem token, e o CPF aparece mascarado.',
    'O mecânico inicia o diagnóstico.',
    'Registra o que encontrou.',
    'E finaliza, liberando o orçamento.',
    'O orçamento reúne o serviço, a peça e o insumo. O sistema soma o total, reserva as peças no estoque e a ordem passa a aguardar aprovação.',
    'A aprovação do cliente é registrada, e a ordem entra em execução.',
    'Ao iniciar a execução, as peças reservadas saem do estoque.',
    'Serviço concluído: a ordem fica Finalizada.',
    'Na entrega, a ordem fica Entregue e o sistema gera o comprovante.',
    'Na consulta do cliente aparece a linha do tempo, com data e hora de cada status.',
]

reqs = requisicoes()
assert len(reqs) == len(FALAS_REQ), len(reqs)
for r in reqs:
    print(r[4], r[0])

foto('http://localhost:8080/swagger-ui.html', f'{W}/img/swagger.png', 8000)
ps = subprocess.run(['docker', 'compose', '-f', str(COMPOSE), 'ps'], capture_output=True, text=True, check=True).stdout
trivy = '\n'.join(open(REPO / 'docs/security/trivy-report.txt', encoding='utf-8').read().split('\n')[:34])
build = '\n'.join((W / 'build.log').read_text(encoding='utf-8').strip().split('\n')[-6:])

cenas = [
    ('c01', slide(1), 'Olá, somos o grupo 29 do SOAT, Fase 1. Os integrantes são Alan Teixeira Fideles, André Rafael Cardoso Reis e Cairo Augusto de Andrade. Vamos apresentar o back-end da Oficina Mecânica, feito em Kotlin com Spring Boot, PostgreSQL e Docker.'),
    ('c02', slide(2), 'A solução é uma API que acompanha o veículo da abertura da Ordem de Serviço até a entrega. Ela cobre três frentes: a Ordem de Serviço, com cliente, veículo, serviços, peças e orçamento; o acompanhamento, com seis status controlados pelo sistema e consulta do cliente pela API; e a gestão administrativa, com os cadastros, o controle de estoque e o tempo médio de execução.'),
    ('c03', slide(3), 'Antes do código, modelamos o domínio com um Event Storming. Vamos ao board no Miro.'),
    ('c04', cartao('miro', '<h1>Aqui entra o board Tech01 no Miro</h1><div class="sub">No vídeo final, navegue pelo board ao vivo, um fluxo por vez.</div>'), 'Modelamos os três fluxos do enunciado: a criação da Ordem de Serviço, o acompanhamento e a gestão de peças e insumos. Aqui estão os eventos de domínio, sempre no passado, como Ordem de Serviço Criada e Orçamento Aprovado. Antes de cada evento vem o comando que o dispara, e acima, o ator que executa: o atendente, o mecânico ou o cliente. As políticas ligam um evento ao próximo comando. Por exemplo: quando o orçamento é aprovado, a execução pode começar. Os hotspots marcam as dúvidas que surgiram, e cada um virou uma decisão. Encerrada parecia ser um status, mas o enunciado define seis. Então, quando a ordem sai do fluxo normal, ela vai para Finalizada. Desse board saíram os agregados e os bounded contexts, que mostramos agora.'),
    ('c05', slide(4), 'Identificamos quatro bounded contexts. Ordem de Serviço é o subdomínio principal, onde está o valor do negócio. Estoque e Cadastro são de suporte, e Autenticação é genérico. A Ordem de Serviço reserva e baixa peças no Estoque e avisa o cliente por e-mail. Cada contexto virou um módulo Gradle e um schema no banco.'),
    ('c06', slide(5), 'O agregado Ordem Serviço protege as transições de status: uma transição inválida é recusada pelo próprio domínio. Item Estoque garante que o saldo nunca fica negativo. CPF, CNPJ e placa são objetos de valor: um valor inválido nem chega a existir no domínio. E cada fato importante vira um evento de domínio. A linguagem ubíqua liga cada termo do negócio à classe correspondente no código.'),
    ('c07', slide(6), 'É um monólito, como pede o enunciado: um único deployable. Por dentro, ele é modular, com um módulo por contexto, e cada módulo é organizado em camadas. A requisição entra pelo adapter, passa pelo caso de uso na camada de aplicação e chega ao domínio. O domínio não depende do Spring; ele define portas, que os adapters de saída implementam com JPA e e-mail. As decisões estão registradas no documento de arquitetura e nos diagramas C4.'),
    ('c08', slide(7), 'Escolhemos PostgreSQL porque o fluxo da ordem exige transações fortes: reservar peça e mudar o status acontecem juntos, ou nada acontece. Ele também garante a integridade, com CPF, CNPJ e placa únicos e o saldo validado no banco. O lock de linha impede que a última peça vá para duas ordens, e cada módulo tem o próprio schema, versionado com Flyway.'),
    ('c09', slide(8), 'Agora vamos ver a solução funcionando: primeiro o ambiente e a documentação da API, depois a autenticação e o fluxo completo de uma Ordem de Serviço.'),
    ('c10', cartao('terminal', f'<h1>Terminal</h1><div class="met">docker compose -f docker/docker-compose.yml ps</div><pre>{html.escape(ps)}</pre>'), 'O ambiente completo sobe com um comando do Docker Compose: a aplicação, o PostgreSQL e o Mailpit, que captura os e-mails em desenvolvimento. O passo a passo está no README.'),
    ('c11', f'{W}/img/swagger.png', 'Toda a API está documentada no Swagger, separada por recurso: clientes, veículos, serviços, estoque e ordens de serviço. As rotas administrativas pedem o token JWT, que entra pelo botão Authorize.'),
    ('c12', slide(9), 'Agora as requisições na API, na ordem, com o banco zerado. Começamos pela autenticação.'),
]
for i in range(4):
    cenas.append((f'r{i + 1:02d}', cartao_req(i + 1, reqs[i]), FALAS_REQ[i]))
cenas.append(('c13', slide(10), 'Estes são os seis status do enunciado. O status não é um campo que se edita: ele muda como resultado de cada ação. Vamos percorrer todos.'))
for i in range(4, 17):
    cenas.append((f'r{i + 1:02d}', cartao_req(i + 1, reqs[i]), FALAS_REQ[i]))
cenas += [
    ('c14', slide(11), f'São {TESTES} testes, sem falhas: unitários no domínio e nos casos de uso, de integração com um PostgreSQL real via Testcontainers e cenários BDD. O Jacoco exige no mínimo 80% de cobertura de linhas no domínio e na aplicação, e todos os módulos ficam entre {FAIXA}. Abaixo de 80%, o build falha, e o CI roda tudo isso a cada pull request.'),
    ('c15', cartao('build', f'<h1>Saída do build</h1><div class="met">./gradlew build</div><pre>{html.escape(build)}</pre>'), 'Aqui está o resultado do build, com os testes e o gate de cobertura aprovados.'),
    ('c16', slide(12), 'Analisamos a imagem Docker e as dependências com o Trivy. Não há vulnerabilidades HIGH ou CRITICAL em aberto. Restam dois riscos aceitos no spring-webmvc, documentados: eles dependem de recursos que esta API não usa.'),
    ('c17', cartao('trivy', f'<h1>docs/security/trivy-report.txt</h1><pre>{html.escape(trivy)}</pre>'), 'O código, a documentação DDD e o board estão nos links da entrega. Obrigado.'),
]

lista, total = [], 0.0
for nome, img, fala in cenas:
    aud = f'{W}/aud/{nome}.aiff'
    subprocess.run(['say', '-v', VOZ, '-o', aud, fala], check=True)
    info = subprocess.run(['afinfo', aud], capture_output=True, text=True).stdout
    dur = float(re.search(r'estimated duration: ([\d.]+)', info).group(1)) + 0.6
    total += dur
    seg = f'{W}/seg/{nome}.mp4'
    subprocess.run([FFMPEG, '-y', '-loglevel', 'error', '-loop', '1', '-framerate', '10', '-i', img, '-i', aud,
                    '-vf', 'scale=1600:900:force_original_aspect_ratio=decrease,pad=1600:900:(ow-iw)/2:(oh-ih)/2:color=white,format=yuv420p',
                    '-af', 'apad', '-t', f'{dur:.2f}', '-r', '10', '-c:v', 'libx264', '-tune', 'stillimage',
                    '-c:a', 'aac', '-ar', '44100', '-ac', '1', seg], check=True)
    lista.append(seg)
    print(f'{nome} {dur:6.1f}s')

with open(f'{W}/lista.txt', 'w') as f:
    f.writelines(f"file '{s}'\n" for s in lista)
# Recodifica na junção: com -c copy, os players da Apple só enxergam parte da trilha de áudio.
subprocess.run([FFMPEG, '-y', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', f'{W}/lista.txt', '-r', '10',
                '-c:v', 'libx264', '-tune', 'stillimage', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '160k',
                '-ar', '48000', '-ac', '2', '-af', 'aresample=async=1:first_pts=0', '-movflags', '+faststart', str(SAIDA)],
               check=True)
print(f'TOTAL {int(total // 60)}:{int(total % 60):02d}', SAIDA)
