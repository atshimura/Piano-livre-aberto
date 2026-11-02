# 111 — Criação: reforma visual responsiva em três blocos (A/B/C)

**Aprovação:** 02/11/2026. **Branch:** `feat/home-ajustes-acumulados`. Alteração de interface acumulada após os ajustes 1–4, **sem APK, integração na main ou Release**. A revisão anterior está em `94-CRIACAO-MINIMALISTA-0958.md` e o contrato musical C2 em `101`–`105`.

## Bloco A — Início, Nova gravação, Minhas Criações

- Início: os dois atalhos **Nova gravação / Minhas Criações** passam ao componente responsivo, inclusive dentro da coluna esquerda do tablet.
- Nova gravação: mantém a linha de transporte com Gravar, Pausar, Continuar e Parar, mostrados conforme o estado; agrupa **Salvar composição / Reproduzir gravação / Exportar tomada MIDI** num conjunto adaptável de até três ações por linha. **Descartar** permanece separado, com a confirmação anterior. O grupo de revisão só aparece nos estados habilitados.
- Minhas Criações: busca, filtro Todas/Salvas/Recuperáveis/Lixeira e ordenação compartilham uma linha em telas largas (largura a partir de 780dp, fonte até 1,25); em telas menores a busca permanece em linha separada para conservar legibilidade. Buscas, seis ordenações, Lixeira e posição da lista continuam funcionando.

## Bloco B — Detalhes, recuperação, Lixeira

- Reprodução no piano: Reproduzir/Pausar/Continuar/Parar compõem **uma linha contextual**; somente ações cabíveis ao estado ficam visíveis. Não há reprodução sem saída MIDI funcional.
- Partitura: quando já existe uma derivação, **Visualizar e ajustar / Estudar esta partitura** formam um grupo responsivo. O primeiro **Gerar partitura** mantém sua função original.
- **Renomear / Exportar MIDI / Informações da gravação** compartilham espaço por largura disponível, sem alterar o painel de informações.
- A ação de recuperação permanece destacada. **Restaurar / Excluir definitivamente**, **Mover para Lixeira** e **Descartar** continuam separados e com confirmações; nenhuma gravação ou derivação é removida automaticamente.

## Bloco C — Prévia, divisão de pautas, diálogos

- Ações de prévia/Salvar/Estudar são agrupadas dinamicamente; os pares já presentes (Desfazer/Refazer, Pauta superior/inferior, Restaurar/Concluir) mantêm igual hierarquia.
- Seleção de notas: cada linha mostra **checkbox da nota + Acorde**, mantendo seleção gráfica, `sourceId` e a descrição acessível completa do acorde. A lista economiza cerca de uma linha por nota, inclusive na janela de 12 notas.
- **Selecionar intervalo de compassos / Desmarcar seleção** compartilham o espaço.
- Diálogos Andamento/Compasso/Precisão e Do/Até compasso reorganizam campos em colunas apenas quando a largura interna efetivamente comportar as fontes.

## Regras de responsividade e segurança

- Os componentes responsivos medem a **largura do contêiner/card** ao organizar até três controles em linha; o número de colunas diminui quando a fonte aumenta ou o espaço fica restrito. Fontes de 14sp (C1), 13sp (C2) e dos demais controles **não são reduzidas**.
- Botões conservam alvo de no mínimo 48dp e até duas linhas de texto quando necessário. Não usar posicionamento rígido baseado apenas na resolução do dispositivo.
- Nenhuma mudança em `MidiTakeRecorder`, `MidiTakePlayer`, `CreationJournal`, `CreationProjectStore`, `CreationScoreStore`, `CreationScoreReview`, `CreationScoreTranscriber`, `BackupArchive`, permissões ou arquivos MusicXML/MIDI.

## Testes e limites da entrega

O workflow de testes básicos da branch executa testes de contrato Python, compilação Java Android e teste de estado da tela Estudar **sem `assemble`**; verifica que não foi criado APK. Testes visuais de fluxo em emulador ou aparelhos reais ficam para a validação conjunta posterior, com autorização específica. Não declarar efeito visual, desempenho ou acessibilidade real comprovados apenas pela revisão do código.

## Correção de regressão identificada na suíte de publicação 0.9.62

O teste Android de gravação C1 encontrou um defeito funcional: durante a revisão de gravação vazia, os botões Salvar/Exportar ainda não existiam na árvore de views porque o agrupamento responsivo só adicionava os botões após o primeiro layout. Corrigido com **montagem imediata em coluna** e redistribuição posterior pela largura do contêiner na thread UI, usando `post` para não modificar a hierarquia no meio do ciclo de layout. Aplicado também aos comandos e campos da prévia C2. A fixture `CreationSelfTest` exige explicitamente a presença imediata das ações e mantém a asserção de que uma gravação vazia não pode ser salva ou exportada.

## Correção adicional no teste funcional da candidata 0.9.62

O segundo percurso Android chegou à entrada Criação e encontrou `IllegalStateException: The specified child already has a parent` durante mudança de colunas. A reestruturação com controles pré-inicializados passa a **desanexar cada Button/View de seu pai anterior** antes de montar novas linhas; vale para o painel Criação e a prévia de partitura. Adicionada regressão nos contratos para impedir retorno do erro. Os resultados completos da nova revisão devem ser coletados pelo SHA da própria correção; nenhum APK foi publicado com a falha.
