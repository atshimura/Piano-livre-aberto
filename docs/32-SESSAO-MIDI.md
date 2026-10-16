# Etapa 3 — Continuidade da sessão MIDI (0.9.22/code31)

## Defeitos identificados no código

A desconexão limpava WaitRecognizer, mas a abertura da nova entrada não rearmava a posição atual. Além disso, o handler podia entregar mensagens da conexão anterior após desconexão, e MIDI na biblioteca/segundo plano podia avançar o estudo oculto.

## Correção

Cada entrada possui decoder/receiver e geração próprios. Pacotes e callbacks de geração encerrada são descartados; uma abertura assíncrona antiga não substitui nova tentativa, mesmo com mesmo ID de dispositivo. Ao reconectar em estudo/Espera, rearma as notas atuais. O retorno à Activity rearma sem aceitar ataques antigos. Novos ataques permanecem obrigatórios.

MIDI recebido ainda atualiza observação/teclas, mas só pode avançar Espera com Activity em foreground e tela de estudo aberta. Biblioteca, leitura livre e background não avançam a sessão. Calibração interrompe coleta ao sair para background. Nenhuma janela numérica nova, bloqueio de extras, duração ou pedal foi introduzido.

## Teste Android

MidiSessionSelfTest é habilitado exclusivamente pelo flag debug midi_selftest. Usa preferências separadas piano_midi_selftest (não a sessão do usuário), requer ausência de dispositivo físico e executa Activity, Handler, decoder e telas reais. Verifica rearmar após limpeza, não avançar em biblioteca/background, avançar com novos ataques em estudo, descartar mensagem já enfileirada da conexão antiga e aceitar a atual. O emulador exige MIDI_SESSION_SELFTEST_PASS, além de backup/UI/lifecycle.

Esse teste não comprova abertura de portas USB, temporização/latência nem comportamento do Yamaha real. Conexão física continua em #11. Verificar Actions do próprio commit antes do merge; não declarar o teste Android executado com base apenas na existência do código.

## Próxima tarefa

Auditar mensagens de canal de desligamento (CC120/123/121), desconexão/notas presas e alternância de modo/mão com regressões. Depois reavaliar escopo técnico da Etapa 3 e avançar biblioteca/perfis/histórico, preservando dados.
