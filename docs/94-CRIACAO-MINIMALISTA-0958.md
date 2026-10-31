# 94 — Criação Musical: interface minimalista responsiva (0.9.58)

## Decisão e limites
Implementação aprovada após cinco revisões da UX. Reformular somente a apresentação da C1, preservando os contratos do motor musical, tomada original, replay MIDI, recuperação por journal, permissões de perfil, backup v3, SAF e Lixeira. **Não** gerar partitura/PDF (C2/C3), nem áudio local, sincronização ou camadas.

## Telas e estados cobertos
1. **Início**: título, perfil, entrada/saída USB-MIDI independentes, gerenciamento da conexão, Nova gravação, Minhas Criações, Recentes (até 3) e estado vazio; no tablet, informações e Recentes aparecem em duas colunas.
2. **Nova gravação**: perfil, status MIDI, relógio, notas, eventos, botões condicionais para Pronto, Gravando, Pausado, Revisão e Interrompido. A revisão mantém Salvar, Reproduzir gravação, Exportar tomada MIDI, Parar reprodução e Descartar confirmado. Durante a reprodução da prévia, Pausar/Continuar continuam possíveis.
3. **Minhas Criações**: busca imediata, filtros Todas/Salvas/Recuperáveis/Lixeira, seis ordenações e busca/posição preservadas; cartões com título, datas/duração e estado, sem expor IDs internos; estado vazio e Limpar busca.
4. **Detalhes**: título e status, resumo (duração, notas e eventos), reprodução no piano com controles contextuais e posição/tempo, opção Ignorar silêncio, Renomear, Exportar MIDI, informações expansíveis (criação/edição/instrumento/perfil), Lixeira separada.
5. **Recuperáveis**: aviso e ação Revisar e salvar trecho recuperado, mantendo exportação, reprodução e Lixeira.
6. **Lixeira**: Restaurar e Excluir definitivamente em ações separadas, exclusão com confirmação contextual pelo título; nenhuma eliminação automática.
7. **Diálogos de segurança e arquivos**: salvar/renomear, sair com gravação não salva, descartar, exclusão definitiva e exportação SAF continuam no fluxo Android, sem alterar lógica.

A barra compartilhada Descobrir / Aprender (indisponível) / Criação permanece na página principal da Criação; telas internas mantêm cabeçalho de volta seguro. Não se perde dado ao sair sem decidir sobre tomada não salva.

## Estilo
Identidade do app: azul-marinho `#101C31`, azul de ação `#286BCB`, fundo `#F7F9FC`, cartões brancos com borda azul-clara, tipografia de alto contraste, alvo mínimo de 48 dp, margens consistentes. Botões perigosos separados visualmente e com confirmação. Não usar ícones sem descrição textual para funções críticas.

## Preservação e limitações
- Nenhuma alteração de `MidiTakeRecorder`, `MidiTakePlayer`, `CreationJournal`, `CreationProjectStore`, `BackupArchive`, `MidiConnection`, serialização e arquivos já existentes.
- A tela de gravação só muda visibilidade dos comandos conforme estado; os botões permanecem no layout para testes de UI e acessibilidade.
- Não fingir reprodução de áudio no dispositivo; saída MIDI funcional é requisito de reprodução.
- Gravações recuperáveis nunca são apagadas automaticamente e projeto mantém UUID/perfil.
- Não alterar a regra `publishApproved=false` nem assinatura: **APK somente release assinado permanentemente na main**.

## Verificações dirigidas
`PianoLivreAndroid/tests/emulator_ui_fast.py` foi estendido para visitar Criação, Minhas Criações, confirmar seletor de filtro e ordenação, abrir gravação Pronta, verificar comando Gravar e retornar com segurança. Testes `CreationSelfTest` e `emulator_creation_navigation.py` permanecem como regressões existentes. A CI pode acionar suites automaticamente por política (arquivos Java classificados `full`); **não acionar suite manualmente**. Declarar resultados efetivos após ações.

## Evidências futuras
Conferir fluxo de gravação/salvamento/revisão em aparelhos reais Realme 7 5G, Lenovo Tab M9 e Yamaha P-45. A simulação em Android não equivale a ensaio físico. Depois da PR aprovada, registrar SHA de merge, workflow de release, fingerprint, integridade e limitações.
