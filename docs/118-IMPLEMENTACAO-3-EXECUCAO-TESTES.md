# 118 — Implementação 3: ritmo por ataques e Uniformizar trecho

**Data:** 03/11/2026. **PR técnico:** [#105](https://github.com/atshimura/Piano-livre-aberto/pull/105) em rascunho, branch `feat/creation-rhythm-uniformize-20261103`, base no planejamento #104, dependente dos PRs #103 e #102. **Base de versão:** 0.9.62-open-beta / code71; `main` e Release permanecem inalteradas.

## Entrega funcional implementada (não publicada)

No modo **Criação → Minhas Criações → Detalhes → Gerar / Visualizar e ajustar partitura**, o botão **Ajustar ritmo** oferece andamento BPM, compasso, grade e **Regularização rítmica**:

- **Rigorosa:** quantização básica à grade configurada, preservando diferenças de ataque que não caem na grade.
- **Equilibrada:** padrão inicial; aproxima ataques de pulsos inteiros somente quando caem dentro de metade da grade rítmica em torno da pulsação.
- **Flexível:** aceita até uma grade de desvio para aproximar ataques de pulsos inteiros.
- Os níveis usam **limites derivados da grade musical** e não milissegundos constantes. A heurística ainda não foi calibrada com um corpus expressivo de pianistas reais; não inferir um andamento variável infalivelmente. Os valores não modificam o MIDI original.

O motor `CreationScoreTranscriber` usa prioritariamente **intervalos entre Note On sucessivos da mesma linha rítmica**, agrupando ataques simultâneos em acordes. Note Off é informação complementar para notas finais, períodos com silêncio muito longo e articulações; uma última nota curta em frase de pulso regular pode receber o valor do intervalo anterior. **Cada linha permanece separada**: a atribuição manual da mão direita/esquerda e de voz permite ritmos independentes dentro de uma pulsação/estrutura de compassos compartilhada. Não confundir pauta com mão física: enquanto não houver atribuição explícita, o app usa **linhas provisórias de notação** por pauta e canal, sem afirmar que sabe que mão tocou.

## Nova ferramenta **Uniformizar trecho**

Na prévia, entre em **Divisão entre pautas**, selecione **notas**, um **acorde** ou **Selecionar intervalo de compassos**, e escolha **Uniformizar trecho**. A ferramenta apresenta:

- Figura musical: **semibreve, mínima, semínima, colcheia, semicolcheia ou fusa**.
- Escopo por mão: todas as selecionadas ou mão direita/esquerda **explicitamente atribuída**.
- Escopo por voz musical: todas ou voz específica. O comando **Atribuir mão / voz** permite corrigir explicitamente o agrupamento sem deduzir mão pela altura.
- Botão **Ver prévia**: recálculo MusicXML validado e comparação textual **antes → depois** das posições/notas afetadas, contagem de notas e linhas. **Aplicar** confirma; **Cancelar** não altera a partitura.
- Seleções não contíguas são tratadas como **sequências independentes** dentro de cada linha. Um acorde parcialmente selecionado, uma figura que invada nota não selecionada, uma sobreposição com nota fixa ou a ultrapassagem do limite dos compassos escolhidos gera **aviso e recusa**, não movimento silencioso de outras notas. O comando reposiciona musicalmente a sequência — não troca apenas o símbolo gráfico.
- **Desfazer/Refazer** tratam a uniformização como uma alteração atômica; **Restaurar ritmo automático** limpa somente os ajustes manuais de ritmo, sem limpar os ajustes de pauta/mão ou os eventos MIDI. A seleção visual direta e a alternativa textual/por compassos existentes foram reaproveitadas.

**Limitações reconhecidas:** a prévia é textual (comparação de notas/posições) antes da confirmação, não uma segunda pauta completa lado a lado; a partitura OSMD é redesenhada **após** aplicar. Pausas reais longas, rubato, ritmos complexos/quiálteras, vozes internas ambíguas e identidade física da mão exigem revisão musical. O sistema recusa operações ambíguas em vez de deslocar elementos não selecionados. O número de linhas de notação é uma interpretação; sem atribuição manual não se afirma qual mão física executou o trecho.

## Persistência, compatibilidade e segurança

- `CreationScoreStore` salva derivadas em **PLS1 formato 2**, contendo `Regularization`, `handOverrides`, `voiceOverrides`, `rhythmOverrides` por `sourceId`, além dos campos anteriores, checksum SHA-256, proprietário e fingerprint do MIDI.
- **PLS1 formato 1** continua legível e é regenerado em modo `legacyTiming=true`, sem alterar o MusicXML antigo. Mudar conscientemente o ritmo no editor passa ao novo modelo e salva em v2. Arquivos legados não são regravados na abertura.
- `BackupArchive` **continua versão 4** e transporta os bytes validados das derivadas v1/v2; a validação/restauração rejeita conteúdo inconsistente. Backups antigos seguem o contrato anterior de compatibilidade. O projeto `.plc`, a execução MIDI original e seus eventos não são reescritos por qualquer ajuste de partitura.
- Limites de tamanho, CRC/fingerprint, salvamento temporário com sync e substituição atômica, controle de perfil e exclusões das Implementações 1/2 continuam intactos. Sem serviços externos/telemetria/servidores/recursos pagos.

## Testes básicos e revisão da suíte completa

**Permitidos e utilizados:** `.github/workflows/creation-rhythm-basic.yml` executa `verify_public_snapshot.py`, contratos Python de Criação/Biblioteca/Implementação 3, `py_compile` dos roteiros Android e regressões JVM do motor (incluindo padrão de semínimas humanas, último ataque, duas mãos/vozes, acordes, conflitos, Undo/Redo, PLS1 v1→v2, reabertura). Finaliza com `:app:compileDebugJavaWithJavac`, **sem assemble/APK**. Consultar os runs no [PR #105](https://github.com/atshimura/Piano-livre-aberto/pull/105); o estado final só será considerado aprovado pelo check no SHA final.

**Suíte completa revisada, NÃO executada:**
- `build.yml`: passa a verificar o contrato da Implementação 3 e executar `CreationRhythmUniformizeTest` junto dos testes C2-A/B/C/D e de persistência existentes; não remove nem reduz gates antigos.
- `c2_d_ui_smoke.py`: amplia a mesma jornada/fixture sintética existente com seleção de compassos, diálogos Atribuir mão/voz e Uniformizar, prévia, Cancelar/Aplicar, Desfazer/Refazer e salvamento posterior; mantém as verificações anteriores de Biblioteca e exclusão, sem duplicar emulador.
- `BackupArchiveSelfTest.java`: o teste futuro do backup v4 passa a incluir uma derivada v2 com atribuição de mão/voz e ajuste de figura, conferindo persistência/recuperação.
- `CreationScoreTranscriberTest.java`: expectativa anterior de que duas teclas mantidas criavam sempre vozes distintas foi substituída pela política aprovada de ataques sucessivos em linha provisória; regressões de polifonia real e vozes explícitas continuam.
- `test_creation_responsive_contract.py` e `test_creation_rhythm_contract.py`: contratos de botões, seleção, regularização e referências da suíte.

**Expressamente não realizados:** `build.yml` completo, `emulator.yml`, scripts de interface Android, `assembleDebug`/`assembleRelease`, geração/assinatura de APK, Release, integração à `main` ou validação nos aparelhos Realme 7 5G, Lenovo Tab M9 e Yamaha P-45.

## Conclusão e próximos passos

A Implementação 3 está codificada na branch técnica e permanece em rascunho, dependente dos PRs #102–#104. Após autorização de integração, executar os gates da `main` no SHA final e corrigir as falhas reais de UI/backup se aparecerem; teste em emulador não substitui aceitação em aparelho real. Não declarar publicada enquanto a `main` e a Release não contiverem os commits verificados.
