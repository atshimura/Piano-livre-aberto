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

## 0.9.23/code32 — controles de canal e teclas retidas

Recepção passa a interpretar CC120 (All Sound Off), CC121 (Reset All Controllers) e CC123 (All Notes Off), conforme a [tabela oficial MIDI Association](https://midi.org/midi-1-0-control-change-messages). Antes eram apenas registrados, deixando a observação de teclas/som sem atualizar quando o equipamento não enviava Note Off individuais.

CC123 libera teclas do canal, conservando som retido pelo CC64. CC120 encerra imediatamente as vozes observadas daquele canal, sem redefinir o estado do pedal. CC121 redefine o controlador acompanhado pelo app (CC64) e encerra suas vozes retidas, preservando teclas ainda fisicamente pressionadas. Uma mesma altura em outro canal continua ativa. Não envia novos comandos ao instrumento nem altera critérios do Espera.

MidiChannelModeTest passou localmente em 12.672 asserções (16 canais × 88 alturas e cenários), além das 7.052 de MidiPerformanceTest. O self-test Android acrescenta sequência real de mensagens na Activity: mesma altura em dois canais, CC123, CC120 e CC121, conferindo downKeys, sustainDown e vozes. A CI Android do commit deve comprovar esse caminho antes de integrar.

Depois: consolidar critérios técnicos da Etapa 3 e avançar à biblioteca/perfis/progresso. Não declarar conexão Yamaha física, latência ou ausência real de notas presas sem aparelho.
