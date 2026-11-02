# 105 — Criação C2-D: regressão dirigida, acessibilidade e evidências

**Data:** 02/11/2026. **Situação:** implementada em branch técnica; integração à main e Release não realizadas.
**Branch:** feat/criacao-c2-d-validacao-entrega, derivada da feat/criacao-c2-a-motor-transcricao após squash da [PR #98](https://github.com/atshimura/Piano-livre-aberto/pull/98) no commit 7ef0b13d1f78ea210b5278f3a05029377cf38cf7.
**Especificação:** [docs/101](101-CRIACAO-C2-PLANO-EXECUCAO.md), [issue #95](https://github.com/atshimura/Piano-livre-aberto/issues/95).

## Implementação

- CreationC2DCorpusTest.java: regressão dirigida para 2/4, 3/4, 4/4 e 6/8, precisões 1/8, 1/16 e 1/32, acordes, vozes, cruzamentos de pautas, pedal, nota igual em canais diferentes, durações longas, overrides e undo/redo. Compara MusicXML gerado com o parser Score.read (contagem, alturas, ataques e compassos). Verifica original intocado, determinismo, IDs inválidos e um corpus sintético de 320 notas.
- MusicCreationScreen: não exibe botões de edição/Estudar para uma composição na Lixeira; após restauração, o vínculo da partitura permanece. Preserva o rótulo acessível completo de botões que são abreviados na tela pequena.
- CreationScorePreviewScreen: controles de nota/acorde recebem identificação acessível exclusiva, divisão/altura têm descrições e movimentos de pauta só são habilitados com notas selecionadas.
- CreationC2DProbe: fixture exclusivamente de depuração, por intenção explícita creation_c2_d_smoke, que recusa dispositivos MIDI físicos. Cria uma composição sintética, salva derivação v4 e verifica que os bytes C1 não mudaram, antes de abrir seus detalhes.
- tests/c2_d_ui_smoke.py: um percurso isolado no Android emulado: detalhes → prévia OSMD → ajustes de pauta → Estudar → voltar à Criação → paisagem. Registra PNG, XML da UI e memória, somente com dados sintéticos, e recusa dispositivos físicos.
- Workflow c2-basic.yml: testa o parser Score junto ao motor C2, o seletor JS, compila APK debug, executa autoteste isolado de backup e, na branch C2-D, realiza um único percurso visual em perfis virtuais de telefone e tablet, com artefatos de sete dias. Não executa emulator-suite ou build.yml.

## Critérios e limitações

- A fonte canônica .plc e a gravação MIDI não devem ser reescritas ao transcrever. O teste JVM confere ataques originais e a fixture Android compara bytes.
- O MusicXML é uma interpretação quantizada, não garantia de reprodução rítmica idêntica ao MIDI. A saída MIDI real continua restrita ao player de C1.
- Backup v4, staging, rollback, SHA-256 e compatibilidade v1/v2/v3 permanecem cobertos pelos testes C2-C; a C2-D reexecuta o autoteste isolado.
- O teste visual verifica somente um percurso sintético. Não valida exaustivamente geometria, acessibilidade por leitor de tela nem comportamento de todos os cenários.
- Yamaha P-45, Realme 7 5G e Lenovo Tab M9 continuam pendentes de validação física.
- Exportação MusicXML/PDF pertence à C3; edição fina de notas, à C4.

## Validação e integração

Não declarar aprovação da C2-D antes do run final do commit HEAD. Registrar nos resultados exatos SHA, logs e artefatos do GitHub Actions. Os jobs são estritamente dirigidos, sem suíte completa, conforme determinação vigente.

O PR funcional da C2-D permanece destinado à branch técnica C2-A/B/C. A main protegida requer gates amplos para código Java, incompatíveis com a ordem de não executar a suíte completa. Não alterar esses gates nem publicar Release ou APK permanente sem autorização e evidência; a consolidação pública permanece pendente. Fotos do emulador não substituem testes físicos.

## Próximos passos

1. Confirmar sucesso do CI dirigido no HEAD final e corrigir qualquer falha observada.
2. Integrar C2-D na branch técnica pelo PR específico e atualizar a issue #95.
3. Resolver de forma autorizada a incompatibilidade da política de testes com a proteção da main antes da integração pública.
4. Registrar validação no hardware real quando disponível.
