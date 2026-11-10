# ZAI-007 — Relatório de implementação e entrega do APK principal 0.9.68

**Data:** 10/11/2026. **Situação:** código ZAI-007 integrado à `zai/lab`, build Android release assinado **SUCCESS**, artefato de download direto GitHub **publicado e verificado**. Nenhum teste físico realizado. Nenhuma alteração na `main`, no repositório histórico privado, em tags, em Releases ou na Play Store.

## Decisão de produto e limites intencionais

- Contrato aprovado: [ZAI-007 — partitura simplificada sem ligaduras](ZAI-007-PARTITURA-SIMPLIFICADA-SEM-LIGADURAS.md).
- Em Criação, **não inserir ligaduras de expressão nem de prolongamento automaticamente**. Uma única nota MIDI ocupa uma única figura musical visível, mesmo quando a tecla foi pressionada por cinco minutos.
- **Notação simplificada, não transcrição de duração exata:** se a nota ultrapassar a barra do compasso, o excedente da sustentação **não é exibido na partitura**. Também não é reproduzido na duração completa ao estudar a partitura simplificada. O Note On, Note Off, sobreposições e durações originais permanecem na gravação MIDI `.plc`, sem modificação. Não dizer que o XML preserva a sustentação de cinco minutos.
- Continuam presentes duas pautas G/F, limite C4/MIDI60, escrita de Mi♭, acordes C/Cm, ritmo automático, staccato e OSMD; nenhuma nova dependência externa.
- A edição **manual** de ligaduras, durações e vozes é uma funcionalidade futura; nenhum botão sem implementação foi introduzido.
- **Reuso seguro:** a prévia só utiliza XML derivado com marcador `ZAI-007-tie-free`. Para partitura PLS2 antiga da mesma gravação, apresenta aviso e botão **Gerar prévia sem ligaduras**; o arquivo antigo permanece até o usuário salvar a nova derivação. PLS1 legado permanece identificado como incompatível. O formato de persistência, projetos e backups não foi mudado.

## Implementação

- [PR #126](https://github.com/atshimura/Piano-livre-aberto/pull/126), merge commit **`6ed3a4648ec1902ae0362f8e150b8b3ba685158b`**, somente em `zai/lab`.
- `ScoreRhythmQuantizer`: `singleFigureLength` aproxima o valor musical a uma única figura convencional que caiba no compasso; `decompose` não fragmenta mais um Note On; remove dos tokens a necessidade de `tieStart/tieStop` e deixa as durações originais inalteradas em `PairedNote`.
- `ScoreXmlComposer`: proíbe emissão de `<tie>`, `<tied>`, `<slur>` na Criação e acrescenta marcador de revisão MusicXML sem tocar na importação externa.
- `CreationScorePreviewScreen`: detecta XML salvo com gerador anterior, informa limitações e oferece regeneração sem substituir arquivo até salvar, além de explicar a abreviação visual de sustentações.
- Testes anteriores foram adaptados **à nova decisão funcional**, sem afrouxar requisitos de contagem de ataques MIDI. Novos `ZScoreTieFreeRegressionTest` e `test_zai007_tie_free_contract.py` testam 5/16, cruzamento de barra, acordes, reataques, sustentação de cinco minutos, marcador de revisão, salvamento seguro e ausência de ligaduras.

## Testes observados

- [GitHub Actions #42896557668](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42896557668): **SUCCESS**, com **355 verificações JVM** aprovadas, contratos Python e Gradle `:app:assembleDebug -Ppianolivre.zai.test=true` compilado com pacote de testes ZAi isolado.
- [GitHub Actions #42897034593](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42897034593): **SUCCESS**, recompilou o mesmo código ZAI-007 com assinatura permanente da edição principal, executou testes/auditorias e conferiu `aapt`/`apksigner`/SHA-256.
- Detalhamento JVM: `ZScoreGeneratorRhythmTest` 86; `ZScoreGeneratorXmlTest` 29; `CreationScoreStoreTest` 27; `CreationC2DCorpusTest` 45; `ZScoreRealMidiRegressionTest` 24; `ZScoreChordFidelityRegressionTest` 59; `ZScoreS1FidelityRegressionTest` 45; `ZScoreTieFreeRegressionTest` 40. **Total 355**.
- **Não executados:** teste físico Yamaha P-45 / Realme 7 5G / Lenovo Tab M9, instalação e preservação de dados num aparelho real, percurso completo OSMD em emulador. Os testes automatizados não equivalem a essa validação.

## APK principal assinado 0.9.68

- Build GitHub Actions: [#42897034593](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42897034593), **SUCCESS**; commit da branch de empacotamento **`f386f465e1b326c7b6ad2e02bfe6cc7a164e42f9`**.
- Artefato temporário ID **13800409410**, nome `PianoLivre-PRINCIPAL-0.9.68-APK-assinado-f386f465e1b326c7b6ad2e02bfe6cc7a164e42f9`. A compilação é reproduzível via workflow dedicado [PR #127](https://github.com/atshimura/Piano-livre-aberto/pull/127).
- Arquivo `app/build/outputs/apk/release/app-release.apk`: **795.372 bytes**. Extraído do ZIP de evidências do GitHub e SHA-256 recalculado independentemente na execução desta tarefa.
- **SHA-256 do APK:** `e948927fc2ec2f32f98049d840230a0e322f9fa3f615fb714bc848c01b4c0ba5`. Corresponde ao comprovante CI e ao arquivo publicado na branch GitHub.
- Certificado permanente SHA-256: `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`. Os segredos permanecem nos GitHub Actions Secrets.
- Android package **`br.org.pianolivre.opensource`**, label **Piano Livre**, `versionCode=77`, `versionName=0.9.68-open-beta`, minSdk 26, targetSdk 36. APK release não depurável, sem permissão `INTERNET`. A versão 77 atualiza, em condições normais, a edição principal code76; não substitui a instalação ZAi com pacote `.opensource.zai`.
- A versão de código não foi escrita nos arquivos de versões da `main`, mas fornecida como parâmetro Gradle só na branch técnica de compilação. Nenhuma Release/tag.

## Distribuição direta sem erro Library

- GitHub Actions [#42897518924](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42897518924): **SUCCESS**, transferência e verificação dos mesmos bytes para `zai/downloads` (workflow [PR #128](https://github.com/atshimura/Piano-livre-aberto/pull/128)).
- **Commit de distribuição:** `76bdf6fa39d49812eee697eeca9f1d8a08aa447a`. O objeto remoto do APK possui 795.372 bytes, e o workflow compara remotamente seu hash ao original antes de declarar sucesso.
- **URL direta por commit, canal primário:**
  `https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/76bdf6fa39d49812eee697eeca9f1d8a08aa447a/apks/principal/PianoLivre-Principal-0.9.68-open-beta-assinado.apk`
- **Página do arquivo no GitHub:**
  `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.68-open-beta-assinado.apk`
- **Comprovante de hash:**
  `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.68-open-beta-SHA256.txt`
- Não depender de links `sandbox:/mnt/data` (Library file not found) nem da expiração do artefato do Actions. A disponibilidade a longo prazo depende do GitHub e de o repositório público continuar acessível.

## Limitações e próximos passos

1. **Fazer backup verificado**, instalar a versão 0.9.68 diretamente sobre o app principal 0.9.67 sem desinstalar. A integridade do APK não garante migração física de dados.
2. Abrir composição `S1` gerada em versão antiga, observar alerta de derivação anterior, clicar **Gerar prévia sem ligaduras** e **Salvar partitura** apenas após conferir os resultados. Sem apagar manualmente `.plc` ou derivados antigos.
3. Repetir duas escalas (staccato e sobreposição), C maior e C menor no Yamaha P-45 para avaliar a legibilidade, número de ataques, acordes e ausência de curvas.
4. Só planejar edição manual de ligaduras posteriormente. Sustentações longas continuam integrais no MIDI original, mas não são representadas integralmente na partitura simplificada.
5. A `main` permanece fora desta linha e não é alterada; promoção ao código principal GitHub exige PR/gates próprios quando autorizados.
