# ZAI-006 — Relatório de entrega, regressão S1 e APK principal 0.9.67

**Data:** 10/11/2026. **Escopo:** linha `zai/lab` e aplicativo principal instalado. **Estado:** implementação e APK principal assinado gerados; download direto GitHub publicado e conferido; validação física pendente. **Branch GitHub `main`: não modificada.** Sem Release/Play Store.

## 1. Plano, implementação e decisão funcional

- Planejamento e critérios: [ZAI-006 — Durações, S1 e sem slurs automáticas](ZAI-006-DURACOES-S1-SEM-LIGADURA-EXPRESSAO.md).
- [PR #122](https://github.com/atshimura/Piano-livre-aberto/pull/122) **integrado à `zai/lab`** no commit `a5a874c39e7be122456f1bc1d1ed2e6bf4e37b17`.
- Decisão aprovada do mantenedor: **não inferir ligaduras de expressão (`<slur>`) a partir de notas sobrepostas** no modo Criação. O XML importado de outras fontes mantém suas próprias articulações; o gerador da Criação não deve inventar slurs.
- `ScoreRhythmQuantizer`: note-on e note-off separados do ritmo notado. Intervalos regulares com teclas liberadas cedo são staccato, sem pausas falsas; pausas efetivas entre frases e acordes não são preenchidas com notas prolongadas. Sobreposições relevantes e notas sustentadas permanecem com durações próprias e vozes quando necessário. Finais de frases respeitam a liberação real, salvo um padrão de staccato musicalmente repetido.
- `ScoreXmlComposer`: **ligaduras de prolongamento (`<tie>`) mantidas** para fragmentos de uma única nota longa, mesmo dentro do compasso. Não gera `<slur>` automático.
- Entrada MIDI (`MidiTakeRecorder`), `ScoreMidiPairer`, gravações originais `.plc`, biblioteca, projetos e backups não foram reestruturados ou apagados. Direitos autorais e GPL-3.0 respeitados.

## 2. Evidência e testes

O teste `PianoLivreAndroid/tests/ZScoreS1FidelityRegressionTest.java` usa **apenas alturas e ticks sanitizados** retirados de `S1.mid` fornecido pelo usuário; o arquivo pessoal original **não foi publicado no repositório**.

Verificações do caso S1: 22 ataques MIDI e 22 liberações pareadas; duas escalas C4–C5 (8 notas cada), C maior (C/E/G), C menor (C/E♭/G); andamento estimado perto de 99 BPM; no MusicXML **nenhuma slur automática**; durações dos dois Dós5 longos aproximadas ao Note Off real; sobreposição Fá4→Sol4; coerência dos dois acordes, notas não artificialmente sustentadas até o acorde seguinte; pausa real entre eles. `Score.read` tem exatamente **22 ataques**, unificando eventuais fragmentos ligados. Outro teste segura **uma única tecla C4 por cinco minutos**: um ataque MIDI, muitas figuras de notação ligadas, um evento no modo Estudar.

### Execução comprovada

- CI de pré-integração [#42887016678](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42887016678) **SUCCESS**; após atualização apenas de documentação/comentários, [#42887392326](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42887392326), [#42887392364](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42887392364) e [#42887392435](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42887392435) **SUCCESS** sobre o HEAD do PR.
- Build assinado definitivo [GitHub Actions #42887784886](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42887784886) **SUCCESS**, commit de compilação `e4d8ddf7dbd5291ecfc2712f9d35888c3d4f2d78`. Código funcional igual ao do PR #122; workflow adicional em branch `zai/ci/apk-principal-zai006-0-9-67-20261110`.
- Suítes de JVM e número de verificações observadas no log:
  - `ZScoreGeneratorRhythmTest`: 86
  - `ZScoreGeneratorXmlTest`: 26
  - `CreationScoreStoreTest`: 27
  - `CreationC2DCorpusTest`: 45
  - `ZScoreRealMidiRegressionTest`: 23
  - `ZScoreChordFidelityRegressionTest`: 59
  - `ZScoreS1FidelityRegressionTest`: 48
  - **Total: 314 verificações JVM aprovadas**, além dos contratos/auditoria Python, compilação Android e metadados da assinatura.
- Não afirmar teste físico nem avaliação visual completa de OSMD no Android: **não ocorreram nesta entrega**.

## 3. APK principal assinado

- **Versão do APK:** `0.9.67-open-beta`, `versionCode 76` (acima do 0.9.66/code75).
- **Pacote:** `br.org.pianolivre.opensource`, rótulo **Piano Livre** — atualiza o principal, **não** a instalação isolada `.opensource.zai`.
- **Tamanho:** 794.684 bytes.
- **SHA-256 do APK:** `eaa604ae79538a741ca95fc91f133e482c7e8bf9ac8ce24eac30370e2e7e691c`. Verificado nos bytes originais do ZIP de artefato descarregado, depois na branch remota do GitHub.
- **Assinatura permanente SHA-256 do certificado:** `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290` via GitHub Actions Secrets. Não compartilhar keystore, senhas ou chaves.
- `minSdk 26`, `targetSdk 36`, APK release não depurável, sem permissão de internet; verificações `aapt`, `apksigner` e `sha256sum` passaram.
- Artefato temporário GitHub Actions ID `13795729820`, nome `PianoLivre-PRINCIPAL-0.9.67-APK-assinado-e4d8ddf7dbd5291ecfc2712f9d35888c3d4f2d78`; prazo de retenção conforme Actions. Não é a URL primária de distribuição.
- Nenhuma versão persistida em `PianoLivreAndroid/gradle.properties` da `main` foi alterada. Parâmetros Gradle usados: `-Ppianolivre.zai.test=false -Ppianolivre.beta.versionCode=76 -Ppianolivre.beta.versionName=0.9.67-open-beta`.
- Reprodutibilidade: [PR #123](https://github.com/atshimura/Piano-livre-aberto/pull/123) com workflow exclusivo `.github/workflows/principal-apk-zai006.yml` de build assinado.

## 4. Download direto sem biblioteca ChatGPT

A política [ZAI-005](ZAI-005-DISTRIBUICAO-APK-GITHUB-DIRETO.md) é obrigatória para evitar o erro **“Library file not found”**.

- Publicação pelo [GitHub Actions #42888321861](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42888321861) **SUCCESS**; recuperou exatamente o artefato assinado já produzido, verificou assinatura/identidade/hash, fez push à branch exclusiva de downloads e confirmou remotamente os mesmos bytes.
- Branch pública `zai/downloads`, **commit imutável de publicação**: `2d5462070eb171a9563e82ceebc2302efaf671e0`.
- Arquivo `apks/principal/PianoLivre-Principal-0.9.67-open-beta-assinado.apk`, 794684 bytes, objeto Git `603ea775ef1ff35d5aab8d27073b4c5e4fd8d5e6`.
- **URL primária — download direto por commit:**
  `https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/2d5462070eb171a9563e82ceebc2302efaf671e0/apks/principal/PianoLivre-Principal-0.9.67-open-beta-assinado.apk`
- **Página alternativa GitHub:**
  `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.67-open-beta-assinado.apk`
- **Comprovante de checksum no repositório:**
  `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.67-open-beta-SHA256.txt`
- Reprodutibilidade: [PR #124](https://github.com/atshimura/Piano-livre-aberto/pull/124), workflow isolado `.github/workflows/zai-downloads-0-9-67.yml`.
- O download direto evita depender de `sandbox:/mnt/data` do ChatGPT ou da validade dos artefatos Actions. A permanência pública do URL depende da disponibilidade do repositório GitHub; não prometer disponibilidade absoluta de terceiros.

## 5. Pendências e riscos

1. **Não testado fisicamente** no Realme 7 5G, Lenovo Tab M9 ou Yamaha P-45. A fidelidade visual e os dados instalados devem ser confirmados pelo usuário quando possível, sem parar o desenvolvimento das outras etapas.
2. Não realizado ensaio completo de Criação→Gerar→Salvar→Estudar no emulador. Os testes JVM e XML são necessários, **não** equivalentes à interface física.
3. O grau de coincidência exata Note Off→nota escrita é limitado à grade rítmica de semicolcheias; pequenas sobreposições inferiores à grade não são completamente representáveis pela notação, mas os pares MIDI originais permanecem intactos.
4. Gravações originais e backups devem ser preservados; derivados `.pls` legados podem exigir regeneração `MusicXML` sem exclusão destrutiva. **Fazer backup verificado antes da atualização**, instalar por cima (não desinstalar). Mesmo pacote/certificado e code superior possibilitam a atualização, mas não comprovam migração física.
5. A branch `main` permaneceu intocada em `55426cf79cbbd652ea5035fb6774d7a31b5380e3`. A promoção do código a `main` exige PR/autorização separados. Repositório histórico privado não foi acessado.
