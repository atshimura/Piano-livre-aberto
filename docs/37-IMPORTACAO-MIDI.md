# Importação MIDI — 0.9.28 / code 37

Leitura local de Standard MIDI File formato 0/1 com PPQN. Escolha explícita direita/esquerda/ignorar para cada trilha com notas, sem inferência por altura. Tempos de todas as trilhas são conservados, mesmo se a trilha de tempo não contiver notas. Nome, mão e advertência de aproximação são apresentados na importação.

Gera MusicXML separado em compassos 4/4, acidentes sustenidos, vozes independentes e tipos de figura aproximados. Ataques, durações por ticks, velocidades e tempos são preservados; notas que cruzam compassos usam ligaduras. Não reconstrói a escrita original: tonalidade, compassos originais, articulação, pedal, programas/instrumentos e demais controladores não integram a conversão. Não quantiza silenciosamente os ataques. Atribuição é por trilha inteira; se as duas mãos estiverem misturadas na mesma trilha, cabe ao usuário separá-las antes ou escolher uma única mão explicitamente.

O .mid original fica ao lado da notação, com sufixo .source.mid, oculto dos cartões da biblioteca e incluído no backup. Nomes duplicados preservam cada par. Falha comum remove apenas o par novo; interrupção abrupta pode deixar par incompleto. Nenhum dado anterior é sobrescrito.

Limites: 16 MiB entrada/saída, 64 trilhas, 200 mil eventos, 10 mil notas, 4000 compassos 4/4. Rejeita formato 2, SMPTE, chunks extras/truncados, VLQ >4 bytes, dados inválidos, notas sem Note Off, Note Off órfão, duração zero e sobreposição da mesma altura/canal dentro de uma trilha. Esses casos não são corrigidos por suposição. Eventos de sistema e meta desconhecidos são ignorados conforme tamanho declarado; não enviados ao piano.

MidiFileImportTest: 17 verificações, incluindo mão direita grave/esquerda aguda, running status, Note On zero, ligadura entre compassos, tempo, nomes repetidos, fonte original e entradas adversariais. MidiFileSelfTest no Android: gravação, mão, nomes e backup do par. Confirmar Actions antes de integração; nenhuma evidência física declarada. Próximo trabalho: exercitar seletor de trilhas no emulador, compatibilidade de caminhos de backup e testes adversariais restantes. Etapa 5 ainda aberta.
