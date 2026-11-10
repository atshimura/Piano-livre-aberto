# ZAI-003 — APK de atualização do Piano Livre principal, 0.9.65

**Decisão do mantenedor em 09/11/2026:** o novo gerador ZAI-002 passa a ser o aplicativo **principal instalado**. Foi autorizado gerar um APK que atualize a edição aberta `br.org.pianolivre.opensource` usando a assinatura permanente. Esta autorização substitui a antiga restrição de que todo APK da ZAi precisa de identificador separado **somente para este instalador de atualização**.

**Escopo efetivamente executado:** APK principal assinado, em branch temporária `zai/ci/apk-principal-0-9-65-20261109`, **sem merge em `main`**; sem criar tag, Release ou publicar na Play Store. A atualização da branch `main` é uma operação distinta, sujeita a PR e gates completos de regressão.

## Código e compilação

- Código funcional: [ZAI-002](ZAI-002-ENTREGA-APK-ISOLADO-20261109.md), incorporado a `zai/lab` pelo [PR #114](https://github.com/atshimura/Piano-livre-aberto/pull/114).
- Base da branch de compilação: `zai/lab` em `994c3e726965fc9fc7c852aa61967e6a9446a2e6`; mudanças somente no workflow de empacotamento principal, sem modificações no código do motor.
- Commit exato do APK principal: `1afd8df8147904fe3ad2c0f8adf34e0576833ed3`.
- Workflow [`.github/workflows/principal-apk-zai002.yml`](https://github.com/atshimura/Piano-livre-aberto/blob/zai/ci/apk-principal-0-9-65-20261109/.github/workflows/principal-apk-zai002.yml), limitado a push na branch temporária. Usa GitHub Actions Secrets de assinatura permanente. O build explícito sem a flag `zai.test` produz a mesma identidade Android do aplicativo principal.
- Versão do **APK distribuído**: `0.9.65-open-beta`, **versionCode 74**, acima da edição `0.9.64-open-beta`/73. Os valores são definidos como propriedades `-P` de build sem alterar `PianoLivreAndroid/gradle.properties` da linha ZAi nem as versões da `main`.
- Não divulgar senha/keystore; não reutilizar o APK ZAi com identificador `.opensource.zai`.

## Execução da CI e artefato

- [GitHub Actions #42733788489](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42733788489): **SUCCESS**, verificação de segurança, regressões JVM (ZAI-001/002, incluindo 14 notas MIDI reais), assinatura permanente e compilação release.
- Artefato `PianoLivre-PRINCIPAL-0.9.65-APK-assinado-1afd8df8147904fe3ad2c0f8adf34e0576833ed3`, **ID 13725253780**, retenção prevista de 30 dias.
- APK original `app/build/outputs/apk/release/app-release.apk`, extraído do ZIP de evidências, **793.584 bytes**.
- **SHA-256 do APK:** `af580353391da3274d596ddd903e52e2e11e1c3e0d0177b6069ed88df5a058fe`; confirmado novamente sobre os bytes baixados e comparado ao comprovante de GitHub Actions.
- Pacote: **`br.org.pianolivre.opensource`** (principal); launcher label **Piano Livre**.
- Assinatura SHA-256: `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290` — **mesmo certificado público permanente** configurado para a edição principal.
- `versionCode='74'`, `versionName='0.9.65-open-beta'`, minSdk 26, targetSdk 36; APK não depurável, sem permissão `INTERNET`, verificação via `apksigner` aprovada.
- Não houve publicação no GitHub Releases; o artefato é temporário e foi disponibilizado nesta conversa como APK independente.

## Validação e riscos restantes

- **Nenhum teste de atualização física em aparelho foi executado pelo agente.** O mesmo `applicationId`, mesma assinatura permanente e `versionCode` ascendente satisfazem as condições Android normais para atualização sobre o 0.9.64, mas não garantem ausência de problema em dados específicos.
- `*.plc` (gravações originais), projetos e backups devem ser preservados na atualização. O gerador novo não suporta o ajuste/derivação `*.pls` antigo: a interface deverá informar incompatibilidade e permitir regeneração a partir das gravações originais, sem apagar arquivos legados silenciosamente.
- **Backup verificado antes da instalação.** Instalar por cima, **sem desinstalar**; uma eventual reversão para code 73 pode ser bloqueada pelo Android. Não apagar dados para forçar downgrade.
- Testes reais pendentes: Realme 7 5G, Lenovo Tab M9 e piano Yamaha P-45; visualização física de notas staccato/legato e teste de persistência após atualização.
- Também está pendente executar os gates de build e emulador completos para eventual PR de integração à branch `main`. O APK pode ser atualizado no aparelho sem que o código seja declarado integrado à `main`.
- **`main` permaneceu no commit `55426cf79cbbd652ea5035fb6774d7a31b5380e3`** durante a execução.

## Histórico da depuração do workflow

As execuções preliminares #42733065661, #42733099459 e #42733555624 falharam na etapa de verificação final; nenhuma distribuiu um APK com identidade incorreta. O diagnóstico detectou que a primeira proposta combinava `-Ppianolivre.zai.test=false` com uma flag `true` remanescente, fazendo prevalecer a identidade ZAi. O workflow foi corrigido, verificado por `aapt` e o **único APK disponibilizado** é o da execução aprovada #42733788489.
