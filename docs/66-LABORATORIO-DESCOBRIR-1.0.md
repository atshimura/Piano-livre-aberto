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

`MusicLabDiscoverySelfTest` é executado somente no APK debug do workflow de emulador. Ele percorre o botão inicial, barra e estado vazio, injeta MIDI sintético e valida por pixels rasterizados os glifos de clave, as linhas suplementares e as posições de C2, C3, F3, C4, G4 e C6; também confere acordes graves/agudos/abrangentes e ordem/altura das notas de sequência. A regressão de sequência longa mede o conteúdo em viewports equivalentes a telefone (360 dp) e tablet (800 dp), exige espaçamento mínimo de 41 dp, todas as cabeças dentro do conteúdo rolável e rasterização da primeira e última notas. A injeção sintética comprova o fluxo interno e não comprova conexão física com Yamaha P-45.

O workflow existente volta a executar testes JVM do catálogo, demonstração, prática, `MusicTheory`, `MusicPatternTracker`, além da bateria MusicXML/MIDI/JavaScript, build Android e jornadas de emulador. O relatório da entrega deve citar somente os Actions do SHA deste PR.

## Resultados executados em 26/10/2026

Código final auditado no SHA `7ce5d2fcd99c49349fabefe17341ccd431073dd4` da branch #65:

- [Build/JVM/JavaScript/Chromium/MusicXML/APK — run 40369894641](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40369894641) — sucesso. Marcadores: `MUSIC_THEORY_TEST_PASS checks=34`, `MUSIC_SEQUENCE_ANALYZER_PASS checks=30`, `MUSIC_PATTERN_TRACKER_PASS checks=33`, `MUSIC_DISCOVERY_SESSION_PASS checks=49`, catálogo 177, demonstração 54, prática 54 e `PUBLIC_DISTRIBUTION_APK_PASS`.
- [Emulador Android — run 40369894644](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40369894644) — sucesso após repetir o job no mesmo SHA. Marcadores: `MIDI_SESSION_SELFTEST_PASS assertions=13`, `DISCOVERY_UI_SELFTEST_PASS checks=215`, `PUBLIC_ANDROID_EMULATOR_SMOKE_PASS`; os perfis responsivos do Estudo de 1440×2160 e 1600×2560 também passaram.
- Uma primeira execução do emulador falhou no teste responsivo preexistente do Estudo, com a tela aberta e a pauta vazia. O mesmo job, no mesmo SHA, passou na repetição; não houve nova falha nem alteração para reduzir asserções.
- [Artefato com capturas do emulador](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40369894644/artifacts/12674092710): 11 PNGs. Inclui C2/C3/F3/G4/C6, acorde grave/agudo/abrangente, sequência que cruza as pautas e sequência longa renderizada nos viewports equivalentes a telefone (360×420 dp) e tablet (800×600 dp). As capturas individuais têm 990×440 px; as duas sequências longas, 2503×1155 e 2503×1650 px. Os pixels das claves, pontos F3, linha G4 e linhas suplementares são conferidos pelo teste.
- O emulador usa MIDI sintético. Esta evidência valida o software, não a ligação física com Yamaha P-45.

## Limites e aceite

- A identificação de escalas e arpejos cobre apenas padrões completos exatos já reconhecidos pelo `MusicSequenceAnalyzer`; improvisação, padrões parciais e sequência com sobreposição de teclas não são classificados como escala/arpejo.
- A janela calibrada já persistida pertence ao perfil e não é inventada pelo Descobrir. Sem ela, eventos separados são conservadores; ataques MIDI agrupados num pacote ou teclas sobrepostas permitem avaliação conjunta.
- Não há sintetizador nem conexão simulada de Yamaha em produção.
- Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 exigem aceite físico do usuário. Nenhum desses ensaios é declarado feito remotamente.
- A versão proposta para o APK da branch é `0.9.49-open-beta`, code 58. A beta 0.9.48 existente não é substituída ou republicada por esta branch; Release/publicação aguardam autorização expressa.
- A Entrega C e o Laboratório Musical 1.0 integrado permanecem trabalho separado; este PR não conclui a versão 1.0.


## Integração e publicação da beta 0.9.49 — 26/10/2026

O PR #65 foi integrado em `main` no merge commit `abfd4b51213be17df091288e436eef7e19453e9a`. Durante a validação da `main`, a suíte encontrou uma expectativa obsoleta no teste de atualização (0.9.48/code 57); foi corrigida no PR #66. O PR #67 ajustou os filtros para executar o emulador quando scripts/testes mudarem e permitir os checks do mesmo SHA. Ambos foram integrados; o SHA de distribuição final é `9d5c3793f1b3e97132dfebec3a36e25c20723993`.

- [Build/JVM/JavaScript/Chromium/MusicXML/APK — run 40401082178](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40401082178): sucesso, incluindo 49 checks de sessão, certificado estável e APK release não depurável.
- [Emulador — run 40401082464](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40401082464): sucesso no mesmo SHA; 215 checks de UI Descobrir, 13 asserções MIDI e smoke público. Também passou a atualização in-place da beta pública 0.9.46 para 0.9.49 (code 55→58), com o mesmo certificado e dados sintéticos preservados.
- Release oficial: [v0.9.49-open-beta](https://github.com/atshimura/Piano-livre-aberto/releases/tag/v0.9.49-open-beta). [Baixar APK](https://github.com/atshimura/Piano-livre-aberto/releases/download/v0.9.49-open-beta/PianoLivre-0.9.49-open-beta.apk).
- Arquivo: `PianoLivre-0.9.49-open-beta.apk`, 592.580 bytes. SHA-256 `9a4343c232a175d7b420e1dfbc6d2268c88bb776ef1129ea015a98c8fd697e59`; coincide com digest GitHub do APK e checksum publicado.
- Metadados verificados: package `br.org.pianolivre.opensource`, `versionCode=58`, `versionName=0.9.49-open-beta`, release sem depuração. Certificado permanente `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`, igual ao da beta 0.9.48.

O conteúdo inclui correção visual da clave de fá, verificações por renderização rasterizada, notas/acordes/padrões suportados e preservação do resultado durante análise. Escalas/arpejos continuam limitados a padrões completos já reconhecidos pelo motor. Yamaha P-45, Realme 7 5G e Lenovo Tab M9 ainda exigem validação física; emulador e MIDI sintético não substituem esse aceite. A beta 0.9.49 não declara a versão 1.0 concluída. Próxima etapa: Entrega C.

## Ajustes solicitados pelo usuário — nova revisão, 26/10/2026

A beta `0.9.49-open-beta` permanece publicada sem alteração. Esta revisão, ainda não publicada, trata quatro observações reportadas após o uso:

- Notas isoladas confirmadas em `Note Off` passam a atualizar a identificação na tela, inclusive durante uma frase em análise. A captura contínua da sequência permanece independente; assim uma nota isolada não bloqueia a classificação posterior de uma escala/arpejo. A detecção de acorde continua dependente de sobreposição MIDI ou da janela medida pelo perfil, sem um novo limite fixo.
- A pauta Descobrir passa a usar `NotationView` e o OpenSheetMusicDisplay local, o mesmo mecanismo de gravura usado no Estudar. MusicXML sintético mantém notas, acidentes, oitavas, acordes simultâneos e sequências ordenadas. As claves resultam do mesmo renderer, em vez dos caminhos desenhados separadamente pela tela Descobrir.
- A análise de escala maior é exercitada no fluxo MIDI da sessão com a sequência completa de Dó4 a Dó5 e pausas de 2 s entre ataques. O reconhecedor continua conservador: exige a oitava completa, em ordem e sem sobreposição que a classifique como acorde.
- A tela acrescenta uma faixa horizontal de cifras com as alturas identificadas: uma cifra por nota, todas as notas de acorde em ordem de altura e todas as notas de escala/arpejo em ordem de execução. O título mantém o símbolo/qualidade do acorde e a inversão aparece no detalhe.

A suíte do emulador agora aguarda a renderização assíncrona do OSMD, verifica que o DOM contém os glifos reais de clave, compara as formas de sol/fá e exporta capturas C2, C3, F3, C4, G4, C6, acordes grave/agudo/abrangente, sequência entre pautas e Dó maior. Os testes JVM exercitam Dó→Ré após 2 s, escala completa com o mesmo espaçamento, nota atualizada a cada liberação e transições para padrões completos/incompletos.

A versão de próxima build sobe para `0.9.50-open-beta`, versionCode 59, para permitir atualização da beta aberta 0.9.49 com o mesmo pacote e o publicador oficial, após revisão e autorização específica de publicação. A `main`, a Release `v0.9.49-open-beta` e o APK publicado não foram modificados. Testes locais aprovados: teoria (34), sequência (30), tracker (33), sessão MIDI (51), MusicXML Descobrir (41), contrato OSMD (77), clefs JS (46), perfil/janela, auditoria do pacote público e publicação (8). Build Android, emulador e capturas finais ainda dependem do workflow no SHA do PR. Testes físicos Yamaha P-45, Realme 7 5G e Lenovo Tab M9 permanecem pendentes.
