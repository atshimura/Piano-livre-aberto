> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 24 — Auditoria, notação e MIDI: execução das etapas 1, 2 e 3

> **Estado consolidado em 15/10/2026:** este documento descreve a **intervenção na branch do PR #1**, que **gerou APK beta privado 0.9.11/code20**. **O PR #1 foi integrado à main em `4f8db43b71bfa85ca7689c3228da663a4d5b4844`**, que contém agora 0.9.11/code20. As afirmações abaixo sobre “versão a incrementar”, “CI a confirmar” e “beta ainda não distribuída” são **etapas históricas do planejamento original**, superadas por [docs/25](25-CONTINUIDADE-0.9.9-0.9.11.md). Os três workflows da referência `81590024b...` terminaram com **success**: [build 38730155811](https://github.com/atshimura/piano-livre/actions/runs/38730155811), [emulador 38730155782](https://github.com/atshimura/piano-livre/actions/runs/38730155782) e [auditoria MIDI 38730155826](https://github.com/atshimura/piano-livre/actions/runs/38730155826). **O build direto da 0.9.11 foi normalizado**: `gradle.properties` contém code20/name0.9.11 e `settings.gradle` os aplica via AGP `finalizeDsl`; `build.gradle` mantém apenas literal legado 0.9.10, que não determina o APK. Script de preparação tornou-se validador opcional. Hardware real não validado.

**Data:** 15/10/2026. **Referência inicial:** `main` em `dff949ce6fb19f3f3f63ad5a0d7aa1408ba6e2cc`, beta 0.9.10, versionCode 19. **Integração:** branch `feature/etapas-1-3-revisao-musical-20261015` e respectivo pull request (revisão obrigatória antes de integrar). Esse trecho reflete o planejamento inicial anterior ao merge; a **beta privada 0.9.11 foi compilada e integrada à main** (não representa V1 concluída).

## Etapa 1 — Auditoria e consolidação

Inspecionados `AGENTS.md`, README, estado/roadmap, documentação das versões 0.9.7–0.9.10, código do renderer temporal, `Score.java`, `MainActivity.java`, `WaitRecognizer.java`, `ChordCalibration.java`, `MidiProtocol.java`, `MidiPlayback.java`, `MidiPerformance.java`, testes Java/JS, versão Android e os três workflows de CI. Os **dez MXL de referência** estão presentes no inventário do repositório.

Evidência **pré-alteração**: na HEAD acima, o [build de APK e testes Java/JS](https://github.com/atshimura/piano-livre/actions/runs/38713435101) e o [smoke test Android em emulador](https://github.com/atshimura/piano-livre/actions/runs/38713435109) concluíram com sucesso. Esses resultados **não testam as alterações desta branch** nem substituem o Yamaha P-45 em Realme 7 5G/Lenovo Tab M9.

A auditoria constatou que o plano anterior ainda citava 0.9.8, embora a implementação vigente seja **0.9.10-beta**; a fase não reimplementou recursos já corrigidos em 0.9.9 e 0.9.10. Foram identificados dois comportamentos delimitados para correção sem alterar MIDI musical/timestamps. Também foi adicionada a execução dos workflows em `pull_request` para análise das alterações antes de integrar em `main`.

## Etapa 2 — Símbolos musicais e renderização

**Problema reproduzível:** notas de graça isoladas com `<type>eighth</type>`/`16th` eram desenhadas sem bandeirolas, confundindo sua figura impressa. Grupos com `<beam>` de notas de graça não formavam vigas, porque `collectBeamGroups` os descartava para proteger as vigas das notas de execução.

**Comportamento novo:** o renderer mantém notas de graça separadas das temporais por pauta, voz e ataque escrito; usa a ordem MusicXML para as notas de graça do mesmo instante. Calcula a posição gráfica usando `_graceShift` (sem mudar `at`, altura ou reprodução). Adiciona bandeirolas para figuras isoladas e vigas primárias/secundárias, com espessura e hastes proporcionais à escala de 67%. As extremidades dos segmentos são cortadas por bloco SVG, inclusive quando uma viga atravessa uma fronteira de tile; há margem adicional de coleta para grupos ornamentais.

**Aceite verificável:** oitavas/semicolcheias de graça soltas com bandeirolas; duas vigas pequenas conectadas a três notas de graça em um mesmo beat, cada haste tocando a viga, presentes nos dois SVGs de teste; sem juntá-las às vigas das notas temporais; sem criar ataques MIDI ornamentais não definidos.

**Limitações declaradas:** desenhos estilizados, não certificação editorial; grupos de grace de eventos fora do mesmo ataque, vigas entre pautas, grupos de grace muito extensos, microtonalidade, acidentes próximos e combinações MusicXML incomuns exigem testes adicionais. O modo Leitura Livre mantém seu renderer independente.

## Etapa 3 — MIDI e Aprendizagem/Espera

**Problema reproduzível:** a interface calculava `sustainDown` somente a partir da **última** mensagem CC64 recebida. Com sustain nos canais 1 e 2, soltar somente o pedal do canal 1 apagava o indicador visual mesmo se o canal 2 continuasse ativo; o observador `MidiPerformance` já mantinha os estados separadamente.

**Comportamento novo:** após cada CC64, a tela obtém de `MidiPerformance.pedalDown(channel)` o estado agregado dos 16 canais. A desconexão continua limpando o estado. Teste ampliado para pressionar pedal em canais distintos, soltar só um, soltar o segundo e desconectar.

**Regras pedagógicas preservadas:** notas extras são indicadas mas não bloqueiam; uma nota incorreta isolada nunca avança; acordes requerem novos ataques e só usam janela temporal após calibração real por perfil; duração e pedal são orientativos no modo Aprendizagem. Mãos não são inferidas por altura; teclado guia continua representando ambas. Não foram introduzidos limiares em ms, ataques MIDI para graça ou critérios de avaliação novos.

## Testes, risco e critérios de integração

- **Testes alterados:** `NotationOrnamentsTest.js` (figuras com bandeirolas, vigas de dois níveis, encaixe de hastes, corte em dois tiles) e `MidiPerformanceTest.java` (CC64 multicanal e reinicialização por desconexão).
- **Regressões obrigatórias na branch:** `TemporalLayoutTest.js`, `ClefEngravingTest.js`, `NotationSpannersTest.js`, `NotationAdvancedTest.js`, `NotationOrnamentsTest.js`, `musicxml_visual_crosscheck.js`, suíte Java de Espera/PerformanceRoute/MIDI e dez MXL, assinatura e instalação de APK, smoke UI no emulador, e auditoria de reconhecimento. O novo gatilho `pull_request` permite executar essas verificações sem integrar alterações não avaliadas.
- **Estado dos testes após o patch:** consultar os resultados do PR; os workflows verdes da HEAD 0.9.10 são a referência **anterior**, não uma aprovação automática deste patch.
- **Não testado:** cabo USB-MIDI, Yamaha P-45 real, medições de latência de acordes, Android Realme 7 5G, Lenovo Tab M9, fluidez física prolongada. Não afirmar sucesso em hardware físico.
- **Compatibilidade de instalação:** o `build.gradle` ainda declara **0.9.10-beta / versionCode 19** nesta branch. **Antes de gerar e distribuir APK de atualização, incrementar ambos os campos** (ex.: 0.9.11-beta / versionCode 20) e confirmar assinatura/teste de atualização sem perder dados. Não distribuir APK modificado sob o mesmo código como se fosse uma nova versão.
- **Riscos residuais:** a camada MusicXML Java e o parser JS continuam independentes; cobertura de notação complexa é limitada; há pendências maiores de dados e biblioteca no roadmap; não copiar partituras proprietárias nem expor credenciais de assinatura.

## Preparação da próxima beta instalável (15/10/2026)

**Situação histórica anterior à normalização:** o script inicialmente reescrevia os campos de versão no workspace, sem alterar assinatura. **Situação atual:** `PianoLivreAndroid/gradle.properties` contém os valores canônicos code20/name0.9.11, e `settings.gradle` os aplica a `android.defaultConfig` na fase `androidComponents.finalizeDsl`, após a leitura de `app/build.gradle`. Portanto `gradle --no-daemon :app:assembleDebug` gera diretamente 0.9.11, sem scripts de escrita nem modificação de credenciais. O script `prepare-beta-version.py` foi mantido exclusivamente como verificador não mutante.

Os workflows `build-apk.yml` e `emulator-smoke.yml` compilam diretamente com Gradle e verificam a versão efetiva do **APK compilado** com `aapt dump badging`. A mesma assinatura beta histórica continua sujeita à validação do `apksigner` e ao teste `adb install -r` com preservação de dados. O usuário deve criar backup manual antes da atualização e nunca desinstalar o aplicativo para atualizar. Somente indicar links de download depois do resultado verde do commit exato.

## Registro da intervenção

**Arquivos de produção:** `temporal-notation.js`, `MainActivity.java`. **Testes:** `NotationOrnamentsTest.js`, `MidiPerformanceTest.java`. **Integração:** workflows `build-apk.yml`, `emulator-smoke.yml`, `recognition-audit.yml`. **Documentação:** este arquivo, `docs/07` e README (quando integrado).

**Próximo passo concreto:** confirmar a execução verde dos três workflows no PR, revisar imagens da partitura no emulador, incrementar versão antes de qualquer APK distribuível, integrar após revisão e calibrar acordes em P-45/Realme/Lenovo.
