> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 17 — Regras pedagógicas do modo Espera e calibração de acordes

Data da decisão: 14/10/2026. Primeira implementação: beta **0.9.4 (versionCode 13)**. Este documento é normativo para o comportamento do modo **Espera / Aprendizagem** e não define ainda uma modalidade de Avaliação.

## Decisões expressamente aprovadas

1. **Notas extras:** o aluno pode avançar quando tocar todas as notas corretas, mesmo que haja notas adicionais incorretas. **As extras são indicadas, não bloqueiam** a prática. Uma nota errada, sozinha, jamais aprova a posição.
2. **Simultaneidade de acordes:** a exigência é de uma **janela temporal ajustável**, medida/calibrada no instrumento. Não se adota um número de milissegundos inicial presumido pelo desenvolvedor.
3. **Duração e pedal no modo Aprendizagem:** o avanço acontece após o(s) **ataque(s) correto(s)**. A manutenção da nota pela duração escrita e o uso do pedal **orientam visualmente, não são requisitos** de aprovação. É diferente de uma futura Avaliação, cujas regras ainda não foram definidas.

## Implementação do motor

- **WaitRecognizer.java:** a posição musical é rearmada com as alturas esperadas. Somente novos ataques MIDI contam; teclas pressionadas anteriormente e mensagens repetidas sem soltura não contam como novos ataques.
- No ataque de nota não esperada, o motor informa `Result.extra=true` e registra a altura. A camada visual sinaliza **“Nota extra: [nota] · continue com as notas da partitura”**. Se as notas corretas forem completadas com extras presentes ou tocadas naquela posição, ela avança e informa que as extras não bloquearam.
- Para um acorde, todas as notas da posição devem ter recebido novos ataques e continuar pressionadas para completar a condição.
- **Com janela calibrada:** se o intervalo observado entre o primeiro e o último ataque exceder a janela configurada, não ocorre avanço. Ataques anteriores que excederam a janela deixam de contar; o estudante pode soltar e tocar novamente as notas.
- **Sem calibração:** `chordWindowMs=null` e nenhum limite temporal numérico é aplicado. Assim, não se inventa um valor e o estudo continua utilizável antes do ensaio físico. Esse estado deve ser apresentado como **não calibrado**, não como “sincronização validada”.
- Nota única é reconhecida pelo ataque correto independentemente da janela de acordes. Duração, tempo de sustentação e CC64 não entram na condição de avanço. O `MidiPerformance` continua registrando ataques, soltas, duração física e pedal separadamente para diagnóstico e evolução futura.

## Calibração na interface e persistência

- Na biblioteca: **Opções → Calibrar acordes MIDI**. No estudo: toque prolongado em **Espera** para abrir as configurações.
- Com a entrada USB-MIDI aberta, o usuário inicia a calibração e toca **pelo menos três acordes**, soltando todas as teclas entre os exemplos.
- **ChordCalibration.java** mede, em cada acorde válido de duas ou mais alturas, a diferença entre o primeiro e o último ataque detectados pelo app. A primeira proposta de janela usa **o maior intervalo efetivamente observado** entre as amostras coletadas. Não existe constante de milissegundos pré-aprovada.
- Ao concluir, o app abre campo editável, já preenchido com a medida observada. O usuário pode **ajustar o valor, salvar ou cancelar**. Também pode recomeçar a calibração e remover o ajuste.
- O valor é salvo em `user_<id>_chord_window_ms` em SharedPreferences: cada perfil possui sua própria calibração. O valor entra automaticamente no backup v2 e nas rotinas de restauração. Se ausente/zero, a janela não é aplicada.
- Como limite **técnico de entrada**, o campo aceita inteiros positivos de até 60.000 ms; este teto não representa recomendação pedagógica ou valor de simultaneidade para tocar.
- O cálculo usa timestamps de chegada ao Android; o atraso USB, agendamento do app e adaptação do aluno podem afetar a medida. A configuração deve ser **validada/recalibrada no Yamaha P-45 real** no Realme 7 5G e Lenovo Tab M9.

## Indicadores visuais

- `PianoScreens.showWaitFeedback`: faixa discreta sobre a área da partitura. Mostra nota extra, acorde reconhecido, acordes fora da janela e estado do pedal; não ocupa permanentemente a altura destinada à partitura.
- A cor amarela do teclado guia continua indicando **as duas mãos escritas**, sempre, independente do seletor de estudo. A cor verde indica teclas fisicamente pressionadas por MIDI.
- O motor de reconhecimento usa o filtro da mão escolhida; o teclado visual e as informações da partitura continuam representando ambas as mãos.
- Ao reconhecer acorde, o aviso chama atenção para **a duração indicada**, sem avaliação temporal. O pedal recebido gera apenas um aviso orientativo e permanece disponível no diagnóstico MIDI.

## Testes obrigatórios de regressão

- **WaitRecognizerTest**: 88 notas × três configurações × repetições; tecla errada não aprova; extra não bloqueia; sem calibração aceita conclusão lenta; com janela medida aceita ataques dentro da janela, rejeita ataques fora e aceita nova tentativa; duração/pedal não interferem.
- **ChordCalibrationTest**: zero valor antes de calibrar, registros com duas ou mais alturas, amostras apenas após soltar todas as notas, máximo observado efetivamente, redefinição e limitação de memória.
- **MidiPerformanceTest, MusicTimelineConsistencyTest, KeyboardGuideTest e musicxml_visual_crosscheck.js**: preservar o comportamento previamente aprovado do MIDI, duas mãos, notas ativas e renderização.
- **Android emulator smoke test**: instalação, atualização, biblioteca, estudo, teclado e restauração de backup. O emulador **não equivale a teste do USB-MIDI do P-45**.

## Limites e próximos passos

A janela configurável ainda não é uma calibração física verificada; será necessário medir sua utilidade e sensibilidade nos dois dispositivos e em várias combinações de notas. Ainda falta decidir se e como avaliações futuras exigirão ritmo, duração, pedal e punições por notas extras. Não assumir tais exigências no modo Aprendizagem.

Atenção: **três ou mais notas com a mesma altura são representadas pelo mesmo pitch MIDI**; identificar dedos ou mãos humanas fisicamente não é função dos dados MIDI comuns. As partituras MusicXML mais complexas continuam com limitações documentadas.

Referências: [16 — infraestrutura de duração e dados](16-BACKUP-MIDI-CONSISTENCIA.md), [01 — escopo aprovado da V1](01-VISAO-E-REQUISITOS.md), [08 — decisões](08-DECISOES.md), [AGENTS](../AGENTS.md).
