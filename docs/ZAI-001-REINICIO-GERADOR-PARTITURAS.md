# ZAI-001 — Reinício do gerador de partituras: piano básico e reconhecimento rítmico

**Data:** 08/11/2026. **Estado:** planejamento aprovado; **nenhuma implementação ou validação executada nesta entrega**.  
**Repositório:** `atshimura/Piano-livre-aberto`. **Linha:** `zai/`; base de análise `zai/lab` em `cd25b03a347f1c0bd1ce45710da44d45e58880b6`.  
**Autorização do mantenedor:** substituir integralmente o gerador de partitura e seus ajustes antigos; **não** é exigida compatibilidade com o mecanismo/ajustes/derivações antigas. Manter a gravação MIDI C1 como entrada, não a lógica de transcrição C2.  
**Precedência:** este contrato substitui **na linha ZAi e apenas para a geração nova em Criação** os controles e algoritmos C2 descritos em docs/101–105, 117–120. Esses documentos continuam como histórico. Não altera o modo Estudar, o renderizador OSMD, o restante do produto nem a `main`.

## 1. Objetivo e trajetória mínima

Do projeto gravado (`.plc`) → tocar **Gerar partitura** → analisar ataques, liberações e pulsação → gerar MusicXML de piano → desenhar com OSMD → conferir e **Salvar**. O foco inicial são escalas, acordes, duas mãos e valores rítmicos simples, não uma transcrição universal ou um editor profissional.

**Critério de produto inegociável:** tocar a escala de Dó maior em semínimas e depois em mínimas deve produzir semínimas seguidas de mínimas, identificadas automaticamente a partir da execução MIDI, não por seleção manual das figuras. Aceitar primeiro uma execução estável e metrificada; não declarar reconhecimento infalível para execuções ambíguas.

## 2. Regras musicais da primeira versão

1. **Piano com duas pautas sempre presentes:** superior em clave de Sol (G2), inferior em clave de Fá (F4), ambas desde o primeiro compasso, ainda que uma mão esteja em silêncio.
2. **Fronteira fixa em Dó4, MIDI 60:** notas com pitch >= 60 vão para pauta superior; pitch <= 59 (incluindo Lá3, Si♭3 e Si3) para pauta inferior. Separação **gráfica**, não detecção da mão física. Um acorde atravessando a fronteira distribui as notas pelas duas pautas sem mudar altura.
3. **Compasso inicial:** 4/4; armadura de Dó maior (zero alterações); notas da escala natural sem acidentes. Valores a reconhecer na V0: semibreve, mínima, semínima, colcheia e semicolcheia, além de pausas equivalentes. Notas ligadas ao ultrapassar barras de compasso. Quiálteras, anacruses deliberadas, ornamentações, fórmulas variáveis, múltiplas tonalidades, soletração enarmônica sofisticada e edição manual ficam fora deste marco.
4. **Acordes:** ataques quantizados para o mesmo instante rítmico e mesma pauta com durações iguais são escritos empilhados (elemento MusicXML `<chord/>`). Notas que começam juntas e terminam em tempos diferentes **não devem perder duração**: usar vozes independentes quando necessário, sem fingir que todo ataque tem fim comum.
5. **Notas sobrepostas:** preservar cada par Note On/Note Off, incluindo nota longa enquanto outra começa. Atribuir vozes simples deterministicamente se houver conflito de ocupação; nunca encerrar nota no ataque seguinte de outro pitch apenas para caber em uma única voz.
6. **Pausas e espaço:** gerar pausa para intervalo musical sem notas na voz/pauta; não interpretar milissegundos de afastamento articulatório mínimo como nova pausa sistematicamente.
7. **Fonte original:** a gravação conserva canal, pitch, velocidade, Note On/Off, pedal, segmentos, pausas de gravação, interrupções e relógio monotônico; gerar partitura **não edita** `.plc`. CC64/pedal não aumenta automaticamente a duração **escrita** nesta fase. Note Off sintético (interrupção, stop, retrigger) deve ser identificado como encerramento de segurança.
8. **OSMD é único renderizador**: reusar o renderizador e a verificação de compatibilidade Criação → Estudar já existentes; não introduzir pauta esquemática, motor alternativo, serviço remoto ou nova dependência externa.

## 3. Reconhecimento automático da duração e do andamento

A notação exige **pulsação** (duração de uma semínima), **posição do ataque** na grade e **valor escrito**. O tempo real isolado não identifica a figura: uma tecla pressionada durante 1 s pode representar mínima a 120 BPM ou semínima a 60 BPM. O reconhecimento é uma inferência musical, não conversão fixa de milissegundos.

### 3.1. Dados e modelo intermediário

- Parear Note On (velocity > 0) com Note Off ou Note On velocity = 0 **pelo canal/pitch e ordem**, com eventos de encerramento sintético sinalizados. Tratar ataques repetidos, eventos órfãos, ordem, canais e limites de duração sem corrupção; registrar avisos legíveis.
- Converter tempos monotônicos para uma linha do tempo única de **segmentos ativos** (retirando pausas da função Gravar/Pausar); não converter a gravação para SMF de exportação para depois transcrever.
- Construir lista imutável de eventos notacionais `pitch, canal, startNs, endNs, syntheticEnd, sourceId`. Manter a relação auditável entre nota escrita e par MIDI original.
- A transcrição gera `beatOnset`, `beatEnd`, pauta, voz e razão de confiança; só então serializa MusicXML. **Separar captura, inferência musical, escrita XML e visualização**, com testes independentes.

### 3.2. Estimação de pulsação

- Testar **vários candidatos** de período de semínima usando diferenças de ataques consecutivos e durações efetivamente sustentadas; considerar múltiplos inteiros e subdivisões do pulso, não apenas média ou mediana bruta.
- Para cada candidato, avaliar erro de encaixe dos ataques e durações à grade (V0 = 1/16 de semínima), estabilidade do andamento, proporção de figuras usuais e penalidades por notação excessivamente fragmentada. Escolher hipótese consistente e determinística; preservar um indicador de confiança para evitar certeza falsa.
- Usar o **início relativo da tomada musical**, não o primeiro evento de conexão. Em execução simples sem anacruse, primeiro ataque fixa tempo 0.
- **Não fixar 120 BPM como resultado automático universal**. Se houver informação suficiente, apresentar BPM estimado e figuras identificadas. Se faltar referência (por exemplo, nota ou acorde isolado, ou andamento de dupla/metade indistinguível), assinalar “andamento não identificável com segurança”; permitir prévia aproximada com 120 BPM **claramente rotulada provisória**, sem afirmar que o valor musical está provado. A V0 não introduz caixa de configurações para contornar a falha.
- Evitar “oscilar” entre tempos a cada nota na V0; hipótese de pulso é global e estável para o exercício. Gravações com rubato amplo/variações reais de andamento devem emitir aviso de limitação, não corrigir tempos arbitrariamente.

### 3.3. Quantização e duração escrita

- Grade de 1/16 da semínima; testar tolerância temporal em torno de pulsos e subdivisões sem eliminar ataques distintos. Quantizar início e fim **separadamente**, usando duração entre Note On e Note Off; proibir notas de duração zero.
- Para execução **ligada ou sustentada**, o Note Off define o fim esperado; se coincide, dentro de tolerância, com o ataque seguinte, representa semínima/mínima contínua sem pausa espúria.
- **Ritmo x articulação:** ataque seguinte pode ajudar a estimar pulso, mas **não deve cortar** uma nota ainda pressionada. Em staccato, o intervalo entre ataques pode indicar valor de pulso diferente do tempo físico da tecla: V0 deve reconhecer essa ambiguidade e não prometer notação de articulação perfeita. Não criar pausa ou prolongamento que altere substancialmente a estrutura sem evidência.
- Para ataque quantizado igual mas tempos de soltura diferentes, manter comprimentos por voz/nota; se a notação suportada não acomodar a situação sem perda, devolver erro/aviso específico em vez de sobrescrever.
- Para valores que cruzam compasso, fragmentar com ligaduras de prolongamento. Pausas preenchem cada voz de cada compasso de modo musicalmente válido, sem duplicar silêncios de vozes que nunca aparecem.

### 3.4. Exemplo obrigatório

**Entrada de referência:** escala ascendente Dó4–Ré4–Mi4–Fá4–Sol4–Lá4–Si4–Dó5 tocada uma vez com 8 semínimas seguidas da mesma escala com 8 mínimas, em pulso constante, sem lacunas intencionais.

A 120 BPM, semínima ≈ 500 ms e mínima ≈ 1.000 ms; a 90 BPM, ≈ 667 ms e ≈ 1.333 ms. **Em ambos os casos** a saída esperada são **8 semínimas + 8 mínimas**, em **6 compassos de 4/4**, com notas na pauta superior, clave de Fá sempre visível e pausas apropriadas na pauta inferior. Não aceitar 16 notas escritas todas iguais, nem um BPM rígido de 120 que transforme o teste de 90 BPM em figuras falsas. Os tempos são aproximações para os fixtures sintéticos, não limiares fixos em produção.

## 4. MusicXML correto e legível

- MusicXML `score-partwise` com uma parte Piano, `<staves>2</staves>`, `<key><fifths>0</fifths></key>`, `<time>4/4</time>`, claves `G/2` e `F/4`, divisões fixas (por exemplo 960 ticks/semínima).
- Para cada pauta, escrever alturas reais e oitavas corretas; vozes por alocação de intervalos sem truncar durações; `<backup>`/posição temporal coerentes; `<staff>`, `<voice>`, `<duration>`, `<type>`, `<rest/>`, `<chord/>` e `<tie>`/ `<notations><tied>` onde pertinentes.
- Rejeitar XML inválido/instável, overflow de limites e perdas de notas antes de oferecer **Salvar** ou **Estudar**. A prévia, o material salvo e o fluxo Estudar devem usar **os mesmos bytes de MusicXML** quando possível; OSMD deve renderizar sem fallback.
- Manter a saída determinística para a mesma gravação e mesmas regras; título deve ser escapado corretamente; sem restrições de cor/zoom na codificação musical.

## 5. Interface simplificada

**Minhas Criações → Detalhes → Gerar partitura → Prévia → Salvar/Voltar**. Previa com partitura ocupando a maior área útil, cabeçalho compacto, zoom +/−, ajustar à largura e rolagem por compassos, sem recriar WebView desnecessariamente a cada ação. Exibir “Andamento estimado: X BPM” quando confiável ou aviso discreto de estimativa incerta.

**Remover** os controles da transcrição anterior: regularização rigorosa/equilibrada/flexível, ajuste de andamento, fórmula de compasso, grade, divisão manual de pautas, atribuição de mão e voz, uniformizar trecho, seleção de notas para correção, desfazer/refazer e restauração de sugestões. A V0 não possui edição musical; não esconder o mesmo ajuste antigo em outra tela.

A saída salva deve continuar acessível por **Visualizar**, **Estudar** e, se aplicável, **Adicionar à Biblioteca**, desde que a compatibilidade OSMD/Estudar tenha sido comprovada. Os controles existentes de reprodução MIDI da gravação original e exportação `.mid` não devem desaparecer.

## 6. Descarte do gerador antigo e limites de dados

**Decisão expressa:** não manter `CreationScoreTranscriber` e sua arquitetura de opções/heurísticas como motor legado, fallback ou modo alternativo; eliminar o código e a interface antiga **quando as ligações de compilação forem substituídas**, incluindo `CreationScoreReview`, ajustes em `CreationScorePreviewScreen` e testes que cobrem comportamentos revogados. Não adaptar o algoritmo antigo; implementar componentes novos e coesos, renomeados conforme arquitetura.

**Não é requisito** migrar ou reproduzir formato/ajustes de partituras derivadas `.pls` anteriores, nem manter o parser legado só para elas. Entretanto, **não executar exclusão automática silenciosa** dos arquivos já existentes: se forem inacessíveis, exibir estado adequado e permitir gerar nova derivação a partir da gravação `.plc`; remover vínculos obsoletos da Biblioteca com segurança, sem apagar gravações nem outros dados. Definir o formato mínimo novo de persistência e as rotinas de integridade/backup pertinentes antes da troca de código. A ausência de compatibilidade do derivado antigo **não autoriza apagar projetos, backups, outras partituras importadas, favoritos ou histórico**.

Manter `MidiTakeRecorder`, `CreationProjectStore`, estrutura de criações, transporte e saída MIDI, `Score`/OSMD, diagnóstico offline e demais recursos não relacionados. Planejar auditoria de dependências reais em `MusicCreationScreen`, `MainActivity`, `CreationScoreStore`, `CreationScoreDeletion`, `CreationStudyCompatibility`, `BackupArchive`, `LibraryStore` e self-tests/CI antes de apagar classes.

## 7. Etapas de implementação na ZAi (sequenciais)

| Etapa | Entrega | Critério de saída |
|---|---|---|
| Z-P0 | Inventário de referências C2 antigas, contratos e dependências de dados; fixtures de entrada/resultado; fechar estrutura do modelo novo | Mapa de remoções; nenhuma modificação destrutiva ainda |
| Z-P1 | Novo pareador MIDI e estimador de pulso + quantizador, sem uso do transcritor antigo | Testes JVM para 8 semínimas + 8 mínimas em 90/120 BPM, diferentes articulações e eventos MIDI anômalos |
| Z-P2 | Compositor de duas pautas/vozes e serializador MusicXML próprio | Casos de escalas, acordes, duas pautas, pausas e ligaduras; parser e OSMD/Chromium reproduzem estrutura musical |
| Z-P3 | Interface de prévia limpa, substituição do fluxo Salvar/Estudar e remoção física do mecanismo C2 antigo | Nenhum controle antigo ou fallback; compilação Android e inspeção de dependências; integridade das gravações `.plc` |
| Z-P4 | Testes de regressão dirigidos, backup/integridade, renderização visual celular/tablet; eventual APK de ensaio | Resultados registrados com SHA/execução real, pendências físicas explícitas |

Trabalhar em branches `zai/feat/...`, commits `[zai]`, PRs para `zai/lab`, mantendo a `main` e releases intocadas. Não alterar workflows antigos, assinatura, versionCode/versionName, serviços online ou dependências externas sem autorização. Uma integração eventual Z → `main` exige nova autorização e gates próprios, conforme `ZAI-CONTINUIDADE.md`.

## 8. Matriz mínima de testes e aceite

| ID | Gravação / cenário | Resultado exigido |
|---|---|---|
| R01 | Dó4→Dó5, 8 semínimas a 120 BPM | 8 semínimas corretas; Sol superior/Fá inferior |
| R02 | Dó4→Dó5, 8 semínimas e depois 8 mínimas, a 120 BPM | Exatamente 8 semínimas + 8 mínimas; 6 compassos 4/4 |
| R03 | Igual a R02 a 90 BPM | Mesmas figuras/compassos; andamento estimado coerente, sem impor 120 |
| R04 | Dó3–Mi3–Sol3, simultâneos | Acorde em Fá, três alturas corretas |
| R05 | Dó4–Mi4–Sol4, simultâneos | Acorde em Sol, três alturas corretas |
| R06 | Dois acordes R04/R05 simultâneos | Mesmos ataques em ambas as pautas; nenhuma nota perdida |
| R07 | Si3 e Dó4 juntos | Si3 na pauta inferior, Dó4 superior; limite MIDI 59/60 |
| R08 | Nota sustentada enquanto outra inicia | Duração integral preservada; vozes separadas se necessário |
| R09 | Nota atravessando barra | Ligadura MusicXML correta, sem novo ataque musical |
| R10 | Pausa intencional de 1 tempo entre notas | Pausa escrita e posição do ataque seguinte correta |
| R11 | Staccato e legato, microvariações e jitter | Sem corte arbitrário; limite de precisão e ambiguidades registrados |
| R12 | Única nota ou acorde, andamento indeterminado | Não anunciar BPM como identificado com certeza; comportamento provisório explícito |
| R13 | Gravação pausada/retomada, liberação sintética, Note Off órfão | Eventos/segmentos tratados ou aviso sem crash/corrupção |
| R14 | Salvar → Reabrir → Estudar com OSMD | Mesmo MusicXML, mesmas notas, pautas, compassos e durações; sem fallback |
| R15 | Projeto gravado, bibliotecas, backup e exclusão de derivado | `.plc` preservado; referências seguras; nenhum dado apagado inadvertidamente |

**Aceite musical:** fixtures com relógio de referência + partitura esperada legível, comparação de alturas, início, duração, compassos, pausas, vozes e claves. XML válido ou simples execução sem erro **não** bastam. **Aceite visual:** capturas em larguras de celular/tablet com pautas legíveis, zoom e navegação. **Aceite físico:** Yamaha P-45, Realme 7 5G e Lenovo Tab M9 pelo usuário quando possível; CI/emulador nunca equivale a esses ensaios.

## 9. Fora de escopo imediato e riscos

- Não detectar mão real: a pauta usa exclusivamente altura/ponto de corte.
- Não prometer classificação única para todas as execuções sem metrônomo. Há ambiguidade matemática entre pulsos de metade/dobro, especialmente uma nota isolada. Primeiro assegurar R02/R03; se o estimador não for confiável em casos reais, avaliar **referência explícita de pulso/metrônomo opcional** numa etapa futura, sem recolocar os antigos ajustes.
- Expressão, pedal gráfico, variação de BPM, ritmo pontuado complexo, polifonia avançada, cifras, exportação PDF/MusicXML externa (projeto C3), editor integral (C4) e gravação em camadas são expansões futuras; **não** bloquear o marco inicial por elas.
- A remoção do gerador antigo pode invalidar contratos históricos e antigos `.pls`; atualizar a suíte sem remover cobertura dos riscos de integridade, gravação, backup, biblioteca, OSMD e Estudar. Documentar incompatibilidade assumida para derivados históricos.

## 10. Evidência desta entrega documental

**Feito:** análise estática dos caminhos e aprovação de requisitos; plano e testes propostos.  
**Não feito:** exclusão de código, refatoração, commit funcional, execução de testes JVM/Android/OSMD, build APK, validação em dispositivos ou publicação.  
**Próximo passo técnico:** executar Z-P0 e Z-P1 em branch própria da ZAi, começando pelo fixture R02/R03; continuar sequencialmente quando os testes básicos de cada etapa passarem.
