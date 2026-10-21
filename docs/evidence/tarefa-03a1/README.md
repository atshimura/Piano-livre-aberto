# Evidência geométrica da Tarefa 03A.1/20

Coleta nova sobre a HEAD `6435255d93d4cc782a54274851003b1c4f77f4c0`. Scripts documentais de diagnóstico, fora do aplicativo, dos testes e dos workflows; não constituem dependências Android.

- `geometry.json`: caixas de todos os **2.418 / 394 / 134 elementos-folha SVG**, ancestralidade/IDs, classificação nota/pausa pelo modelo VexFlow, limites dos 78 / 16 / 3 compassos das duas pautas, regras efetivas e arrays skyline/bottomline completos. Caixas `[xmin,ymin,xmax,ymax]` em coordenadas do SVG raiz.
- `summary.json`: estatísticas e restrição máxima por compasso, derivadas pelo `summarize.py`.
- `production-check.json`: execução adicional de `notation.html` inalterado, com as três fontes e sem eventos MIDI nativos; fit, CTM e logs. Não valida reconciliação MIDI. O upstream é servido no caminho esperado por interceptação exclusiva no navegador de diagnóstico, sem criar asset no projeto.
- `probe.cjs`: instancia o motor com as opções da prévia e lê geometria antes dos overlays. `production-check.cjs` executa o HTML existente e lê após fit/contexto. A criação inicial do render pelo motor precede a sonda, que não modifica viewBox, pautas, glifos ou regras.

Ambiente: Linux x86_64, Node/Playwright do runtime de ferramentas, Chrome for Testing **131.0.6778.204**, viewport **738×196 CSS px**, device scale factor padrão 1. Sem medidas raster, Android WebView/Chromium 74 ou aparelho físico.

Bundle: URL exata de `prepareOfflineNotation` em `app/build.gradle`: `https://cdn.jsdelivr.net/npm/opensheetmusicdisplay@1.9.8/build/opensheetmusicdisplay.min.js`. SHA256 em `geometry.json`. Foi usado upstream 1.9.8 **antes da transpilação esbuild chrome60** realizada pelo build Android; não foi extraído de APK. Não se afirma identidade binária com o bundle transpilado. Fontes de sistema/backend de skyline podem diferir entre ambientes. Nenhuma biblioteca foi incorporada ao app.

## Reprodução

Fornecer caminhos absolutos e instalações de browser/Playwright externas ao repositório. `PLAYWRIGHT_MODULE`, opcional, aponta para a instalação existente. Extrair as fontes, da raiz do projeto:

```sh
python3 - <<'PY'
import json,zipfile
from pathlib import Path
base=Path('PianoLivreAndroid/app/src/main/assets/scores')
scores={}
for name in ['gymnopedie-no-1-satie','ode-to-joy-ludwig-van-beethoven']:
    with zipfile.ZipFile(base/(name+'.mxl')) as z:
        member=next(n for n in z.namelist() if n.endswith(('.xml','.musicxml')) and not n.startswith('META-INF'))
        scores[name]=z.read(member).decode('utf-8-sig')
scores['engraving-context-change']=Path('PianoLivreAndroid/app/src/debug/assets/scores/engraving-context-change.musicxml').read_text()
Path('/tmp/piano-scores.json').write_text(json.dumps(scores))
PY
node docs/evidence/tarefa-03a1/probe.cjs /caminho/osmd.js /caminho/chrome /tmp/piano-scores.json /tmp/geometry.json
python3 docs/evidence/tarefa-03a1/summarize.py /tmp/geometry.json /tmp/summary.json
node docs/evidence/tarefa-03a1/production-check.cjs "$PWD" /caminho/osmd.js /caminho/chrome /tmp/piano-scores.json /tmp/production-check.json
```

Hashes das fontes XML e bundle estão registrados. IDs automáticos dependem da ordem: Gymnopédie → Ode → fixture. `sourceHead` identifica o código examinado, não o commit posterior de arquivamento.

## Conferência matemática e limites

Cada caixa local é transformada por `root.getScreenCTM().inverse().multiply(element.getScreenCTM())`, aplicada aos quatro cantos. `rootScreenCTM` distingue SVG de CSS; layout CSS tem arredondamentos. As dez linhas confirmam **10 unidades SVG por espaço de pauta**; uma unidade do modelo OSMD equivale a um espaço nesta configuração.

Para cada índice `j`, a restrição do motor subtrai do bottomline superior o mínimo entre zero e o skyline inferior em `j−6…j+6`; toma o máximo, soma `MinSkyBottomDistBetweenStaves` e aplica o piso `StaffHeight+MinimumStaffLineDistance`. Esse piso explica os 8 espaços entre primeiras linhas na Ode/fixture. Índice → `x=10*(staffModelX+j/SamplingUnit)`, com `SamplingUnit=3`; associação ao compasso por caixa SVG é aproximada nas bordas. Não identifica inequivocamente a entidade que gerou cada amostra de skyline.

A menor folga considera caixas musicais de pautas opostas com intervalos X sobrepostos. Inclui notas/pausas, modificadores, linhas suplementares, textos, ties, slurs e cunhas `vf-line`; exclui linhas de pauta, barras/conectores e brace. São **caixas retangulares sem expansão por stroke**, não distância entre pixels de tinta nem distância mínima editorial segura. Exemplo verificável: registros `i=908` e `i=1597` da Gymnopédie.

`vf-notehead` também contém pausas. A sonda cruza `sourceNote.isRestFlag` e ID de `vfnote[0]`; cabeças `semantic=pitched`: 469 / 81 / 19. Outras contagens são folhas, não eventos nem quantidade de ligaduras. Nenhum grupo `vf-beam` foi encontrado: a cobertura não valida vigas em repertório com figuras menores.

`getBBox()` padrão exclui stroke, antialiasing e sombras; caixas transformadas são envelopes. Não comprova margem segura de recorte. Numeração ultrapassa levemente o canvas da Ode/fixture, e hastes da Gymnopédie alcançam seu limite inferior. Tooltip, rail e anéis não entram na coleta primária; a leitura adicional do HTML preserva transformações, mas não audita colisões desses overlays.

## Tentativas de ambiente

Playwright estava instalado sem navegador. O instalador Chromium 151 recebeu arquivo ZIP inválido; URL alternativa do Chromium 1169 retornou `Site Unavailable`. `apt-get update` inicialmente falhou ao trocar usuário; com sandbox root obteve índices, sem instalar Chromium. A alternativa efetiva foi Chrome for Testing 131 obtido do armazenamento oficial Google e executado somente no diagnóstico. Nenhuma tentativa alterou o projeto. A sonda arquivada foi executada novamente e conferida contra a coleta inicial.
