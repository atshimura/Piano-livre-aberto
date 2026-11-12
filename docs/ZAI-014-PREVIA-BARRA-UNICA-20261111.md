# ZAI-014 — Prévia da Criação: barra única e máximo espaço para a partitura

**Data:** 11/11/2026. **Aprovação:** o mantenedor aprovou substituir as faixas da prévia ZAI-013 por **uma única barra superior** com Voltar, título, andamento, zoom, Informações e Salvar. A barra inferior deve desaparecer. A prioridade é a legibilidade da partitura, especialmente a última pauta, em Realme 7 5G e Lenovo Tab M9. Base `zai/lab` após ZAI-013/0.9.74.

## Diagnóstico e decisão visual

A ZAI-013 evita que a última pauta seja *encoberta*, mas consome altura com três áreas persistentes: cabeçalho de 58dp, barra de tempo e zoom de 60dp e rodapé de observações MIDI + ações (pelo menos ~80dp). A área musical restante fica pequena em telefone horizontal.

**Layout aprovado:** uma **barra horizontal fixa de 56dp** sobre todo o espaço restante. À esquerda, Voltar com acessibilidade e título truncável; em seguida BPM, A−, A+, ajuste à largura e ícone `ⓘ`; no extremo direito, uma única ação primária. O botão primário assume os seguintes estados, sem ocultar funções:
- **Sem partitura salva:** `Salvar` → mantém verificação de compatibilidade com Estudar e persistência original.
- **Salva e ainda fora da Biblioteca:** `+ Biblioteca` → vincula a derivação por referência, sem duplicá-la.
- **Salva e já na Biblioteca:** `✓ Na Biblioteca` (estado identificado, não aciona gravação).
- **Voltar à composição:** botão superior esquerdo, mesmo fluxo anterior.

A informação de observações MIDI não desaparece: o ícone `ⓘ` abre caixa nativa acessível contendo contagem de avisos, explicação da notação simplificada e preservação da gravação MIDI original. Andamento provisório/estimado recebe rótulo de acessibilidade. Fundo da barra azul-marinho, botões secundários azul-acinzentados de alto contraste e botão primário azul-vivo, 48dp de alvo tátil, ripple, foco e rótulos acessíveis.

**Responsividade horizontal:** título recebe apenas a largura livre, com reticências e descrição acessível; em janelas estreitas, os controles de zoom/BPM podem rolar **horizontalmente dentro da própria barra**, sem deslocar Voltar e o botão primário, sempre visíveis. Não criar barra extra nem rolagem vertical da tela. O conteúdo musical ocupa todo o espaço restante por `LayoutParams(-1,0,1)`.

## Segurança e critérios de aceite

- Nunca esconder a última pauta atrás da barra: `NotationView` é filho da área musical abaixo da barra e mantém a margem de segurança de **64 CSS px** do OSMD da ZAI-013.
- Zoom 0,5–2,0 preservado, ajuste à largura continua usando `setCreationZoom(1)`; tempo apenas informativo, não altera MusicXML.
- **Sem modificações** nos arquivos `.plc`/`.pls`, geração MusicXML, backup, perfis, bibliotecas, licenças, dados ou assinatura.
- Manter telas de loading, derivação antiga, regeneração e falha acessíveis, com barra Voltar.
- Testes Python de contrato: apenas uma barra persistente, sem `scoreFooter`, ações condicionais intactas, information dialog; contratos ZAI-013 adaptados.
- Testes de geometria Android em emulador: confirmar BPM, zoom, informações, salvar e Voltar na **mesma linha**, dentro do visor, sem rodapé, pauta começando abaixo da barra e alcançável até a borda inferior, interação de zoom/ajuste e consulta às observações MIDI; incluir cenário compacto.
- Repetir testes OSMD Chromium em 3 tamanhos × 3 zooms comprovando a última pauta acessível; build debug isolado, testes musicais e APK release com assinatura permanente por GitHub Actions Secrets.

## Entrega

Fluxo por branches e PRs **exclusivamente em `zai/lab`**; `main` e o repositório privado histórico permanecem intocados. Compilar APK principal incremental **0.9.75-open-beta / versionCode 84**, pacote `br.org.pianolivre.opensource`, sem permissão INTERNET e não depurável. Publicar os mesmos bytes em `zai/downloads` via workflow próprio, conferir certificado, identidade e SHA-256 e registrar os resultados/limitações. **Não** publicar tag/Release ou Play Store. Teste em emulador não equivale à validação nos aparelhos físicos.
