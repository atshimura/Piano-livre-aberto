# 59 — Análise automática e apresentação adaptável do Estudo

### Evidência medida da reserva do contexto fixo (SHA b8ed11e, build 39875463421)

O teste da fixture `engraving-context-change.musicxml` encontrou na implementação anterior uma faixa fixa que podia ocupar 168,94 CSS px enquanto o cursor permanecia próximo de 167 px no terceiro compasso. A revisão passou a reservar, antes do planejamento de janela e cursor, a **largura máxima do contexto efetivamente gravado** na partitura. No Chromium 738×196 a faixa mede 168,94 CSS px e o cursor está em 180,94 px, com 12 px livres e sem recorte nas transições 1→2→3→2→1. No tablet 883×409, faixa 360,34 px e cursor 372,34 px, igualmente com 12 px de folga. Ambos preservam a origem vertical. Isso demonstra ausência da colisão detectada nessa fixture, não prova legibilidade editorial universal.

A nova rodada de 27 arquivos, incluindo Gymnopédie, corpus nativo e fallback temporal, passou no [build 39875463421](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39875463421) do código `b8ed11e63f54d83afc64cdfeb2c8848b2fc2bc46`. As 12 capturas comparáveis da Gymnopédie mantiveram os espaçamentos 5,40241 / 5,16247 / **4,85294** / 5,03093 CSS px (abertura/meio/extremo/final) em 738×196; as telas 738×351 e 883×409 mediram 8,85439 e 10,35163 em todas as regiões. Cortes detectados: zero; rolagem vertical: zero; mapeamento: 469/469. A mudança no extremo baixo segue **insuficiente**, e transições da escala variável, bem como validação real, continuam pendentes. Ver [relatório versionado](evidence/study-profile/b1-window/comparison-validated-2026-10-22.json) e o artifact do run correspondente. Não atribuir esta medição a outro SHA nem ao hardware.

## B1 — continuação: câmera ancorada, contexto e vozes nomeadas (22/10/2026)

**PR #51 permanece aberto, sem merge.** A câmera horizontal agora utiliza a faixa efetivamente alcançável pela área pintada, levando em conta a posição inicial, a margem e a linha de referência. A escala é reavaliada contra o envelope no enquadramento final, com preservação de ambas as pautas, MusicXML e MIDI. O guardião anterior considerava tinta à direita além do que efetivamente poderia entrar na viewport; isso podia diminuir desnecessariamente a escala local.

A faixa de contexto fixo à esquerda agora adapta sua largura ao conteúdo ativo (clave, armadura e fórmula de compasso) e sincroniza o viewBox após deslocamento ou mudança de escala. O corpus passou a pedir capturas de transições 1→2→3→2→1 na fixture engraving-context-change.musicxml e a conferir limites dos glifos, área visível e alinhamento. Testes de janela conferem os extremos, ausência de recorte, integridade do mapeamento e redimensionamentos nos três perfis. Essas verificações não certificam fluidez ou legibilidade subjetiva.

**native-mapping:** em named-voices-ties.xml há quatro eventos nativos C4/MIDI 60 e quatro cabeças OSMD nos mesmos tempos; as vozes nomeadas upper/lower não obtiveram identidade gráfica verificável na evidência anterior (matched=0, ambiguous=4, unmatchedNative=4). Não existe demonstração de nota, andamento ou duração divergente pelo multiconjunto de eventos; existe ambiguidade de voz. Não forçar emparelhamento nem suprimir alerta. O teste exige explicitamente a ambiguidade e compara também a sequência da alternativa temporal. Os quatro fixtures incompletos para OSMD preservam fallback temporal explícito.

**Evidências:** StudyProfileBrowserTest.js produz no artifact adaptive-study-corpus as capturas gymnopedie.window-{opening,middle,extreme,ending}.{738x196,738x351,883x409}.png, b1-viewport-comparison.json, context-change.measure-*.png e b1-context-transitions.json. O script e este protocolo ficam versionados; os novos binários produzidos pela CI não são automaticamente versionados. Consultar o SHA e as medições no PR antes de declarar aprovação. O valor de 6 CSS px continua referência experimental, nunca padrão aceito pelo usuário.

**Checklist físico pendente:** Realme 7 5G — abertura, meio e extremos da Gymnopédie, variação de escala, cursor e retorno; Lenovo Tab M9 — contexto, mudança de compasso, teclado guia ligado/desligado, redimensionamento, repetição e trechos; Yamaha P-45 — USB-MIDI, ataques, mãos, Espera, pausa/retomada e sincronização. Registrar percepção de leitura e movimentos com consentimento. **Nenhum teste físico foi realizado.** B1 e V1 não foram concluídas. Sem alteração de dados, versão, pacote, certificado, APK de distribuição ou release.

## Escopo e arquitetura

Branch `feat/analise-estudo-adaptavel`, baseada na main `801543c` (Entrega A, PR #49 integrado). Não integra nem modifica a pesquisa #46. Os documentos 57/58 foram copiados da HEAD `62a9443` do PR #48 apenas para acrescentar este estado; aquele PR e sua branch permanecem intactos. Há sobreposição documental a reconciliar antes de integrar, sem trazer o restante daquela branch.

`StudyProfile` analisa o `Score.sourceXml` recebido pela mesma `NotationView` após a leitura/validação de `ScoreImport`/`Score.read`. Vale para importações XML/MXL e assets: não há regra por título. A análise visual ocorre ao abrir o Estudo (não durante a cópia transacional do arquivo). O catálogo não recebe alterações nos arquivos.

1. Analisa MusicXML: notas por compasso, quantidade de elementos, vozes/pautas, acidentes, acordes, ligaduras, vigas, quiálteras, repetições, letras, palavras, cifras, dedilhados e marcas de ensaio.
2. Gera cópia transitória de apresentação, desconsiderando atributos de quebra de sistema/página em `<print>`, sem `<lyric>` e sem listas auxiliares em `<words>` compostas exclusivamente por pelo menos quatro nomes de solfejo ou quatro números de dedos (1–5). Preserva outras palavras, andamento, dinâmica, cifras, dedilhado e instruções de repetição. A interface informa a ocultação e remete à Partitura Livre. O XML original não é regravado.
3. Entrega um `Document` XML local ao OSMD, evitando sua heurística de tratar strings curtas sem declaração XML como URLs. Renderiza OSMD e usa `EngravingAudit` sem o limite diagnóstico histórico de 96 notas. Examina caixas das cabeças, escala efetiva e reconciliação com os eventos escritos nativos.
4. Efetua no máximo uma nova gravura pelo OSMD: colisão entre ataques adjacentes amplia os fatores de espaçamento de vozes em 1,5. Pauta pequena isoladamente não amplia mais o espaçamento horizontal. Em telas baixas, a janela horizontal abaixo ajusta proporcionalmente a escala do SVG completo conforme os símbolos visíveis, sempre com as duas pautas e sem rolagem vertical. Alertas remanescentes não são declarados resolvidos.
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

Caixas são aproximações de contornos; a inspeção de colisões de cabeças adjacentes inclui triagem de textos sobre cabeças e envelope de todos os símbolos, mas não certifica todas as sobreposições de hastes, vigas ou arcos. A densidade estrutural é diagnóstico, não prova de legibilidade. Mais de duas pautas, ornamentos e percursos MusicXML mantêm os limites dos importadores/renderizadores existentes. A gravura pode continuar pequena devido a extremos verticais distantes. A B1 tenta compactar o espaço entre pautas somente quando há ganho seguro; inclui hastes, barras de conexão e chave na verificação. Quando necessário, usa uma janela horizontal conservadora sobre a gravura completa, sem reescrever compassos. Gymnopédie melhora no viewport baixo, mas segue abaixo do limiar diagnóstico de 6 CSS px; isso não comprova legibilidade editorial ou física. Falhas/alertas remanescentes requerem avaliação visual, temporal alternativo ou correção futura; não remover notas para acomodar a tela.

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

## Histórico B1 — primeira compactação (22/10/2026)

**Evidência substituída pela revisão abaixo:** a primeira auditoria não incluía a tinta de hastes com bbox de largura zero. Os ganhos desta seção não certificam a segurança da compactação atual. Capturas e runs antigos são mantidos para rastreabilidade.

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

Nesta revisão, cifras isoladas passam pela política geométrica; dinâmicas mantêm a posição de origem, exceto colisão efetivamente medida. Pauta pequena, isoladamente, deixa de ampliar o espaçamento horizontal, que alongava ligaduras e piorava o encaixe vertical. Os caminhos de abertura e resize passam pelo mesmo critério de segurança. O diagnóstico de alternativas de gravura OSMD em `layout-probes.json` no run exploratório foi usado para comparação, não como aceite; não faz parte do caminho de produção. Os resultados atuais estão abaixo; B1 não concluída.

### Janela horizontal de conteúdo — implementação atual

O run 39826905026 (`985671a`) confirmou a reprovação do ganho mínimo ao incluir hastes: apenas apertar pautas não é solução segura. A continuação usa uma câmera local sobre o SVG completo, com escala uniforme e envelopes conservadores de todos os símbolos que cruzam a faixa horizontal em leitura (inclusive ligaduras com extremos fora dela). O contexto das duas pautas participa de todos os envelopes. Os elementos musicais não são removidos, reescritos nem regravados; eventos, x nativo, tempo e fontes permanecem os mesmos. A câmera se atualiza no relógio existente e no deslocamento horizontal manual. A escala é limitada a 7 CSS px por espaço e nunca fica menor que o encaixe integral. Região extrema ainda pode permanecer abaixo de 6 px; não há aceite físico inferido.

O harness passa a gravar cada dimensão após uma abertura independente, confere fidelidade em cada dimensão, percorre todos os beats escritos da Gymnopédie nos dois sentidos e testa resize reversível. Esses testes passaram no head `1e8ca06` conforme os runs abaixo. Novas execuções devem repetir as verificações no próprio SHA.


## Estado verificado da continuação — 22/10/2026

**B1 permanece parcial, em revisão no PR #51.** Código de aplicação e testes aprovados no head `1e8ca06bae8270ad66e8586d5ec731b81ac818ed`: [build/corpus 39828730389](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39828730389) e [emulador 39828730333](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39828730333), ambos `success`. A atualização documental posterior não transforma essas evidências em prova de outro SHA: o [PR #51](https://github.com/atshimura/Piano-livre-aberto/pull/51) registra o head final e os respectivos workflows. O artifact passa a distinguir head de origem e checkout de integração (`sourceSha`/`checkoutSha`).

### Medidas comparáveis

Valores em CSS px no Chromium 131/Playwright 1.49.1, OSMD 1.9.8. Área útil vertical = viewport menos 8 px; SVG ocupa essa altura. A estimativa de leitura usa apenas distância entre linhas, com limiar diagnóstico experimental de 6 px, sem substituir avaliação humana.

| Partitura | Viewport | Área útil (largura × altura) | Espaço entre linhas | Mapeamento | Alertas após ajuste |
|---|---|---|---|---|---|
| Gymnopédie | 738×196 | 738×188 | 5,40241 na abertura; 4,85294–5,40241 no percurso | 469/469 | `small-staff-space` |
| Gymnopédie | 738×351 | 738×343 | 8,85439 | 469/469 | nenhum |
| Gymnopédie | 883×409 | 883×401 | 10,35163 | 469/469 | nenhum |
| Canon em Ré | 738×196 | 738×188 | 6,01637 | 160/160 | nenhum |
| Canon em Ré | 738×351 | 738×343 | 10,97667 | 160/160 | nenhum |
| Canon em Ré | 883×409 | 883×401 | 12,83278 | 160/160 | nenhum |
| Für Elise | 738×196 | 738×188 | 8,57291 | 104/104 | nenhum |
| Für Elise | 738×351 | 738×343 | 15,61065 | 104/104 | nenhum |
| Für Elise | 883×409 | 883×401 | 18,27542 | 104/104 | nenhum |

As nove aberturas tiveram zero cabeças cortadas, zero sobreposições de ataques adjacentes, zero colisões texto/cabeça após a política e nenhum overflow vertical. Não equivale a certificar toda combinação de hastes, arcos e vigas. [Medições por arquivo/dimensão e capturas](evidence/study-profile/b1-window/).

**Gymnopédie:** comparação controlada desligado/ligado, mesma fonte e viewport baixo: 4,85313→5,40241 (+11,32%) na abertura. A compactação entre pautas é recusada por ganho seguro insuficiente; o aumento vem da câmera local. São 15 janelas geométricas sobre o SVG integral, com envelopes que incluem espessuras, duas pautas, claves, armadura, fórmula e ligaduras que cruzem a região, mesmo com extremos fora da tela. Não há corte de conteúdo musical da região em leitura. As posições x/beats e o relógio continuam nos mecanismos existentes; somente escala uniforme e enquadramento mudam. Não há alteração de notas, duração, ligaduras, MIDI, trechos ou fonte XML. O retorno a regiões altas reduz a escala, conservando a música completa. O menor valor observado volta a 4,85294; portanto **não está resolvida a legibilidade ao longo de toda a peça em tela baixa**. Escala variável e fluidez precisam de avaliação editorial/física. Telas média/tablet mantêm a gravura integral, sem câmera local.

**Canon:** a origem das listas é `<direction><direction-type><words>` da fonte, não notas extras nem geração de solfejo pelo app. `words:note-names` e `words:fingering-list` são filtrados na cópia transitória. As sequências `D A Bm F#m` e `G D G A` são cifras úteis e permaneceram visíveis nas três dimensões; `hiddenChordLabels=[]`. Crédito Paulo Macedo e andamento também foram preservados. A classificação reconhece cifras isoladas maiúsculas sem confundir a dinâmica `f` com a cifra `F`; ocultação de cifra exige colisão medida. A Partitura Livre continua recebendo a fonte completa.

**Für Elise:** o `p` do compasso 13, beat 36, pauta 2, não é ocultado. O teste localiza o glifo pelo texto e a cabeça por identidade musical G♯3/MIDI 56, mede tinta com a fonte ativa (`actualBoundingBox*` ou métricas de fonte disponíveis) e transforma ambos para coordenadas CSS com a escala efetiva. A caixa SVG de linha tipográfica não é tomada como tinta. Há colisão real reproduzível antes da política, e ausência do par depois. O deslocamento é calculado por dimensão: 5,48 / 6,70 / 7,15 CSS px nas três alturas, respectivamente. Esses valores são o ajuste antes do encaixe final do envelope; não transpor um número fixo entre viewports. Notas e dinâmica permanecem visíveis e não cortadas. [Antes/depois em 738×220](evidence/study-profile/b1-window/).

### Testes e limites de evidência

- Harness executa todos os acessos a `window`/DOM dentro de `page.evaluate` ou `waitForFunction`, no Chromium. O `ReferenceError: window is not defined` não reapareceu; corpus completo concluiu `STUDY_PROFILE_BROWSER_PASS files=27`.
- Cada dimensão começa por carregamento independente; mapeamento e assinatura musical são conferidos em todas. Foram 23 OSMD e quatro fallbacks temporais explícitos, com eventos integralmente conferidos: `decimal-divisions`, `explicit-dynamics`, `integer-equivalent`, `tempo-sound-offset`. A fixture `named-voices-ties` continua com alerta `native-mapping`; não foi mascarado.
- Gymnopédie: 376 amostras de beats escritos, ida/volta; caixas reais visíveis verificadas independentemente do planejador, nenhuma tinta verticalmente cortada e 469 notas mapeadas. Envelopes conservadores de arcos podem limitar o ganho. Teste sintético de haste com largura geométrica zero impede compactação insegura.
- Resize com teclado guia: 351→196→218→351→196, contexto Sol/Fá visível e mapeamento preservado; final idêntico à abertura nova. Não certifica conforto das mudanças de escala durante execução física.
- Build completo passou JVM, JavaScript, fidelidade MusicXML, reconhecimento/reprodução MIDI, teclado das duas mãos, modos e compilação Android; emulador passou o roteiro e a verificação visual. Os jobs de assinatura permanente/publicação foram ignorados por ser PR. Nenhuma distribuição foi publicada, nenhuma chave acessada ou substituída.
- Local: testes JS do perfil/janela/navegação/contrato e validador público; o Chromium local não inicia por restrição de socket do ambiente. A prova de navegador e Android vem dos Actions, não de uma aprovação local presumida.

**Para fechar B1:** obter leitura confortável também nas regiões que ainda medem cerca de 4,85 px, avaliar mudanças de escala e revisar editorialmente as capturas. Depois, usuário precisa avaliar presencialmente Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 (legibilidade, fluidez, cursor, mãos, teclado guia, trechos e MIDI). Não houve ensaio físico. Issues #4/#11, B, C e V1 permanecem abertas. Sem merge, nova release, migração de dados, dependência online de execução, alteração de pacote/certificado/versão ou APK de distribuição nesta tarefa.
