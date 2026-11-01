# AGENTS.md — continuidade do Piano Livre aberto

**C2-B (01/11/2026):** prévia e revisão de partitura implementadas **parcialmente**, em branch `feat/criacao-c2-b-previa-ajustes`, derivada da C2-A (ainda não integrada). Código: `CreationScoreReview`, `CreationScorePreviewScreen` e botão Gerar partitura em `MusicCreationScreen`; **seleção acessível por lista**, acorde, múltiplas e intervalo de compassos, ritmo/quantização/duas pautas, Desfazer/Refazer e descarte da sessão; **não** existe seleção direta no cabeçote OSMD nem armazenamento/restauração/Estudar (C2-C). Consultar [docs/103](docs/103-CRIACAO-C2-B-PREVIA-AJUSTES.md) e [issue #95](https://github.com/atshimura/Piano-livre-aberto/issues/95). Só testes básicos locais, sem PR funcional/merge por conflito com CI full. Não declarar UI/OSMD validados em Android nem Release publicada.

**Regra de testes definida pelo usuário em 01/11/2026, para implementações futuras:** executar **somente testes básicos direcionados; não rodar a suíte completa**. A CI atual dispara a suíte completa em todo PR com mudança Java (`build.yml` e `classify_pr_risk.py` + `emulator-suite`). **Não abrir PR que a dispare sem resolver esse conflito de processo**, não enfraquecer silenciosamente verificações/proteção de `main` e não declarar uma branch como integrada sem os gates efetivos. Para C2-A, consultar [relatório 102](docs/102-CRIACAO-C2-A-MOTOR-IMPLEMENTACAO.md) e [issue #95](https://github.com/atshimura/Piano-livre-aberto/issues/95). O planejamento C2 está no [PR documental #96](https://github.com/atshimura/Piano-livre-aberto/pull/96); este registro não implica que o PR foi integrado.

Consolidacao vigente: beta **0.9.54-open-beta (63)** em PR unico reunindo #76, #78 e #80, autorizada pelo usuario. Consultar [registro e protocolo final](docs/77-CONSOLIDACAO-PUBLICACAO-0954.md). Estados anteriores de PR sem merge/Release sao historicos. Validar checks do conjunto e da main antes de publicar; C1 continua planejada, issue #79 aberta; hardware/V1 pendentes.


Este repositório é a **base principal de desenvolvimento** do Piano Livre. Leia este arquivo antes de alterar código. Trabalhe em português do Brasil; preserve a GPL-3.0-only do código próprio e as licenças de terceiros.

## Estado e identidade
- Prompt 4 autoriza consolidar os PRs #71–#74 e publicar 0.9.52-open-beta (61), preservando proteção da main e assinatura permanente; evitar Release intermediária 0.9.51. Consultar `docs/73-CONSOLIDACAO-PUBLICACAO-PROMPT-04.md` e seu registro final vinculado. Restrições de merge/publicação nas entregas anteriores são históricas; próximas versões continuam em branch/PR e exigem seus próprios checks.
- Repositório de trabalho: `atshimura/Piano-livre-aberto`; branch estável `main`. Verifique a HEAD, a versão em `PianoLivreAndroid/gradle.properties` e os Actions em cada intervenção.
- O repositório histórico `atshimura/piano-livre` **continua privado**, e não deve ser usado para novos PRs públicos, testes públicos, materiais de assinatura ou downloads pelos Actions.
- O pacote debug aberto é `br.org.pianolivre.opensource`, separado de `br.org.pianolivre` histórico. Não existe atualização in-place entre os pacotes. Backup/restauração manual é a via prevista para dados do usuário, **após validação**.
- A V1 **não está concluída**. Não confundir CI bem-sucedido, emulador e hardware real. Testes com Yamaha P-45, Realme 7 5G e Lenovo Tab M9 exigem execução física.
- Há testes Android, JVM, JavaScript, auditoria MusicXML/MIDI e UI no emulador em `.github/workflows/build.yml` e `emulator.yml`; executores Linux hospedados pelo GitHub. Nunca vincular runner Windows pessoal ao repositório público.

## Decisões de produto obrigatórias

**Continuidade futura — Implementação E do Descobrir (planejamento de 01/11/2026, ainda não codificada):** quando o usuário disser **“Implemente o Bloco E-A/E-B/E-C/E-D da Implementação E”**, consultar [docs/95](docs/95-IMPLEMENTACAO-E-DESCOBRIR-PAUTA.md), [docs/96](docs/96-IMPLEMENTACAO-E-QUATRO-BLOCOS.md) e [issue #89](https://github.com/atshimura/Piano-livre-aberto/issues/89) e **executar o bloco pedido** com os pré-requisitos confirmados, sem exigir repetição de decisões já aprovadas. Nomes: E-A **Diagnóstico Direto**, E-B **Pauta Grande e Alinhada**, E-C **Escala Inteligente Reversível**, E-D **Validação e Entrega**. Uma branch/PR funcional única, diferente do PR #90 documental; testes dirigidos nos blocos intermediários, gates completos antes de integração, assinatura existente e nenhuma Release sem autorização aplicável. Preservar todo o restante do Descobrir. **Estado posterior em 01/11/2026:** E-A (diagnóstico) e E-B (pauta maior + início alinhado) estão na branch funcional PR #91; E-B passou no [build Chromium #41316106385](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41316106385), com [documento 98](docs/98-IMPLEMENTACAO-E-E-B-PAUTA-ALINHADA.md). E-C (E4) **implementado na branch #91 com testes JVM/Chromium e UI Android focada aprovados**; [documento E-C](docs/99-IMPLEMENTACAO-E-E-C-ENQUADRAMENTO-REVERSIVEL.md), [build #41318276669](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276669) e [ui-fast #41318276483](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276483). **E-D (E5+E6) em validação final de candidata 0.9.59/code68**; [registro de gates](docs/100-IMPLEMENTACAO-E-E-D-VALIDACAO-ENTREGA.md). APK e merge somente após checks; Release não aprovada. Nenhum merge/release funcional desta implementação. O planejamento inicial em docs 95/96 precede este registro.
**Continuidade futura — Criação C1 (somente planejamento em 30/10/2026):** quando a solicitação disser “vamos continuar a implementação do criação - C1”, ler [docs/76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md](docs/76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md), [docs/prompts/CRIACAO_C1_IMPLEMENTACAO.md](docs/prompts/CRIACAO_C1_IMPLEMENTACAO.md) e [issue #79](https://github.com/atshimura/Piano-livre-aberto/issues/79) antes de agir. O usuário aprovou a opção A (uma tomada MIDI inicial, camadas depois); C1 cobre gravação/reprodução/projetos, não gera MusicXML/PDF (C2/C3). Verificar PRs #76/#78 e issue #77 no estado atual, sem pressupor integração. **A C1 não está implementada por este registro.**

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

## Política experimental de CI visual V1
A branch de infraestrutura propõe rota rápida somente para recursos raster isolados, sem dispensar a suíte completa para publicação. Consulte [docs/90](docs/90-POLITICA-CI-VISUAL-V1.md). Não ampliar para Java, XML, lógica MIDI ou persistência sem revisão explícita. A main mantém regras de proteção; o workflow de publicação passa a exigir disparo manual na main após suíte completa. Antes de usar, conferir status do PR e checks.
