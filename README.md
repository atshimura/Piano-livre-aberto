# Piano Livre — desenvolvimento aberto (candidato em preparação)

Aplicativo Android de estudo de piano, usando MIDI USB, MusicXML, leitura de partitura e exercícios musicais.

**Beta de código aberto em preparação; ainda não é V1 nem distribuição oficial validada.**

## Licença

O código-fonte próprio do projeto é oferecido sob a [GNU GPL-3.0-only](LICENSE). Bibliotecas e outros componentes de terceiros conservam suas próprias licenças. Arquivos musicais, arranjos e fontes externos **não são automaticamente cobertos pela GPL**; a biblioteca desta cópia contém somente exercícios originais gerados para testes.

## Compilar

Com JDK 17, Gradle 8.9, Android SDK 35 e Node/npm:

```sh
cd PianoLivreAndroid
gradle --no-daemon :app:assembleDebug
```

Resultado: `PianoLivreAndroid/app/build/outputs/apk/debug/app-debug.apk`. O build prepara o mecanismo de notação offline a partir das dependências upstream.

**O APK público de desenvolvimento utiliza o pacote separado `br.org.pianolivre.opensource` e assinatura de depuração local, ou uma chave pública de beta estável configurada exclusivamente por segredos do GitHub em compilações confiáveis.** Quando o segredo de assinatura persistente ainda não estiver configurado, APKs de CI distintos poderão ter assinaturas diferentes e não atualizar uns aos outros. O APK público pode coexistir com a beta privada `br.org.pianolivre`; não tente substituir diretamente a beta privada com APK de assinatura diferente, pois dados não migram automaticamente.

## Catálogo de demonstração

Esta versão inicial exportável vem com **dez exercícios MusicXML originais**, para permitir compilação e regressões sem distribuir obras de direitos não autorizados. O plano do mantenedor é disponibilizar **oito partituras de autoria de transcrição própria** (sete clássicas e *Bella Ciao*) após as condições aplicáveis de redistribuição; *Asa Branca* e *Aquarela* não estão incluídas no catálogo distribuível sem autorização.

**Importante:** uma transcrição de ouvido não transfere direitos autorais da composição preexistente. A licença GPL-3.0-only cobre o código do projeto, não transforma automaticamente músicas protegidas em domínio público.

## Testes e limitações

A automação em `.github/workflows/` cobre compilação, verificações musicais Java/JavaScript, comparação MusicXML e inicialização Android em emulador. **Esses workflows só serão considerados comprovados após o primeiro build real do repositório público.** Dispositivos reais e Yamaha P-45 ainda requerem testes.

Dúvidas, correções e contribuições são bem-vindas conforme [CONTRIBUTING.md](CONTRIBUTING.md); problemas de segurança devem seguir [SECURITY.md](SECURITY.md).
