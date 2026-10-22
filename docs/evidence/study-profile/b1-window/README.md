# B1 — evidência da janela horizontal

Extraída do artifact `adaptive-study-corpus` do [build 39828730389](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39828730389), aprovado no head `1e8ca06bae8270ad66e8586d5ec731b81ac818ed`. Checkout de integração do PR: `a325e57357ae458ab4ca5142840ac8665dda343b`. [Emulador do mesmo head](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39828730333): aprovado.

`measurements.json` reúne os resultados por arquivo/dimensão, comparações controladas, 376 posições nos dois sentidos e resize. As capturas são de Chromium desktop; não representam aparelhos reais. A fonte musical e os eventos são os mesmos antes/depois. O corpus integral e todas as capturas permanecem no artifact. Novas execuções registram separadamente `sourceSha` (head) e `checkoutSha` (merge de teste), e capturam também os extremos da janela. Os workflows da HEAD final estão no PR #51; não atribuir as capturas desta pasta a outro commit.

Gymnopédie melhora na abertura de 4,85313 para 5,40241 CSS px, mas chega novamente a 4,85294 em regiões extremas. O alerta abaixo de 6 px permanece. Não é aceite editorial, físico ou conclusão da B1. Ver [documento 59](../../../59-ESTUDO-ADAPTAVEL.md).
