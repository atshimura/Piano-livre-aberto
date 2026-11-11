# ZAI-013 — Prévia de partitura: última pauta integralmente visível e zoom no topo

**Data:** 11/11/2026. **Estado inicial:** proposta visual aprovada pelo mantenedor após inspeção no Realme 7 5G da prévia da Criação; versão anterior 0.9.73-open-beta/code82. **Base:** `zai/lab`. `main` protegida e repositório histórico privado intocados. Usar PRs, GitHub Actions Secrets permanentes e GPL-3.0-only.

## Problema reproduzível e causa

Na tela de revisão de partitura da Criação, a última linha musical aparece encoberta/atrás dos controles de zoom, mesmo após os ajustes responsivos gerais. O código em `CreationScorePreviewScreen.renderPreview` monta o zoom **dentro do mesmo cartão da pauta**, logo após o WebView, e reserva altura fixa `Math.max(dp(210), displayMetrics.heightPixels*0.65f)` ao WebView. A prévia toda ainda fica dentro de um `ScrollView` Android. Portanto, há concorrência entre rolagem externa, rolagem vertical interna do OSMD e controles abaixo do visor; a altura fixa não utiliza a altura realmente disponível.

## Proposta visual aprovada

1. Cabeçalho azul-marinho com `← Voltar` e nome da composição, como hoje.
2. **Barra de ferramentas superior**, abaixo do cabeçalho: andamento em BPM (indicando quando provisório), `A−`, `A+` e `Ajustar à largura`, em linha única quando houver largura; estilos em cápsulas/segmentos arredondados, boa hierarquia, contraste, espaçamento, toque ≥48dp e tratamento de escala de fonte/estreitamento.
3. **Área exclusiva da partitura**, em cartão branco amplo e com borda arredondada, ocupando a altura livre da janela; a partitura inteira continua navegável por rolagem **vertical do próprio visualizador**. O último sistema/pauta recebe **margem inferior de segurança**, de modo que todas as linhas e notas possam entrar no viewport sem colisão com controles.
4. Mensagem resumida de preservação MIDI abaixo da partitura, quando aplicável; ações `Voltar à composição` (secundário) e `Salvar partitura` (primário) alinhadas na faixa inferior, responsivas. Quando a partitura já estiver salva, preservar `Adicionar à Biblioteca`; sem remover fluxos existentes.
5. Quando a janela for realmente baixa ou fontes aumentadas exigirem, priorizar leitura e acessibilidade: controles acessíveis, área musical com altura mínima e scroll próprio; sem rolagem vertical global que empurre o topo para fora.

## Plano de execução

- **P13-0:** ler `AGENTS.md`, continuidade e fonte atual da prévia, OSMD, testes e workflows; registrar este documento.
- **P13-1:** reestruturar o layout Java da prévia em cabeçalho, barra de tempo/zoom, viewport musical expandido e rodapé de ações. Remover zoom do cartão. Modernizar estados normal/primário, foco, bordas, toque e feedback sem novos recursos externos.
- **P13-2:** tornar o fim do SVG rolável no visualizador OSMD `creation-page`, com margem inferior suficiente e independente do zoom, sem tocar nos modos Estudar, Descobrir ou importação MIDI. Garantir zoom funcional e renderização com largura adequada.
- **P13-3:** adicionar testes de regressão de contrato Python (posição dos botões, topologia da tela, preservação dos comandos), teste Chromium OSMD (última nota dentro do viewport após ir ao fim em vários zooms/alturas) e, se disponível, roteiro de emulador com identificação de controles/viewport e scroll. Executar CI e corrigir até aprovar.
- **P13-4:** integrar funcionalidade somente à `zai/lab` via PR após todos os checks exigidos.
- **P13-5:** compilar APK principal incremental **0.9.74-open-beta/code83**, pacote `br.org.pianolivre.opensource`, certificado público permanente via Secrets, release não depurável sem INTERNET. Publicar **os mesmos bytes** em `zai/downloads` e conferir SHA-256, sem tag/Release nem alteração da `main`.
- **P13-6:** atualizar `ZAI-CONTINUIDADE.md` e índice com PRs, CI, hash, distribuição e limitações.

## Invariantes e critérios de aceite

- **Nenhuma alteração do MIDI original** (`.plc`), partitura salva (`.pls`), geração de MusicXML, ritmo, duas pautas, backups ou catálogo/licenças. O visualizador recebe a mesma partitura.
- `A−`, `A+`, `Ajustar à largura` continuam chamando `setCreationZoom` com os mesmos limites 0.5–2.
- Sem ações sobrepostas ao SVG; navegação até a **última pauta por scroll**, com margem visível abaixo da nota final.
- `Salvar partitura`, `Voltar à composição`, `Adicionar à Biblioteca`, reuso da derivada, regeneração e compatibilidade com Estudar preservados.
- Testes JVM/Chromium/Android não se equiparam ao teste real no Realme 7 5G e no Lenovo Tab M9, a realizar posteriormente.
