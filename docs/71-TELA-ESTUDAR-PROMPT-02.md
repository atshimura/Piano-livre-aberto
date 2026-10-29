# Tela Estudar — Prompt 2

Entrega na branch `feat/tela-estudar-piano-livre`, no [PR #73](https://github.com/atshimura/Piano-livre-aberto/pull/73), dependente do Prompt 1/PR #71. O registro de conclusão técnica, com SHA final e resultado dos checks desse HEAD, fica na descrição e no comentário de fechamento do PR. Esta entrega permanece separada da `main`, sem Release oficial. Preparação documental no [PR #72](https://github.com/atshimura/Piano-livre-aberto/pull/72); imagem original em [referência aprovada](references/tela-estudar-aprovada.png), incorporada por `22e193b8f31e8f38ac63b7f6cb6d8561c0c29338`. SHA-256 do PNG: `f6cafcab53a80bd9cccbb111d06e6f6beea8ef4a64ee38c20c9c8125b1bf5c7d`; o blob remoto foi conferido com o original local. A referência contém três vistas e foi inspecionada antes da implementação.

## Estrutura e decisões

- Cabeçalho azul-marinho único: Biblioteca, título real, Partitura Livre, andamento/velocidade/posição, perfil, opções e identificação USB-MIDI. Sem compositor, nível ou categoria inventados. O valor percentual representa velocidade, não zoom. Em telas compactas, ações secundárias ficam no menu acessível e o título inclui andamento/posição.
- Faixa MODO/MÃOS com seleções azuis exclusivas e sem inferir mão pela altura. Em telas estreitas a faixa permite rolagem horizontal mantendo alvos de 44 dp.
- Pauta gerada pelos renderizadores reais e barra inferior permanente com Início, Iniciar/Pausar, Parar, velocidade e Trecho. Ícones de Início/Parar têm nomes de acessibilidade no celular.
- Teclado guia removido do Estudar, incluindo view, botão, handlers e dependências de altura. A preferência legada continua sendo lida/persistida e incluída no backup; o fallback nativo do Estudar não a desenha. Componentes do Laboratório e motor musical permanecem.
- Seleção automática de renderizador: OSMD normal, temporal após falha real de OSMD e esquemático nativo apenas se o temporal também falhar, com aviso explícito. A representação esquemática não comprova fidelidade editorial. Partitura Livre permanece separada. O caminho temporal de teste só pode ser solicitado por intent em APK depurável, sem controle manual na interface de produção.
- `StudyChromeState` recolhe cabeçalho e modos/mãos uma vez por início efetivo de transporte/Espera; pausa/parada restaura. Toque concluído na pauta revela sem consumir a seleção de notas/trechos; toques sucessivos são idempotentes. Não há timer de reocultação. Ao iniciar novamente, um novo ciclo recolhe as barras. A transição não escreve no relógio, MIDI, posição, modo, mãos, velocidade ou loop. No Espera, o botão de execução também permite interromper a preparação/espera, preservando a posição.
- D-007/D-008 e AGENTS registram a decisão posterior de remoção do guia. Versão de validação: `0.9.51-open-beta` / code `60`, pois o APK mudou. Sem merge ou Release.

## Validação e medições

O workflow Android compila a base pública fixa do PR #71 (`32b83a3b6d37a831900de4bafd5ec8cbd406d0d8`) e mede no próprio emulador a altura da WebView em 1280 × 720 e 1920 × 1080, densidade 240. Remove somente essa instalação descartável antes do ensaio já existente de upgrade da 0.9.46 para o candidato. Assim, preserva a cobertura anterior de atualização e não usa dados reais.

A suíte ampliada inclui duas compilações de bases públicas e os cenários novos antes das jornadas longas. O limite global do job passa a 60 minutos para acomodar essa cobertura; prazos e asserções dos testes musicais permanecem. Medições em execução usam limites reais das views após o layout completo, com diagnóstico somente no APK depurável, pois UiAutomator exige ociosidade e pode rejeitar a árvore durante animação. As capturas continuam sendo screenshots reais e o transporte não é congelado para medir. O ensaio de release exige que esses diagnósticos e intents de falha não sejam ativados no APK sem depuração.

## Correções identificadas durante a validação

- Origem gráfica estabilizada no início da cronologia: resize durante reprodução não pode redefinir a margem a partir do beat corrente e deslocar o alvo original ao voltar ao início. A regressão toca a mesma nota nas coordenadas anteriores depois de restaurar a altura e reposicionar no beat zero.
- Primeiro desenho aguarda dimensões nativas e CSS válidas. Antes, um carregamento podia produzir o primeiro mapa gráfico com largura/altura zero e corrigi-lo apenas no resize seguinte. O agendamento usa eventos de layout e próximo quadro, sem temporizador pedagógico nem alteração do relógio musical.
- Tinta da chave de agrupamento acomodada dentro da margem esquerda do contexto fixo, preservando claves, notas e eventos MIDI.
- Roteiros de navegação passam a reconhecer o nome de acessibilidade do botão compacto e a fechar o seletor de perfil sem uma segunda volta que sairia do Estudar. Leituras de log do teste de comparação são limitadas ao processo da instalação atual, separadas das bases de teste.
- Avisos de revisão de leitura apontam para Partitura Livre, sem prometer um seletor manual removido. Seleções e botão principal usam azul da referência, com grupos centralizados quando há largura.

`emulator_study_layout.py` mede o candidato nos mesmos viewports, exige ganho normal sobre a base e mais área com barras ocultas, restauração exata da altura e enquadramento uniforme das duas pautas, sem rolagem vertical. Produz `piano-prompt02-comparison.json`, capturas de antes e três capturas reais por viewport (`piano-03d-prompt02-{phone,tablet}-{normal,playing,restored}.png`). A suíte mantém inspeção de música longa, extremos, simultaneidade, notação, mapa gráfico/MIDI, loop e lifecycle.

Os testes antigos de guia/seletor manual passam a verificar ausência dos controles removidos, ocultação/restauração, persistência legada e renderização automática. A cobertura temporal continua por fixture debug; falha real do OSMD exige fallback automático. `MidiSessionSelfTest` exercita notas extras e ataques completos em Ambas/Esquerda/Direita sem guia e preserva o MusicXML. `StudyChromeStateTest` cobre início, ticks, toque repetido, pausa e retomada sem timer.

### Medições e prova visual

Alturas reais observadas no emulador, com densidade 240, usando a mesma configuração para a base do PR #71 e para a interface nova:

| Viewport | Antes (PR #71) | Normal | Em execução, barras ocultas | Controles restaurados | Ganho normal / execução |
|---|---:|---:|---:|---:|---:|
| Celular, 1280 × 720 | 388 px | 417 px | 567 px | 417 px | +29 / +179 px |
| Tablet, 1920 × 1080 | 748 px | 777 px | 927 px | 777 px | +29 / +179 px |

O ganho corresponde a aproximadamente 19,3 dp no estado normal e 119,3 dp em execução, em relação à base. A barra inferior conserva seus limites; a altura da pauta retorna exatamente ao valor normal. A escala do SVG é uniforme, as duas pautas permanecem no envelope disponível e a verificação não aceita rolagem vertical para esconder recortes. O contexto fixo/cursor adapta sua geometria ao espaço e à escala, preservando o beat musical e a origem que permite retornar aos alvos anteriores.

As seis capturas comparáveis ficam no artefato `PianoLivre-public-emulator-evidence`: `piano-03d-prompt02-phone-{normal,playing,restored}.png` e `piano-03d-prompt02-tablet-{normal,playing,restored}.png`. Os estados são obtidos por ações reais na interface, sem montagem de screenshot. O ensaio de toque em Espera usa uma nota gráfica real com as barras ocultas, exige identificação MIDI correta e compara beat, mãos, modo, velocidade, loop, sessão MIDI e hash do MusicXML antes/depois. Outro toque reutiliza o alvo original após reprodução, resize e volta ao início. `piano-prompt02-comparison.json` guarda medições e resultados desses casos.

### Resultados e proveniência

Consulte o [registro de validação do PR #73](https://github.com/atshimura/Piano-livre-aberto/pull/73) para os links específicos de build, emulador, APK e capturas aprovados no HEAD final. O registro distingue o SHA da branch do merge temporário usado pelo checkout do Actions. `piano-visual-evidence.json` inclui esse checkout; o artefato também identifica o HEAD de origem. Nenhum run de revisão anterior deve ser apresentado como validação de alteração posterior. O PR reúne o resultado final sem exigir outro commit apenas para citar o próprio SHA.

Cobertura: auditoria pública; JVM/JavaScript/Chromium e corpus MusicXML/MIDI; 15.127 verificações da sessão musical JVM curta/longa; estado de apresentação; reconhecimento MIDI sintético nas três mãos; backup/upgrade, perfis, biblioteca, importação, favoritos/coleções, pesquisa e histórico; notação complexa, polifonia, pauta longa, mapa gráfico e toque; fallback real OSMD→temporal e falha dupla→esquemático; transporte, trechos, pausa/retomada e background; release sem depuração com intents internos ignorados. Os prazos e asserções musicais permanecem exigidos.

O APK de validação publicado pelo workflow é a variante **release sem depuração**, pacote `br.org.pianolivre.opensource`, versão `0.9.51-open-beta` (60), assinada com certificado descartável do runner no PR. Não é uma Release oficial nem garante atualização de uma instalação assinada com outro certificado. APK/evidências são artefatos temporários de 14 dias; links e expiração observada ficam no PR. A referência original e os scripts de reprodução permanecem versionados.

## Limitações e continuidade

O desenho da pauta respeita o MusicXML real, portanto não copia notas/diagramação da imagem estática. Ícones/tipografia nativos e menus compactos são adaptações para toque e fidelidade dos dados. O nome Yamaha P-45 não é fixado: só aparece se obtido do Android após abertura real; sem conexão, USB-MIDI.

Aceite físico pendente no Yamaha P-45 (USB, nome, reconexão, pedal, notas presas), Realme 7 5G e Lenovo Tab M9 (legibilidade, fluidez, fonte, toque e lifecycle). CI e MIDI sintético não substituem os aparelhos. Prompt 3 consolidará casos limítrofes e comportamento MIDI entre telas; não foi iniciado. V1 continua não concluída. Integração/publicação pertencem à etapa posterior aprovada.
