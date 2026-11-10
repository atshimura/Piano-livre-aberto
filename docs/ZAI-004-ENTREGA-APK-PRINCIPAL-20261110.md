# ZAI-004 — Entrega: acordes corrigidos e APK principal 0.9.66

**Data:** 10/11/2026. **Produto:** Piano Livre, atualização da edição principal instalada (`br.org.pianolivre.opensource`). **Escopo GitHub:** branches ZAi somente; nenhuma alteração em `main`, nenhuma tag, Release ou publicação na Play Store.

## Implementação

- Plano: [ZAI-004](ZAI-004-ACORDES-COESAO-ALTURAS-APK-PRINCIPAL.md).
- [PR #118](https://github.com/atshimura/Piano-livre-aberto/pull/118) integrado em **`zai/lab`** pelo commit **`e2d114ab8607590d0d06d2594a64884870179561`**.
- `ScoreRhythmQuantizer`: coesão de notas simultâneas na mesma pauta com liberações MIDI próximas, tolerância limitada a 70ms; na ambiguidade da grade não separar o acorde em durações 4/5 semicolcheias, vozes e ligaduras artificiais. Liberações substancialmente distantes continuam separadas em vozes, preservando duração sustentada.
- `ScoreXmlComposer`: grafia de alturas alteradas alinhada ao `DiscoveryScoreBuilder`: **MIDI63 → E♭4** (Mi bemol) em lugar de D♯4; mantém inalterado o número MIDI original.
- `ZScoreChordFidelityRegressionTest`: fixture de eventos extraída e sanitizada da gravação real `acordes.mid` (sem distribuir o MIDI pessoal), duas tríades C / Cm com seis Note On e Note Off, diferença de 5–15ms em solturas. Exige dois acordes completos, seis cabeças musicais sem ataques extras, mesma voz e figura entre as três notas, Mi♭4, `Score.read` com seis eventos, split C4/B3 e preservação de uma nota realmente sustentada mais que suas companheiras. Verifica as 12 classes cromáticas contra a escrita do Descobrir.
- Mantidos `MidiTakeRecorder`, `.plc`, biblioteca, backups, projetos e as interfaces de assinatura já estabelecidas. Não criar dependências externas, servidores ou telemetria.

## Testes automatizados observados

No workflow ZAi [GitHub Actions #42799892913](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42799892913), commit funcional `7d73a52de6e26abeb5876251a40721f6d60718af`, **SUCCESS**:

| Suíte JVM | Verificações |
|---|---:|
| `ZScoreGeneratorRhythmTest` | 86 |
| `ZScoreGeneratorXmlTest` | 26 |
| `CreationScoreStoreTest` | 27 |
| `CreationC2DCorpusTest` | 45 |
| `ZScoreRealMidiRegressionTest` | 23 |
| `ZScoreChordFidelityRegressionTest` | 59 |
| **Total de verificações JVM** | **266** |

Auditoria de cópia pública e contratos Python **SUCCESS**. Gradle `:app:assembleDebug -Ppianolivre.zai.test=true` **BUILD SUCCESSFUL** e confirmação do pacote isolado no ensaio. Executadas outras validações finais com mesmo código e apenas documentos adicionais antes do merge.

## APK principal assinado — entrega conferida

- [GitHub Actions #42800557154](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42800557154): **SUCCESS**, commit da branch exclusiva de empacotamento `556022529775fc8723f0aa6290f7e075d04719ef`. O código musical corresponde ao PR #118 já integrado à `zai/lab`.
- **Artefato GitHub ID 13756365992**, `PianoLivre-PRINCIPAL-0.9.66-APK-assinado-556022529775fc8723f0aa6290f7e075d04719ef`, com APK e arquivos de auditoria; retenção de 30 dias prevista.
- APK de distribuição: `app/build/outputs/apk/release/app-release.apk`, **794.540 bytes**.
- **SHA-256:** `352890f1c5b337964b79de20cd062de4c1ec4afd53de4b5ae9662055766fe8f4`; calculado novamente nos bytes extraídos do ZIP do GitHub e comparado ao comprovante emitido no workflow.
- **`applicationId = br.org.pianolivre.opensource`**, rótulo **Piano Livre** (não `.opensource.zai`).
- **`versionCode=75`**, **`versionName=0.9.66-open-beta`**; é superior à 0.9.65/code74 já entregue ao usuário. Versão configurada por parâmetros Gradle apenas durante este build, sem alterar `gradle.properties` ou a versão da branch `main`.
- **SHA-256 do certificado de assinatura permanente:** `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`, mesmo das betas principais anteriores. GitHub Actions Secrets usados sem exposição de credenciais.
- `minSdkVersion 26`, `targetSdkVersion 36`, APK release **não depurável**, sem permissão **`INTERNET`**; `aapt` e `apksigner verify` passaram. Nenhuma Release, tag ou Play Store criada.
- O workflow específico `.github/workflows/principal-apk-zai004.yml` só é executado em `zai/ci/apk-principal-zai004-0-9-66-20261110`; não roda em `main`. Não altera workflows públicos existentes.

## Limitações e próximos passos

1. **Nenhum ensaio físico foi executado** neste ciclo. O teste no Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 continua pendente. Verificar especialmente que os dois acordes surgem como tríades empilhadas, Mi♭ no segundo e que o Estudar lê seis notas.
2. Não houve navegação automatizada completa da interface de Criação/Estudar no emulador Android nesta entrega. JVM, compilação, parse de MusicXML e OSMD renderizável não equivalem à confirmação visual física.
3. **Antes de instalar:** executar backup verificado. Atualizar diretamente a edição principal, sem desinstalar, para preservar dados locais. O Android normalmente atualiza com mesmo pacote, certificado e code ascendente; não se pode garantir migração física sem teste real. Partituras derivadas PLS1 antigas são incompatíveis com o novo gerador e podem precisar de regeneração a partir do MIDI original `.plc`. Não apagar derivações legadas silenciosamente.
4. O sistema ainda usa 120 BPM provisórios quando só há dois ataques distintos de acorde; isso é matematicamente necessário na ausência de informação rítmica suficiente. Não afirmar precisão universal de andamento.
5. Para incorporar ZAI-004 ao código GitHub da branch `main`, é necessária entrega adicional em PR expressamente autorizada com gates completos de build/emulador. O APK principal entregue ao usuário não prova que a `main` recebeu código.
6. **`main` permaneceu em `55426cf79cbbd652ea5035fb6774d7a31b5380e3`** até a última verificação; não tocamos no repositório histórico privado.
