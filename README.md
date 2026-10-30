# Piano Livre — Android, código aberto

Consolidação **0.9.52-open-beta (61)**: nova Inicial, Estudar e conexão MIDI compartilhada. [Registro da integração, validação e publicação](docs/73-CONSOLIDACAO-PUBLICACAO-PROMPT-04.md) · [Pré-release oficial](https://github.com/atshimura/Piano-livre-aberto/releases/tag/v0.9.52-open-beta) · [Baixar APK](https://github.com/atshimura/Piano-livre-aberto/releases/download/v0.9.52-open-beta/PianoLivre-0.9.52-open-beta.apk). A disponibilidade e os resultados efetivos são registrados na Release após os checks do merge; não usar artefatos de PR para atualizar a beta pública.

Aplicativo experimental de estudo de piano com leitura MusicXML, importação MIDI aproximada, USB-MIDI, modo Espera e demonstração MIDI. Na implementação do Prompt 2, a tela Estudar prioriza a pauta, com controles responsivos e sem teclado guia. Desenvolvido em Java/Android e JavaScript. **O código aberto não é uma versão V1 concluída.** Consulte [a entrega e seus limites](docs/71-TELA-ESTUDAR-PROMPT-02.md).

## Onde o desenvolvimento acontece
**Planejamento futuro — Laboratório Musical / Criação C1:** [especificação completa e navegação entre projetos](docs/76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md) · [roteiro para implementação em outra conversa](docs/prompts/CRIACAO_C1_IMPLEMENTACAO.md) · [issue #79](https://github.com/atshimura/Piano-livre-aberto/issues/79). Quando solicitado “vamos continuar a implementação do criação - C1”, usar esses documentos. **Ainda não é funcionalidade implementada**; a C1 prevê gravação simples MIDI, gestão de projetos e reprodução, deixando partitura/PDF para etapas posteriores.

Este **é o repositório principal do desenvolvimento**. Correções, propostas, versões, testes e APKs futuros devem ser feitos aqui, em branches com pull requests. A base histórica privada permanece separada e **não** é necessária para compilar o aplicativo aberto. Veja [AGENTS.md](AGENTS.md) e o [índice técnico](docs/00-INDICE.md).

## Compilação e testes
Instale JDK 17, Gradle 8.9, Android SDK 35, Node.js/npm:
```sh
cd PianoLivreAndroid
python3 scripts/verify_public_snapshot.py
gradle --no-daemon :app:assembleDebug
```
APK local debug: `PianoLivreAndroid/app/build/outputs/apk/debug/app-debug.apk`. A distribuição usa `:app:assembleRelease`, sem depuração, mantendo o pacote aberto; consulte os Actions/Release do commit. O pacote de notação OSMD é baixado e convertido **durante a compilação** e embutido no APK; o app instalado funciona offline. O GitHub Actions executa [testes e APK](https://github.com/atshimura/Piano-livre-aberto/actions/workflows/build.yml) e [emulador](https://github.com/atshimura/Piano-livre-aberto/actions/workflows/emulator.yml). A primeira validação pública com os dez exercícios sintéticos passou em 15/10/2026; novas versões devem ser verificadas pelo resultado do seu próprio workflow.

O APK de distribuição público tem identificador `br.org.pianolivre.opensource`. Pode coexistir com a versão privada, mas não a substitui, não compartilha dados automaticamente e só atualiza instalações públicas anteriores assinadas pelo **mesmo certificado**. A distribuição pela main exige a assinatura permanente configurada; builds locais e PRs usam chave debug de desenvolvimento. Consulte [assinatura e atualização](docs/ASSINATURA.md).

Pré-releases aprovadas ficam em [Releases](https://github.com/atshimura/Piano-livre-aberto/releases), com APK, certificado e SHA256SUMS. Quando ainda não houver release da versão, use o artefato do Actions da main e confira o commit.

## Música e direitos
A biblioteca publicada combina exercícios independentes e, quando liberadas, transcrições próprias de composições em domínio público. O inventário e a política estão em [licenças das partituras](docs/LICENCAS-PARTITURAS.md) e no manifesto `score-rights.json`. **A GPL-3.0-only se aplica ao código próprio**, não automaticamente a composições/arranjos externos. *Asa Branca* e *Aquarela* não são distribuídas; *Bella Ciao* permanece em revisão de versão/arranjo. O usuário pode importar localmente seus próprios arquivos, respeitada a legislação.

## Limites e segurança
Os testes automatizados **não substituem** uso real com Yamaha P-45, Realme 7 5G e Lenovo Tab M9. O app funciona offline. A importação possui limites e validações explícitos; consulte os documentos 36–40 sobre formatos, aproximações e aceite. Não use dados privados de alunos em issues nem envie chaves ou backups ao GitHub.

- [Manual da beta pública](docs/11-MANUAL-DE-USO-BETA.md)
- [Estado e roadmap](docs/07-ESTADO-E-ROADMAP.md)
- [Como contribuir](CONTRIBUTING.md) · [Segurança](SECURITY.md) · [Licença do código](LICENSE)
