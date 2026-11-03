# 117 — Implementação 3: interpretação rítmica e Uniformizar trecho

**Decisão de produto:** 03/11/2026. **Estado:** especificação aprovada para desenvolvimento posterior; **não implementada neste PR documental**.
**Dependências:** Implementação 1 (#102), Implementação 2 (#103), base atual `feat/creation-score-separate-delete-20261103`; `main` protegida não é alterada.
**Arquivos de referência:** `CreationScoreTranscriber.java`, `CreationScoreReview.java`, `CreationScorePreviewScreen.java`, `CreationScoreStore.java`, `docs/101-CRIACAO-C2-PLANO-EXECUCAO.md`.

## 1. Decisões aprovadas — objetivo musical

O compositor deve poder gravar uma sequência imaginada como semínimas e obter **semínimas na partitura**, mesmo se os intervalos entre ataques humanos variaram. A transcrição atual usa predominantemente a diferença Note On–Note Off para a duração notada e quantiza ao andamento escolhido. A Implementação 3 deverá priorizar **intervalos entre ataques Note On sucessivos de uma mesma voz**, interpretados em uma pulsação musical coerente. Note Off, pedal e articulação passam a evidências complementares, sobretudo para staccato, pausas, sustentação da última nota e ambiguidades.

**Exemplo de aceitação:** em 100 BPM, ataques de uma melodia aos intervalos de 570, 640, 550 e 610 ms podem formar uma sequência de semínimas, respeitado o contexto. Não converter qualquer intervalo arbitrário em semínima, nem prometer inferência infalível da intenção.

**Separação obrigatória:** mãos direita e esquerda são linhas de interpretação independentes, sincronizadas por andamento e compassos comuns; quando há polifonia, cada voz mantém sua própria sequência de ataques. Acordes não são interpretados como sucessão de notas individuais. A **pauta superior/inferior não comprova a mão física**: se a gravação não trouxer informação confiável, a aplicação deverá oferecer atribuição/ajuste explícito, mantendo notas não atribuídas sem inventar mão pela altura.

## 2. Ajuste geral: regularização rítmica

- Em **Ajustar ritmo**, incluir controle de **Regularização rítmica** para a partitura inteira e, quando houver seleção, aplicação localizada. Níveis acessíveis como Rigorosa / Equilibrada (padrão proposto) / Flexível, com prévia recomputada. Valores numéricos, limites, histerese e comportamento de cada nível dependem de calibração com corpus sintético e execuções consentidas; não escolher percentuais arbitrários.
- **Andamento BPM, compasso e grade 1/8, 1/16, 1/32** continuam disponíveis e têm papéis distintos: BPM escala a pulsação; compasso organiza barras; grade define resolução; regularização define tolerância para agrupar padrões de ataques semelhantes sem apagar ritmos contrastantes.
- Não promover notas de outras vozes/mãos a próximos ataques de uma voz; preservar semínimas/colcheias/síncopes legítimas, pequenas antecipações de acordes e simultaneidade.
- Reportar casos ambíguos e permitir intervenção, sem forçar notas de duração zero ou alterar automaticamente os dados MIDI originais.

## 3. Nova ferramenta obrigatória: Uniformizar trecho

### 3.1. Local e seleção

Na **prévia/ajuste da partitura**, oferecer ação claramente denominada **Uniformizar trecho**, integrada à seleção visual existente e à alternativa por lista. O usuário poderá:

1. Selecionar **uma ou várias notas**, inclusive sequência não contígua (informando explicitamente o escopo), ou selecionar **intervalo de compassos inclusivo** (ex.: 3–6); deve ser possível selecionar por toque na pauta ou por lista acessível.
2. Escolher o alcance entre **mão direita**, **mão esquerda**, **ambas** e, em trechos polifônicos, **voz específica**, quando a atribuição estiver disponível; jamais deduzir a mão apenas da clave/pauta. Seleção vazia não habilita a ação.
3. Escolher a **figura musical de referência**: **semibreve, mínima, semínima, colcheia, semicolcheia e fusa** (figuras já representáveis no motor atual), com valores pontuados somente quando viabilizados e testados na interface.
4. Visualizar uma **prévia antes/depois**, a quantidade de ataques/vozes/compassos afetados e os eventuais conflitos. Confirmar **Aplicar**, ou **Cancelar** sem mudar nada.

A intenção do comando não é meramente trocar o símbolo de uma nota: deve **reconstruir a organização rítmica notada** das notas selecionadas para que, por exemplo, as quatro notas pretendidas como semínimas ocupem posições de semínimas na voz correspondente. Notas simultâneas de um acorde mantêm início comum e duração coerente; vozes independentes e as outras mãos permanecem sincronizadas.

### 3.2. Segurança musical e comportamento em conflitos

- Preservar **identidade dos ataques-fonte (sourceId), alturas, ordem de ataques, evento MIDI, perfil e gravação original .plc**, sem alterar tempos capturados nem inventar/deletar ataques.
- **Nunca deslocar, encurtar, alongar nem substituir outras notas ou vozes não selecionadas silenciosamente.** Se a uniformização exigir deslocar notas fora do trecho, ultrapassar o compasso, colidir com ataque fixo, atravessar pausa intencional ou gerar sobreposição impossível, explicar o conflito e bloquear a aplicação; o usuário poderá ampliar a seleção, escolher outra figura ou cancelar. Não provocar efeitos colaterais invisíveis.
- Respeitar início e término do trecho, estrutura de compassos, pausas e ligaduras; não converter automaticamente silêncio real em sustentação, nem alongar a última nota até o próximo ataque arbitrário. Atribuições de mão/pauta já corrigidas não podem ser apagadas ao uniformizar.
- Não igualar ataques simultâneos de vozes distintas apenas por serem próximos no tempo; agrupamento de acorde requer validação do contexto/voz.
- Aplicar como **uma transação de edição**, com **Desfazer/Refazer**, **Restaurar interpretação automática** com aviso, recálculo MusicXML antes de confirmar persistência e indicação de alterações não salvas. Cancelar/erro preserva a partitura anterior.
- Salvar a interpretação/overrides junto com a derivação `.pls`, de modo que reabrir, fazer backup v4 e restaurar preservem a decisão. A evolução do formato deverá ser versionada e ler arquivos `.pls` legados v1 e backups antigos sem perda; não alterar `.plc`. Não alegar compatibilidade pronta antes dos testes.

### 3.3. Exemplo explícito

Composto em 4/4, um trecho com quatro ataques de uma voz, separados aproximadamente por 0,57/0,64/0,55/0,61 s (pulsação de 0,60 s), pode ter sido escrito automaticamente com figuras diferentes. O usuário seleciona **as quatro notas → Uniformizar trecho → Semínima → prévia → Aplicar**. A voz deve apresentar quatro semínimas em posições ritmicamente coerentes, totalizando quatro tempos no compasso quando o trecho começa em seu início e não há conflitos. Mão oposta, vozes não selecionadas e gravação original permanecem intactas. Se um acorde da mesma mão fizer parte da seleção, notas simultâneas continuam juntas.

## 4. Arquitetura prevista (não implementada)

1. Refatorar a modelagem rítmica em `CreationScoreTranscriber`: agrupar ataques por voz musical atribuída, separar mão da pauta, inferir pulsação/padrões, propor durações de escrita e resolver pausas; `Note Off` informa articulação, não define sozinho figura.
2. Evoluir `CreationScoreReview` para guardar regularização, seleções de fonte e **overrides rítmicos por sourceId**, além dos atuais overrides de pauta; aceitar seleção de intervalo de compassos, notas individuais e vozes sem perder identidades após requantização.
3. Expor **Uniformizar trecho** em `CreationScorePreviewScreen` com escolha de figura, prévia de conflitos, confirmação e leitura acessível. A prévia deve indicar claramente o que mudará e permitir desfazer; reaproveitar a infraestrutura existente de seleção de notas/compassos.
4. Atualizar serialização, verificação, backup e restauração `CreationScoreStore` / `BackupArchive` para guardar decisões rítmicas de forma compatível. Preservar integridade, orçamento de armazenamento, hash do MIDI e operação atômica.
5. Atualizar os roteiros da suíte completa para verificar a nova ferramenta, mas respeitar autorizações específicas: **somente testes básicos quando o desenvolvimento for solicitado; não executar suíte completa nem gerar APK sem autorização**.

**Risco destacado:** a C2-B atual identifica pauta e voz de notação, mas **não conhece necessariamente a mão física**; a separação direita/esquerda exigirá modelo/atribuição explícitos, e não uma simples troca no seletor de pauta. Não tratar a proposta como recurso existente.

## 5. Matriz mínima de testes futuros

| Situação | Resultado exigido |
| --- | --- |
| Quatro ataques com variações humanas em 100 BPM | Regularização ou Uniformizar trecho → quatro semínimas coerentes |
| Seleção de três notas em sequência e de compassos 3–6 | Apenas notas/vozes selecionadas alteradas; escopo mostrado antes de aplicar |
| Acordes simultâneos e mãos em ritmos diferentes | Sem ataques duplicados, separação de mãos/vozes e sincronização preservadas |
| Voz interna sustenta mínima enquanto outra toca semínimas | Nota sustentada não encurtada por ataque de outra voz |
| Falsa identificação de mão por registro agudo/grave, mãos cruzadas | Atribuição explícita respeitada, sem dedução por altura/pauta |
| Figura não cabe no trecho / colide com nota fixa | Aviso claro; não salvar nem mover notas não selecionadas |
| Pausas, staccato, pedal e último ataque de voz | Nenhum prolongamento automático injustificado; Note Off complementar |
| Cancelar, Desfazer, Refazer e reabrir | Versão anterior intacta após cancelar/erro; escolhas persistem após salvar |
| Backup antigo, `.pls` v1, backup v4 | Leitura/restauração compatíveis sem perda e sem modificar `.plc` |
| Fluxos da Implementação 1 e 2, Lixeira/remoção | Continuam distinguindo desvincular, excluir partitura e excluir composição |
| Telefone/tablet, seleção visual e lista | Cobertura básica + suíte UI futura; hardware real validado somente pelo usuário |

## 6. Estado desta entrega

**Somente planejamento e documentação** desta decisão aprovada. Nenhum algoritmo de reconhecimento por ataques, controle de regularização, botão Uniformizar trecho ou mudança de persistência está implementado ainda; nenhum APK, emulador, suíte completa ou teste físico foi executado nesta entrega.

**Próxima ação de implementação:** iniciar pelo motor de ataques/vozes e corpus de regressão; em seguida, parâmetros/edição `Uniformizar trecho`, UI, persistência retrocompatível, testes básicos e adaptação da suíte completa sem executá-la. Não integrar à `main` protegida sem os requisitos de publicação.
