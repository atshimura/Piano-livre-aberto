# B1 — evidência da janela horizontal

Extraída do artifact `adaptive-study-corpus` do [build 39828730389](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39828730389), aprovado no head `1e8ca06bae8270ad66e8586d5ec731b81ac818ed`. Checkout de integração do PR: `a325e57357ae458ab4ca5142840ac8665dda343b`. [Emulador do mesmo head](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39828730333): aprovado.

`measurements.json` reúne os resultados por arquivo/dimensão, comparações controladas, 376 posições nos dois sentidos e resize. As capturas são de Chromium desktop; não representam aparelhos reais. A fonte musical e os eventos são os mesmos antes/depois. O corpus integral e todas as capturas permanecem no artifact. Novas execuções registram separadamente `sourceSha` (head) e `checkoutSha` (merge de teste), e capturam também os extremos da janela. Os workflows da HEAD final estão no PR #51; não atribuir as capturas desta pasta a outro commit.

Gymnopédie melhora na abertura de 4,85313 para 5,40241 CSS px, mas chega novamente a 4,85294 em regiões extremas. O alerta abaixo de 6 px permanece. Não é aceite editorial, físico ou conclusão da B1. Ver [documento 59](../../../59-ESTUDO-ADAPTAVEL.md).


## Continuação de 22/10/2026 — roteiro reproduzível

O arquivo de teste StudyProfileBrowserTest.js gera 12 capturas novas da Gymnopédie (abertura, meio, extremo em torno do beat 181 e final), em 738×196, 738×351 e 883×409 CSS px. Os campos medidos são escala, espaçamento, câmera, viewBox, dimensões pintadas, recorte, scroll vertical, mapeamento e contexto. O relatório se chama b1-viewport-comparison.json no artifact adaptive-study-corpus. A fixture engraving-context-change.musicxml produz capturas de ida e volta entre compassos 1–3 em tela baixa e tablet, mais b1-context-transitions.json.

IMPORTANTE: as novas PNG/JSON são geradas no artifact do workflow; não estão automaticamente incorporadas ao Git. O histórico durável nesta pasta é do SHA anterior e serve como linha de base, não prova da revisão. O script gerador e o presente índice ficam no repositório. A linha de base da Gymnopédie tinha 4,85294–5,40241 CSS px em 738×196, 8,85439 em 738×351 e 10,35163 em 883×409. Novos números precisam de evidência da CI do mesmo SHA e de inspeção editorial; sem ensaio físico.

### Evidência medida da reserva do contexto fixo (SHA b8ed11e, build 39875463421)

O teste da fixture `engraving-context-change.musicxml` encontrou na implementação anterior uma faixa fixa que podia ocupar 168,94 CSS px enquanto o cursor permanecia próximo de 167 px no terceiro compasso. A revisão passou a reservar, antes do planejamento de janela e cursor, a **largura máxima do contexto efetivamente gravado** na partitura. No Chromium 738×196 a faixa mede 168,94 CSS px e o cursor está em 180,94 px, com 12 px livres e sem recorte nas transições 1→2→3→2→1. No tablet 883×409, faixa 360,34 px e cursor 372,34 px, igualmente com 12 px de folga. Ambos preservam a origem vertical. Isso demonstra ausência da colisão detectada nessa fixture, não prova legibilidade editorial universal.

A nova rodada de 27 arquivos, incluindo Gymnopédie, corpus nativo e fallback temporal, passou no [build 39875463421](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39875463421) do código `b8ed11e63f54d83afc64cdfeb2c8848b2fc2bc46`. As 12 capturas comparáveis da Gymnopédie mantiveram os espaçamentos 5,40241 / 5,16247 / **4,85294** / 5,03093 CSS px (abertura/meio/extremo/final) em 738×196; as telas 738×351 e 883×409 mediram 8,85439 e 10,35163 em todas as regiões. Cortes detectados: zero; rolagem vertical: zero; mapeamento: 469/469. A mudança no extremo baixo segue **insuficiente**, e transições da escala variável, bem como validação real, continuam pendentes. Ver [relatório versionado](comparison-validated-2026-10-22.json) e o artifact do run correspondente. Não atribuir esta medição a outro SHA nem ao hardware.
