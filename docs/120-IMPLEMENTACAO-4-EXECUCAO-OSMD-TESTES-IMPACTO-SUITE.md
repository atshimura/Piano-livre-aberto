# 120 — Implementação 4: OSMD único, compatibilidade de Criação/Estudar, erros e suíte

**Data:** 03/11/2026. **Estado:** código desenvolvido em branch técnica, **não publicado** e sem merge na `main`.
**Branch:** `feat/impl4-osmd-only-errors-20261103`, baseada no planejamento #106 e dependente dos PRs #105, #104, #103 e #102.
**Planejamento:** [119](119-IMPLEMENTACAO-4-OSMD-UNICO-COMPATIBILIDADE-CRIACAO-ESTUDAR.md).
**Validação básica do SHA técnico `49dde74c295f7348b038958ef65babd85677dd32`:** [run #41646220734 — SUCCESS](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41646220734).
**Proibido nesta entrega:** suíte completa, `assembleDebug`, `assembleRelease`, APK, emulador e publicação. Nada disso foi executado.

## Diagnóstico confirmado e correção aplicada

O modo Criação desenhava o MusicXML usando **OSMD em `free`**, enquanto o modo Estudar tentava **OSMD em `engraving-poc`** com reconciliação visual/musical adicional. O código ainda mostrava `PianoView` (pauta esquemática) durante a carga e tinha fallback em duas etapas para `TemporalNotation.render` e `PianoView` na falha. Portanto, a aparência antiga podia ocorrer **sem** partitura de origem antiga. Não foi obtida a partitura/log específico do aparelho do mantenedor; a causa exata desse caso concreto não é comprovada.

**Realizado:**

1. `NotationView` utiliza OSMD **exclusivamente**, nas apresentações Criação e Estudar. Retirado fallback automático; cada abertura possui geração/callback protegido contra resposta atrasada, controle de timeout e erro estruturado. `notation.html` não carrega `temporal-notation.js`, e deixa de suportar o modo temporal.
2. `PianoScreens` substitui o desenho esquemático durante carregamento por **“Preparando partitura moderna…”**; só libera a experiência normal após `onRendered`. Eliminados do aplicativo `PianoView.java` e `temporal-notation.js`. Estado não visual de teclado/posição e MIDI mantido em `StudyInputState`. A opção temporal não está disponível ao usuário nem como alternativa automática.
3. `CreationStudyCompatibility` valida uma partitura produzida **com o mesmo caminho OSMD de Estudar** num WebView local real e com geometria antes de aceitar **Salvar partitura** ou **Adicionar à Biblioteca**. Confere leitura pelo `Score.read`, pertença à composição/perfil, identidade do MusicXML salvo e revalidação antes do vínculo. Erro ou cancelamento preservam `.plc` e o trabalho não salvo. PLS1 v1/v2 e MusicXML histórico permanecem legíveis.
4. `NotationFailure`/ `NotationDiagnosticsStore` guardam **código de incidente, etapa, hash do XML e metadados limitados** em arquivo privado do app com escrita atômica, tamanho máximo de 256 KiB e até 50 ocorrências. Não armazenam partitura/MIDI, não transmitem dados nem exigem serviço pago. `MainActivity` apresenta diálogo detalhado **“Não foi possível exibir a partitura”** com **Tentar novamente**, **Copiar diagnóstico** e **Voltar à Biblioteca**, além de histórico local acessível pelas opções, exportação voluntária e exclusão do histórico.
5. Falhas de leitura MusicXML, de persistência e de OSMD recebem tratamento visível; a reprodução/estudo é interrompida com segurança em falha. O código de diagnóstico é gerado no próprio dispositivo.

**Limitações:** o diálogo não confirma reprodutibilidade no Realme 7 5G/Lenovo Tab M9/Yamaha P-45, pois não houve teste físico. A meta de 100% de compatibilidade é um **contrato de aceitação e bloqueio**, demonstrado nesta entrega por uma **amostra dirigida de seis partituras sintéticas em dois modos (12 casos reais no Chromium)**; não representa prova universal de todas as combinações MusicXML, WebView e aparelhos. A suíte de UI Android ainda não foi executada. O oráculo do renderizador histórico permanece **somente em `tests/fixtures/retired-temporal-oracle.js`**, excluído do APK e não utilizado pela aplicação; trata-se de referência independente para regressões e migração dos testes, **não** de um motor de produção.

## Testes básicos autorizados e evidências

Workflow novo `.github/workflows/impl4-osmd-basic.yml`, SHA `49dde74c295f7348b038958ef65babd85677dd32`:
- `verify_public_snapshot.py`: **PASS**, com auditoria de pacote e fontes públicas.
- **49 testes Python** de contratos (4 + 5 + 5 + 6 + 6 + 8 + 9 + 6), além de `py_compile` dos roteiros Android.
- JavaScript direcionado: `OSMD_POC_CONTRACT_PASS checks=79`, `STUDY_NOTE_TIP_PASS checks=34`, `OPTICAL_TIME_MAP_PASS checks=160`, responsividade/mapeamento, gesto e identificação de nota; sintaxe JS inline verificada por `node --check`.
- Corpus JVM de Criação e persistência existente: `CREATION_RHYTHM_BASIC_PASS checks=35`, `CREATION_C2_A_BASIC_PASS checks=47`, `CREATION_C2_B_BASIC_PASS checks=27`, `CREATION_C2_C_BASIC_PASS checks=26`, `CREATION_C2_D_CORPUS_PASS checks=141`; total **276 verificações JVM** nesses grupos.
- **Seis fixtures MusicXML realmente geradas** (`melody`, `legacy`, `chords`, `crossing`, `long`, `assigned`). `CreationOSMDStudyBrowserTest.js` abriu o arquivo em `free` e `engraving-poc`: **`IMPL4_OSMD_CREATION_STUDY_BROWSER_PASS cases=12`**. Falha OSMD forçada também retorna código `PL-NOT-RENDER`, sem fallback.
- `gradle --no-daemon :app:prepareOfflineNotation` e `:app:compileDebugJavaWithJavac`: **BUILD SUCCESSFUL**; verificação **`IMPL4_BASIC_NO_APK_PASS`**.

Falhas de runs preliminares foram corrigidas: contratos que procuravam `legacyFallback`/`scoreView`, compilação do autoteste `MidiSessionSelfTest` ainda referindo `legacyFallback`, preparação de asset OSMD ausente no teste de navegador e inicialização incorreta de contador de um teste JS. O último run acima é a evidência vinculante, não os runs preliminares.

## Impacto sobre a suíte completa — atualizada, NÃO executada

- `.github/workflows/build.yml`: inclui contrato da Implementação 4 e **`CreationOSMDCompatibilityFixture` + `CreationOSMDStudyBrowserTest.js`** no corpus Chromium já existente; deixa de exigir `TemporalNoteTipBrowserTest.js` como cobertura do motor removido. **Não alterado o gate de assinatura/publicação da `main`.**
- `.github/workflows/emulator.yml`: mantém roteiros UI e remove a expectativa do screenshot temporal antigo; preserva imagens OSMD e cobertura MIDI.
- `tests/c2_d_ui_smoke.py`: acrescentada prova de OSMD nos três passos **Criação → Salvar → Biblioteca → Estudar**, com exigência de `POC_OSMD_READY` e ausência de fallback.
- `tests/emulator_study_failure.py`: agora exige erro OSMD com **diálogo, incidente privado sem conteúdo musical, código estável e retry**, e proíbe retorno ao motor temporal.
- `tests/emulator_ui.py`: substituídos percursos e métricas temporais por OSMD óptico, mantendo validação de controles, loops, transporte e retorno em segundo plano.
- `EngravingProofContractTest.js`, `StudyNoteTipTest.js`, `StudyProfileBrowserTest.js`, `test_creation_library_contract.py`, `test_workflow_contract_audit.py`: expectativas alinhadas ao motor único e ao bloqueio de Biblioteca.
- Testes históricos de renderização temporal em `TemporalLayoutTest.js`, `TemporalRenderingTest.js`, `NotationAdvancedTest.js`, `NotationSpannersTest.js`, `NotationOrnamentsTest.js`, `NoteIdentificationTest.js`, `MusicXmlFixturesTest.js`, `musicxml_visual_crosscheck.js` usam oráculo **arquivado somente em testes**, para não perder verificações de semântica dos formatos legados e sem embarcá-lo no aplicativo.
- Alterações de teste são **revisão estática e execução básica seletiva**, não equivalem à execução da suíte inteira: possíveis problemas em fluxos do emulador/integração serão conhecidos somente em gates posteriores autorizados.

## Próximos passos e critérios para integração

Revisão do PR de implementação sobre o planejamento, validação de cobertura em corpus MusicXML mais amplo, inspeção de exatidão da correspondência `StudyProfile.compare` para escritas complexas, testes de timeout/retomada/histórico em WebView real, e execução autorizada da suíte completa antes de integração `main`. Publicação/assinatura exigem os gates da branch protegida. Validação musical em aparelhos reais permanece separada e será do mantenedor. **Não declarar 100% de compatibilidade comprovada em todos os arquivos existentes/aparelhos somente com os 12 casos do Chromium.**
