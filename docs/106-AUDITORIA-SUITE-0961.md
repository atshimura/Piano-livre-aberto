# Auditoria integral da suíte 0.9.61 — contratos de interface e migração

Data: 02/11/2026. Referência: PR #100; build aprovado no SHA `9556426`; suíte Android `41454876158` reprovada em `emulator_release.py:36`, **depois** da aprovação dos dois estudos completos. Esta auditoria não pressupõe aprovações posteriores.

## Matriz de execução e decisão

| Bloco | Scripts e finalidade | Avaliação / tratamento |
| --- | --- | --- |
| Build obrigatório | Verificação pública/licenças, contratos de risco, navegador Chromium, MusicXML, JVM, MIDI, Criação C2, publicação e APK Gradle | Aprovado no run 41454876438; preservado integralmente e acrescido de preflight estático |
| UI rápida | `emulator_home_refinements`, `emulator_discovery_layout`, `emulator_ui_fast`, `emulator_discovery_components`, `emulator_creation_functional` | Aprovado no run 41454876158; conserva testes reais de Android |
| Instalação e compatibilidade debug | `emulator_study_baseline`, `emulator_study_layout`, `emulator_midi_interfaces`, `emulator_upgrade` (baselines 0.9.46/52/54) e os autotestes de banco/backup/MIDI | Passou no run 41454876158; mantém comparadores históricos deliberados |
| UI musical extensa | `emulator_ui`, `emulator_creation_navigation`, `emulator_creation_functional`, `emulator_discovery_components`, `emulator_study_failure`, `emulator_polyphony`, `emulator_notation06`, `emulator_responsive`, `emulator_study_session` | Passou até `STUDY_B_ANDROID_SESSION_PASS` no run citado; preservadas as asserções de nota, geometria, navegação e MIDI |
| APK release não depurável | `emulator_release.py` | **Teste desatualizado:** exigia `80%` na Home. Agora exige Home, perfil certo, ausência de controles de Estudar, e após navegação explícita exige `80%` na tela Estudar, ausência de guia e bloqueio de introspecção/debug |
| Atualizações públicas assinadas (somente na main) | `emulator_public_upgrade.py`, baselines reais 0.9.46/52/54 | **Teste desatualizado:** exigia velocidade e modo Estudar ao iniciar. Agora verifica Home, certificado permanente, dados/arquivo intactos **antes** de qualquer escolha, e velocidade efetiva após abrir Estudar |
| Criação C2-D | `c2_d_ui_smoke.py` | Antes ficava **após** a troca debug → release (assinaturas potencialmente incompatíveis). Agora executa no debug antes da troca, preserva evidências e restaura rotação do sistema |

## Correção de compatibilidade encontrada

A versão 0.9.60 deliberadamente abre na Home sem carregar a partitura salva. O início explícito subsequente usava `loadScore(reset=true)` com fator de velocidade padrão 100%; o salvamento da sessão poderia substituir a preferência de 80% do perfil. A `MainActivity` passa a hidratar **apenas o estado em memória** de velocidade, mão e modo do perfil ativo ao iniciar na Home, sem abrir Estudar nem sobrescrever a partitura, cursor ou backup. O teste real exige 80% ao escolher música, não apenas valor XML.

## Critérios ainda obrigatórios

- Compilação/fluxos JVM/JS/Chromium e teste dirigido de release Android no SHA exato.
- Suite Android integral de PR, sem desativar `build`/`emulator` ou modificar proteção de `main`.
- Após integração, os testes de APK assinado e atualização pública com certificado permanente em `main`; não considerar a passagem da PR como prova de assinatura real.
- Testes físicos Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 pendentes.

Os testes de `KeyboardGuide` em JVM permanecem pertinentes aos componentes MIDI compartilhados, embora o guia **visual** tenha sido removido da tela Estudar; rótulos de arquivos de evidência históricos não significam que o guia visual voltou.
