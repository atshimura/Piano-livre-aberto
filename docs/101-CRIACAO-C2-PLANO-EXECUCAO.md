# 101 — Criação C2: gravar → transcrever → ajustar → salvar → estudar

**Data:** 01/11/2026. **Estado:** planejamento/contrato para implementação futura; **nenhum código C2 foi implementado por este documento**.  
**Repositório:** `atshimura/Piano-livre-aberto`. **Base analisada:** `main` `248c77787cab00761021bc84a804dd565921daec` (candidata 0.9.60/code69).  
**Issue:** [#95 — Criação C2](https://github.com/atshimura/Piano-livre-aberto/issues/95).  
**Predecessor:** [C1 — escopo](76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md), [C1 — implementação](78-CREACAO-C1-IMPLEMENTACAO.md), [Criação minimalista](94-CRIACAO-MINIMALISTA-0958.md).  
**Execução:** [guia com quatro blocos nomeados](prompts/CRIACAO_C2_EXECUCAO.md).

> **Prioridade de produto:** transformar uma gravação MIDI salva em uma partitura legível com um toque, permitir ajustes opcionais e preservar integralmente a execução original. O usuário aprovou a experiência detalhada com distribuição geral, por nota, acorde, seleção múltipla e intervalo de compassos. C2 não é um editor completo de notas.

## 1. Escopo e restrições

### 1.1. O que C2 entrega
1. **Gerar partitura** em detalhes de uma composição **salva** em **Laboratório Musical → Criação → Minhas Criações**, mesmo sem piano conectado. A pessoa não precisa ajustar parâmetros antes da primeira prévia.
2. Prévia offline MusicXML (renderizador OSMD existente) para piano nas duas pautas padrão (Sol/Fá). A primeira geração usa defaults **100 BPM, 4/4 e 1/16**, todos alteráveis; valores são propostas de interpretação, não tempos/compassos inferidos infalivelmente da fonte.
3. **Ajustar partitura:** andamento, compasso (inicialmente 2/4, 3/4, 4/4, 6/8), quantização rítmica (1/8, 1/16, 1/32) e **Ajustar divisão entre pautas** por sugestão musical, ponto de corte por altura e intervenções pontuais/manuais.
4. Ajustar diretamente **nota, acorde, múltiplas notas e trecho/intervalo de compassos**. Seleção múltipla explícita e seleção de intervalo são dois modos, não um substituto do outro. Mover para pauta superior ou inferior mantém altura, ataque, duração e identidade de cada nota.
5. **Desfazer**, **Refazer** (recomendado), **Restaurar sugestão**, **Cancelar/Descartar ajustes** e **Salvar divisão**. Uma sugestão geral alterada não apaga ajustes manuais sem confirmação. Sem gravação destrutiva.
6. **Salvar partitura** derivada vinculada ao mesmo projeto, reabrir/revisar e acessar **Estudar esta partitura** sem transformar o MIDI original nem cadastrá-la como partitura importada automaticamente.
7. **Ouvir trecho** usando exclusivamente saída MIDI real e reprodução da tomada original existente; se não houver saída, exibir ação indisponível com explicação, sem prometer som pelo telefone.

### 1.2. Fora da C2
- Exportação externa **MusicXML e PDF** profissional: **C3** (C2 pode armazenar MusicXML **internamente**).
- Edição arbitrária de altura/notas isoladas (adicionar/apagar/recolocar no tempo), editor integral de pauta e versões de edição avançada: **C4**.
- Gravação em camadas/overdub, gravação por microfone/áudio, sintetizador interno, serviços externos, nuvem, login, telemetria, bibliotecas/serviços pagos sem autorização.
- Identificação factual da **mão física** tocada. A separação entre pautas é apenas uma decisão de notação, não um resultado confiável de identificação de mão.

## 2. História do usuário — percurso e estados

1. Usuário grava uma música na **Criação** (C1), revisa e salva. O projeto conserva ID, perfil, segmentos, eventos MIDI, pedal e tempo original.
2. Vai a **Minhas Criações → Detalhes** e encontra **Gravação original** (reproduzir no piano/exportar .mid) e **Partitura** (ausente: botão **Gerar partitura**). O mesmo vale para gravação antiga: não precisa regravar ou conectar MIDI.
3. Toca em **Gerar partitura**. Se projeto inválido/sem ataques musicais/recuperável ainda não finalizado, recebe orientação sem apagar nada. Em gravação válida, geração ocorre fora da thread UI. Para execução mais longa, exibir progresso honesto por fases (ler/interpretar/compor/desenhar) e **Cancelar**. Não usar percentual falso.
4. Resultado abre **Prévia da partitura** com nome, número de compassos, pautas Sol/Fá, compasso/andamento/quantização, rolagem e realce de nota/compasso. Ações claras: **Ajustar**, **Salvar partitura**, **Voltar** e, após persistência, **Estudar esta partitura**. Pequena etiqueta: “Interpretação aproximada da gravação; a execução original não foi alterada”.
5. Em **Ajustar**, alteração de parâmetros recalcula **apenas a derivação** e atualiza a prévia. Recomenda-se debounce/cancelamento de geração antiga; não tentar reproduzir ou regravar a tomada no cálculo.
6. Em **Ajustar divisão entre pautas**, partitura é o centro visual. Tocar um cabeçote de nota mostra a identidade musical, compasso e pauta atual, com escolhas **Pauta superior (Sol)** / **Pauta inferior (Fá)**. Seleção direta, grupos, acordes e intervalo de compassos na mesma interface; controles fixos de desfazer/salvar.
7. Usuário visualiza e salva o resultado. **Partitura** passa a mostrar **Visualizar / Ajustar interpretação / Estudar**. **Gravação original** continua intacta e reproduzível. Revisar depois reabre os parâmetros/overrides salvos.
8. Se sair com edições não salvas, pedir **Salvar / Descartar alterações / Continuar editando**. Cancelamento, rotação, background ou erro não corrompem a partitura anterior nem a tomada. Se substituída uma derivação salva, preservar versão anterior até nova escrita atômica confirmada.

### 2.1. Telas/estados visuais
- **Detalhes C1 expandido:** dois grupos “Gravação original” e “Partitura”; estado “Não gerada”, “Gerando”, “Prévia não salva”, “Salva”, “Revisando” ou “Erro recuperável”. Se na Lixeira, mostrar sem editar/reproduzir normalmente; recuperar preserva derivação.
- **Prévia:** OSMD reusado via `NotationView`/motor MusicXML; cabeçalho compacto, partitura protagonista, parâmetros resumidos, Ajustar/Salvar/Estudar. Não duplicar controls de navegação nem acrescentar outra aba ao Laboratório.
- **Ajustar:** controles recolhíveis de BPM, compasso, grade e pautas. Alterações com prévia sem bloquear rolagem; status de atualização. Avisos discretos sobre interpretação aproximada.
- **Divisão entre pautas:** seleção com toque, contorno de seleção perceptível (não somente cor), ação “Pauta superior/inferior”; seleção de acorde, seleção múltipla e “Selecionar compassos: do X ao Y”. Botões Desfazer/Refazer, Restaurar sugestão e Salvar divisão.

## 3. Divisão entre pautas — contrato detalhado

### 3.1. Suggestão inicial
- Modo padrão **Sugestão musical automática**, heurística determinística de registro, continuidade melódica, agrupamento de ataques e economia de linhas suplementares. A heurística **não declara mão direita/esquerda** e deve exibir a nota técnica de que é uma sugestão.
- Alternativa **Corte por altura** com nota-limiar editável (exemplo inicial Dó4), em que a nota limiar ou acima vai, por sugestão, à pauta superior. A escolha por altura **não impede** overrides manuais nem destrói o arranjo anterior antes de confirmar.
- A nota escrita deve conservar pitch MIDI, instante e duração musical. Claves Sol/Fá permanecem padrão; linhas suplementares, acidentes, ligaduras, vozes e pausas são **regravados para a nova pauta** quando necessário.
- Sobreposições e mão cruzada: conservar vozes simultâneas, inclusive duas notas de mesma altura oriundas de ataques/canais diferentes. Nunca fundir/deselecionar eventos por ter mesmo pitch ou instante.

### 3.2. Seleção, identidade e permanência
- Cada **evento de ataque canônico** recebe uma identidade estável independente da renderização (p.ex. combinação de índice/ordem de evento, epoch, segmento e canal). A ligação nota visual → evento original deve sobreviver a mudança de compasso, andamento, quantização, nova paginação e reinício. **Não usar posição no SVG ou número sequencial de nota após requantizar como chave persistida.**
- Um ataque pode originar fragmentos MusicXML ligados ao cruzar compasso. Selecionar fragmento deve resolver **o mesmo evento-fonte** e mover todos os fragmentos da nota.
- Seleção de acorde: definir explicitamente o grupo pelo início rítmico renderizado, permitindo remover uma nota específica antes de mover; não misturar notas apenas por proximidade gráfica.
- Seleção de trecho: selecionar compassos [início,fim] inclusivos, com prévia da quantidade de notas afetadas; seleção múltipla acumula itens tocados. Impedir ação surpresa sobre toda a partitura.
- Recalcular sugestão geral preserva overrides manuais por ID. **Restaurar sugestão** pede confirmação antes de limpar overrides; Desfazer e Refazer garantem reversibilidade das ações da sessão; Salvar divisão persiste somente a configuração e overrides.
- Se nota for movida para pauta que torne leitura difícil, manter decisão e informar discretamente muitas linhas suplementares; não realizar mudança de altura para “acomodar” a pauta.

## 4. Motor de transcrição — política musical

**Fonte de verdade:** `MidiTakeRecorder.Event` + `Segment` armazenados em `CreationProjectStore`. O `.plc` da C1 contém eventos de status/dados MIDI, canal, tempo monotônico em ns, ordenação, epoch, flags de eventos sintéticos e metadados de entrada. `MidiStandardFileWriter` exporta SMF com mapa fixo 120 BPM/960 PPQN por contrato de interoperabilidade da C1: **não usar esse andamento de exportação como andamento do usuário na C2**. Os valores C2 são parâmetros independentes e não alteram o original.

1. **Normalização:** emparelhar Note On (vel>0) com Note Off/Note On(vel=0) por canal+altura e ordem; conservar rearticulações, ataques simultâneos de mesma altura em canais distintos, off sintético e fim por interrupção. Respeitar segmentos/pausas de C1 sem introduzir silêncio de pausa; preservar origem para avisos. Eventos órfãos/inconsistentes → diagnóstico seguro ou estado incompleto, **nunca descarte silencioso**.
2. **Duração física vs. pedal:** notas escritas seguem preferencialmente a duração de tecla; CC64 é informação expressiva separada e não deve estender todas as durações até soltar pedal. Eventuais marcas de pedal C2 só se houver suporte/teste no renderizador; sem prometer equivalência sonora da notação com o pedal.
3. **Tempo musical:** transformar ns de segmento em unidade musical contínua usando BPM escolhido (default 100). Definir início padrão na primeira entrada musical para evitar compassos iniciais vazios acidentais, mantendo duração/silêncio originais na fonte e oferecendo tratamento explícito de anacruse/silêncio inicial se necessário; compasso escolhido governa linhas de barra, incluindo 6/8 composto. Tempo fixo inicial; rubato/variações da execução podem exigir revisão e nunca devem ser “corrigidos” sem transparência.
4. **Quantização:** ajustar ataques e durações à grade selecionada (1/8,1/16,1/32) com política determinística e limite de deslocamento; manter ordem de ataques/nota de duração mínima, não converter nota rápida em duração zero, não fundir notas diferentes, conservar ataques repetidos. Mostre se passagens não puderem ser simplificadas sem perda expressiva. Suporte seguro a quiálteras/ritmos não binários: escrever corretamente quando suportado ou avisar e manter uma interpretação aproximada, **sem fabricar leitura precisa**.
5. **Ritmo escrito:** construir compassos, acordes, vozes independentes, pausas implícitas/explicitas, valores pontuados quando cabíveis, ligaduras de prolongamento através de barras, articulação de reataques e agrupamentos de colcheias; preservar alturas e correspondência 1:1 com notas-fonte, admitindo fragmentos ligados. **MusicXML estruturalmente correto** com `<divisions>`, `<backup>`/`<forward>` coerentes por voz e compassos completos quando aplicável, duas pautas e claves definidas.
6. **Ortografia de notas:** MIDI não distingue Dó♯/Ré♭ nem tonalidade. Usar escolha cromática determinística inicial, permitir armadura/tonalidade como futuro ajuste opcional se confirmado em C2, nunca afirmar detecção infalível. Sem edição fina do nome/altura até C4.
7. **Saída:** construir MusicXML intermediário **interno** e verificar via `Score`, `NotationView`/OSMD e um auditor independente de eventos/tempos. Diferenciar “arquivo de escrita legível” de “reprodução fiel da gravação”, pois a segunda segue o MIDI original. Não usar sem revisão o conversor aproximado de `MidiFileImport.musicXml()` como motor C2 definitivo: ele fixa 4/4 e contém políticas limitadas de vozes/duração.
8. **Escala:** gerar fora da UI thread, tamanho/tempo/memória limitados, cancelar por geração/versão, tratamento de arquivo longo e resultado excessivo, sem caches globais persistentes indevidos. Os limites reais deverão ser medidos na C2-A antes de definir números de corte; **nunca aumentar os atuais da C1 silenciosamente**.

## 5. Dados, perfil, backup e integridade

**Decisão técnica proposta, a validar na C2-A:** manter fonte C1 `creations/<UUID>.plc` intacta e guardar a transcrição em um **sidecar privado versionado** por ID do projeto (manifesto, parâmetros, overrides, versão do motor, MusicXML final, SHA-256 da **fonte MIDI canônica** (eventos ordenados, segmentos, epochs e marcadores sintéticos, sem título/data/Lixeira) e da derivação; opcionalmente, checksum do arquivo .plc para integridade local. Renomear ou mover para Lixeira não deve invalidar uma transcrição por mudar metadados; sem usar título como caminho). O projeto v1/v2 continua legível. Este formato separado evita modificar os bytes originais apenas para mudar a pauta. Se a auditoria da C2-A indicar outra solução mais segura, documentar tradeoffs antes de codificar.

- Salvar/revisar por escrita temporária + `fsync` e rename atômico, validação antes de publicar e preservação da derivação anterior em caso de falha; transcrição idempotente para mesmos dados/parametrização. Não permitir saída de storage corromper arquivo de gravação.
- Associar exclusivamente a `projectId` e `profileId` estáveis; respeitar Lixeira/restauração, exclusão definitiva confirmada, troca/exclusão de perfil e biblioteca por usuário. Status de arquivo derivado ausente/corrompido deve gerar aviso e opção “Gerar novamente”, jamais excluir o projeto.
- **`BackupArchive.VERSION=3` exporta só arquivos `.plc` em `creations`; sidecars NÃO entram no backup atual.** A entrega C2-C **deve estender o backup para formato novo versionado (proposta v4)** para incluir todas as derivações; reter leitura v1/v2/v3, preflight, checksums, recuperação, rollback e merge conservador. Não divulgar C2 como pronta antes de teste real de exportação/restauração do sidecar.
- Um backup legado sem partitura não deve apagar partitura local existente. O backup novo só importa derivação se projeto-fonte correspondente existe e fonte hash/proprietário são compatíveis; em colisão divergente não sobrescrever silenciosamente. Restaurar derivação não registrada como arquivo importado no repositório de partituras.
- Determinar limites de size por projeto/derivação e de biblioteca a partir de medições e do orçamento real (biblioteca C1 de 6 MiB / 200 projetos), com mensagem de erro recuperável ao exceder limite; evitar corrupção em upgrade/downgrade.
- Não manter dados de gravações em logs de CI ou em capturas públicas; usar apenas fixtures sintéticas ou composições autorizadas. Manter GPL-3.0 e inventário de direitos.

## 6. Estudar partitura derivada

- A partir de projeto **salvo/fora da Lixeira**, comando **Estudar esta partitura** abre `Score` gerado, por rota/identificador interno seguro, no Estudar existente. Não copiar silenciosamente para `imported/` nem sobrescrever outra MusicXML. Mostrar título e origem “Minha Criação” e retornar corretamente ao projeto.
- Espera/Contínuo/Demonstração podem usar os dados de notas MusicXML **quantizados**. Transparência: notação derivada e timing original podem divergir; reproduzir **a gravação original** só através do fluxo MIDI de C1. Desconexão segue políticas atuais; sem síntese interna presumida.
- Garantir que a tela Estudar não recarregue automaticamente ao reiniciar o app: a Home permanece tela de abertura, conforme issue #92.

## 7. Critérios de aceite e matriz mínima

| Cenário | Evidência / resultado exigido |
| --- | --- |
| Melodia simples, silêncios e pausas de gravação | Compassos/figuras legíveis, sem inserção silenciosa de silêncio de pausa |
| Acordes e múltiplas alturas simultâneas | Mesmo instante de ataque musical e acordes/vozes corretos |
| Notas repetidas, ligadas, notas sustentadas cruzando barras | Não colapsar ataques; ligaduras por compasso sem duplicação |
| Mesma altura em canais diferentes, cruzamento de pautas | Identidade por evento e dois eventos preservados |
| Pedal CC64, releases e interrupções sintéticas | Notação independente do pedal; warnings sem perda do original |
| 2/4, 3/4, 4/4, 6/8; 1/8, 1/16, 1/32 | Tempos e limites coerentes, mudança reprodutível |
| Mover uma nota, acorde e compasso inteiro | Som/altura/timing e hash original idênticos; escolha persistida |
| Sugestão geral + overrides individuais | Overrides mantidos; restaurar só após confirmação |
| Desfazer/refazer, salvar, cancelar, voltar, rotação, reboot | Estado consistente, sem perda da derivação anterior |
| Reabrir projeto sem MIDI; Estudar própria composição | Visualização offline, saída MIDI opcional, navegação correta |
| Backup v1/v2/v3, backup C2 novo, colisão e falha de disco | Compatibilidade, preflight SHA, rollback, originais intocados |
| Partitura longa, carga e cancelamento | Limites e erros explícitos sem UI bloqueada/crash e sem resultado parcial salvo |
| Fontes ampliadas e phone/tablet paisagem | Controles legíveis, seleção por nota acessível com alternativa textual |
| Hardware Realme 7 5G, Lenovo Tab M9, Yamaha P-45 | **Pendente até teste físico pelo usuário**; nunca substituir por emulador |

Teste unitário/JVM para algoritmo e persistência, XML parse+OSMD/Chromium para notação e geometrias, Android `ui-fast` nos ciclos curtos e `emulator-suite` para integração final. Não enfraquecer gates de publicação, assinatura permanente nem aprovar Release só com teste no emulador.

## 8. Quatro blocos de execução com entregáveis

### C2-A — Núcleo de transcrição
**Alvo:** contrato imutável da tomada → notas/eventos estáveis → grade rítmica/compassos/vozes/duas pautas → MusicXML válido em memória.  
**Entregáveis:** esquema/IDs e testes determinísticos JVM sobre acordes, notas repetidas, pausas, CC64, tie, compassos, quantização; provas MusicXML e medição de limites. Não habilitar UI ou salvar sidecar ainda.  
**Saída:** dados escritos relacionáveis 1:1 aos ataques originais; nenhuma perda silenciosa em fixtures. Revisar riscos antes do bloco B.

### C2-B — Interface de geração e revisão
**Alvo:** fluxo completo no **estado temporário**: Detalhes → Gerar → Prévia → Ajustar → Divisão entre pautas.  
**Entregáveis:** reusar estilo minimalista, OSMD, parâmetros, seleção de nota/acorde/múltiplas/compassos, inversão de pauta, undo/refazer, reset, mensagens e cancelamento seguro. Testes rápidos Android em telefone/tablet e XML visual. Sem persistência derivada definitiva até C.
**Saída:** cenários de UX completos com resultado provisório e sem alteração do projeto original.

### C2-C — Persistir, restaurar, Estudar
**Alvo:** derivação versionada atômica, vínculo por projeto/perfil, backup C2 com downgrade seguro e compatibilidade v1/v2/v3, Lixeira, proteção de concorrência; carregar MusicXML no Estudar.
**Entregáveis:** arquivo/manifesto com checksums, testes upgrade/restauração e colisões, roteamento Estudar/voltar, reabertura sem conexão MIDI, casos de falta de espaço/interrupção.
**Saída:** aceitar somente com backup/restauração real comprovados nos testes; derivação nunca fora do backup documentado.

### C2-D — Regressão, acessibilidade e entrega
**Alvo:** testes de corpus MusicXML+MIDI, grandes partituras, compasso/vozes e rotina de usuário, fotos de UI em aparelho virtual, compatibilidade com Criação C1, Descobrir, Estudar, importação e histórico. Documentar limites e mudanças de dados, testes, evidências, versões.
**Entregáveis:** CI completa do PR e do SHA da `main`, APK assinado permanentemente quando houver autorização da release da versão específica; registro claro de testes físicos pendentes.
**Saída:** código integrado com checks aprovados, documentação e artefatos; não chamar C2 concluída antes dos resultados.

## 9. Sequência, checkpoints e decisões técnicas futuras

- **Sem dependência de C2 para publicação da 0.9.60.** A C2 parte da `main` confirmada quando cada implementação iniciar, em branches/PRs por bloco ou pequenos subconjuntos; não usar a branch documental como código funcional. Não entrar no repositório privado histórico.
- Nomes das tarefas solicitáveis: **“Implemente C2-A”**, **“Implemente C2-B”**, **“Implemente C2-C”**, **“Implemente C2-D”**. Podem ser desenvolvidos sequencialmente e testados continuamente, com PRs pequenos. Agrupar testes de CI e evitar reprocessamentos desnecessários, **sem suprimir gates**.
- Preferências de produto já definidas: botão **Gerar partitura** (não ao salvar gravação), prévia imediata sem configurações prévias, duas pautas, divisão geral + manual + por acorde/trecho, seleção por compassos e nota, defaults 100/4-4/1-16 editáveis, original intocado, salvar e estudar. **Não abrir novas perguntas para esses itens.**
- Técnicas que exigem prototipagem em A/B: modelo eficiente de voz/pauta para polifonia longa, soletração de acidentes, fallback seguro para quiálteras/rubato, forma de mapear toque OSMD→evento, tamanho máximo da derivação e limites, formato exato sidecar/backup v4. Decisões devem resultar de testes e ser documentadas antes de codificar C/C-D.
- Mudanças de produto significativas, licenciamento de recursos externos, serviço pago ou direitos autorais exigem autorização do usuário. Sem implementação funcional, versão nova ou autorização de Release neste documento.
