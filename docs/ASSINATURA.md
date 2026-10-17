# Assinatura de APKs da edição aberta

Sem uma chave persistente, os APKs de debug produzidos por diferentes executores podem ser incompatíveis numa tentativa de atualização. **O pacote aberto é `br.org.pianolivre.opensource`**, independente da edição histórica.

O mantenedor deve gerar e manter, **fora de qualquer repositório público**, uma chave Android própria e um backup seguro. Pode usar a ferramenta `keytool` do JDK 17 para criar um arquivo PKCS12/JKS e alias exclusivos para a versão aberta. Não reutilizar certificados do repositório privado.

## Cadastro no GitHub
No repositório aberto, Settings → Secrets and variables → Actions → New repository secret. O workflow público já reconhece estes nomes:

- `PIANOLIVRE_PUBLIC_KEYSTORE_BASE64`: conteúdo da chave pública-beta codificado em base64.
- `PIANOLIVRE_PUBLIC_STORE_PASSWORD`: senha do armazenamento.
- `PIANOLIVRE_PUBLIC_KEY_ALIAS`: alias de assinatura definido pelo mantenedor.
- `PIANOLIVRE_PUBLIC_KEY_PASSWORD`: senha da chave de assinatura.

Mantenha arquivo, senhas e cópias de recuperação fora da árvore Git e nunca os compartilhe por PR, issue ou conversa. O workflow usa os segredos apenas em compilações de push confiável, não em pull requests.

## Validação
Depois de configurar todos os segredos, rode **Open Piano Livre - tests and APK** na `main`. Confira indicação de assinatura estável no log e a fingerprint do certificado emitida pelo apksigner. Para testar atualizações, instale dois APKs públicos sucessivos, com versões crescentes e mesma fingerprint, e confirme preservação dos dados por `adb install -r`.

O build produz um APK de desenvolvimento. Uma Release estável requer preparação e validação próprias. **Não desinstale o aplicativo histórico sem backup e restauração verificados.**

## Consolidação 0.9.31

Build da main exige os Secrets permanentes e o certificado SHA-256 7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290; falha antes de distribuir se faltar/divergir. Publicação de pré-release depende de build/emulador aprovados no mesmo commit. O ensaio de atualização do emulador usa chave debug descartável própria, separada dos Secrets. Não equivale a aceite físico. Detalhes no documento 40.

## APK de distribuição desde 0.9.35

Main compila e compara certificados de debug/release, exige a fingerprint permanente e distribui a variante release sem depuração, pacote br.org.pianolivre.opensource. O artefato é PianoLivre-open-release-apk. PR/emulador usam chave descartável; ensaio debug→release preserva dados sintéticos e bloqueia run-as. Ver documento 44 e CI efetiva, sem extrapolar para hardware.
