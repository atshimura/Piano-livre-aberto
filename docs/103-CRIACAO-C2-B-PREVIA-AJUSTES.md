# 103 — Criação C2-B: prévia e ajustes de partitura

**Data:** 01/11/2026. **Estado técnico:** implementação C2-B em branch com **compilação Android de depuração e testes básicos aprovados**; integração em `main` ainda pendente. **Branch:** `feat/criacao-c2-b-previa-ajustes`, derivada da C2-A. **Issue:** [#95](https://github.com/atshimura/Piano-livre-aberto/issues/95). Contrato: [plano C2 no PR #96](https://github.com/atshimura/Piano-livre-aberto/pull/96).

## Entrega de código e experiência de usuário

1. Em **Criação → Minhas Criações → composição salva**, o novo botão **Gerar partitura** inicia uma transcrição MIDI **temporária**, sem conexão com piano, fora da thread de UI. Arquivos na Lixeira ou recuperáveis não ativam esse fluxo.
2. Uma prévia de duas pautas usa o MusicXML do motor `CreationScoreTranscriber` (C2-A), validado na leitura por `Score.read`, e o `NotationView`/OSMD offline existente. Ajustes opcionais: 100 BPM/4/4/1/16 iniciais, andamento, compasso 2/4–6/8, precisão 1/8–1/32, sugestão de pautas e corte por altura.
3. **`CreationScoreReview.java`** mantém seleção por identidades estáveis de ataques Note On. Permite mover **nota, acorde, múltiplas notas ou intervalo inclusivo de compassos** para pauta superior/inferior; Desfazer/Refazer (48 ações), Restaurar sugestão e cancelar. A mudança não altera a altura, a duração MIDI original ou os arquivos C1.
4. **Seleção diretamente na partitura:** `creation-note-picker.js` extrai coordenadas dos cabeçotes do OSMD e associa cada um ao `sourceId` canônico por pauta, altura e tempo de execução musical. `notation.html` e `NotationView.java` estabelecem um bridge JS→Java com **lista permitida de IDs**, e `CreationScorePreviewScreen.java` sincroniza toque, realce SVG e checkboxes. Suporta ligaduras cruzando compassos. **Não adivinha** quando há uníssonos simultâneos indistinguíveis: nesse caso a seleção acessível pela lista permanece disponível.
5. O botão **Salvar partitura — C2-C** permanece desabilitado, com explicação visível. **C2-B não grava derivados, nem altera `.plc`, nem abre a composição no Estudar**. Persistência segura, backup versionado e Estudar são escopo C2-C. A janela informa que ajustes não salvos são descartados; botão Voltar/Back pergunta confirmação. A reprodução original e a exportação MIDI C1 continuam inalteradas.

## Validação básica real no commit GitHub

[**Workflow C2 — somente testes básicos #41365891175**](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41365891175) concluído **success** em **02/11/2026 00:13 UTC**, no SHA `89b2cb3f5a0c430ba58ed65409a6fa94252f0aee` (o código acima de seleção já estava presente; alterações documentais posteriores não mudam esse código). Executou somente:

| Prova executada | Resultado |
| --- | --- |
| `javac` de `MidiTakeRecorder` + `CreationScoreTranscriber` + `CreationScoreReview` e dois testes do próprio repositório | Compilação Java aprovada |
| `CreationScoreTranscriberTest` | `CREATION_C2_A_BASIC_PASS checks=47` |
| `CreationScoreReviewTest` | `CREATION_C2_B_BASIC_PASS checks=27` |
| `node tests/CreationNotePickerTest.js` | `CREATION_C2_B_NOTEHEAD_BASIC_PASS checks=14` |
| `gradle --no-daemon :app:assembleDebug` | `BUILD SUCCESSFUL in 28s` |

Total: **88 verificações dirigidas** (47+27+14), além da compilação Android. **O resultado 47 da C2-A é o número efetivamente registrado na CI com fontes reais**; textos anteriores mencionaram 48 em teste de cópia local. A referência definitiva é o log acima. O `notation.html` foi adicionalmente analisado como JavaScript sintaticamente válido usando o código da branch.

A compilação foi **sem assinatura permanente nem publicação**. A suíte completa, suíte de emulador, testes de toque físico, realismo de geometria OSMD, teste físico Realme 7 5G/Lenovo Tab M9/Yamaha P-45 e Release **NÃO foram executados**. Passar no `assembleDebug` prova compilação, não renderização ou qualidade visual no aparelho.

### Regra de testes e proteção da `main`

Em 01/11/2026 o usuário determinou: **somente testes básicos, nunca executar suíte completa a cada implementação**. Foi criado `.github/workflows/c2-basic.yml` em branch de desenvolvimento, acionado apenas por `push` nela, com JVM C2 e `assembleDebug`. A CI geral `build.yml` e `emulator.yml` **não foram modificadas nem enfraquecidas**; nenhum workflow de suíte completa foi executado nesta entrega.

Um PR diretamente à `main` dispara hoje a suíte ampla, contrariando a determinação do usuário, e a `main` tem gates protegidos. Portanto a integração em `main` continua pendente de política de CI compatível, sem contorno da proteção. A C2-A também ainda não foi integrada em `main`. A C2-B pode ser revisada separadamente em PR tendo a branch C2-A como base, sem acionar a suíte completa, desde que os gatilhos sejam conferidos.

## Limitações e próxima etapa

- Mapeamento direto no OSMD foi implementado e testado com **fixtures JavaScript**; toque/escala visual em Android real precisa ser validado pelo usuário quando houver oportunidade. A seleção por lista é alternativa segura para notas sobrepostas.
- Notação derivada pode ser aproximada para rubato, quiálteras e vozes musicais complexas; limites C2-A documentados em [102](102-CRIACAO-C2-A-MOTOR-IMPLEMENTACAO.md).
- **C2-C:** versão derivada não destrutiva vinculada ao projeto/perfil, armazenamento atômico, inclusão explícita no backup e restauração compatível, abertura da própria partitura no Estudar. Nada do trabalho C2-B deve autorizar salvar derivados sem essa camada.

**Resultado:** código C2-B implantado e compilado na branch; **não ainda distribuído em APK público ou integrado à `main`**.
