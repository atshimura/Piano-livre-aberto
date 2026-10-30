# PROMPT DE RETOMADA E IMPLEMENTAÇÃO — CRIAÇÃO C1

> **Gatilho de outra conversa:** “vamos continuar a implementação do criação - C1”.
>
> **Estado:** instruções prontas para **uma execução futura**. A entrega que criou este arquivo foi **apenas documental**: nenhum motor de gravação, UI da Criação, APK ou teste da C1 foi implementado.

## Missão

Retomar o aplicativo Android **Piano Livre**, repositório exclusivo de desenvolvimento público **https://github.com/atshimura/Piano-livre-aberto**, e executar progressivamente a **Criação C1**: terceira opção do Laboratório Musical para capturar e gerenciar uma tomada MIDI, reproduzir no próprio instrumento conectado, guardar projetos locais recuperáveis e exportar .mid. Construir em subentregas testáveis, com máxima autonomia.

**A especificação canônica é docs/76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md.** Não substituir o documento pelo resumo deste prompt. **Issue de acompanhamento #79**: https://github.com/atshimura/Piano-livre-aberto/issues/79.

## Etapa zero: reconciliação antes de programar

1. Ler **AGENTS.md**, README.md, docs/00-INDICE.md, docs/10-CONTINUIDADE.md e a **especificação C1 integral**. Complementar com docs/05-DADOS-BACKUP.md, 33-PERFIS-ESTAVEIS.md, 37-IMPORTACAO-MIDI.md, 38-COMPATIBILIDADE-DE-DADOS.md, 45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md, 66-LABORATORIO-DESCOBRIR-1.0.md, versões/regras atuais e documentos criados após esta especificação.
2. Verificar **HEAD/proteção de main**, releases, branches, Issues e PRs. Na data da documentação, #76 (Home), #78 (Descobrir) eram PRs abertos; #77 era a issue de acelerar testes. **Pode ter mudado**: não supor que foram integrados, não usar o SHA histórico da main como base imutável.
3. Ler a implementação real: MainActivity (createReceiver/onMidiMessage/onPause/onResume), MidiProtocol, MidiConnection/AndroidMidiPorts, MidiPerformance, MidiFileImport, MidiImportStorage, MidiPlayback, MusicLabPlaybackPlan, PianoScreens/MusicLabDiscoveryScreen, LocalProfiles, BackupArchive, ScoreImport, SessionPreferences.
4. Conferir se o modo rápido da CI já está funcionando; se sim, usar durante iterações. Em qualquer caso, **exigir suíte completa antes de merge**, sem reduzir cobertura nem gates de publicação; conferir SHA do APK/CI.
5. Registrar análise de compatibilidade para a evolução do **BackupArchive v2** (preferências + imported) ao incluir **creations/**. Não começar com um armazenamento invisível ao backup.
6. Trabalhar apenas no repositório público, nunca no histórico privado. Branches/PRs sem push direto à main; respeito à GPL-3.0, licenças musicais, offline, ausência de telemetria/servidores e certificado permanente somente em Actions confiáveis.

## Produto: requisitos não negociáveis

- Barra Laboratório: **Descobrir | Aprender (continuará desabilitado) | Criação**, sem regressão do Descobrir. Não colocar um cartão Criação extra na Home.
- **Criação início → Nova gravação / Minhas Criações → projeto → Reproduzir/Renomear/Exportar MIDI/Excluir → Lixeira/Restaurar**. Recentes podem aparecer na home da Criação.
- Gravar **uma tomada**, controles iniciar/pausar/continuar/parar/salvar/descartar, UI paisagem telefone/tablet e proteção para Back/saída. Gravação livre sem metrônomo.
- Captura cronometrada MIDI baseada em eventos reais **Note On/Off, canais, velocidades e CC64** quando transmitido; log autoritativo **sem quantização**, sem confundir recebimento com renderização, sem misturar épocas de conexão, sem gravar eco da saída. Note On velocity zero tratado como Off, notas repetidas e sustain confiáveis.
- Salvar projetos com ID estável e perfil proprietário; nomes duplicados, pesquisa sem caixa/acentos, ordenação por data/nome/duração, filtros Todas/Salvas/Recuperáveis/Lixeira, exclusão reversível e confirmação definitiva. Não permitir troca de perfil durante captura sem decisão segura.
- Log incremental com journal e recuperação de trecho íntegro após interrupção; ao parar ou pausar com notas/pedal ativos, finalizar para reprodução de modo seguro **sem falsificar o original**. Não executar gravação oculta em background.
- Reproduzir apenas se houver **saída MIDI efetiva** no instrumento; pausa/parada, disconnect e background cancelam agendamentos/soltam notas+pedal e impedem retomada automática. Sem áudio interno.
- Exportar **Standard MIDI File (.mid)** sem quantização com SAF; validar roundtrip com importador existente e medir a eventual precisão finita de ticks.
- Backup com projetos, Lixeira e associação por perfil, **compatibilidade de leitura v1/v2**, preflight/rollback/recuperação; nenhuma restauração de backup legado pode apagar criações silenciosamente. Proteção contra caminhos arbitrários, hashes inválidos, estouro de limites e corrupção; não destruir dados existentes.
- Parâmetros de transcrição **100 BPM, 4/4, 1/16** são somente **proposta inicial de C2**, editáveis, **não** devem transformar a gravação da C1.
- **Não implementar na C1:** converter partitura, PDF, MusicXML exportado, edição de partitura, overdub/camadas, áudio por microfone, sintetizador, servidor, login, serviços pagos.

## Sequência sugerida

1. **C1.1 — UX e navegação:** novas telas e rotas, listagem vazia/recentes, perfil e MIDI reais, Back/abas; testes de interface.
2. **C1.2 — captura:** motor puro de eventos, relógio monotônico, estados, pauses, epochs, CC64/Note On-Off; testes determinísticos.
3. **C1.3 — persistência:** IDs, esquema versionado, projeto/journal, crash recovery, listagem, busca, ordenação, Lixeira, perfil e tratamento de exclusão; testes transacionais.
4. **C1.4 — reprodução/exportação:** agendador MIDI original com cancelamento e liberação segura; arquivo .mid via SAF; testes roundtrip e falhas.
5. **C1.5 — backup, migração e aceite:** extensão versionada do BackupArchive, testes antigos/novos, atualização beta, regressões completas, documentação, PR final.

É permitido reajustar a ordem técnica para **não expor funções sem persistência segura**. Documentar dependências e PRs pequenos, sempre conferindo o estado real. Não encerrar após criar a aba ou somente o motor. Pode prosseguir de C1.1 até C1.5 sem pedir permissão técnica rotineira; respeitar condições de merge da branch.

## Testes mínimos e evidências

- JVM: ataques/acordes/repetição/canais/velocity, CC64, pausa e retomada, shutdown, tempo e serialize/deserialize, SMF export→import, casos limite e entrada adversarial.
- Android: interface em paisagem celular/tablet, autorização de entrada e saída distinta, Back, longos títulos, lista cheia/vazia, filtros, restauração Lixeira, troca/exclusão de perfil, interrupção, journal recuperável, desconexão e reconexão sem ghost notes.
- Backup: novo/legado v1/v2, snapshot contendo composições e Lixeira, invalid checksum, limite, falha no meio de restore e rollback, preservar projetos diante de restauro antigo.
- Integração: não regredir Estudar, Descobrir, biblioteca, MusicXML, MIDI existente. Testes completos do **SHA efetivo** antes de merge e novamente da main antes de eventual Release.
- Teste físico (Yamaha P-45, Realme 7 5G, Lenovo Tab M9) **somente com usuário**; emulador não prova áudio/latência/pedal físico. Não declarar hardware validado.
- Capturas e logs sintéticos sem dados pessoais; commits, PRs, Actions e checksum/versões somente se comprovados.

## Entregas e comunicação

Atualizar docs/76, docs/00-INDICE, docs/10-CONTINUIDADE, documentação técnica, roadmap/changelog e **issue #79** com o status verdadeiro. Criar branch e PR, aplicar testes, corrigir regressões, registrar arquivos alterados e resultados. Somente publicar APK oficial quando a main final aprovada tiver checks, assinatura permanente e release autorizado.

Ao fechar cada subetapa, informar **o que foi feito / testes / limites / próximo trabalho** e manter um ponteiro persistente no GitHub. Se a conversa acabar no meio, registrar no PR/issue o estado e o próximo comando. Nunca confundir “planejado”, “implementado”, “aprovado pelo CI” e “validado em hardware”.

**Critério de C1 concluída:** usuário consegue gravar, parar/pausar, salvar, reabrir, encontrar, gerenciar, exportar e reproduzir a tomada no sistema MIDI, com preservação de projetos/perfis/backup e evidência suficiente. A fidelidade no teclado real continuará pendente até ensaio físico.
