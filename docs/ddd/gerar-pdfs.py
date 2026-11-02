"""Gera event-storming.pdf e diagramas.pdf a partir dos .md desta pasta (Chrome headless + marked + mermaid).

Uso: python3 docs/ddd/gerar-pdfs.py   (precisa do Google Chrome e de acesso à internet para o CDN)
"""
import json
import pathlib
import re
import subprocess
import sys
import tempfile

PASTA = pathlib.Path(__file__).resolve().parent
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" if sys.platform == "darwin" else "google-chrome"

HTML = """<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><base href="{base}">
<style>
  @page {{ size: A4 landscape; margin: 12mm; }}
  body {{ font-family: -apple-system, "Segoe UI", Helvetica, Arial, sans-serif; font-size: 11px; color: #1a1a1a; }}
  h1 {{ font-size: 22px; border-bottom: 2px solid #333; padding-bottom: 4px; }}
  h2 {{ font-size: 17px; margin-top: 22px; break-before: page; }}
  h3 {{ font-size: 13px; }}
  h2, h3, p {{ break-after: avoid; }}
  table {{ border-collapse: collapse; width: 100%; margin: 8px 0; break-inside: auto; }}
  th, td {{ border: 1px solid #bbb; padding: 3px 6px; text-align: left; vertical-align: top; }}
  th {{ background: #eee; }}
  tr {{ break-inside: avoid; }}
  code {{ font-size: 10px; background: #f3f3f3; padding: 0 2px; }}
  .mermaid {{ text-align: center; break-inside: avoid; margin: 10px 0; }}
  .mermaid svg {{ max-width: 100%; max-height: 140mm; height: auto; }}
  img {{ max-width: 100%; max-height: 135mm; display: block; margin: 0 auto; }}
</style></head><body><div id="c"></div>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script>
<script>
  document.getElementById('c').innerHTML = marked.parse({md});
  document.querySelectorAll('code.language-mermaid').forEach(function (el) {{
    var div = document.createElement('div');
    div.className = 'mermaid';
    div.textContent = el.textContent;
    el.parentElement.replaceWith(div);
  }});
  mermaid.initialize({{ startOnLoad: false, theme: 'default', flowchart: {{ useMaxWidth: true }} }});
  mermaid.run();
</script></body></html>"""


def incluir_mmd(md: str, origem: pathlib.Path) -> str:
    # Um link sozinho na linha para um .mmd vira o próprio diagrama no PDF (no GitHub continua link).
    def troca(m):
        titulo, caminho = m.group(1), (origem.parent / m.group(2)).resolve()
        return f"### {titulo}\n\n```mermaid\n{caminho.read_text(encoding='utf-8')}\n```"

    return re.sub(r"^\[([^\]]+)\]\(([^)]+\.mmd)\)$", troca, md, flags=re.M)


def gerar(nome: str) -> None:
    fonte = PASTA / f"{nome}.md"
    md = incluir_mmd(fonte.read_text(encoding="utf-8"), fonte)
    html = HTML.format(base=PASTA.as_uri() + "/", md=json.dumps(md))
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False, encoding="utf-8") as f:
        f.write(html)
    saida = PASTA / f"{nome}.pdf"
    subprocess.run(
        [CHROME, "--headless=new", "--disable-gpu", "--no-pdf-header-footer", "--allow-file-access-from-files",
         "--virtual-time-budget=20000", f"--print-to-pdf={saida}", f.name],
        check=True, capture_output=True,
    )
    print(saida)


if __name__ == "__main__":
    for nome in ("event-storming", "diagramas"):
        gerar(nome)
