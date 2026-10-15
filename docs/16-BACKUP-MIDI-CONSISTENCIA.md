> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 16 — Evolução 0.9.3: segurança, reconhecimento e consistência

Data: 14/10/2026. A versão de desenvolvimento 0.9.3-beta (versionCode 12) incorpora os trabalhos técnicos solicitados, sem declarar o modo Espera pedagógico concluído ou USB-MIDI físico validado.

## Dados e backup

BackupArchive.java exporta JSON v2 com tipos exatos das preferências, checksum SHA-256 dos arquivos, limites de tamanho e validação integral antes da substituição. Lê backups v1 anteriores. A restauração agora exige confirmação, prepara arquivos em diretório temporário e usa registro de transação para recuperação de interrupções antes do carregamento dos perfis. Não introduz sincronização ou nuvem. Os limites técnicos atuais são 200 arquivos, 16 MiB cada, 64 MiB no conjunto, 95 MiB no JSON. BackupArchiveSelfTest exercita dados isolados em emulador: roundtrip, integridade, v1, nome malicioso e simulação de interrupção.

A sessão salva música, posição, mão, modo, velocidade, limites do trecho, teclado visível e perfil ativo. A troca de perfil foi corrigida para não sobrescrever o progresso do novo usuário com a sessão do anterior. O JSON não é criptografado e a restauração é substitutiva.

## Reconhecimento de durações e pedal

MidiPerformance.java registra ataque e soltura física por canal+altura, velocity, beat, horário monotônico e extensão de som por CC64. Mantém até 1.024 intervalos concluídos. A observação é ligada ao diagnóstico da Activity. O teste MidiPerformanceTest passou em 7.048 verificações sintéticas sobre durações, 88 teclas repetidas, pedal e canais. **O modo Espera não passou a exigir duração nem pedal**, pois a política pedagógica precisa da decisão do responsável pelo projeto.

## Consistência partitura–MIDI–teclado

MusicTimelineConsistencyTest.java comparou 40.071 asserções em 12.423 posições/mãos, entre Score.events, KeyboardGuide e MidiPlayback nas dez músicas. ScoreInventory.java e musicxml_visual_crosscheck.js verificaram correspondência de início, pauta e altura em 2.335 eventos entre parser Java e renderizador JS. Testes internos passaram. Ainda faltam testes de andamento variável, repetições e notação musical complexa. Nada substitui prova física com P-45 e Androids.

## Três decisões ainda necessárias

1. Notas extras no Espera: permitir avanço com acorde correto mesmo com notas adicionais erradas (indicando o erro), ou bloquear até que sobrem apenas as notas esperadas?
2. Acordes: permitir formação gradual enquanto todas as teclas permanecem apertadas, ou exigir simultaneidade dentro de uma janela temporal ajustável? O valor em milissegundos deve ser calibrado e aprovado.
3. Duração e pedal: na modalidade Aprendizagem, avançar ao reconhecer o ataque correto ou exigir nota mantida pelo tempo escrito? Quando houver indicação de pedal, exigir seu uso no Espera ou apenas orientar visualmente? As regras de Avaliação poderão diferir.

A próxima alteração pedagógica deverá ser feita após essas respostas. Até lá, as mudanças disponíveis preparam infraestrutura, dados e testes sem mudar indevidamente os critérios atuais.

## Controle de qualidade

Consultar workflow Build Piano Livre APK, Android emulator smoke test e Audit MIDI notes and hand selection na revisão mais recente. Apenas declarar a restauração validada após BACKUP_SELFTEST_PASS no emulador. Nunca afirmar que a aprovação da simulação valida o Yamaha P-45 físico.