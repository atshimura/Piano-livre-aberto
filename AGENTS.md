# AGENTS.md — continuidade do Piano Livre aberto

Criação C1.1–C1.5 implementada no [PR #82](https://github.com/atshimura/Piano-livre-aberto/pull/82), candidata 0.9.55-open-beta/code64, sobre a beta pública 0.9.54/code63 (`cf4b24b`). A revisão ampliou captura, recuperação, persistência e testes funcionais Android. [Registro técnico e protocolo de aceite](docs/78-CREACAO-C1-IMPLEMENTACAO.md). Integração autorizada após build/emulador completos no SHA final; Release depende de autorização específica e está bloqueada por `publishApproved=false`. Hardware/V1 pendentes.


Este repositório é a **base principal de desenvolvimento** do Piano Livre. Leia este arquivo antes de alterar código. Trabalhe em português do Brasil; preserve a GPL-3.0-only do código próprio e as licenças de terceiros.

## Estado e identidade
- Prompt 4 autoriza consolidar os PRs #71–#74 e publicar 0.9.52-open-beta (61), preservando proteção da main e assinatura permanente; evitar Release intermediária 0.9.51. Consultar `docs/73-CONSOLIDACAO-PUBLICACAO-PROMPT-04.md` e seu registro final vinculado. Restrições de merge/publicação nas entregas anteriores são históricas; próximas versões continuam em branch/PR e exigem seus próprios checks.
- Repositório de trabalho: `atshimura/Piano-livre-aberto`; branch estável `main`. Verifique a HEAD, a versão em `PianoLivreAndroid/gradle.properties` e os Actions em cada intervenção.
- O repositório histórico `atshimura/piano-livre` **continua privado**, e não deve ser usado para novos PRs públicos, testes públicos, materiais de assinatura ou downloads pelos Actions.
- O pacote debug aberto é `br.org.pianolivre.opensource`, separado de `br.org.pianolivre` histórico. Não existe atualização in-place entre os pacotes. Backup/restauração manual é a via prevista para dados do usuário, **após validação**.
- A V1 **não está concluída**. Não confundir CI bem-sucedido, emulador e hardware real. Testes com Yamaha P-45, Realme 7 5G e Lenovo Tab M9 exigem execução física.
- Há testes Android, JVM, JavaScript, auditoria MusicXML/MIDI e UI no emulador em `.github/workflows/build.yml` e `emulator.yml`; executores Linux hospedados pelo GitHub. Nunca vincular runner Windows pessoal ao repositório público.

## Decisões de produto obrigatórias
**Continuidade Criação C1:** ler docs/76, docs/78 e o prompt completo de execução. C1 implementa tomada original MIDI, projetos, recuperação, reprodução e exportação; C2/C3 ficam fora. Não reutilizar os resultados do smoke antigo como prova da revisão. Integração autorizada após gates completos; publicação exige autorização específica. Não alterar `publishApproved=false` como escolha técnica rotineira.

- App de aprendizagem musical local/offline; **sem login, servidor, telemetria ou sincronização automática** sem aprovação explícita.
- Modo Espera: notas esperadas avançam; notas extras não bloqueiam, mas devem ser indicadas. Não penalizar demora. Acordes: janela temporal ajustável após calibração, sem valores arbitrários fixados antecipadamente. Duração/pedal: orientação após ataque correto, não bloqueio.
- Onde houver teclado guia visual, mostrar ambas as mãos, inclusive simultaneidade, dinâmica e duração. **Decisão posterior aprovada no Prompt 2 (29/10/2026): remover o guia do modo Estudar**, preservando MIDI/reconhecimento, preferências legadas e componentes compartilhados. **O guia visual não é um sintetizador de áudio**. Diferenciar reprodução MIDI no instrumento de visualização de teclas.
- Claves Sol/Fá devem ser posicionadas por G4/F3 nas respectivas linhas, mantendo pautas/claves fixas à esquerda durante rolagem.
- Não inferir mão pela altura da nota, nem modificar MusicXML importado ao aplicar preferências de estudo.
- Prompt 3: Inicial/Estudar compartilham `MidiConnection`; disponibilidade e abertura não equivalem a conexão. Nome só com porta funcional e metadado real. Preservar geração/época de recepção, pausa segura sem rebobinar e encerramento de notas/pedal antes de liberar recursos controláveis. Ver `docs/72-CONSOLIDACAO-MIDI-INTERFACES-PROMPT-03.md`.
- Backup v2 exige pré-validação, confirmação e rollback; nunca apagar dados do usuário antes de validar restauração.

## Regras de colaboração e segurança
1. Trabalhe em branch e PR. Não faça force-push, não desative testes, não exponha credenciais. Proteção da branch `main` deve ser ativada nas configurações do GitHub.
2. Não copie histórico Git do repositório privado, chave beta privada, arquivos `signing/`, backups reais, logs sensíveis ou fichas de alunos.
3. Mantenha a licença do **código** separada dos direitos das **composições e partituras**. Consulte `docs/LICENCAS-PARTITURAS.md` e o manifesto de autorização em `PianoLivreAndroid/app/src/main/assets/score-rights.json` antes de incluir músicas.
4. Não incorporar `Aquarela` nem `Asa Branca` sem licença de redistribuição; `Bella Ciao` depende de revisão da versão/arranjo efetivamente transcritos. A declaração de que MXL foram transcritos de ouvido não elimina direitos sobre composições preexistentes.
5. Mantenha assinatura pública estável **somente por GitHub Actions Secrets**, nunca em arquivos públicos. PRs não devem receber segredos. Consulte `docs/ASSINATURA.md`.
6. Para cada alteração: atualizar documentação, versão se o APK mudar, adicionar teste adequado, verificar execução da CI e relatar o que não foi testado. Não apresentar logs de branches antigas como prova da branch atual.

## Leitura sugerida
`README.md` → `docs/00-INDICE.md` → `docs/01-VISAO-E-REQUISITOS.md` → `docs/02-ARQUITETURA.md` → `docs/07-ESTADO-E-ROADMAP.md` → `docs/08-DECISOES.md` → `docs/10-CONTINUIDADE.md` → `docs/09-SEGURANCA-LICENCAS.md`.

## Fluxo de compilação
```sh
cd PianoLivreAndroid
python3 scripts/verify_public_snapshot.py
gradle --no-daemon :app:assembleDebug
```
A fonte de versão é `gradle.properties` aplicada por `settings.gradle`. Abra os workflows Actions para baixar o APK de debug. Os artefatos expiram; disponibilização durável exige uma Release. Não tratar APK com chave debug efêmera como atualizável.

## Critérios para passagem entre sessões
Confira `main`, PRs e runs atuais; preserve os invariantes acima; verifique catálogo/manifesto de direitos/validador; rode testes; atualize docs relevantes e registre PR, commit, resultados e pendências de hardware. O projeto deve permanecer utilizável por outra pessoa ou IA sem acesso ao repositório privado.
