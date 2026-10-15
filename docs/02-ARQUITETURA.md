> **Arquivo de referência técnica transferido para a edição aberta.** Parte das afirmações sobre versão, CI, aplicativo privado ou certificados descreve apenas o histórico de desenvolvimento. Para a situação vigente no repositório público, consulte [AGENTS.md](../AGENTS.md), [build e testes](06-BUILD-TESTES.md) e [estado](07-ESTADO-E-ROADMAP.md). Links para execuções privadas podem estar inacessíveis a terceiros.

# 02 — Arquitetura técnica e mapa do código

**Base auditada:** HEAD anterior à documentação **59112581fbd7deb9bb5b01dde426b3132bf4944f** (14/10/2026). Consulte o histórico para mudanças posteriores. Este é um protótipo **Android Java + WebView JavaScript**; não há servidor/backend.

> **Atualização de arquitetura para beta privada 0.9.11 (15/10/2026):** a referência `59112581f...` abaixo é **histórica (0.9.0)**. O fluxo Android ↔ JS mantém dois parsers MusicXML independentes, cobertos por testes de comparação; `WaitRecognizer` gerencia progressão no Espera, `ChordCalibration` ajusta janela por perfil, `KeyboardGuide` sempre considera as duas mãos, `MidiPerformance` conserva estado CC64 por canal, e `MainActivity` agrega os 16 canais apenas para indicador de pedal. `BackupArchive` implementa v2 transacional. A 0.9.11 desenha vigas/bandeirolas de graça sem alterar eventos MIDI. Ver [05](05-DADOS-BACKUP.md), [17](17-REGRAS-ESPERA-CALIBRACAO.md), [24](24-AUDITORIA-NOTACAO-MIDI-ETAPAS-1-3.md) e [25](25-CONTINUIDADE-0.9.9-0.9.11.md). O PR #1 ainda não foi mesclado à main. **Não alterar arquitetura de execução sem confirmar HEAD e testes.**

## 1. Visão por camadas

~~~mermaid
flowchart TD
  U[Aluno no Android] --> UI[PianoScreens / PianoView]
  Y[Yamaha P-45 via USB-MIDI] <--> MIDI[MidiManager / MidiProtocol]
  MIDI --> CORE[MainActivity: sessão, estado, modos]
  UI <--> CORE
  CORE --> XML[Score: parser MusicXML/MXL]
  CORE --> PLAY[MidiPlayback: eventos de saída]
  PLAY --> MIDI
  CORE --> WV[NotationView: WebView e bridge]
  WV <--> PAGE[notation.html / PianoNotation]
  PAGE --> TEMP[temporal-notation.js: estudo]
  PAGE --> OSMD[OpenSheetMusicDisplay: partitura livre]
  CORE <--> STORE[SharedPreferences e filesDir]
  XML --> TEMP
~~~

**Direções relevantes:** o MusicXML original é guardado em **Score.sourceXml**; o mesmo arquivo também é analisado na WebView para desenho. O conjunto **Score.events** é usado pelos modos de estudo e pela demonstração MIDI. Os dois caminhos **não compartilham um modelo de eventos único**, o que pode gerar divergências e deve ser resolvido numa evolução arquitetural.

## 2. Componentes e responsabilidades

| Componente | Caminho sob PianoLivreAndroid | Responsabilidade |
|---|---|---|
| MainActivity.java | app/src/main/java/br/org/pianolivre/ | Activity, controles de execução, relógio de 25 ms, entrada/saída MIDI, sessões, importação, backup |
| PianoScreens.java | mesmo diretório | Telas nativas: biblioteca, estudo, partitura livre, botões, alternância do teclado imersivo |
| PianoView.java | mesmo diretório | Teclado guia Canvas e fallback esquemático quando a notação WebView falha |
| Score.java | mesmo diretório | Decodifica MusicXML/MXL em eventos: beat, duração, MIDI, staff, voz, compasso, tie |
| MidiProtocol.java | mesmo diretório | Parser de fluxo de bytes MIDI com running status |
| MidiPlayback.java | mesmo diretório | Ordena Note On/Off por beat; controla notas ativas da demonstração |
| WaitRecognizer.java / ChordCalibration.java | mesmo diretório | Espera, novos ataques, notas extras, calibração temporal por perfil |
| KeyboardGuide.java | mesmo diretório | Guia visual com ambas as mãos sem depender do filtro pedagógico |
| MidiPerformance.java | mesmo diretório | Estado MIDI recebido, inclusive CC64 separado por canal |
| BackupArchive.java | mesmo diretório | Backup v2 manual, validação prévia, estágio e rollback |
| NotationView.java | mesmo diretório | WebView local, ciclo de render, bridge Java–JS, seek e seleção de trecho |
| notation.html | app/src/main/assets/ | Carrega motores offline, expõe window.PianoNotation.load/seek/loopEdit/clearLoop/resize |
| temporal-notation.js | app/src/main/assets/ | Parsing gráfico MusicXML, tempo→pixel, desenho SVG em tiles, pauta fixa, animação, seleção |
| osmd/ | gerado em assets/ pelo build | OpenSheetMusicDisplay 1.9.8 (leitura tradicional) e licença |
| catalog.json / scores/ | app/src/main/assets/ | Índice de dez partituras MXL locais |
| .github/workflows/ | raiz GitHub | Compilação, testes estáticos/musicais e smoke test Android |

## 3. Estados de sessão e interface

O estado central está em **MainActivity**:
- **score**: Score atual ou null; **currentFile/currentName**: origem e rótulo.
- **position**: posição em unidades de **semínima (quarter-note beats)**, double.
- **currentHand**: 0 ambas, 1 direita (staff 1), 2 esquerda (demais pautas).
- **mode**: 0 Espera, 1 Contínuo, 2 Demonstração.
- **running**: clock contínuo; **waitTraveling/waitTravelTarget**: animação de deslocamento entre notas no Espera.
- **tempoFactor**: razão percentual (ex.: 1.0 = 100%); andamento efetivo atual = **score.tempo × tempoFactor**.
- **loopStart/loopEnd**: início/fim do trecho em beats, com **Double.MAX_VALUE** significando sem limite ativo.
- **downKeys**, **activeNotes**: teclas detectadas e notas tocadas pelo app, respectivamente.
- **MidiPlayback**: agenda de eventos na demonstração.
- **user** e **prefs**: índice do perfil ativo e persistência.

**PianoScreens** mantém estados visuais, incluindo **editingLoop**, visibilidade do teclado e barras. Esse estado não substitui o estado central. **Não introduzir dois relógios concorrentes para avanço musical.**

## 4. Fluxo ao abrir uma música

1. Biblioteca chama **MainActivity.loadScore** a partir de asset ou arquivo privado importado.
2. **Score.read(InputStream, fileName)** descompacta MXL ou aceita XML e produz lista de eventos (máscara pedagógica básica).
3. **PianoScreens.showStudy/displayScore** exibe o layout nativo, configura fallback e chama **NotationView.loadScore**.
4. **NotationView** transmite XML para **PianoNotation.load(xml, beat, 'study')** via **evaluateJavascript** (a ponte cita/escapa a string com JSONObject.quote).
5. **notation.html** chama **TemporalNotation.render**, dispara callback **AndroidNotation.onRendered**; **PianoScreens.followScore** posiciona a visualização.
6. Em **Partitura Livre**, a mesma WebView usa **PianoNotation.load(..., 'free')**, cuja renderização é feita pelo OpenSheetMusicDisplay.

## 5. Fluxo de execução/sincronização

- **MainActivity.ticker** usa Handler no thread principal, agendado a cada ~25 ms; calcula delta real por SystemClock.uptimeMillis e o converte para beats; limita delta a 250 ms.
- **PianoScreens.followScore** encaminha beat, BPM e movimento a **NotationView.seek**.
- **NotationView.seek** evita chamadas redundantes de Android para WebView durante movimento, transmitindo normalmente no máximo a cada ~110 ms; pausas e mudanças de estado precisam passar imediatamente.
- **PianoNotation.seek** chama **TemporalNotation.sync**.
- **temporal-notation.js** anima de fato em **requestAnimationFrame** usando performance.now e um relógio visual extrapolado. A posição de desenho deve continuar proporcional ao beat; o Java **continua autoridade para avanço musical**.
- **MidiPlayback.advance(position)** em Demonstração despacha eventos até o beat alcançado. A interrupção chama **allOff**.
- **Risco arquitetural:** mudanças súbitas de tempo, atrasos na UI, rotação, loops e retorno podem produzir dessincronizações ainda não medidas em dispositivo real.

## 6. Bridge JavaScript ↔ Android (contrato atual)

| Origem | Método | Parâmetros / efeito |
|---|---|---|
| Java → JS | PianoNotation.load(xml,beat,'study'/'free') | Renderiza partitura temporal ou convencional |
| Java → JS | PianoNotation.seek(beat,animate,bpm,moving) | Sincroniza posição visível de estudo |
| Java → JS | PianoNotation.loopEdit(boolean) | Habilita/desabilita seleção visual |
| Java → JS | PianoNotation.clearLoop() | Limpa seleção visual |
| Java → JS | PianoNotation.resize() | Reconstrói layout após alteração de altura/largura |
| JS → Java | AndroidNotation.onReady() | Página carregada; pode enviar partitura pendente |
| JS → Java | AndroidNotation.onRendered() | Render terminou; habilita WebView |
| JS → Java | AndroidNotation.onError(texto) | Reporta falha |
| JS → Java | AndroidNotation.onLoopSelected(start,end) | Pede que Java aplique trecho e reposicione |

**Atenção de segurança:** **addJavascriptInterface** permite chamadas de JavaScript para código Android. A página deve carregar somente URLs locais controladas, nunca conteúdo remoto ou MusicXML HTML não confiável como script. Os métodos de callback postam alterações no thread principal.

## 7. UI, layout e modo teclado ampliado

A Activity usa orientação **landscape**, tela cheia. No modo de estudo normal existem:
- Cabeçalho global (marca, conexão MIDI, perfil, USB-MIDI), ~45 dp;
- Faixa do título da música / Biblioteca / Partitura Livre, ~40 dp;
- Controles de modo e mãos, ~42 dp, roláveis horizontalmente;
- Área central com partitura;
- Teclado guia opcional, ~105 dp;
- Barra inferior (Início, Iniciar/Pausar, Parar, Velocidade, Teclado, Trecho), ~46 dp.

Ao abrir teclado guia via **PianoScreens.setKeyboardOpen(true)**, o cabeçalho, a faixa do título e os controles superiores somem; a barra inferior permanece. **notation.resizeToFit()** é disparado após atualização de medidas. Ao fechar, restaura visibilidade e escala. As rotas biblioteca/leitura livre devem sair do modo imersivo sem deixar o cabeçalho oculto.

## 8. Arquitetura offline, configurações e riscos

- Manifesto: **minSdk 26**, **targetSdk 28**, orientação paisagem, recursos MIDI/USB opcionais, **nenhuma permissão INTERNET**, **allowBackup=false** (backup do Android não é usado; exportação manual própria).
- O build pode usar internet para baixar OSMD; o **APK resultante embute o JS**.
- O código usa UI nativa Java sem arquitetura ViewModel/Room; isso torna MainActivity central demais. Uma evolução desejável é separar serviços **Clock/Transport**, **MidiSession**, **ScoreRepository**, **StudyState**, **Persistence** e **NotationBridge**, sem quebrar dados e funcionamento atuais.
- **Não substituir motor de partitura temporal por OSMD convencional sem reproduzir os contratos de tempo, posição, seleção, pauta fixa e desempenho.**

Ver [03](03-NOTACAO-TEMPO.md), [04](04-MIDI-ESTUDO.md), [05](05-DADOS-BACKUP.md) e [07](07-ESTADO-E-ROADMAP.md).
