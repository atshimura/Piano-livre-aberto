# 76 — Laboratório Musical / Criação C1: escopo-base, UX e continuidade

**Data do planejamento:** 30/10/2026  
**Situação atual (31/10/2026):** implementação em andamento na branch `feat/criacao-c1`, baseada na main `cf4b24b` (0.9.54/63). O documento continua sendo a fonte autoritativa de escopo; consulte o [registro técnico da implementação](78-CREACAO-C1-IMPLEMENTACAO.md) para distinguir código, testes e gates ainda pendentes.
**Issue de acompanhamento:** [#79 — Criação C1](https://github.com/atshimura/Piano-livre-aberto/issues/79).  
**Instrução de retomada:** [prompt C1](prompts/CRIACAO_C1_IMPLEMENTACAO.md).  
**Frase do usuário para a próxima conversa:** “vamos continuar a implementação do criação - C1”.

> **Primeiro ato na retomada:** ler AGENTS.md, este documento e o prompt associado; conferir main, PRs, Issues e Actions atuais. Este planejamento foi escrito enquanto main estava em f91c278 (beta 0.9.52), PR #76 (Home) e PR #78 (Descobrir + testes rápidos) estavam abertos, e issue #77 tratava da otimização da CI. **Não presumir que esse estado continue atual.**

## 1. Decisões e fronteira do escopo

### 1.1. Aprovado pelo usuário

- Acrescentar **Criação** como terceira opção do Laboratório, ao lado de **Descobrir** e **Aprender**.
- Propósito de produto: gravar músicas tocadas no teclado/piano MIDI, reproduzir no próprio instrumento e, em etapas seguintes, gerar uma partitura que possa ser salva e exportada em PDF.
- **Opção A:** começar com **uma gravação simples (uma tomada por composição)**; gravação em camadas, acompanhamento sobre gravações anteriores e múltiplas trilhas editáveis ficam para depois.
- Prosseguir com o planejamento detalhado da **C1**, incluindo navegação entre arquivos/projetos.

### 1.2. Arquitetura/experiência proposta para executar a C1

As seções abaixo concretizam o escopo-base solicitado; escolhas técnicas de baixo nível devem ser verificadas com o código no início da implementação. Não confundir protótipo de interface, resultado esperado e funcionalidade existente. Planejamos:

- **C1:** navegação, captura de eventos MIDI, reprodução via USB-MIDI, salvamento por projeto/perfil, busca/ordenação, Lixeira, recuperação e exportação .mid.
- **C2:** gerar MusicXML/partitura a partir de uma **cópia derivada** da execução original, com andamento, compasso, quantização, atribuição de pautas/mãos e revisão.
- **C3:** exportar MusicXML e PDF profissional paginado, preservar os metadados e os arquivos originais.
- **C4:** edição musical de notas/ritmo/compasso, versões e refinamento da transcrição.
- **Futuro:** gravação em camadas/overdub/múltiplas tomadas, somente depois de outra definição de produto.

**Configuração proposta para a futura tela C2, não aplicada na C1:** andamento inicial **100 BPM**, compasso **4/4**, quantização **1/16**. São valores iniciais editáveis, não regras inferidas da execução nem decisões irreversíveis. **Jamais quantizar ou alterar o arquivo de execução original.**

**Não incluído na C1:** geração de partitura, PDF, editor de pauta, exportação MusicXML, áudio por microfone, sintetizador interno, serviço remoto, conta online, telemetria ou distribuição paga.

## 2. Navegação e arquitetura de informação

### 2.1. Hierarquia

1. **Inicial** → cartão **Laboratório Musical**.
2. Barra do Laboratório: **Voltar | Descobrir | Aprender | Criação**. Descobrir permanece funcional; Aprender continua **visível e desabilitado** até sua entrega; Criação habilitada somente após C1 concluída.
3. **Criação — início:** duas ações claras, **Nova gravação** e **Minhas Criações**; quando houver, seção compacta **Recentes** (até três projetos, sem duplicar armazenamento).
4. **Nova gravação:** tela dedicada de captura → revisão da tomada → **Salvar projeto** (ou **Descartar**, com confirmação) → **Detalhes do projeto**.
5. **Minhas Criações:** lista pesquisável e ordenável → **Detalhes** → **Reproduzir / Renomear / Exportar MIDI / Excluir (Lixeira)** → voltar à mesma lista com busca, filtro, ordenação e posição aproximada mantidos.
6. **Lixeira:** lista separada → **Restaurar** ou **Excluir definitivamente**, sempre com confirmação nesta última ação.
7. **Recuperáveis:** lista de sessões interrompidas com diário válido → **Revisar / Salvar trecho íntegro / Descartar com confirmação**.

O botão Voltar deve respeitar a pilha acima; não voltar diretamente à Home ao sair dos detalhes. Voltar da página inicial da Criação retorna à Home; alternar para Descobrir encerra apenas a observação de Criação, nunca apaga dados.

### 2.2. Home da Criação

- Título “Criação Musical”; nome/ID público do perfil ativo; conexão MIDI com estados reais **Entrada** e **Saída** (uma porta não implica a outra).
- Ações grandes **Nova gravação** e **Minhas Criações**. Se o instrumento não recebe MIDI, a lista e a exportação permanecem acessíveis; o botão Gravar explica a exigência de porta de entrada.
- Recentes (máximo 3) com título, duração, data, acesso ao projeto.
- Vazio: “Suas composições aparecerão aqui. Conecte seu piano e comece uma gravação.”
- Não adicionar um quarto cartão à tela inicial geral: Criação mora dentro do Laboratório.

### 2.3. Minhas Criações — navegação entre arquivos/projetos

**Cada cartão/linha mostra** título, data de criação, data de última edição quando diferente, duração, estado (salvo/recuperável/na Lixeira) e affordance clara de abrir. Não exibir nomes internos de arquivo ou detalhes técnicos desnecessários.

**Busca:** campo “Buscar composição...” em títulos, sem distinção de caixa e de acentos; atualização durante digitação; mensagem e ação Limpar quando zero resultados. Busca não inclui conteúdo binário MIDI.

**Ordenação:** Mais recentes (padrão; por última alteração), Mais antigos, Nome A–Z, Nome Z–A, Maior duração, Menor duração. Desempate determinístico por ID. Preservar filtro, texto de busca e ordenação na navegação detalhes ↔ lista, sem confundir seleções entre perfis.

**Filtros:** Todas (apenas projetos não excluídos), Salvas, Recuperáveis e Lixeira. Lixeira nunca misturada com Todas; recuperáveis só se de fato recuperáveis por validação. Na lista normal, um projeto é aberto por toque no cartão e as ações destrutivas ficam no menu de detalhes, não em ícones pequenos demais.

**Detalhes:** título completo, data de gravação, última alteração, duração, quantidade de notas, nome do dispositivo quando observado, status e capacidade de reprodução; botões **Reproduzir no piano**, **Renomear**, **Exportar MIDI**, **Excluir** e **Voltar**. Não mostrar “Gerar partitura” ou “Exportar PDF” como botões habilitados na C1; futuras capacidades entrarão aqui quando prontas.

**Renomeação:** títulos duplicados permitidos; projeto tem ID imutável independente do nome. Validar vazio, caracteres de controle, comprimento limitado e Unicode; nunca derivar caminho do título. Exibição segura de títulos longos (quebra/elipse com nome completo nos detalhes).

**Exclusão:** mover à Lixeira, preservando arquivo original; restauração mantém ID e propriedade. Exclusão definitiva com confirmação contextual mostrando título e caráter irreversível; sem limpeza automática. A Lixeira entra no backup; evitar perda silenciosa nas operações de perfil e restauração.

**Responsividade:** tablet em paisagem pode ter painel/lista ampla; celular em paisagem deve ter controles maiores e lista compacta, sem exigir gestos ocultos, com texto acessível, scroll e Back coerente. Revisar também rotação, tamanho de fonte do sistema e tela curta.

## 3. Gravação — interação e máquina de estados

### 3.1. Tela Nova gravação

Mostrar título provisório “Nova composição”, perfil, conexão/portas, cronômetro, contador de notas, estado de captura, botões **Gravar / Pausar / Continuar / Parar**. Gravar não exige metrônomo, prazo para tocar ou contagem regressiva. Após Parar: **Reproduzir**, **Salvar**, **Descartar**, **Voltar**. Avisar explicitamente quando não há notas gravadas.

**Back, saída de aba, troca de perfil e fechamento:** durante gravação ou revisão não salva, usar aviso e ação segura “Continuar / Salvar trecho / Descartar” conforme estado; jamais descartar silenciosamente. A gravação não prossegue oculta em background nesta primeira versão.

**Pausa:** comprimir o intervalo de pausa na linha do tempo de reprodução, mas conservar no journal os timestamps originais e a informação de segmentos; registrar desligamentos sintéticos de vozes/pedal com proveniência, sem atribuí-los ao teclado. Ao retomar, só novos Note On efetivos iniciam vozes; não inventar teclas sustentadas. Ao parar com notas ainda ativas, gerar finalização segura derivada, preservando os eventos recebidos sem adulteração.

### 3.2. Estados autorizados

**PRONTO → GRAVANDO ↔ PAUSADO → REVISÃO → SALVO**.  
**GRAVANDO/PAUSADO → INTERRUPÇÃO → RECUPERÁVEL / ERRO**.  
**SALVO → REPRODUZINDO ↔ REPRODUÇÃO_PAUSADA → SALVO**.  
**SALVO → LIXEIRA → SALVO ou EXCLUSÃO_DEFINITIVA**.

Usar transições explícitas, invalidação por geração/epoch e ações idempotentes quando cabível; proibir capturar e reproduzir o mesmo projeto ao mesmo tempo. O estado visual deve corresponder à persistência confirmada, não ao clique apenas.

### 3.3. Relógio, dados originais e semântica

**Fonte autoritativa:** log cronológico de eventos MIDI recebidos com **tempo monotônico de recepção**, ordem e índice de evento, tipo/status, canal, dados, origem da entrada e epoch da conexão. Preservar, quando disponível e válido, timestamps fornecidos pelo Android como metadados separados; não confiar que o timestamp de renderização da UI é o instante do ataque. Medir e documentar precisão, jitter e limites.

Capturar ao menos **Note On, Note Off, velocity, canal e CC64**, inclusive valor intermediário se recebido, preservando ordenação e simultaneidade; Note On velocity 0 equivale semanticamente a Off sem perder evento original. Eventos CC120/121/123 e outros suportados exigem tratamento explícito e seguro; não inventar controladores recebidos nem prometer suportar todos os comandos do protocolo nesta versão.

**Critérios críticos:** repetir a mesma altura após soltar gera novo ataque; notas em canais distintos não são colapsadas; pedal por canal independente de soltura física; mensagens obsoletas de conexão/foreground descartadas; sem eco do MIDI produzido por reprodução; em pausa ou cancelamento, notas/pedal controláveis devem ser liberados. Preservar tanto registro bruto quanto derivação reproduzível e o motivo das correções de segurança.

**Silêncio de abertura/fecho:** permitir na reprodução uma opção **Ignorar silêncio inicial/final**, com padrão recomendado ativado, sem alterar o original; depois é possível desativar. Não remover pausas internas.

## 4. Reprodução pelo instrumento

- Exigir **porta de saída USB-MIDI funcional**, não apenas dispositivo listado. A possibilidade de entrada e saída deve ser exibida separadamente.
- Agendar eventos na ordem e no tempo efetivo gravado; notas com mesma timestamp mantêm ordenação musical segura, liberando Off antes de On quando preciso rearticular; permitir CC64 proporcional aos eventos compatíveis; evitar desfigurar dinâmica e duração.
- Controles Reproduzir, Pausar, Continuar, Parar, posição/duração. **Busca/seek** livre só se puder recompor com segurança notas e controladores; caso contrário, deixar explicitamente fora da C1.
- Desconexão, saída da tela, processo em background, falha de envio: cancelar callbacks/epoch e emitir Note Off, pedal Off (CC64=0) e All Notes Off conforme porta efetiva e mecanismo existente. Não religar reprodução automaticamente após reconectar.
- Não prometer som pelo celular, nem áudio do P-45 sem teste físico. O app não será sintetizador; a reprodução depende das capacidades do instrumento conectado.
- Não alterar MidiPlayback para Estudar de modo que o comportamento musical existente regrida; preferir camada de reprodução de eventos gravados que reutilize envio e silenciamento de MidiConnection.

## 5. Persistência e projetos

### 5.1. Organização proposta, não destrutiva

Pasta privada sugerida: **filesDir/creations/{projectId}/**; ID UUID ou equivalente aleatório, estável, não reutilizável. Não usar título como nome físico. Componentes lógicos:

- **project.json:** schema/version, ID, profileId estável, título, criação/edição, status, duração, contadores, instrumento observado, integridade, referência à execução e opções de reprodução.
- **events.log / journal:** eventos com timestamp monotônico relativo, sessão, pausas e checksums/proveniência para recuperação incremental.
- **original.mid:** arquivo SMF gerado a partir do log, sem quantização e sem converter para partitura.
- **Futuro:** partitura derivada MusicXML, preferências de transcrição e PDF exportado (não implementar na C1).

O log cronológico do recebimento é **o original exato no domínio suportado**; SMF é formato interoperável com resolução finita, portanto tolerâncias de arredondamento precisam ser medidas e documentadas. Exportar SMF 0/1 com PPQN e mapa de tempo explícitos; escolher resolução após teste e validar roundtrip com MidiFileImport, evitando classificar aproximação de ticks como preservação temporal matemática absoluta.

**Salvamento:** reserva de novo ID, journals incrementais, limites antes de escrever, flush/fsync conforme viável, arquivo temporário, validação e publicação por rename; dados incompletos não aparecem como projeto SALVO. Nunca sobrescrever outro projeto. Manter integridade e referências de metadados/SMF de maneira consistente após falha. Projeto recuperável tem estado próprio até finalização.

**Metadados:** limite explícito/validado para nome, eventos, duração, volume, notas/CC; não deixar que projetos grandes bloqueiem a UI ou consumam memória não limitada. Pré-limites técnicos devem resultar de testes, sem inventar uma duração máxima como decisão de produto.

### 5.2. Interrupções

- Ao ir ao background: suspender captura, registrar fronteira, preservar o que já foi gravado com segurança; ao retornar, oferecer **Salvar trecho / Retomar explicitamente / Descartar**. Não iniciar gravação oculta.
- Falha ou desconexão: registrar causa sem dados sensíveis, congelar epoch, preservar log válido e não juntar automaticamente eventos de outra conexão.
- Na abertura do app: verificar journals/pares metadados-SMF, validar checksums e sequência, identificar RECUPERÁVEL. Recuperar apenas prefixo íntegro com aviso de eventuais eventos perdidos; não inventar notas, não promover arquivo truncado a válido.
- Espaço insuficiente, corrupção e exceções: não excluir criações anteriores; bloqueio de escrita e aviso com possibilidade de exportar o último trecho íntegro.

## 6. Perfis, exclusão e biblioteca existente

- Usar ID de perfil estável de **LocalProfiles** (ver docs/33-PERFIS-ESTAVEIS.md); renomear/avatar não altera dono. A seleção ativa determina filtragem, **não a localização física por nome de perfil**.
- Troca de perfil com sessão ativa exige encerrar/salvar/suspender; nunca registrar eventos no perfil errado. Manter filtros/seleção local por perfil.
- Exclusão de perfil não pode apagar composições silenciosamente. **Proposta de UX:** oferecer transferência para perfil existente antes de excluir; alternativamente bloquear exclusão até decisão explícita de exportar/remover projetos. Implementador deverá respeitar proteção de último perfil e invariantes do método LocalProfiles; não assumir que apagar apenas as preferências resolve o ciclo de vida de projetos.
- Biblioteca de partituras importadas permanece separada da biblioteca Minhas Criações; nenhum .mid aparece como MusicXML existente automaticamente. Favoritos/coleções de partituras não são afetados.
- Lixeira, recuperáveis e projetos salvos precisam estar coerentes em listagem, busca, backup e troca de perfil.

## 7. Backup, restauração e atualização — gate central

**Estado de base auditado:** BackupArchive.java versão **2** inclui preferências tipadas e **filesDir/imported** (limites documentados 200 arquivos, 16 MiB cada, 64 MiB no total, arquivo JSON máximo 95 MiB), com pré-validação, journal, staging e rollback. **Não inclui filesDir/creations**, portanto **salvar projetos sem ampliar backup seria entrega incompleta**.

**Direção proposta:** formato de backup novo (por exemplo v3, somente após inspecionar contrato/testes de v2) com seção de projetos versionada, hashes, arquivos originais e Lixeira, associação aos perfis e limites explícitos; continua **lendo backups v1/v2** existentes. Aplicar transação consistente a preferências + imported + creations, com rollback/recuperação após interrupção; não adaptar por exclusão superficial de diretórios.

**Restauração de arquivo legado sem criações:** não apagar silenciosamente criações que já existem no destino. Solução segura inicial: bloquear a substituição quando houver criações locais e a operação não puder preservá-las com referências de perfil corretas; mostrar instrução clara para fazer backup novo e confirmar uma estratégia explícita. Implementar qualquer opção de preservação/mescla somente com teste de colisões, IDs, perfis, Lixeira e rollback. Os avisos da restauração atual são substitutivos; não declarar a mesma semântica segura para projetos sem nova prova.

**Testes:** roundtrip novo; legado v1/v2 com e sem criações locais; IDs/perfis ausentes, diretórios maliciosos, duplicações, limites, checksum inválido; simular interrupções antes/depois de journal e commit; atualizar beta pública preservando projetos sintéticos e dados antigos sem mexer no pacote privado.

**Exportação .mid:** usar ACTION_CREATE_DOCUMENT/SAF local com MIME apropriado, nome sugerido de título sanitizado, escolha de destino pelo usuário, bytes derivados da execução original, sem alterar projeto. Selecionar “Salvar” não precisa de acesso amplo a armazenamento nem permissão de internet. Testar reimportação com MidiFileImport e comparação nota/velocidade/canal/tempo dentro de tolerância documentada.

## 8. Implementação e módulos de referência

Antes de editar código, ler: **AGENTS.md; README.md; docs/00-INDICE.md; docs/10-CONTINUIDADE.md; docs/05-DADOS-BACKUP.md; docs/33-PERFIS-ESTAVEIS.md; docs/37-IMPORTACAO-MIDI.md; docs/38-COMPATIBILIDADE-DE-DADOS.md; docs/45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md; docs/66-LABORATORIO-DESCOBRIR-1.0.md**, além de quaisquer docs mais recentes de Descobrir.

Código inicial a inspecionar (nomes verificados no estado de referência):

| Componente | Por que importa |
| --- | --- |
| MainActivity.java / createReceiver, onMidiMessage, onPause/onResume | entrada MIDI/epoch/foreground, roteamento e lifecycle |
| MidiProtocol.java, MidiConnection.java, AndroidMidiPorts.java | decodificação, portas reais, envio seguro e liberação |
| MidiPerformance.java | política de notas/pedal; **não** usá-lo como fonte do log bruto sem auditoria |
| MidiPlayback.java, MusicLabPlaybackPlan.java | motores atuais; distinção reprodução de partitura x captura livre |
| MidiFileImport.java, MidiImportStorage.java | leitura/roundtrip SMF, importação e limites; não duplicar parsers |
| PianoScreens.java, MusicLabDiscoveryScreen.java | navegação das telas; preservar Descobrir e Aprender desabilitado |
| LocalProfiles.java, StudyHistory.java | propriedade de projetos por ID; histórico de estudo não é biblioteca |
| BackupArchive.java, ScoreImport.java, SessionPreferences.java | backup v2, staging/rollback e validação de nomes/limites |
| NotationView.java, Score.java | uso futuro C2/C3, **sem renderização de partitura na C1** |

Consultar a main atual antes de editar: PR [#76](https://github.com/atshimura/Piano-livre-aberto/pull/76) (Home), PR [#78](https://github.com/atshimura/Piano-livre-aberto/pull/78) (Descobrir + CI) e issue [#77](https://github.com/atshimura/Piano-livre-aberto/issues/77) (testes rápidos). **Não basear código em um branch documental ou de tela anterior automaticamente**; sequenciar merges e resolver conflitos sem regredir.

**Subentregas recomendadas (PRs pequenos conforme dependências):**
- **C1.1** UI de Criação, navegação, listagem vazia, rotas/Back, testes UI.
- **C1.2** captura MIDI e estado/tempo, pausa/desconexão, testes JVM e emulador.
- **C1.3** persistência local, journals/recuperação, perfil, busca, ordenação, Lixeira.
- **C1.4** saída MIDI cronometrada, cancelamento e exportação SMF.
- **C1.5** backup/restore versionados, compatibilidade/upgrade, suíte completa e documentação.

A ordem interna pode ajustar-se para evitar entregar uma gravação sem salvamento confiável: **não integrar uma subentrega que exponha um botão funcional sem proteção de dados**.

## 9. Matriz de aceitação

| Cenário | Resultado esperado |
| --- | --- |
| Home → Laboratório → Criação | navegável; Descobrir intacto; Aprender não habilitado indevidamente |
| Abertura sem entrada MIDI | biblioteca acessível; gravar informa impossibilidade, sem estados falsos |
| Gravar nota, acorde, rearticulação, velocity, CC64 | log preserva canal/ordem/tempo, sem quantização |
| Pausar, continuar e parar com pedal/notas ativos | evento original preservado; encerramento derivado seguro; intervalos de pausa não invadem reprodução |
| Gravar, salvar, reiniciar app, reabrir | mesmo projeto ID, arquivo e metadados, tempo preservado |
| Vários projetos e nomes repetidos | acesso por ID sem sobrescrita, busca/ordenação determinísticas |
| Voltar com gravação não salva, fechar app, interrupção de energia simulada | sem perda silenciosa; recuperar apenas journal íntegro |
| Trocar perfil / renomear perfil | projetos só do ID correspondente; sem gravação atravessando perfil |
| Excluir/restaurar/excluir definitivo | Lixeira coerente; confirmação antes da exclusão irreversível |
| Reproduzir com saída MIDI válida | evento enviado em ordem e tempo; pedal/notas liberados em parada e falha |
| Reproduzir sem saída MIDI | aviso; nenhum “som interno” inventado |
| Exportar .mid e reimportar | arquivo válido, sem quantização, com arredondamento e suporte explicitamente testados |
| Backup novo/antigo e falha de restauração | integridade, perfis, lixeira, rollback; projetos existentes não desaparecem silenciosamente |
| UI telefone/tablet em paisagem | controles legíveis, scroll, foco, Back e títulos longos corretos |
| Build/CI | tests JVM/JS/Android pertinentes, prova de regressão completa antes da integração |
| Hardware físico | Yamaha P-45 + Realme 7 5G + Lenovo Tab M9 **pendentes até teste do usuário** |

**CI:** quando issue #77/PR #78 trouxerem modo rápido *realmente implantado*, usá-lo nos ciclos de UI; manter gate completo no SHA de PR antes de merge e no SHA da main antes da Release. Não inventar tempos de execução ou confundir testes sintéticos com Yamaha físico.

## 10. Continuidade, documentação e registro de entrega

Ao ouvir **“vamos continuar a implementação do criação - C1”**, a IA deve:
1. localizar issue #79, ler este documento, prompt e AGENTS.md; verificar links e estado do código;
2. apurar PRs #76/#78, issue #77, workflows, versions, release e trabalhos simultâneos; se já integrados, usar main atual;
3. implementar C1 progressivamente em branches/PRs com testes e documentação, sem precisar perguntar sobre decisões técnicas rotineiras;
4. respeitar separação entre **funções solicitadas**, **implementadas** e **testadas**, e registrar SHA/checks/artefatos/limitações físicas ao final;
5. não declarar C1 concluída apenas por criar abas, documentação ou primeiro APK; aplicar a matriz de aceite.

O Plano Geral até V1 e a C1 são trilhas distintas: nada aqui declara versão 1.0 concluída ou autoriza substituir a assinatura permanente, divulgar dados reais ou acessar o repositório histórico privado. O app permanece **offline**, sem novos serviços, telemetria ou permissões amplas.

**Estado atual:** C1.1–C1.5 implementadas e validadas no SHA `a86ea4d6d57e8cfb3dc834d03ee989ef742a022d`; build #752 e suíte integral de emulador #729 passaram. PR #82 aberto para revisão, sem merge nem Release. Os testes físicos no Yamaha P-45, Realme 7 5G e Lenovo Tab M9 continuam pendentes. A próxima etapa de produto é C2; não declarar versão 1.0.


## Registro da entrega C1 — 31/10/2026

- PR #82 (`feat/criacao-c1`) contra a base consolidada `cf4b24b` (0.9.54/code63); candidata 0.9.55/code64.
- `build` #752: sucesso — https://github.com/atshimura/Piano-livre-aberto/actions/runs/41177942271.
- `emulator` #729: sucesso, incluindo `debug-apk`, `ui-fast` e `emulator-suite` — https://github.com/atshimura/Piano-livre-aberto/actions/runs/41177941440.
- Artefato APK de validação (debug): https://github.com/atshimura/Piano-livre-aberto/actions/runs/41177941440/artifacts/13034403866. Evidências de UI: https://github.com/atshimura/Piano-livre-aberto/actions/runs/41177941440/artifacts/13034094737.
- Testes físicos Yamaha P-45 + Realme 7 5G e Lenovo Tab M9 pendentes com o usuário.
- Não houve merge ou publicação: a integração na `main` dispara a geração de beta e aguarda autorização explícita de release. Próxima etapa após integração e hardware: C2.