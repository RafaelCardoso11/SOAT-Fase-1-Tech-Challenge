import html, json, os, re, subprocess, sys, urllib.error, urllib.request
from pathlib import Path

import imageio_ffmpeg
from playwright.sync_api import sync_playwright

REPO = Path(__file__).resolve().parents[3]
W = REPO / 'build' / 'video'
SLIDES = REPO / 'docs/apresentacao/slides.html'
DEMO = REPO / 'docs/apresentacao/demo.http'
COMPOSE = REPO / 'docker/docker-compose.yml'
SAIDA = W / 'apresentacao-fase1.mp4'
CHROME = os.environ.get('CHROME', '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome')
VOZ = os.environ.get('VOZ', 'Luciana')
PIPER = os.environ.get('PIPER_MODELO', str(W / 'voz' / 'pt_BR-jeff-medium.onnx'))
PRONUNCIA = {'FIAP': 'fíap', 'JWT': 'jota dáblio tê', 'API': 'a pê i', 'CPF': 'cê pê éfe', 'CNPJ': 'cê ene pê jota', 'DDD': 'dê dê dê',
             'SOAT': 'sôat', 'Event Storming': 'ivent stórmin', 'Miro': 'míro', 'Kotlin': 'cótlin', 'Spring Boot': 'spring but',
             'spring-webmvc': 'spring web em vê cê', 'Docker Compose': 'dóquer compôus', 'Docker': 'dóquer', 'Swagger': 'suéguer',
             'Authorize': 'ótoraize', 'README': 'ríd mi', 'bounded contexts': 'bâunded cóntexts', 'deployable': 'deplóiabol',
             'adapters': 'adápters', 'adapter': 'adápter', 'schema': 'esquema', 'Gradle': 'grêidol', 'Flyway': 'fláiuei',
             'PostgreSQL': 'póstgres', 'JPA': 'jota pê a', 'lock': 'lóqui', 'hotspots': 'rót spóts', 'board': 'bórd',
             'back-end': 'béquiend', 'login': 'loguim', 'token': 'tôquen', 'build': 'bíld', 'gate': 'guêit', 'Jacoco': 'jacôco',
             'Testcontainers': 'test contêiners', 'BDD': 'bê dê dê', 'CI': 'cê i', 'pull request': 'pul requést',
             'Trivy': 'trívi', 'HIGH': 'rai', 'CRITICAL': 'crítical', 'Mailpit': 'mêilpit', 'e-mail': 'imeio', 'C4': 'cê quatro'}


def falar(texto, destino, abertura=False):
    if not PIPER:
        subprocess.run(['say', '-v', VOZ, '-o', destino, texto], check=True)
        return
    for termo, som in PRONUNCIA.items():
        texto = re.sub(rf'\b{re.escape(termo)}\b', som, texto)
    subprocess.run([sys.executable, '-m', 'piper', '-m', PIPER, '-f', destino, '--length-scale', '1.22' if abertura else '1.08',
                    '--sentence-silence', '0.7' if abertura else '0.45'],
                   input=texto, text=True, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
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
</style>'''


def foto(url, destino, espera=3000):
    subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars', f'--user-data-dir={W}/chrome',
                    f'--virtual-time-budget={espera}', '--window-size=1600,900', f'--screenshot={destino}', url],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)


def cartao(nome, corpo):
    caminho = f'{W}/html/{nome}.html'
    with open(caminho, 'w', encoding='utf-8') as f:
        f.write(f'<!doctype html><meta charset="utf-8">{CSS}{corpo}')
    destino = f'{W}/img/{nome}.png'
    foto(f'file://{caminho}', destino, 500)
    return destino


def slide(n):
    destino = f'{W}/img/slide{n}.png'
    foto(f'file://{SLIDES}#{n}', destino)
    return destino


def token_do_email():
    mensagens = json.load(urllib.request.urlopen('http://localhost:8025/api/v1/messages'))['messages']
    corpo = json.load(urllib.request.urlopen(f"http://localhost:8025/api/v1/message/{mensagens[0]['ID']}"))
    foto(f"http://localhost:8025/view/{mensagens[0]['ID']}", f'{W}/img/mailpit-orcamento.png', 4000)
    return re.search(r'/api/public/orcamentos/([^/\s]+)', corpo['Text']).group(1)


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
        if req[1].endswith('/orcamento'):
            vs['tokenOrcamento'] = token_do_email()
        saida.append((titulo, req[0], req[1], corpo, c, j))
    return saida


def cartao_req(i, r, com_resposta):
    titulo, met, url, corpo, c, j = r
    tela = dict(j) if isinstance(j, dict) else j
    if isinstance(tela, dict) and 'token' in tela:
        tela['token'] = tela['token'][:24] + '…'
    resposta = json.dumps(tela, ensure_ascii=False, indent=2)
    resposta = re.sub(r'(?m)^( {4,})\{\n([^{}]*?)\n\s*\}', lambda m: m.group(1) + '{ ' + ' '.join(m.group(2).split()) + ' }', resposta)
    linhas = resposta.split('\n')
    if len(linhas) > 24:
        resposta = '\n'.join(linhas[:24]) + '\n…'
    cor = '#1d7a46' if c < 400 else '#b3261e'
    bloco = f'<pre>{html.escape(corpo)}</pre>' if corpo else ''
    return cartao(f'req{i:02d}{"b" if com_resposta else "a"}', f'<h1>{html.escape(titulo)}</h1><div class="met">{met} {html.escape(url)}</div>{bloco}'
                                  + (f'<div class="st" style="background:{cor}">{c}</div><pre>{html.escape(resposta)}</pre>' if com_resposta else ''))


BOARD = 'https://miro.com/app/board/uXjVHq1XvOs=/?share_link_id=344863119386'
QUADROS = [('geral', '3458764686742412957', 4), ('legenda', '3458764686739807016', 0), ('etapas', '3458764686743413036', 0),
           ('criacao', '3458764686742412957', 2), ('acompanhamento', '3458764689229280395', 2), ('pecas', '3458764689229280398', 2)]


def capturar_miro():
    with sync_playwright() as p:
        nav = p.chromium.launch(channel='chrome', headless=True, args=['--enable-unsafe-swiftshader', '--use-angle=swiftshader', '--ignore-gpu-blocklist'])
        pag = nav.new_context(viewport={'width': 1600, 'height': 900}, locale='pt-BR').new_page()
        for nome, quadro, cliques in QUADROS:
            pag.goto(f'{BOARD}&moveToWidget={quadro}', wait_until='domcontentloaded', timeout=60000)
            pag.wait_for_timeout(25000)
            for _ in range(cliques):
                pag.mouse.click(1423, 867)
                pag.wait_for_timeout(700)
            pag.mouse.move(800, 890)
            pag.wait_for_timeout(6000)
            pag.screenshot(path=W / 'img' / f'miro-{nome}.png')
        nav.close()


def capturar_swagger(token):
    with sync_playwright() as p:
        nav = p.chromium.launch(channel='chrome', headless=True)
        pag = nav.new_context(viewport={'width': 1600, 'height': 900}, locale='pt-BR').new_page()
        pag.goto('http://localhost:8080/swagger-ui.html', wait_until='networkidle')
        pag.wait_for_selector('.opblock')
        pag.screenshot(path=W / 'img' / 'swagger-lista.png')
        pag.click('.btn.authorize')
        pag.fill('.auth-container input', token)
        pag.screenshot(path=W / 'img' / 'swagger-authorize.png')
        pag.click('.auth-btn-wrapper .authorize')
        pag.click('.btn-done')
        op = pag.locator('.opblock-get:has(.opblock-summary-path[data-path="/api/admin/ordens"])').first
        op.locator('.opblock-summary-control').click()
        op.locator('.try-out__btn').click()
        op.locator('.execute').click()
        op.locator('.responses-table.live-responses-table').wait_for()
        op.locator('.responses-table.live-responses-table').scroll_into_view_if_needed()
        pag.wait_for_timeout(800)
        pag.screenshot(path=W / 'img' / 'swagger-execucao.png')
        nav.close()


FALAS_REQ = [
    ('Primeiro, confirmamos que a aplicação está no ar, chamando a rota de verificação.', 'A resposta é duzentos: a aplicação está funcionando.'),
    ('Agora, chamamos uma rota administrativa, a listagem de ordens de serviço, sem enviar nenhum token.', 'A resposta é quatrocentos e um: sem autenticação, o acesso é recusado.'),
    ('Fazemos o login com um usuário de demonstração.', 'O login devolve o token JWT, que vale por uma hora, ou seja, três mil e seiscentos segundos.'),
    ('Repetimos a mesma rota, agora enviando o token no cabeçalho de autorização.', 'Dessa vez, a resposta é duzentos. A lista ainda está vazia, porque o banco começou zerado.'),
    ('Com a autenticação resolvida, cadastramos a cliente, com CPF, nome e e-mail.', 'O cadastro valida o CPF, e é nesse e-mail que ela vai receber o orçamento.'),
    ('Depois, o catálogo. Cadastramos o serviço de troca de óleo, a cento e vinte reais.', 'O serviço foi criado no catálogo.'),
    ('Em seguida, uma peça: o filtro de óleo, a quarenta e cinco reais, com cinco unidades em estoque.', 'O estoque separa o saldo físico, o que está reservado e o que está disponível.'),
    ('E um insumo: o óleo, vendido por litro, a quarenta reais, com doze litros em estoque.', 'Peças e insumos ficam no mesmo controle de estoque, diferenciados pelo tipo.'),
    ('Agora abrimos a Ordem de Serviço, identificando a cliente pelo CPF e informando placa, marca, modelo e ano do veículo, além do relato do problema.', 'A ordem foi criada com o número um e nasce com o status Recebida. CPF e placa são validados pelo domínio antes disso.'),
    ('A partir daqui, a cliente já pode acompanhar a ordem pela API pública, sem token.', 'Ela vê o status e a linha do tempo, e o CPF aparece mascarado, para proteger o dado.'),
    ('O mecânico inicia o diagnóstico.', 'O status muda automaticamente para Em diagnóstico.'),
    ('Ele registra as observações da inspeção do veículo.', 'As observações ficam na ordem, e o status continua o mesmo.'),
    ('Ao finalizar, ele informa que identificou as peças necessárias.', 'A ordem agora está liberada para receber o orçamento.'),
    ('Esta é a geração do orçamento. Repare que a requisição informa só o serviço, as peças e as quantidades. Nenhum valor é digitado.', 'O sistema buscou os preços no catálogo e calculou o total: cento e vinte do serviço, quarenta e cinco do filtro e cento e vinte dos três litros de óleo, somando duzentos e oitenta e cinco reais. As peças foram reservadas, e o status passou para Aguardando aprovação.'),
    ('Pelo link, sem login, a cliente consulta o orçamento.', 'Ela vê os itens, o total e a validade.'),
    ('A própria cliente aprova o orçamento pelo link.', 'O status muda para Em execução.'),
    ('O mecânico inicia a execução do serviço.', 'Nesse momento, as peças reservadas são baixadas do estoque.'),
    ('Ao terminar, ele confirma que todos os itens foram aplicados.', 'O status muda para Finalizada, e a cliente recebe o aviso de que o veículo está pronto.'),
    ('Por fim, a oficina registra a entrega do veículo.', 'O status muda para Entregue, e o sistema devolve o comprovante.'),
    ('Para fechar, a cliente consulta a ordem mais uma vez.', 'Aqui está o acompanhamento completo: os seis status, cada um com data e hora.'),
]

login = urllib.request.Request('http://localhost:8080/api/auth/login', method='POST', headers={'Content-Type': 'application/json'},
                               data=json.dumps({'nomeUsuario': 'admin', 'senha': 'admin@123'}).encode())
capturar_swagger(json.load(urllib.request.urlopen(login))['token'])
reqs = requisicoes()
assert len(reqs) == len(FALAS_REQ), len(reqs)
for r in reqs:
    print(r[4], r[0])

capturar_miro()
ps = subprocess.run(['docker', 'compose', '-f', str(COMPOSE), 'ps'], capture_output=True, text=True, check=True).stdout
trivy = '\n'.join(open(REPO / 'docs/security/trivy-report.txt', encoding='utf-8').read().split('\n')[:34])
build = '\n'.join((W / 'build.log').read_text(encoding='utf-8').strip().split('\n')[-6:])
img = lambda nome: f'{W}/img/{nome}.png'

cenas = [
    ('a01', slide(1), 'Olá. Somos o grupo 29 da pós-graduação em Arquitetura de Software da FIAP, Fase 1. Os integrantes são Alan Teixeira Fideles, André Rafael Cardoso Reis e Cairo Augusto de Andrade. Vamos apresentar o back-end da Oficina Mecânica, feito em Kotlin, com Spring Boot, PostgreSQL e Docker.'),
    ('a02', slide(2), 'A solução é uma API que acompanha o veículo da abertura da Ordem de Serviço até a entrega. Ela cobre três frentes. A Ordem de Serviço, com cliente, veículo, serviços, peças, insumos e orçamento. O acompanhamento, com seis status controlados pelo sistema e consulta do cliente pela API. E a gestão administrativa, com os cadastros, o controle de estoque e o tempo médio de execução.'),
    ('a03', slide(3), 'Antes de escrever código, modelamos o domínio com um Event Storming. Vamos ver o board no Miro.'),
    ('m01', img('miro-geral'), 'Este é o nosso board. Modelamos os três fluxos pedidos no enunciado: a criação da Ordem de Serviço, o acompanhamento e a gestão de peças e insumos. Como aqui passamos mais por cima, o board completo pode ser visto com calma, em tempo real, pelo link disponibilizado na entrega.'),
    ('m02', img('miro-legenda'), 'A legenda mostra a notação usada. Em laranja, os eventos de domínio, sempre no passado. Em azul, os comandos que os disparam. Em amarelo, os atores e os agregados. Em roxo, as políticas, que são as regras de negócio. Em verde, os read models. E em rosa, os sistemas externos.'),
    ('m03', img('miro-etapas'), 'Cada etapa foi organizada assim. No orçamento, por exemplo, o mecânico executa o comando Gerar Orçamento, que produz os eventos Orçamento Gerado e Peça Reservada. Uma política define a regra: se faltar peça, as reservas são liberadas e a ordem é encerrada.'),
    ('m04', img('miro-criacao'), 'Depois, ligamos as etapas em um fluxo. A criação da ordem começa na recepção, com o atendente, e passa pelo diagnóstico com o mecânico, até o orçamento, a execução e a entrega.'),
    ('m05', img('miro-acompanhamento'), 'No acompanhamento, cada evento muda o status da ordem, e cada mudança grava data e hora no histórico. É esse histórico que o cliente consulta pela API pública.'),
    ('m06', img('miro-pecas'), 'Na gestão de peças e insumos, o atendente cadastra, altera, registra entradas e inativa itens. Já a reserva, a liberação e a baixa acontecem ao longo do fluxo da ordem.'),
    ('a04', slide(4), 'Desse trabalho saíram quatro bounded contexts. Ordem de Serviço é o subdomínio principal, onde está o valor do negócio. Estoque e Cadastro são de suporte, e Autenticação é genérico. A Ordem de Serviço reserva e baixa peças no Estoque e avisa o cliente por e-mail. Cada contexto virou um módulo Gradle e um schema no banco.'),
    ('a05', slide(5), 'O agregado Ordem de Serviço protege as transições de status: uma transição inválida é recusada pelo próprio domínio. O Item de Estoque garante que o saldo nunca fica negativo. CPF, CNPJ e placa são objetos de valor: um valor inválido nem chega a existir no domínio. Cada fato importante vira um evento de domínio. E a linguagem ubíqua, documentada no repositório, liga cada termo do negócio à classe correspondente no código.'),
    ('a06', slide(6), 'A arquitetura é um monólito, como pede o enunciado: um único deployable. Por dentro, ele é modular, com um módulo por contexto, e cada módulo é organizado em camadas. A requisição entra pelo adapter, passa pelo caso de uso na camada de aplicação, onde ficam as transações, e chega ao domínio. O domínio não depende do Spring. Ele define portas, que os adapters de saída implementam com JPA e e-mail. As decisões estão registradas no documento de arquitetura e nos diagramas C4.'),
    ('a07', slide(7), 'Escolhemos PostgreSQL porque o fluxo da ordem exige transações fortes: reservar a peça e mudar o status acontecem juntos, ou nada acontece. Ele também garante a integridade, com CPF, CNPJ e placa únicos e o saldo validado no banco. O lock de linha impede que a última peça vá para duas ordens. E cada módulo tem o próprio schema, versionado com Flyway.'),
    ('a08', slide(8), 'Agora, vamos ver a solução funcionando: primeiro o ambiente e a documentação da API, depois a autenticação e o fluxo completo de uma Ordem de Serviço.'),
    ('a09', cartao('terminal', f'<h1>Ambiente em execução</h1><div class="met">docker compose -f docker/docker-compose.yml ps</div><pre>{html.escape(ps)}</pre>'), 'O ambiente completo sobe com um comando do Docker Compose. São três contêineres: a aplicação, o PostgreSQL e o Mailpit, que captura os e-mails em desenvolvimento. O passo a passo está no README.'),
    ('a10', img('swagger-lista'), 'A API está documentada no Swagger, separada por recurso, com a descrição de cada rota.'),
    ('a11', img('swagger-authorize'), 'As rotas administrativas exigem o token JWT, informado aqui, no botão Authorize.'),
    ('a12', img('swagger-execucao'), 'Com o token, executamos a listagem de ordens de serviço direto pelo Swagger e recebemos a resposta real da aplicação.'),
    ('a13', slide(9), 'Agora, as requisições na API, em ordem e com o banco zerado. Começamos pela autenticação.'),
]


def blocos_req(inicio, fim):
    for i in range(inicio, fim):
        pedido, resposta = FALAS_REQ[i]
        cenas.append((f'r{i + 1:02d}a', cartao_req(i + 1, reqs[i], False), pedido))
        cenas.append((f'r{i + 1:02d}b', cartao_req(i + 1, reqs[i], True), resposta))


blocos_req(0, 4)
cenas.append(('a14', slide(10), 'Estes são os seis status do enunciado. O status não é um campo que se edita: ele muda como resultado de cada ação no sistema. Vamos percorrer todos eles.'))
blocos_req(4, 14)
cenas.append(('a15', img('mailpit-orcamento'), 'O orçamento foi enviado por e-mail para a cliente. Em desenvolvimento, o Mailpit captura a mensagem. Ela traz o valor e um link exclusivo daquele orçamento.'))
blocos_req(14, 20)
cenas += [
    ('a16', slide(11), f'A qualidade é garantida por testes automatizados. São {TESTES} testes, sem falhas: unitários no domínio e nos casos de uso, de integração com um PostgreSQL real, via Testcontainers, e cenários BDD. O Jacoco exige no mínimo 80 por cento de cobertura de linhas no domínio e na aplicação, e todos os módulos ficam entre {FAIXA}. Abaixo de 80 por cento, o build falha.'),
    ('a17', cartao('build', f'<h1>Saída do build</h1><div class="met">./gradlew build</div><pre>{html.escape(build)}</pre>'), 'Aqui está o resultado do build, com os testes e a verificação de cobertura aprovados. O CI repete tudo isso a cada pull request.'),
    ('a18', slide(12), 'Por fim, a análise de vulnerabilidades. Analisamos a imagem Docker e as dependências com o Trivy. Não há vulnerabilidades HIGH ou CRITICAL em aberto. Restam dois riscos aceitos no spring-webmvc, documentados: eles dependem de recursos que esta API não usa.'),
    ('a19', cartao('trivy', f'<h1>docs/security/trivy-report.txt</h1><pre>{html.escape(trivy)}</pre>'), 'Este é o relatório, versionado no repositório. O código, a documentação DDD e o board estão nos links da entrega. Obrigado.'),
]

lista, total = [], 0.0
for nome, imagem, fala in cenas:
    aud = f'{W}/aud/{nome}.wav'
    falar(fala, aud, abertura=nome in ('a01', 'a02', 'a03'))
    info = subprocess.run(['afinfo', aud], capture_output=True, text=True).stdout
    dur = float(re.search(r'estimated duration: ([\d.]+)', info).group(1)) + 1.2
    total += dur
    seg = f'{W}/seg/{nome}.mp4'
    subprocess.run([FFMPEG, '-y', '-loglevel', 'error', '-loop', '1', '-framerate', '10', '-i', imagem, '-i', aud,
                    '-vf', 'scale=1600:900:force_original_aspect_ratio=decrease,pad=1600:900:(ow-iw)/2:(oh-ih)/2:color=white,format=yuv420p',
                    '-af', 'apad', '-t', f'{dur:.2f}', '-r', '10', '-c:v', 'libx264', '-tune', 'stillimage',
                    '-c:a', 'aac', '-ar', '44100', '-ac', '1', seg], check=True)
    lista.append(seg)

with open(f'{W}/lista.txt', 'w') as f:
    f.writelines(f"file '{s}'\n" for s in lista)
# Recodifica na junção: com -c copy, os players da Apple só enxergam parte da trilha de áudio.
subprocess.run([FFMPEG, '-y', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', f'{W}/lista.txt', '-r', '10',
                '-c:v', 'libx264', '-tune', 'stillimage', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '160k',
                '-ar', '48000', '-ac', '2', '-af', 'aresample=async=1:first_pts=0', '-movflags', '+faststart', str(SAIDA)],
               check=True)
print(f'TOTAL {int(total // 60)}:{int(total % 60):02d}', SAIDA)
