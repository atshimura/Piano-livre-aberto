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

