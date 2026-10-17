# 06 — Build e testes da cópia pública

O repositório público possui dois workflows em executores padrão do GitHub:
- `.github/workflows/build.yml`: validação de segurança/licenças de assets, testes Java/JVM, JavaScript, MusicXML, MIDI, Gradle, metadados e assinatura do APK; upload do artefato em push confiável.
- `.github/workflows/emulator.yml`: instala APK no emulador Android, executa smoke de interface/backup e envia evidências.

Versões de entrada: JDK 17, Gradle 8.9, Android SDK 35, Node/npm. A fonte de `versionCode` e `versionName` é `gradle.properties` aplicada em `settings.gradle`.

```sh
cd PianoLivreAndroid
python3 scripts/verify_public_snapshot.py
gradle --no-daemon :app:assembleDebug
```

Para obter um APK: abra Actions → **Open Piano Livre - tests and APK** → execução bem-sucedida da `main` → artefato `PianoLivre-open-release-apk` desde 0.9.35 (debug nas versões anteriores). Artefatos duram poucos dias; uma Release exige publicação específica. Se o usuário não configurar uma chave persistente no GitHub, não prometer atualização incremental de APKs gerados em execuções distintas.

**Evidência inicial verificada em 15/10/2026:** [build público 38801580110](https://github.com/atshimura/Piano-livre-aberto/actions/runs/38801580110) e [emulador 38801580087](https://github.com/atshimura/Piano-livre-aberto/actions/runs/38801580087), ambos success na versão com dez exercícios sintéticos. Essa evidência **não aprova automaticamente alterações posteriores**. Consulte o run do commit efetivo.

Nenhum workflow público depende de checkout, artefato, chave, runner pessoal ou Actions do repositório privado. A CI não testa diretamente o Yamaha P-45 nem instalação física no Realme/Lenovo. Atualização da antiga 0.9.9 privada é outra linha de compatibilidade e não deve ser reivindicada para o pacote público com appId distinto.

## Consolidação da beta 0.9.31

Main exige Secrets de assinatura permanente e o certificado público registrado; uma chave faltante/divergente impede distribuição. PRs não recebem esses Secrets. Artefatos incluem APK/metadados/certificado por 30 dias. Após mudança de versão na main, publicação de pré-release durável aguarda build e emulador aprovados no mesmo commit. Seis testes sem rede exercitam as barreiras dessa publicação.

Emulador desliga Wi-Fi/dados, compila a base pública fixa 0.9.25, instala-a com dados sintéticos e atualiza para o APK atual. UPGRADE_SELFTEST_PASS verifica persistência. A chave desse ensaio é debug descartável do runner; assinatura permanente é verificada no build confiável separado. Confira o run atual, sem presumir sucesso a partir desta descrição.

## Distribuição release

Desde 0.9.35, Actions compilam debug e release, conferem pacote aberto/certificado/versão e publicam somente release sem depuração. O emulador instala release sobre debug preservando dados sintéticos, exige flags de self-test ignoradas e run-as bloqueado; ver documento 44 e PUBLIC_RELEASE_UPGRADE_UI_PASS no commit efetivo. Isso não substitui atualização/MIDI em hardware real.
