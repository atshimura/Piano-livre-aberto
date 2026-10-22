# 59 — Análise automática e apresentação adaptável do Estudo

## Escopo e arquitetura

Branch `feat/analise-estudo-adaptavel`, baseada na main `801543c` (Entrega A, PR #49 integrado). Não integra nem modifica a pesquisa #46. Os documentos 57/58 foram copiados da HEAD `62a9443` do PR #48 apenas para acrescentar este estado; aquele PR e sua branch permanecem intactos. Há sobreposição documental a reconciliar antes de integrar, sem trazer o restante daquela branch.

`StudyProfile` analisa o `Score.sourceXml` recebido pela mesma `NotationView` após a leitura/validação de `ScoreImport`/`Score.read`. Vale para importações XML/MXL e assets: não há regra por título. A análise visual ocorre ao abrir o Estudo (não durante a cópia transacional do arquivo). O catálogo não recebe alterações nos arquivos.

1. Analisa MusicXML: notas por compasso, quantidade de elementos, vozes/pautas, acidentes, acordes, ligaduras, vigas, quiálteras, repetições, letras, palavras, cifras, dedilhados e marcas de ensaio.
2. Gera cópia transitória de apresentação, desconsiderando atributos de quebra de sistema/página em `<print>`, sem `<lyric>` e sem listas auxiliares em `<words>` compostas exclusivamente por pelo menos quatro nomes de solfejo ou quatro números de dedos (1–5). Preserva outras palavras, andamento, dinâmica, cifras, dedilhado e instruções de repetição. A interface informa a ocultação e remete à Partitura Livre. O XML original não é regravado.
3. Entrega um `Document` XML local ao OSMD, evitando sua heurística de tratar strings curtas sem declaração XML como URLs. Renderiza OSMD e usa `EngravingAudit` sem o limite diagnóstico histórico de 96 notas. Examina caixas das cabeças, escala efetiva e reconciliação com os eventos escritos nativos.
4. Efetua no máximo uma nova gravura pelo OSMD: colisão entre ataques adjacentes amplia os fatores de espaçamento de vozes em 1,5; pauta pequena amplia em 1,25. Mantém escala proporcional, duas pautas e rolagem vertical bloqueada, usando o ajuste existente. Isso reduz compassos visíveis horizontalmente, mas **não aumenta por si o tamanho vertical das notas**. Alertas remanescentes não são declarados resolvidos.
5. O mapa óptico ignora como âncora temporal as pausas de compasso inteiro centralizadas pelo gravador: sua posição visual não representa o começo do compasso. A pausa continua desenhada. Essa regra geral corrige o recuo encontrado no corpus de Für Elise, sem alterar tempos ou notas.
6. Compara o multiconjunto completo de cabeças escritas (altura, grafia, oitava, beat, duração, pauta e compasso) com os eventos nativos, separando notas de graça. Divergência impede declarar sucesso do OSMD e ativa o aviso/fallback, sem modificar a reprodução.
7. Mantém relógio, MIDI, Espera/Contínuo/Demonstração, mãos, teclado e trechos nos mecanismos existentes. Partitura Livre recebe o XML completo; temporal continua selecionável. Se a WebView falhar, o fallback realmente ativo é identificado como **Esquemático**, com aviso e possibilidade de tentar novamente os renderizadores.

## Critérios técnicos experimentais

Valores implementados nesta branch, **não limites aprovados pelo usuário nem normas de gravura**:

| Diagnóstico | Medição |
|---|---|
| `small-staff-space` | distância entre linhas após escala menor que 6 CSS px |
| `adjacent-attack-overlap` | interseção das caixas de cabeças adjacentes na mesma pauta maior que 0,5 unidade SVG nos dois eixos, excluindo ataques simultâneos (diferença de beat até 1e-6) |
| `notehead-clipping` | caixa de cabeça fora do viewBox ajustado por mais de 0,5 unidade SVG |
| `vertical-overflow` | altura SVG maior que altura disponível em mais de 0,5 CSS px |
| `symbol-envelope-clipping` | envelope SVG total excede o viewBox por mais de 0,5 unidade SVG |
| `text-note-overlap` | limites de tinta medidos pela fonte ativa do navegador intersectam cabeça de nota em mais de 0,5 CSS px; conservador, sem contorno vetorial de cada glifo |
| `native-mapping` | uma ou mais cabeças não reconciliadas ou ambíguas |

Não há pontuação global de complexidade determinando o perfil. O cache é uma única entrada em memória, por igualdade exata do conteúdo XML e versão do analisador; mudança de conteúdo força nova análise. Geometria não é persistida e deve ser reavaliada em cada nova renderização. Nenhum schema, preferência, backup ou histórico é migrado. O custo adicional é local/offline; Playwright é usado somente nos testes de desenvolvimento.

## Limitações explícitas

Caixas são aproximações de contornos; a inspeção de colisões de cabeças adjacentes inclui triagem de textos sobre cabeças e envelope de todos os símbolos, mas não certifica todas as sobreposições de hastes, vigas ou arcos. A densidade estrutural é diagnóstico, não prova de legibilidade. Mais de duas pautas, ornamentos e percursos MusicXML mantêm os limites dos importadores/renderizadores existentes. A gravura pode continuar pequena devido a extremos verticais distantes. A B1 comprime localmente o espaço entre as duas pautas quando a geometria permite, sem janelas de compassos nem recorte horizontal por região. Gymnopédie melhora no viewport baixo, mas segue abaixo do limiar diagnóstico de 6 CSS px; isso não comprova legibilidade editorial ou física. Falhas/alertas remanescentes requerem avaliação visual, temporal alternativo ou correção futura; não remover notas para acomodar a tela.

Não se detecta uma nota musicalmente errada no próprio MusicXML sem referência independente. Comparar XML, Java e OSMD prova consistência entre representações, não fidelidade à composição. Sem testes físicos Realme 7 5G, Lenovo Tab M9 ou Yamaha P-45; não conclui V1.

## Validação

- `StudyProfileTest.js`: limites, caixas, simultaneidade e alertas.
- `StudyProfileCorpus.java`: catálogo completo + fixtures debug/importação; ataques de reprodução possuem referência escrita; bytes originais preservados.
- `StudyProfileBrowserTest.js`: mesmo corpus com OSMD real, grafia/oitava/duração, cache/invalidação, política de letras e falha real do OSMD. Relatório em artifact `adaptive-study-corpus`.
- Suítes preexistentes JVM/JS, auditorias XML/MIDI e Android permanecem obrigatórias nos workflows.
- Ambiente local: teste unitário, catálogo JVM e ScoreImportTest aprovados; Chromium local impedido por `socket() Operation not permitted` antes da renderização. Isso é bloqueio de ambiente, não aprovação da gravura. CI e emulador devem ser registrados por SHA após execução.

Versão proposta 0.9.48-open-beta / code57; mesmo pacote, sem APK de distribuição nesta PR, sem merge. Assinatura permanente somente no workflow confiável após eventual integração.

### Fixtures sem metadados suficientes de gravura

Fixtures de `tests/fixtures/motor` foram originalmente feitas para parsers e podem omitir tipos/claves. A auditoria não exige que o OSMD consiga gravar esses arquivos incompletos: exige erro explícito, nenhuma declaração falsa de sucesso e conferência integral da alternativa temporal. O relatório identifica cada fallback e seu motivo. Catálogo e fixtures de gravura continuam exigindo OSMD fiel; falhas neles reprovam o teste. Isso é compatibilidade parcial documentada, não suporte OSMD universal.

## Evidência de base do PR #51 (anterior à B1)

Código no SHA `e1fac0ec47871adf1c4185d55781187602086465`: [build 39676817138](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39676817138) **success**, incluindo JVM/JS/XML/MIDI, auditoria gráfica e compilação debug/release de teste; sem distribuição. Corpus de **27 arquivos**: 23 OSMD e quatro alternativas temporais verificadas. [Métricas duráveis](evidence/study-profile/summary.json), três dimensões CSS (738×196, 738×351, 883×409), nove capturas das três regressões no artifact `adaptive-study-corpus`. Foram inspecionadas as capturas baixas das três peças; isso não é aceite editorial/físico.

Catálogo verificado: Canon em Ré (160 cabeças), Estudos Livres 01/02/03 (32 cada), Für Elise — Easy (104), Gymnopédie nº 1 (469 escritas; 455 ataques), Lago dos Cisnes (172), Minueto em Sol maior (219), Minueto em Sol menor (200), Ode à Alegria (81). Fonte, ataques MIDI e dados permanecem preservados.

Alertas remanescentes: Gymnopédie no perfil baixo tem 4,853 CSS px entre linhas; Für Elise mantém uma interseção conservadora texto/cabeça; `named-voices-ties.xml` mantém alerta de reconciliação de voz, apesar de alturas/oitavas/durações conferidas. Quatro fixtures do motor (`decimal-divisions`, `explicit-dynamics`, `integer-equivalent`, `tempo-sound-offset`) falham na gravura OSMD e tiveram a alternativa temporal conferida, sem esconder a incompatibilidade.

Histórico de reprovações: `8343b7e` comparava graça com eventos principais; `c65a7be` revelou recuo de âncora em Für Elise; `44fa7ae` revelou também Minueto e a heurística de URL para XML curto; `13e22b6`/`622a35d` registraram os quatro casos de gravura incompleta. Esses resultados não são aprovação. No Android `44fa7ae`, o roteiro preexistente passou até o novo teste de recuperação, que falhou porque o helper de toque recebia uma string em vez da lista esperada; o seletor foi corretamente rotulado, mas o teste tocou outra ação. A correção do teste não altera código de produção. A conclusão do emulador da HEAD e seu SHA/run ficam registrados no [PR #51](https://github.com/atshimura/Piano-livre-aberto/pull/51), sem inferir sucesso a partir de execuções antigas.

Limitações registradas antes da B1: não havia adaptação local para a legibilidade vertical, e Für Elise mantinha alerta conservador. A B1 abaixo substitui esses dois estados; continuam sem certificação integral de colisões de símbolos e permanece o limite histórico de transmissão de 20 mil eventos para o caminho OSMD. Sem testes físicos, sem declarar V1 ou fechar B/C.

## B1 — legibilidade local e resposta a colisões (22/10/2026)

Continuação incorporada ao próprio PR #51 enquanto ele permanece aberto; a análise geral do PR foi preservada. A fonte e a reprodução continuam intactas: a compressão move somente o grupo gráfico da pauta inferior, recompõe o encaixe com o envelope completo, reposiciona as âncoras ópticas verticais correspondentes e conserva x/beats, claves, armadura, fórmula de compasso e rolagem vertical bloqueada. Não há janela por compassos nem regra por título.

### Gymnopédie nº 1

Comparação controlada no mesmo Chromium, com o ajuste desligado/ligado e gravação independente em cada dimensão. Na tela 738×196 CSS px, o espaçamento entre linhas passou de **4,77976** para **5,07480 CSS px** (**+0,29504 px; +6,17%**); o grupo inferior moveu-se 39,868 SVG units. O SVG final mediu 188 CSS px de altura em viewport de 196, sem rolagem vertical, corte de tinta ou novas interseções entre pautas. O alerta `small-staff-space` permanece, pois 5,075 ainda é menor que 6.

Em 738×351, o espaço ficou em 8,85439 CSS px antes e depois; em 883×409, em 10,35163 antes e depois. Não houve overflow vertical nos três perfis. Capturas comparativas: [738×196 antes](evidence/study-profile/b1/gymnopedie.before.738x196.png) / [depois](evidence/study-profile/b1/gymnopedie.after.738x196.png), [738×351 antes](evidence/study-profile/b1/gymnopedie.before.738x351.png) / [depois](evidence/study-profile/b1/gymnopedie.after.738x351.png), [883×409 antes](evidence/study-profile/b1/gymnopedie.before.883x409.png) / [depois](evidence/study-profile/b1/gymnopedie.after.883x409.png). As capturas documentam Chromium em dimensões CSS; não são ensaio em aparelho físico nem aprovação editorial.

### Canon em Ré e Für Elise

Canon foi validada pela gravura efetivamente ativa `OSMD SVG`, com 160 cabeças escritas: o modo Estudo classifica e oculta as listas auxiliares `words:note-names` e `words:fingering-list`; preserva texto do compositor e andamento, cifras/acordes renderizados e os demais sinais. Nenhuma cifra foi ocultada: `hiddenChordLabels=[]`, sem alerta de colisão. Partitura Livre continua a receber a representação completa do XML original. [Captura OSMD 738×196](evidence/study-profile/b1/canon-in-d.osmd-738x196.png).

Em Für Elise, o texto **p** é a dinâmica no compasso 13, beat 36, pauta 2; a cabeça correspondente é G♯3 (MIDI 56, grafia G, oitava 3). A medição de tinta no navegador encontrou interseção vertical de **3,15 CSS px** com a cabeça/linha na visualização anterior. O modo Estudo preserva o **p** e o moveu **7,15 CSS px para cima**; após a mudança não há par de colisão nem corte. A decisão e a medição ficam em `geometry.dynamicRepositionDetails`; o MusicXML, o evento e a duração não mudam. [Antes](evidence/study-profile/b1/fur-elise-dynamic-beat-36.before.png) / [depois](evidence/study-profile/b1/fur-elise-dynamic-beat-36.after.png) (738×220 CSS px, OSMD SVG).

### Regressão, corpus e limites

No head `d11a85845d7fa7d40f0fd7495a800502d62e7354`, o Chromium concluiu `STUDY_PROFILE_BROWSER_PASS files=27`: 23 gravuras OSMD e quatro alternativas temporais explícitas (`decimal-divisions.xml`, `explicit-dynamics.xml`, `integer-equivalent.xml`, `tempo-sound-offset.xml`). Cada fixture temporal foi conferida integralmente contra eventos; essas fontes permanecem inalteradas. Canon e Für Elise reconciliaram grafia, altura, oitava, beat e duração com eventos nativos. A fixture `named-voices-ties.xml` segue com alerta `native-mapping`, apesar da conferência das demais dimensões musicais. O relatório integral e as capturas ficam em [`evidence/study-profile/b1/corpus.json`](evidence/study-profile/b1/corpus.json).

O ganho da Gymnopédie é mensurável, mas limitado: o espaço entre linhas ainda é pequeno e as medições não certificam os contornos de todo símbolo, nem refluem os compassos visíveis conforme uma janela musical. O aceite de legibilidade, a confirmação nos Realme 7 5G/Lenovo Tab M9/Yamaha P-45 e a issue #11 continuam pendentes. CI/Chromium e emulador não são validação física; B, C e V1 permanecem abertas. Nenhum APK de distribuição foi criado ou publicado por esta continuação.

## Continuação B1 — revisão da segurança geométrica (22/10/2026)

Em desenvolvimento no mesmo PR #51, a partir do HEAD `7f8ac63` (build 39805252472 e emulador 39805252486 aprovados). A auditoria ampliada inclui espessura das hastes e linhas: a compactação anterior ignorava primitivas com largura/altura geométrica zero. Isso invalida a alegação de ausência de novas colisões para todas as hastes na evidência anterior. Barras de conexão e chave precisam acompanhar a distância entre pautas sem deslocar o extremo superior.

Nesta revisão, cifras isoladas passam pela política geométrica; dinâmicas mantêm a posição de origem, exceto colisão efetivamente medida. Pauta pequena, isoladamente, deixa de ampliar o espaçamento horizontal, que alongava ligaduras e piorava o encaixe vertical. Os caminhos de abertura e resize passam pelo mesmo critério de segurança. O diagnóstico de alternativas de gravura OSMD em `layout-probes.json` é exploratório, não aceite. CI do novo SHA ainda pendente; B1 não concluída.
