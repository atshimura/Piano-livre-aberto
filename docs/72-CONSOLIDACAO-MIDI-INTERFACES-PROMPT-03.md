# Consolidação MIDI e interfaces — Prompt 3

Branch `feat/consolidacao-midi-interface-piano-livre`, [PR #74](https://github.com/atshimura/Piano-livre-aberto/pull/74), dependente da implementação final do Prompt 2: `2739bb2223cf7efa0a066ec7fc8380bdcde9430d`, [PR #73](https://github.com/atshimura/Piano-livre-aberto/pull/73). Preparação: PRs #71/#72/#73, referências [Inicial](references/tela-inicial-aprovada.png) e [Estudar](references/tela-estudar-aprovada.png) inspecionadas. [Especificação integral fornecida](prompts/PROMPT_03_CONSOLIDACAO_MIDI_INTERFACES_PIANO_LIVRE.md). Sem redesenho, merge à main, Release ou início do Prompt 4. Versão de validação: **0.9.52-open-beta (61)**.

## Estado inicial e problemas encontrados

`MainActivity` já centralizava portas Android, geração de receiver, notas, reconhecimento e transporte; `HomeMidiIndicator` formatava a mesma legenda nas duas telas. Porém o critério visual era apenas dispositivo aberto, mesmo se nenhuma porta funcionasse. A abertura assíncrona e o estado visual dependiam de campos separados. Faltava reconciliação de inventário no retorno do segundo plano/alteração de metadados e o callback Android não era desregistrado ao destruir a Activity.

O fechamento de todas as portas ficava num único bloco try: uma exceção podia impedir as liberações seguintes. O teste de saída agendava Note Off sem vincular a conexão; após troca, poderia desligar uma nota no novo instrumento. O desligamento de demonstração não liberava explicitamente CC64. Pacotes já enfileirados antes de pausa e mensagens parciais do decoder podiam reaparecer após retorno.

## Arquitetura consolidada

`MidiConnection` é o único proprietário do estado e dos recursos da conexão. Estados: `DISCONNECTED`, `OPENING`, `CONNECTED`, `FAILED`. O inventário Android informa disponibilidade; uma tentativa recebe geração própria; somente portas efetivamente abertas produzem conexão. Uma entrada ou saída funcional é suficiente, com capacidades independentes. Nenhuma porta aberta significa falha, fechamento e legenda USB-MIDI.

`AndroidMidiPorts` adapta `MidiDevice`, `MidiOutputPort` (instrumento→app) e `MidiInputPort` (app→instrumento). Cada abertura/fechamento é independente. O coordenador é independente do Android para testar as mesmas transições, callbacks e bytes enviados com portas sintéticas. Isso não constitui driver USB simulado nem prova hardware.

Todas as transições de conexão e atualizações visuais ocorrem no Handler principal. O receiver decodifica em sua thread e publica um lote no Handler; ele confere geração e época de foreground tanto na chegada como na entrega. O decoder conserva enquadramento/running status inclusive no background, sem entregar eventos; cada mensagem leva a época de seu primeiro dado. Assim descarta ataques iniciados antes de pausar sem perder novos ataques em running status após retornar. Não existe um segundo relógio musical.

### Botão e gerenciamento

- Desconectado, abrindo, falha ou conectado sem nome: **USB-MIDI**.
- Conectado com nome: somente o nome fornecido pelas propriedades Android, sem prefixo USB-MIDI nem fabricante fixado.
- Inicial e Estudar leem o mesmo snapshot. Navegação e retorno atualizam ambos; nome completo permanece na descrição de acessibilidade e no diagnóstico existente, mesmo com truncamento visual.
- Botões continuam clicáveis. Reutilizam seleção de dispositivos e diagnóstico de estado, capacidades, contadores, últimas mensagens, notas/pedal e teste de saída. O título do diagnóstico é genérico: USB-MIDI.
- A ausência de nome é explicitada no diagnóstico e na acessibilidade. Não há conexão presumida a partir de uma enumeração.

### Conexão, troca e interrupção

Inicialização e eventos de adição usam o inventário Android para tentar USB quando não existe conexão/tentativa ativa. `onDeviceStatusChanged` atualiza os metadados do alvo corrente. Ao retornar ao foreground, o inventário é conciliado: alvo removido é encerrado; alvo ainda presente preserva geração e nome atualizado. Falha pode ser tentada novamente pelo seletor, sem laço infinito de tentativas automáticas.

Troca, remoção, falha de envio e destruição invalidam a geração anterior. Completions tardios fecham seus recursos e não substituem a sessão nova; a proteção também cobre reutilização do mesmo ID Android. Um completion repetido do recurso ativo não o fecha. A Activity desregistra o callback Android ao ser destruída.

A interrupção pausa o transporte e a preparação do Espera, restaura as barras e conserva música, beat, mãos, modo, velocidade, trecho e loop. Limpa ataques, teclas físicas, pedal observado e calibração em curso. A reconexão não reinicia a execução automaticamente; Iniciar pede um novo ciclo/ataques. A sessão musical anterior é carregada antes de iniciar a conexão USB na abertura do aplicativo, evitando sobrescrevê-la por uma pausa de conexão ainda sem música carregada.

Notas enviadas pelo aplicativo são acompanhadas por canal/altura. Parada e encerramento enviam Note Off, liberação CC64 e CC123 nas saídas controláveis, com tentativas independentes em caso de erro. O teste de saída é vinculado à geração e à sequência, cancelado por parada/background/troca: seu Note Off não alcança o instrumento seguinte. Isso reduz notas presas em encerramentos controláveis; não promete enviar bytes por um cabo já removido nem silenciar notas geradas por outros emissores.

## Barras dinâmicas e preservação

`StudyChromeState` foi preservado. Início efetivo recolhe cabeçalho e MODO/MÃOS; toque concluído revela até novo ciclo; pausa/parada/desconexão/background restaura. Transporte inferior permanece. Não há timer de ocultação. O toque de nota/trecho continua sendo tratado pelo renderizador existente; o observador de revelação não consome o gesto. As jornadas do Prompt 2 continuam exigidas.

O ensaio de gerenciamento revelou uma perda de 36 px após fechar o diálogo MIDI no Android API 29: a barra de status do sistema reaparecia. A Activity reaplica seu modo de tela cheia ao recuperar foco, e `NotationView` ajusta a partitura às novas dimensões nativas. A verificação aguarda a aplicação dos insets, mantendo as mesmas alturas mínimas do Prompt 2; não reduz o critério para aceitar o defeito.

A inspeção dos PNGs encontrou regiões da pauta sem pintura no celular. A guarda de viewport e a confirmação de quadro eliminaram o enquadramento antigo no tablet, mas não bastaram para a pintura no celular. `NotationView` só solicita o ajuste quando a viewport CSS corresponde às dimensões nativas, protegendo a requisição por sequência/geração; o ensaio espera geometria atual e `WebView.postVisualStateCallback`, sem pausar o transporte. A camada de software já prevista para OSMD agora é mantida, removendo a restauração imediata da camada original que anulava essa escolha. OSMD, MusicXML e mapeamento permanecem os mesmos; desempenho e prova visual continuam exigidos na suíte completa. O job curto `midi-visual` produz as capturas antes da suíte longa para inspecionar essa questão específica; não substitui ou remove os testes existentes.

As capturas da revisão `5e3eb33` confirmaram a pintura completa com a camada de software. A suíte longa dessa revisão identificou uma corrida do roteiro antigo: mudar a resolução antes de encerrar a Activity com flags de self-test podia reexecutar a checagem de upgrade após o teste de exclusão ter removido sua fixture. A preparação das capturas de Home agora encerra essa instância antes de mudar a resolução, preservando a checagem inicial de upgrade, a exclusão de importados e os ensaios de resize/lifecycle nas jornadas próprias. O resultado final continua dependendo dos dois workflows do HEAD final.

Nenhuma alteração em parser MusicXML, `WaitRecognizer`, calibração de acordes, seleção de mãos por pauta, renderizadores ou agenda `MidiPlayback`. Extras continuam indicadas sem bloquear; duração e pedal orientam, sem prazo novo. Dados, esquema de backup, perfis e arquivos originais não são migrados ou reescritos. Preferência antiga do guia continua preservada sem devolver o guia ao Estudar.

## Testes e evidências

O fechamento técnico é registrado na descrição e no comentário final do [PR #74](https://github.com/atshimura/Piano-livre-aberto/pull/74), com HEAD final, checkout temporário do Actions, runs, resultados, hashes, artefatos e expiração. Resultados de revisão anterior não validam mudança posterior. Essa forma permite registrar o próprio SHA final sem criar outro commit documental recursivo.

- `MidiConnectionTest`: conexão real do coordenador com portas sintéticas; enumeração/abertura sem conexão presumida; nomes/metadata; entrada/saída isoladas; falhas de abertura/portas/envio/fechamento; remoção durante abertura, ID reutilizado, callbacks tardios/duplicados, destruição e 100 sessões sucessivas; bytes de Note Off/CC64/CC123. Primeira execução local: **226 verificações aprovadas**.
- `MidiProtocolEpochTest`: **4.226 verificações locais aprovadas**, 16 canais e 88 alturas, fragmentos iniciados antes da pausa, running status novo após retorno, real-time intercalado, SysEx e mensagens de um dado. O decoder mantém o enquadramento do fluxo enquanto a Activity rejeita ataques de épocas anteriores; não exige que o instrumento reenvie status em toda nota.
- `MidiSessionSelfTest`: Activity/Handler/decoder reais, reconhecimento nas três mãos, extras/acordes, CCs por canal, geração anterior, lotes enfileirados e pacote parcial antes de background; novos ataques após retorno. Preferências sintéticas isoladas.
- `MidiInterfaceSelfTest` + `emulator_midi_interfaces.py`: backend explicitamente **SIMULADO**, usando o coordenador real e as views reais. Captura Inicial desconectada/abrindo/conectada, Estudar conectado/oculto/restaurado/desconectado/sem nome/troca e retorno ao foreground. Confere igualdade dos dois indicadores, acessibilidade, gerenciamento, recursos, notas/pedal e conservação dos parâmetros musicais. O driver captura com `adb screencap` e só então libera o próximo estado; nenhum screenshot montado.
- A comparação exige pauta pelo menos 417/567 px no celular 1280×720 e 777/927 px no tablet 1920×1080, densidade 240, estados normal/oculto. São referências de regressão do Prompt 2, não tamanhos para todos os aparelhos. A suíte anterior mede também antes/depois, restauração, toque MIDI64, escala uniforme e nota original após resize/rewind.
- Permanecem todas as suítes JVM/JS/Chromium, corpus MusicXML/MIDI, notação complexa, polifonia, fallback automático, sessão curta/longa, trechos, biblioteca/Laboratório, dados, backup, upgrade e release sem depuração. Nenhuma asserção existente foi removida para aprovar esta entrega. O teste antigo de reconexão agora pede reinício explícito após a pausa de desconexão, conservando a exigência de novos ataques.

Artefatos do workflow: `PianoLivre-prompt-03-validation-apk` e `PianoLivre-public-emulator-evidence`, retenção de 14 dias. Capturas novas: `piano-prompt03-{phone,tablet}-*.png`; matriz de estados: `piano-prompt03-midi-interfaces.json`; logs por viewport. Capturas e métricas do Prompt 2 permanecem no mesmo artefato. O manifesto visual registra o SHA realmente testado.

O APK é release **sem depuração**, pacote `br.org.pianolivre.opensource`, versão 0.9.52-open-beta (61), certificado descartável do runner. O ensaio debug→release deve conservar dados sintéticos e ignorar intents de teste, inclusive a nova fixture MIDI. A atualização desde a base pública usa certificado compatível no runner; a atualização com certificado permanente só roda no fluxo confiável de main, não recebe segredos em PR e não é alegada como executada neste PR.

## Limitações e próxima etapa

Não foram realizados testes físicos Yamaha P-45, Realme 7 5G ou Lenovo Tab M9. Recepção real USB, latência, permissões/driver Android, cabo/OTG, reconexão física, pedal e ausência física de notas presas continuam pendentes. Portas sintéticas comprovam transições e integração de software; não equivalem ao instrumento.

Layouts e adaptações aprovadas são preservados: ícones/fontes nativos, menus compactos e fonte musical real; sem novas barras nem redução deliberada da área de partitura. O aceite subjetivo de legibilidade/fluidez continua dependente dos aparelhos. V1 não concluída.

Próxima etapa: **Prompt 4 — Consolidação, integração e publicação**, somente após encerramento técnico e validação automatizada deste Prompt 3 e autorização própria. Nada foi integrado ou publicado por esta entrega.
