# 113 — Atualização da suíte após as seis mudanças de interface

**Estado:** documentação e testes atualizados na branch técnica `feat/home-ajustes-acumulados` em 02/11/2026. **Sem APK, suíte integral, merge ou Release nesta entrega.**

## Testes obsoletos corrigidos

- `MidiInterfaceSelfTest.java` e `emulator_midi_interfaces.py`: a reprodução MIDI não oculta as barras superiores. Os estados sintéticos são `study-playing` e `study-note-tap`, com checagem da visibilidade de ambas as barras, do transporte, do espaço vertical estável, da nota MIDI, das conexões e do MusicXML original.
- `emulator_ui.py`: remove as antigas buscas por “Buscar título ou arquivo” ou “buscar título”; identifica o campo pelo atributo de acessibilidade “Buscar por título ou nome de arquivo”.
- `emulator_study_layout.py`: esclarece que o toque após retorno ao compasso não precisa restaurar barras escondidas; mantém comparação de altura da partitura e notas.
- `c2_d_ui_smoke.py`: após Adicionar à Biblioteca, verifica mensagem de sucesso e fecha o diálogo OK antes da navegação; localiza o cartão com a ação acessível **Estudar C2-D partitura de teste**.
- `emulator_creation_navigation.py`: no fluxo celular/tablet, verifica botões Nova gravação e Minhas Criações alinhados, com larguras iguais.
- `emulator_study_baseline.py`: **manter** o botão “Começar pela Ode à Alegria”, pois testa intencionalmente uma versão histórica por worktree isolado.

## Gates e preflight

- O workflow completo `build.yml` executa **antes de Chromium e Gradle APK** todos os cinco contratos: `test_home_banner_contract.py`, `test_study_compact_chrome_contract.py`, `test_creation_responsive_contract.py`, `test_creation_library_contract.py` e `test_suite_six_changes_contract.py`, além de `py_compile` nos scripts Android críticos.
- `test_workflow_contract_audit.py` controla referências dos workflows, inclusão dos novos testes nos gates e incompatibilidades de visibilidade/Criação.
- O workflow `home-basic.yml` executa os mesmos contratos e compila Java sem `assemble` ou geração de APK.

## Preservações e limites

Não foi desabilitado teste de reconhecimento MIDI, notação, MusicXML, importação, backup, histórico, segurança, assinatura ou versões antigas. A suíte completa exige APKs e emulador; **sua execução permanece pendente**, bem como testes físicos no Realme 7 5G, Lenovo Tab M9 e Yamaha P-45. Não confundir testes básicos aprovados com validação funcional Android completa.

## Correções motivadas pela primeira execução do PR #101

- O preflight de Chromium, MIDI, estudos e biblioteca progrediu até o passo JVM de `LibrarySearchTest`, que falhou por omitir `SessionFiles.java` na linha `javac` do workflow completo. Corrigido mantendo as asserções de chaves portáteis.
- O primeiro `ui-fast` encontrou `KeyError: labels` no teste de tipografia dos botões da Biblioteca. O coletor de métricas é agora explícito: elementos `TextView` individuais (Button e EditText) exportam `sizeSp` diretamente; apenas contêineres têm `labels`. O teste passa a comparar os valores nativos medidos sem dispensar a verificação da fonte.
- O resultado anterior do PR não conta como gate aprovado; repetir os workflows completos no commit de correção antes de integrar.

## Ajuste do teste real de cabeçalho no emulador

No Pixel 3 720×540, o menu da engrenagem pode rolar verticalmente e UIAutomator só expõe entradas visíveis. O teste do layout confirmou que encontrou a engrenagem, mas falhou exigindo também a entrada legada **Calibrar acordes MIDI** simultaneamente com Histórico/Como usar. A asserção dinâmica verifica as duas novas entradas (no início do menu) e os contratos Java conservam a obrigatoriedade da opção de calibração sem depender da altura de tela. A funcionalidade não foi retirada do aplicativo.

## Diagnóstico do gate completo do PR #101 (02/11/2026)

- No run [#41514402241](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41514402241), `ui-fast` passou, mas `emulator-suite` falhou em `emulator_study_layout.py:69` ao executar `responsive_evidence`. O helper `reveal_study_controls` fazia um `dump()` com UIAutomator durante a reprodução contínua. A hierarquia `/sdcard/ui.xml` não foi gerada em três tentativas (WebView em animação). Os logs Android reportavam `headerVisible=true`, `modesVisible=true`, `transportActive=true` e posição da partitura estável. Não há evidência de desaparecimento das barras.
- A correção substitui o gesto legado e a coleta de acessibilidade nesse estado por validação da geometria nativa de ambas as barras e do transporte; o toque e a identidade MIDI das notas permanecem testados por caminhos próprios. A comparação estática/playing/restored permanece exigida, agora sem toque artificial na pauta.
- Auditoria adicional encontrou pressuposto frágil no teste de calibração MIDI de `emulator_ui.py`: a opção ficava fora da primeira janela do menu da engrenagem em telefones menores. O teste agora procura e acessa a opção com rolagem, mantendo as asserções sobre calibração e ausência de entrada MIDI.
- O contrato automatizado das seis alterações verifica que o helper não executa `dump()` nem toque durante a execução, e exige a busca acessível com rolagem. **Correções de testes, não supressão das verificações.**
- Esta atualização deve ser comprovada com `build` e `emulator` completos no SHA final da PR, antes do merge protegido e da publicação do APK permanente. O status anterior de falha não autoriza integração.
