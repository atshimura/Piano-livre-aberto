# 59 — Análise automática e apresentação adaptável do Estudo

## Escopo e arquitetura

Branch `feat/analise-estudo-adaptavel`, baseada na main `801543c` (Entrega A, PR #49 integrado). Não integra nem modifica a pesquisa #46. Os documentos 57/58 foram copiados da HEAD `62a9443` do PR #48 apenas para acrescentar este estado; aquele PR e sua branch permanecem intactos. Há sobreposição documental a reconciliar antes de integrar, sem trazer o restante daquela branch.

`StudyProfile` analisa o `Score.sourceXml` recebido pela mesma `NotationView` após a leitura/validação de `ScoreImport`/`Score.read`. Vale para importações XML/MXL e assets: não há regra por título. A análise visual ocorre ao abrir o Estudo (não durante a cópia transacional do arquivo). O catálogo não recebe alterações nos arquivos.

1. Analisa MusicXML: notas por compasso, quantidade de elementos, vozes/pautas, acidentes, acordes, ligaduras, vigas, quiálteras, repetições, letras, palavras, cifras, dedilhados e marcas de ensaio.
2. Gera cópia transitória de apresentação sem `<lyric>` e sem listas auxiliares em `<words>` compostas exclusivamente por pelo menos quatro nomes de solfejo ou quatro números de dedos (1–5). Preserva outras palavras, andamento, dinâmica, cifras, dedilhado e instruções de repetição. A interface informa a ocultação e remete à Partitura Livre. O XML original não é regravado.
3. Renderiza OSMD e usa `EngravingAudit` sem o limite diagnóstico histórico de 96 notas. Examina caixas das cabeças, escala efetiva e reconciliação com os eventos escritos nativos.
4. Efetua no máximo uma nova gravura pelo OSMD: colisão entre ataques adjacentes amplia os fatores de espaçamento de vozes em 1,5; pauta pequena amplia em 1,25. Mantém escala proporcional, duas pautas e rolagem vertical bloqueada, usando o ajuste existente. Isso reduz compassos visíveis horizontalmente, mas **não aumenta por si o tamanho vertical das notas**. Alertas remanescentes não são declarados resolvidos.
5. Mantém relógio, MIDI, Espera/Contínuo/Demonstração, mãos, teclado e trechos nos mecanismos existentes. Partitura Livre recebe o XML completo; temporal continua selecionável. Se a WebView falhar, o fallback realmente ativo é identificado como **Esquemático**, com aviso e possibilidade de tentar novamente os renderizadores.

## Critérios técnicos experimentais

Valores implementados nesta branch, **não limites aprovados pelo usuário nem normas de gravura**:

| Diagnóstico | Medição |
|---|---|
| `small-staff-space` | distância entre linhas após escala menor que 6 CSS px |
| `adjacent-attack-overlap` | interseção das caixas de cabeças adjacentes na mesma pauta maior que 0,5 unidade SVG nos dois eixos, excluindo ataques simultâneos (diferença de beat até 1e-6) |
| `notehead-clipping` | caixa de cabeça fora do viewBox ajustado por mais de 0,5 unidade SVG |
| `vertical-overflow` | altura SVG maior que altura disponível em mais de 0,5 CSS px |
| `native-mapping` | uma ou mais cabeças não reconciliadas ou ambíguas |

Não há pontuação global de complexidade determinando o perfil. O cache é uma única entrada em memória, por igualdade exata do conteúdo XML e versão do analisador; mudança de conteúdo força nova análise. Geometria não é persistida e deve ser reavaliada em cada nova renderização. Nenhum schema, preferência, backup ou histórico é migrado. O custo adicional é local/offline; Playwright é usado somente nos testes de desenvolvimento.

## Limitações explícitas

Caixas são aproximações de contornos; a inspeção de colisões de cabeças adjacentes não certifica todas as sobreposições de hastes, vigas, arcos ou textos. A densidade estrutural é diagnóstico, não prova de legibilidade. Mais de duas pautas, ornamentos e percursos MusicXML mantêm os limites dos importadores/renderizadores existentes. A gravura global ainda pode ficar pequena devido a extremos verticais distantes: não há segmentação local por janela nesta entrega. Falhas/alertas remanescentes requerem avaliação visual, temporal alternativo ou correção futura; não remover notas para acomodar a tela.

Não se detecta uma nota musicalmente errada no próprio MusicXML sem referência independente. Comparar XML, Java e OSMD prova consistência entre representações, não fidelidade à composição. Sem testes físicos Realme 7 5G, Lenovo Tab M9 ou Yamaha P-45; não conclui V1.

## Validação

- `StudyProfileTest.js`: limites, caixas, simultaneidade e alertas.
- `StudyProfileCorpus.java`: catálogo completo + fixtures debug/importação; ataques de reprodução possuem referência escrita; bytes originais preservados.
- `StudyProfileBrowserTest.js`: mesmo corpus com OSMD real, grafia/oitava/duração, cache/invalidação, política de letras e falha real do OSMD. Relatório em artifact `adaptive-study-corpus`.
- Suítes preexistentes JVM/JS, auditorias XML/MIDI e Android permanecem obrigatórias nos workflows.
- Ambiente local: teste unitário, catálogo JVM e ScoreImportTest aprovados; Chromium local impedido por `socket() Operation not permitted` antes da renderização. Isso é bloqueio de ambiente, não aprovação da gravura. CI e emulador devem ser registrados por SHA após execução.

Versão proposta 0.9.48-open-beta / code57; mesmo pacote, sem APK de distribuição nesta PR, sem merge. Assinatura permanente somente no workflow confiável após eventual integração.
