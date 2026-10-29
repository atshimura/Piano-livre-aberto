# Tela Estudar — Prompt 2

Implementação na branch `feat/tela-estudar-piano-livre`, dependente do Prompt 1/PR #71. Preparação documental no PR #72; imagem original em [referência aprovada](references/tela-estudar-aprovada.png), incorporada por `22e193b8f31e8f38ac63b7f6cb6d8561c0c29338`. SHA-256 do PNG: `f6cafcab53a80bd9cccbb111d06e6f6beea8ef4a64ee38c20c9c8125b1bf5c7d`; o blob remoto foi conferido com o original local. A referência contém três vistas e foi inspecionada antes da implementação.

## Estrutura e decisões

- Cabeçalho azul-marinho único: Biblioteca, título real, Partitura Livre, andamento/velocidade/posição, perfil, opções e identificação USB-MIDI. Sem compositor, nível ou categoria inventados. O valor percentual representa velocidade, não zoom. Em telas compactas, ações secundárias ficam no menu acessível e o título inclui andamento/posição.
- Faixa MODO/MÃOS com seleções azuis exclusivas e sem inferir mão pela altura. Em telas estreitas a faixa permite rolagem horizontal mantendo alvos de 44 dp.
- Pauta gerada pelos renderizadores reais e barra inferior permanente com Início, Iniciar/Pausar, Parar, velocidade e Trecho. Ícones de Início/Parar têm nomes de acessibilidade no celular.
- Teclado guia removido do Estudar, incluindo view, botão, handlers e dependências de altura. A preferência legada continua sendo lida/persistida e incluída no backup; o fallback nativo do Estudar não a desenha. Componentes do Laboratório e motor musical permanecem.
- Seleção automática de renderizador: OSMD normal, temporal após falha real de OSMD e esquemático nativo apenas se o temporal também falhar, com aviso explícito. A representação esquemática não comprova fidelidade editorial. Partitura Livre permanece separada. O caminho temporal de teste só pode ser solicitado por intent em APK depurável, sem controle manual na interface de produção.
- `StudyChromeState` recolhe cabeçalho e modos/mãos uma vez por início efetivo de transporte/Espera; pausa/parada restaura. Toque concluído na pauta revela sem consumir a seleção de notas/trechos; toques sucessivos são idempotentes. Não há timer de reocultação. Ao iniciar novamente, um novo ciclo recolhe as barras. A transição não escreve no relógio, MIDI, posição, modo, mãos, velocidade ou loop.
- D-007/D-008 e AGENTS registram a decisão posterior de remoção do guia. Versão de validação: `0.9.51-open-beta` / code `60`, pois o APK mudou. Sem merge ou Release.

## Validação e medições

O workflow Android compila a base pública fixa do PR #71 (`32b83a3b6d37a831900de4bafd5ec8cbd406d0d8`) e mede no próprio emulador a altura da WebView em 1280 × 720 e 1920 × 1080, densidade 240. Remove somente essa instalação descartável antes do ensaio já existente de upgrade da 0.9.46 para o candidato. Assim, preserva a cobertura anterior de atualização e não usa dados reais.

`emulator_study_layout.py` mede o candidato nos mesmos viewports, exige ganho normal sobre a base e mais área com barras ocultas, restauração exata da altura e enquadramento uniforme das duas pautas, sem rolagem vertical. Produz `piano-prompt02-comparison.json`, capturas de antes e três capturas reais por viewport (`piano-03d-prompt02-{phone,tablet}-{normal,playing,restored}.png`). A suíte mantém inspeção de música longa, extremos, simultaneidade, notação, mapa gráfico/MIDI, loop e lifecycle.

Os testes antigos de guia/seletor manual passam a verificar ausência dos controles removidos, ocultação/restauração, persistência legada e renderização automática. A cobertura temporal continua por fixture debug; falha real do OSMD exige fallback automático. `MidiSessionSelfTest` exercita notas extras e ataques completos em Ambas/Esquerda/Direita sem guia e preserva o MusicXML. `StudyChromeStateTest` cobre início, ticks, toque repetido, pausa e retomada sem timer.

Resultados, SHA efetivamente testado, medidas observadas, links de workflows, capturas e APK serão consolidados após os checks desta implementação. Não tratar evidências do Prompt 1 como validação do código novo.

## Limitações e continuidade

O desenho da pauta respeita o MusicXML real, portanto não copia notas/diagramação da imagem estática. Ícones/tipografia nativos e menus compactos são adaptações para toque e fidelidade dos dados. O nome Yamaha P-45 não é fixado: só aparece se obtido do Android após abertura real; sem conexão, USB-MIDI.

Aceite físico pendente no Yamaha P-45 (USB, nome, reconexão, pedal, notas presas), Realme 7 5G e Lenovo Tab M9 (legibilidade, fluidez, fonte, toque e lifecycle). CI e MIDI sintético não substituem os aparelhos. Prompt 3 consolidará casos limítrofes e comportamento MIDI entre telas; não foi iniciado. V1 continua não concluída. Integração/publicação pertencem à etapa posterior aprovada.
