# ZAI-001 / Z-P5 — Integração em ZAi e APK de teste assinado

**Data:** 08/11/2026. **Natureza:** registro de entrega real; não é nova versão pública, Release, tag nem validação em aparelho físico.

## Integração do novo gerador

- Contrato de produto: [ZAI-001](ZAI-001-REINICIO-GERADOR-PARTITURAS.md).
- PR funcional: [#111](https://github.com/atshimura/Piano-livre-aberto/pull/111), **integrado** exclusivamente à `zai/lab`.
- Commit de merge do gerador: `0415bd6fbcd2ecb594c422a3140f381d3616c5c1`.
- O gerador anterior e seus ajustes deixam de fazer parte da linha ZAi; substitutos: `ScoreMidiPairer`, `ScoreTempoEstimator`, `ScoreRhythmQuantizer`, `ScoreXmlComposer` e `CreationScoreGenerator`.
- PR de CI: [#112](https://github.com/atshimura/Piano-livre-aberto/pull/112), **integrado** à `zai/lab`, introduzindo apenas o workflow dedicado `.github/workflows/zai-apk-teste.yml` (sem editar workflows existentes neste PR).
- Commit de merge do workflow: `f602eaa2489be92714f3726314420536b72bcbbc`.
- `main` permaneceu em `55426cf79cbbd652ea5035fb6774d7a31b5380e3`; nenhuma atualização, publicação ou alteração de versionamento foi efetuada.

## Geração comprovada do APK de teste

**GitHub Actions:** [run #42536602439](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42536602439) — **SUCCESS**, commit `f602eaa2489be92714f3726314420536b72bcbbc`, branch `zai/lab`.

**Artefato:** `PianoLivre-ZAi-APK-teste-f602eaa2489be92714f3726314420536b72bcbbc`, ID **13638893773**, com arquivo `app/build/outputs/apk/release/app-release.apk` e comprovantes, retenção prevista de 30 dias. Não houve criação de GitHub Release.

**Integridade aferida sobre bytes do artefato descarregado:**
- APK: 793.256 bytes.
- SHA-256 do APK: `875c434832104ac8c1fa25b5caa4199bc47ceb3bc98493019fa70fe36e7fbfaf`.
- SHA-256 do certificado: `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290` (assinatura permanente via GitHub Actions Secrets; não houve exposição de chaves/senhas).
- Pacote: `br.org.pianolivre.opensource`; `versionCode 73`, `versionName 0.9.64-open-beta`; `targetSdkVersion 36`, `minSdkVersion 26`.
- Variável de build: APK **release não depurável**, sem permissão `INTERNET`.
- O comprovante de CI contém SHA-256, certificado, pacote, versão e SHA do commit; o arquivo descarregado foi conferido contra o SHA-256 declarado. Não afirmar que a presença de arquivo, isoladamente, prova o comportamento no aparelho.

## Testes observados na execução #42536602439

No GitHub Actions, as etapas **Auditoria da cópia pública e contratos dirigidos**, **Testes do novo motor ZAI-001 na JVM**, **Configurar chave pública permanente**, **Compilar APK release assinado**, **Verificar APK, identidade, assinatura e integridade** e **Publicar artefato temporário** terminaram com **success**.

Não foram executados nesta entrega:
- suíte completa `build.yml` / `emulator.yml` na `main`;
- percurso visual do gerador no emulador Android;
- ensaio de gravação USB-MIDI com Yamaha P-45 no Realme 7 5G ou Lenovo Tab M9;
- validação de atualizações instaladas sobre dados reais de usuários.

## Limites para instalação e próximos passos

O APK é destinado a **testes da linha ZAi**. Apesar da assinatura permanente, usa o **mesmo código/versão** da 0.9.64 e a instalação poderá substituir o aplicativo público que tenha o mesmo package name, caso Android permita a atualização. Antes de instalar, fazer backup manual verificado. Nunca desinstalar o app para forçar uma atualização nem confundir esta entrega com publicação estável.

Próximos passos: executar smoke de emulador Criação→Gerar→Salvar→Estudar e observar escala com oito semínimas mais oito mínimas, acordes e separação em Dó4 nos dispositivos reais quando disponíveis. Abrir correções somente em branches `zai/` e preservar `main`.
