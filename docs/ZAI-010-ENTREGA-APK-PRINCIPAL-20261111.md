# ZAI-010 — Entrega do Laboratório Musical, Criação contextual, notas sem pontos e APK 0.9.71

**Data:** 11/11/2026. **Status:** implementação integrada em `zai/lab` pelo PR #138; APK principal 0.9.71/code80 assinado, verificado e disponibilizado em `zai/downloads`. **Não** integrado à `main`; sem nova GitHub Release, tag ou publicação Play Store. Testes físicos pendentes.

## Funcionalidades entregues

- **Laboratório Musical:** nova tela inicial em paisagem com três opções: **Descobrir** (funcional), **Aprender** (identificado **Em breve**, desabilitado) e **Criação** (funcional); navegação de retorno para Biblioteca e entre módulos.
- **Criação Musical:** painel em duas colunas, esquerda com nova gravação e controles MIDI, direita com gravações recentes, revisão, **Minhas criações**, busca, ordenação, filtros e detalhes contextuais. Botão **Voltar às gravações recentes** no painel direito. Status USB-MIDI compacto e botão de diagnóstico real. Reaproveita recursos locais de gravação, recuperação e persistência sem excluir projetos ao navegar.
- **Partituras com figuras simples:** a nota escrita nunca tem ponto de aumento: semibreve, mínima, semínima, colcheia ou semicolcheia. Uma cabeça de nota por Note On; sem ligaduras/staccato automáticos; pausas invisíveis mas com duração no MusicXML. O comprimento escrito pode ser **mais curto ou diferente da duração física exata** de uma nota sustentada, pois a figura é aproximada sem ponto/ligadura. **O MIDI `.plc` mantém eventos Note On/Off e duração original**, inclusive acordes, sobreposições e notas de cinco minutos. Derivados anteriores requerem regeneração explícita; nenhum `.pls` é substituído silenciosamente.
- Código no [PR #138](https://github.com/atshimura/Piano-livre-aberto/pull/138), merge `abd3267df4f70cd5c6f82236ecd83b95e91402b0`; plano em [ZAI-010](ZAI-010-LABORATORIO-CRIACAO-PAINEL-FIGURAS.md).

## Evidências de teste e build

- **PR #138 — CI de verificação:** [GitHub Actions #42935121239](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42935121239), **SUCCESS**, hash de código `25ce0cae7fe20e27dfc71b3dc89d23e33b6966e1`: contratos Python, JVM musical, compilação debug Android isolada e testes de visualização OSMD em Chromium.
- **APK principal:** [GitHub Actions #42996420673](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42996420673), **SUCCESS**, commit `bdfbe71cfc73b481508abce443a8d5f8b2b0ec4e`. Auditorias e contratos Python aprovados, incluindo ZAI-010. **11 suítes JVM, 1.178 verificações aprovadas**: 86 ritmo, 29 XML, 27 store, 45 corpus, 25 MIDI real, 62 acordes, 45 S1, 40 sem ligaduras, 710 pausas, 59 notação básica, 50 figuras simples. Release assinado gerado via Secrets existentes.
- **Verificação do APK:** pacote `br.org.pianolivre.opensource`, rótulo `Piano Livre`, `versionCode=80`, `versionName=0.9.71-open-beta`, `minSdk=26`, `targetSdk=36`, APK não depurável, sem permissão `INTERNET`; certificado público permanente com fingerprint SHA-256 `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`.
- **Publicação para download:** [GitHub Actions #42996929555](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42996929555), **SUCCESS**: recuperou **o mesmo artefato assinado**, comparou SHA-256 antes/depois de registrar o APK em `zai/downloads`, e validou novamente pacote, versão, ausência de INTERNET/debug e certificado. Commit de publicação `235c97ecbf95f7fbea5a201474bfc39d97fd734a`.

## APK para instalação

- Versão: **0.9.71-open-beta** (code **80**), `PianoLivre-Principal-0.9.71-open-beta-assinado.apk`, **800.484 bytes**.
- **SHA-256:** `e392e16047a7c8f02d18fbabeb43746f7414b213231d97efd04e40014aa9cc87`.
- **Download direto imutável (GitHub raw):** https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/235c97ecbf95f7fbea5a201474bfc39d97fd734a/apks/principal/PianoLivre-Principal-0.9.71-open-beta-assinado.apk
- **Página GitHub:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.71-open-beta-assinado.apk
- **Comprovante:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.71-open-beta-SHA256.txt

`gradle.properties` da linha principal permanece na versão da `main`. A identidade e versão **somente deste APK** são parâmetros explícitos do workflow dedicado; nenhuma Release oficial 0.9.71 foi publicada.

## Limitações e próximos passos

1. Testar a atualização por cima da 0.9.70/code79 **sem desinstalar**, preferencialmente após backup conferido, e conferir preservação de projetos `.plc`, `.pls`, perfis e biblioteca.
2. Testar as três opções do Laboratório, o painel de gravação/revisão, Minhas criações, filtros e a confirmação de saída de tomada não salva em paisagem.
3. Conectar o Yamaha P-45 ao Realme 7 5G e/ou Lenovo Tab M9, gerar partitura de notas sustentadas e acordes, conferir visualmente **ausência de pontos junto às notas** e duração original intacta na gravação/reprodução MIDI.
4. **Não foram realizados testes físicos nem suíte integral de emulador** nesta entrega; Chromium, JVM e build Android são evidências técnicas, mas não comprovam usabilidade em hardware nem compatibilidade universal MusicXML.
5. Integração ZAi → `main`, criação de Release e publicação na Play Store permanecem fora do escopo e exigem decisão/autorização separada.

Não acessar nem modificar o repositório histórico privado. GPL-3.0-only, direitos de partituras, ausência de telemetria e preservação de dados mantidos.
