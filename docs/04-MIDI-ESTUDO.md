> **Arquivo de referência técnica transferido para a edição aberta.** Parte das afirmações sobre versão, CI, aplicativo privado ou certificados descreve apenas o histórico de desenvolvimento. Para a situação vigente no repositório público, consulte [AGENTS.md](../AGENTS.md), [build e testes](06-BUILD-TESTES.md) e [estado](07-ESTADO-E-ROADMAP.md). Links para execuções privadas podem estar inacessíveis a terceiros.

# 04 — MIDI, relógio musical e modos de estudo

> **Status corrente na beta privada 0.9.11:** trechos a seguir sobre `downKeys.containsAll`, `Score.tempo` único, acordes sem calibração e pedal sem tratamento se referem ao **estado histórico da 0.9.0–0.9.1**, não ao estado corrente. O Espera usa `WaitRecognizer` (extras indicadas sem bloquear), `ChordCalibration` por perfil (janela temporal apenas após calibrar), duração/pedal orientativos, e `Score.tempoChanges`/percursos de compassos. `MidiPerformance` rastreia CC64 por canal; o indicador visual da 0.9.11 agrega os 16 canais. A Demonstração usa `MidiPlayback` com seek seguro. Ver [13](13-CORRECOES-MIDI-0.9.1.md), [17](17-REGRAS-ESPERA-CALIBRACAO.md), [18](18-MOTOR-MUSICAL-0.9.5.md), [19](19-PERCURSO-MUSICAL-0.9.6.md), [24](24-AUDITORIA-NOTACAO-MIDI-ETAPAS-1-3.md). **Ainda sem ensaio físico Yamaha/Realme/Lenovo**.

## 1. Instrumento e conexão

O Yamaha P-45 é o instrumento de referência via USB-MIDI. **MainActivity.setupMidi** usa a Android MIDI API, registra **MidiManager.DeviceCallback**, enumera dispositivos USB e tenta conectar. **connectMidi** abre:
- **MidiOutputPort** do dispositivo para receber mensagens **instrumento → Android**, ligada ao **MidiReceiver**;
- **MidiInputPort** do dispositivo para enviar mensagens **Android → instrumento**.
Os nomes acima são relativos às APIs do **MidiDevice** e podem confundir: a propriedade **inputPort** em MainActivity aponta para MidiOutputPort (recepção), enquanto **outputPort** aponta para MidiInputPort (envio).

O usuário encontra seleção e diagnóstico em **USB-MIDI**. A tela mostra contagem de mensagens recebidas/enviadas, últimas notas, canal, velocidade e sustain, além de teste de saída **Dó4 por 0,55 s**. **Ainda é obrigatório validar no P-45 real**, cabo/adaptador OTG e USB host dos dispositivos.

## 2. MIDI de entrada

**MidiProtocol.feed** interpreta:
- mensagens channel MIDI de comprimento 1 ou 2 dados;
- **running status**;
- mensagens System Exclusive descartadas;
- mensagens MIDI real-time ignoradas corretamente sem interromper parsing.

O callback envia eventos ao **Handler do thread principal**. **onMidiMessage** trata Note On (0x90 com velocity>0), Note Off (0x80 ou Note On com velocity 0) e **CC64 sustain** (registra status; não implementa integralmente sustentação pedagógica). Recebe outros comandos para diagnóstico, mas não os aplica à avaliação.

**Histórico (0.9.0):** a progressão antiga comparava `downKeys.containsAll(required)`. **A versão vigente** combina novos ataques do `WaitRecognizer` com `ChordCalibration` por perfil; mantém o estado físico e CC64 no `MidiPerformance`. As restrições abaixo registram riscos da auditoria antiga e devem ser lidas junto às correções da 0.9.1–0.9.4:
- não mede simultaneidade em ms;
- duração/articulação e pedal não são critérios de avanço no **modo Aprendizagem** (são orientação); notas extras devem ser **sinalizadas e não bloqueantes**;
- notas inicialmente pressionadas podem interferir na próxima posição;
- eventos na mesma altura/vozes independentes exigem testes dedicados.

Não descreva isso como o modo Espera completo aprovado no produto.

## 3. MIDI de saída e prevenção de notas presas

**MainActivity.send(pitch,on,velocity)** envia mensagens **Note On (0x90)** e **Note Off (0x80)** no canal MIDI 1 (índice 0), quando há porta aberta. **allOff** interrompe MidiPlayback, envia Note Off às notas no conjunto activeNotes (quando presentes) e também **CC123 All Notes Off** ao instrumento.

**MidiPlayback** organiza para cada Score.Event uma ação de início e fim por beat. Empates no mesmo beat ordenam Note Off antes de Note On. Um mapa com contador de uso por pitch evita desligar prematuramente uma altura mantida por notas simultâneas da agenda. **seek** reconstrói a situação no beat escolhido; **advance** despacha todas as transições atingidas; **stop** libera notas.

**Limitações:** não há canais individuais por mão, programação completa de instrumentos, tratamento fiel de pedal de saída ou eventos de programa/pitchbend; o pipeline é básico. Testar vários canais/instrumentos e garantir ausência de hanging notes ao pausar, desconectar, alternar mão, retornar, sair do app e repetir.

## 4. Estados e controles

| mode | Nome | Clock | Entrada | Saída |
|---|---|---|---|---|
| 0 | Espera | Reconhecimento da(s) nota(s) + deslocamento visual para próxima posição | Yamaha MIDI | Sem acompanhamento da mão inativa automatizado |
| 1 | Contínuo | MainActivity.ticker | Teclas podem ser observadas, sem avaliação obrigatória | Não dispara demonstração |
| 2 | Demonstração | MainActivity.ticker | Sem avaliação do aluno | MidiPlayback encaminha notas ao Yamaha |

**Contínuo e Demonstração:** botão Iniciar/Pausar determina **running**. **Parar** chama **stopPlayback**, interrompe áudio e congela a posição. **Início** também reposiciona para 0, pausa e limpa o trecho. **Demonstração sem outputPort** apresenta aviso; não oferece sintetizador de áudio local.

**Espera:** não depende do botão Play, mas do instrumento estar conectado e de teclas MIDI corretas. **prepareWaiting** e **nextWait** direcionam progressão. Não penalizar pausa por chamada, segundo plano ou desconexão.

**Laço/repetição:** loopStart/loopEnd em beat; no Contínuo/Demonstração volta ao início ao atingir limite; em Espera, ao esgotar os eventos do trecho, a lógica de nextWait reinicia. Há arestas musicais a validar.

**Filtro de mãos:** 0 ambas; 1 staff 1; 2 staff diferente de 1. Essa convenção é provisória; arquivo MusicXML pode ter várias vozes/pautas e mapeamento de mão explícito. Não inferir mãos apenas por altura.

## 5. Fórmulas de relógio, pausas e velocidade

O relógio principal usa **SystemClock.uptimeMillis** e atualiza **position** por deltaMs/60000 × score.tempo × tempoFactor. **Historicamente**, Score.tempo era o BPM único; desde 0.9.5 existe `Score.tempoChanges` e avanço por andamento vigente. Isso não prova compatibilidade com todo MusicXML nem resposta temporal correta em piano real. O controle visual atual usa **25% a 200%** no SeekBar (progress 0..175 ⇒ (progress+25)/100).

O texto de posição em MainActivity indica beat decimal, não compasso+tempo; melhorar legibilidade mais adiante. Usuário aprovou controle explícito BPM e %, mas UI atual oferece sobretudo controle percentual (BPM é apresentado como parte do status).

## 6. Fluxos de interrupção

- **onPause** chama stopPlayback e saveSession (só algumas informações são salvas).
- **Desconexão USB** detectada em DeviceCallback: pausa, fecha portas, atualiza status.
- **Ao retornar à biblioteca / tela de leitura livre**, interromper reprodução.
- **Ao buscar nova música**, interromper antes de carregar.
- **Ao pausar demonstração**, enviar Note Off e CC123; ao retomar, agendar novamente sem tocar notas encerradas.
- **Ao reabrir sessão**, não reiniciar reprodução automaticamente sem intenção explícita.

## 7. Critérios de teste prioritários

1. Note On/Off, velocidade (velocity 0 para Off), running status e acordes com pelo menos 3 notas.
2. Sustain CC64, notas sobrepostas, notas repetidas e acorde tocado em sequência/soltura.
3. Espera não avança com nota incorreta e ignora mão desmarcada; avança com todas corretas; notas sustentadas/ties e pedal exigem novo motor.
4. Demonstração com note-off correto em fim, stop, retorno, loop, desconexão USB, troca de música e segundo plano.
5. MIDI bidirecional e teste do Dó4 em Yamaha P-45, Realme 7 5G e Lenovo Tab M9, com documentação dos resultados e latência.
6. Velocidade em BPM e % e mudanças internas de tempo; relógio musical coerente com partitura.

Testes automatizados básicos: **MidiSmokeTest.java** (sem instrumento físico) e **emulator_ui.py** (emulador sem Yamaha). Nenhum deles demonstra latência ou reconhecimento correto no piano real.

## Implementado após a auditoria: beta 0.9.1 (14/10/2026)

O Espera agora usa **WaitRecognizer**, exigindo ataques novos na posição e evitando avanço por notas mantidas anteriormente; fontes de entrada são diferenciadas por canal+altura. **HandNavigation** evita retornar ao começo da música ao selecionar a mão. A **Demonstração** filtra saída MIDI por mão no MidiPlayback. **Score.read** e o renderer JS suportam dois instrumentos/partes de uma pauta, com a ressalva de que ordem das partes pode exigir configuração explícita. O teclado guia acompanha a região musical em janela móvel. Esses são avanços comprovados em testes simulados; **pedal/duração tornaram-se orientativos e janela de acordes é configurável após calibração na 0.9.4; a validação física ainda permanece pendente**. Consulte [13](13-CORRECOES-MIDI-0.9.1.md).

## Atualização da beta 0.9.5

A execução agora usa Score.tempoAt(position) e Score.advanceBeats para aplicar mudanças de tempo ao cruzar compassos, e RepeatNavigator para simples barras de repetição (subordinadas ao loop A–B). O BPM local é encaminhado ao JS do teclado/partitura. Suporte completo a retornos D.C./D.S., finais alternados e Coda ainda não existe; tais marcações geram aviso de limitação. Consulte [18](18-MOTOR-MUSICAL-0.9.5.md).

## Navegação musical da beta 0.9.6

PerformanceRoute.Cursor avança em um itinerário finito de compassos com casas de repetição, D.C./D.S./Coda nos casos aceitos. MidiPlayback.seek reconstrói notas ativas **sem emitir Note On/Off anteriores** durante o salto. A sessão salva a visita musical. O teclado guia visual continua mostrando ambas as mãos. Consulte [19](19-PERCURSO-MUSICAL-0.9.6.md).
