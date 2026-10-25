# 64 — Entrega B: validação combinada, sem merge

**Branch experimental:** `integration/entrega-b-revisao-sem-merge`.

## Base e escopo

A branch parte estritamente do HEAD documental `15f05d7463794409b64ed9b5efdfc7692ecb4127` do PR #51 (B1), que ainda não está na `main`. Ela incorpora **para fins exclusivos de prova integrada**, por novos commits, o conteúdo funcional e os testes dos PRs independentes de Entrega B:

- **#52:** identificação temporária por toque `Dó4 · C4`, dispensa de grafia não reconciliada, etiquetas temporais e proteção de símbolos.
- **#53:** jornada JVM de `Estudo livre 01` e `Gymnopédie nº 1` com mãos, Espera, guia e MIDI simulado.
- **#54:** marcação de trechos com liberação de MIDI antes de redefinir limites e rota.
- **#55:** jornada real de UI em emulador Android em cada uma das duas peças, incluindo pausa, alternância de renderer, guia, modos e retorno do background.

Não há merge de PRs, force-push, alteração da `main`, mudança no histórico do PR #51 ou publicação de Release/APK. O PR desta branch deverá ter como **base o próprio HEAD/branch do PR #51**, e **não** a `main`, para identificar precisamente as dependências. Uma eventual integração futura exige autorização do usuário e reconciliação das branches independentes antes do merge. Não tratar esta prova experimental como código integrado na `main`.

## Critérios mensuráveis

1. Build e testes públicos no SHA desta branch, incluindo a suíte Chromium com curvaturas/câmera adaptativas da B1 e todas as suites JVM/JS.
2. Emulador Android no mesmo SHA, incluindo toque com rótulo temporário, guia, três modalidades, mãos, Contínuo com pausa e retomada, prevenção de Demonstração sem saída, Espera, loops e lifecycle em duas partituras.
3. Artefatos PNG e JSON correspondentes ao SHA da entrega. Sem cortes verticais injustificados ou divergência MusicXML/MIDI.
4. As normas pedagógicas de notas extras, acordes somente calibrados, duração/pedal não bloqueantes continuam valendo.
5. Testes físicos/USB-MIDI: somente quando o usuário puder executar no Realme 7 5G, Lenovo Tab M9 e Yamaha P-45.

**Status inicial:** implementação de integração preparada; **CI e inspeção editorial do SHA final pendentes**. Registre somente os resultados realmente recebidos dos Actions, incluindo falhas e bloqueios. Esta branch não estabelece aceite editorial/físico.




## Revalidação integrada de B1 + Entrega B — 2026-10-25

O branch de prova aplicou B sobre a B1 conhecida em `15f05d7463794409b64ed9b5efdfc7692ecb4127` e validou a combinação no SHA `ffdda2fcc7081cee363560cb8f79336a5030f2da`. Build/JVM/Chromium: [run 40227586693](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40227586693). Emulador Android: [run 40227586698](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40227586698). Ambos passaram nesse SHA.

Evidências do mesmo ciclo: B1/OSMD Gymnopédie de 78 compassos, contexto de claves/armaduras/compasso fixo, duas pautas e rolagem vertical desativada; polifonia com 30 cabeças gráficas e 30 eventos nativos casados sem ambiguidades; loops A–B; etiquetas OSMD e temporal; sessões curta e longa com controles de mãos, guia, pausa/retomada e background; responsividade low/taller/tablet. A sessão usa eventos sintéticos onde aplicável e não representa teste físico com Yamaha P-45. O arquivo-base B1 e os fixtures do corpus não foram intencionalmente alterados.

O SHA de documentação desta atualização ainda precisa passar nos workflows antes de ser tratado como SHA final da prova.


## Estado documental final da prova — 2026-10-25

As notas anteriores que diziam “CI pendente” ou apontavam SHA `ffdda2fcc7081cee363560cb8f79336a5030f2da` como final são históricas e foram supersedidas. Resultado vigente: código e documentação no SHA `89541ba7f0ffb9f76ca2446320cfc83ba3665fd9`; build/JVM/Chromium/APK CI aprovado em [40229478373](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40229478373) e emulador aprovado em [40229478320](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40229478320), ambos associados a esse mesmo SHA. A jornada Android passou com Estudo livre 01 e Gymnopédie nº 1. #62 continua experimental e não deve ser integrado; nenhuma alteração na `main`, Release ou distribuição foi feita. Aceite físico/editorial pendente.


### Precisão de SHA após a revisão documental

O SHA `89541ba7f0ffb9f76ca2446320cfc83ba3665fd9` é o SHA do código integrado exercitado pelos dois workflows. Revisões finais somente Markdown vieram depois e levaram o HEAD documental da branch a `a36766aa7bec624b66610de83733ceeb08a43995`; não alteraram arquivos de app/teste e não acionaram os workflows por causa dos filtros de paths. Assim, os dois resultados citados acima pertencem ao mesmo SHA exato de código; não se alega que Actions tenham sido executados no HEAD documental posterior.
