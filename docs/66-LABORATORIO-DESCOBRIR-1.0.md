# 66 — Laboratório Musical: Descobrir 1.0

Data da implementação: 26/10/2026. Base: `main` depois da integração da Entrega B no commit `14489deef06d02e85a8bd43ecfc24f53aed22a03`. Esta entrega implementa a interface Descobrir aprovada em 20/10/2026; a proposta antiga de catálogo/prática do doc 45 não define esta tela.

## Interface pública

- A tela inicial abre o novo Descobrir pelo botão **Laboratório Musical**.
- A tela contém somente a barra **Voltar | Descobrir | Aprender** e, antes do primeiro resultado, a instrução exata `toque uma nota, acorde ou escala musical`. Descobrir fica selecionado; Aprender é visível e desabilitado.
- Esta tela não usa o teclado virtual nem expõe catálogo, prática, gravação, metrônomo ou demonstração. A classe histórica `MusicLabScreen` e seus motores continuam no projeto, sem acesso pela experiência pública Descobrir.
- O botão Voltar retorna à biblioteca. Entrada/saída da tela, pausa e retomada invalidam observações pendentes. O último resultado sobrevive a uma pausa e à reconexão MIDI; ao entrar deliberadamente em nova tela Descobrir, inicia-se vazio.

## Motor reutilizado e decisões conservadoras

`MusicDiscoverySession` recebe apenas Note On/Off USB-MIDI já decodificado pela Activity. Note On com velocity zero segue o mesmo fluxo de Note Off; ataques redundantes enquanto uma altura está pressionada são ignorados. CCs não viram notas; CC120/123 liberam alturas mantidas. A entrada é agrupada pelo pacote MIDI; quando o perfil possui janela de acorde calibrada, essa medida ajuda a observar ataques próximos. Sem calibração, não se escolhe tolerância em milissegundos. Para além do mesmo pacote, a sobreposição de teclas pressionadas é evidência de acorde; ataques sucessivos liberados alimentam o `MusicPatternTracker`.

Nota isolada usa grafia convencional fixa sem inferir tonalidade: DÓ/C, DÓ♯/C♯, MI♭/E♭, FÁ♯/F♯, LÁ♭/A♭ e SI♭/B♭. Sem calibração de acorde, uma nota simples é apresentada ao soltar a tecla; isso evita declarar prematuramente um ataque sustentado como nota isolada. Com calibração, o perfil fornece a observação breve. A pauta nativa calcula posições diatônicas, claves de sol/fá e linhas suplementares. Notas de acorde compartilham coluna x; sequências mantêm ordem e registro MIDI. Acordes sem interpretação exata e única são explicitamente ambíguos. Não são inferidos tonalidade, função harmônica, mão ou dedilhado.

`MusicPatternTracker` e `MusicSequenceAnalyzer` continuam reconhecendo somente padrões completos exatos de uma oitava previstos pelo motor atual. A tela substitui o resultado apenas ao confirmar outra nota, acorde ou padrão. Um padrão parcial não é anunciado como escala/arpejo. Após quatro ataques isolados, a pausa de frase usa duas vezes a mediana dos intervalos que a própria pessoa acabou de tocar (sem limite universal em ms); a pauta mostra todas as notas como sequência ambígua/incompleta. O último resultado permanece durante essa análise.

### Auditoria final solicitada — 26/10/2026

A revisão do SHA `701332dcb358f0d3052490a38c730bc235f67f42` reproduziu uma falha gráfica: a pauta de clave única chamava sempre `drawTrebleClef`, mesmo quando as notas estavam mapeadas para clave de fá. A regressão anterior consultava apenas `NotePosition.clef`, sem inspecionar o símbolo rasterizado. A correção seleciona clave de fá em pauta exclusivamente grave, mantém sol acima e fá abaixo na pauta dupla, e ancora os pontos da clave de fá ao redor da linha F3; a volta da clave de sol passa pela linha G4.

A sessão sem calibração também era capaz de trocar o resultado por cada nota liberada durante uma frase. A regra implementada nesta revisão trata a primeira nota liberada como uma identificação válida; as notas seguintes da mesma frase não trocam esse resultado até que um padrão completo seja reconhecido ou o delimitador adaptativo finalize a frase como incompleta/ambígua. Acordes substituem o resultado quando as teclas simultâneas são confirmadas. Essa decisão não usa janela temporal fixa.

O teste Android agora renderiza a `DiscoveryStaffView` em bitmaps reais, confere pixels do símbolo esperado e das linhas suplementares e salva capturas para C2, C3, F3, G4, C6, acordes graves/agudos/abrangentes e sequência que atravessa o C4. As capturas são incluídas no artefato do workflow de emulador. Também foram adicionadas transições de resultado nota→acorde, acorde→escala, escala→arpejo, arpejo→nota e resultado válido→frase incompleta. A validação dessas alterações deve ser citada somente após os dois workflows concluírem no mesmo SHA.

## Validação automatizada

`MusicDiscoverySessionTest` cobre registros grave/médio/agudo, oitavas, grafia, acordes simultâneos e rolados dentro da janela calibrada, escalas e arpejos nos dois sentidos, incompletude, ambiguidade, repetição/rearticulação, cancelamento, callbacks de conexão antiga e retenção do resultado.

`MusicLabDiscoverySelfTest` é executado somente no APK debug do workflow de emulador. Ele percorre o botão inicial, barra e estado vazio, injeta MIDI sintético e valida a posição/clave/linha suplementar de C4, a mesma coluna das notas de acorde, e a ordem/altura das cabeças da sequência, além de persistência e retorno. A injeção sintética comprova o fluxo interno e não comprova conexão física com Yamaha P-45.

O workflow existente volta a executar testes JVM do catálogo, demonstração, prática, `MusicTheory`, `MusicPatternTracker`, além da bateria MusicXML/MIDI/JavaScript, build Android e jornadas de emulador. O relatório da entrega deve citar somente os Actions do SHA deste PR.

## Resultados executados em 26/10/2026

Código validado: `a95c909497cf52471afcb129ed7812b5dc285039`. A árvore de código contém o resultado final; os checks foram executados no PR #65:

- [Build, JavaScript, Chromium, MusicXML, testes JVM e APK](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40340408772) — sucesso. A nova sessão concluiu com `MUSIC_DISCOVERY_SESSION_PASS checks=29`; a suíte manteve os testes de `MusicTheory`, `MusicPatternTracker`, `MusicSequenceAnalyzer`, catálogo, demonstração, prática e os testes de regressão existentes.
- [Emulador Android](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40340408802) — sucesso. Marcadores: `MIDI_SESSION_SELFTEST_PASS assertions=13`, `DISCOVERY_UI_SELFTEST_PASS checks=23` e `PUBLIC_ANDROID_EMULATOR_SMOKE_PASS`. A geometria verificou C4 na clave de sol com uma linha suplementar, C2 na clave de fá, C6 na clave de sol, acorde com cabeças simultâneas e sequência de oito notas em ordem.
- O teste de emulador injeta MIDI sintético. Ele verifica o fluxo de software; não representa uma conexão física com o Yamaha P-45.

## Limites e aceite

- A identificação de escalas e arpejos cobre apenas padrões completos exatos já reconhecidos pelo `MusicSequenceAnalyzer`; improvisação, padrões parciais e sequência com sobreposição de teclas não são classificados como escala/arpejo.
- A janela calibrada já persistida pertence ao perfil e não é inventada pelo Descobrir. Sem ela, eventos separados são conservadores; ataques MIDI agrupados num pacote ou teclas sobrepostas permitem avaliação conjunta.
- Não há sintetizador nem conexão simulada de Yamaha em produção.
- Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 exigem aceite físico do usuário. Nenhum desses ensaios é declarado feito remotamente.
- A versão proposta para o APK da branch é `0.9.49-open-beta`, code 58. A beta 0.9.48 existente não é substituída ou republicada por esta branch; Release/publicação aguardam autorização expressa.
- A Entrega C e o Laboratório Musical 1.0 integrado permanecem trabalho separado; este PR não conclui a versão 1.0.
