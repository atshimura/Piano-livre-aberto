# 115 — Implementação 1: auditoria da suíte completa após exclusão na Biblioteca

**Data:** 03/11/2026. **Branch:** `feat/library-safe-remove-20261103`. **Base:** main 0.9.62-open-beta (code 71). **PR técnico:** #102 (rascunho, base de revisão separada da main).

## Problemas concretos e correções

| Componente da suíte | Risco observado | Correção |
|---|---|---|
| `tests/emulator_ui.py` — importação MIDI | Apagava `Synthetic.musicxml` e `.source.mid` via `run-as rm`. O teste poderia passar com o novo botão de exclusão quebrado. | O mesmo fixture MIDI é agora removido pelo menu de toque prolongado do cartão, com confirmação, ausência de cartão e ausência dos dois arquivos. Apenas o antigo `upgrade.mxl` sintético permanece com limpeza direta, isolada da funcionalidade testada. |
| `tests/emulator_ui.py` — biblioteca de assets | Verificava apenas favoritos, coleções e abertura da música; não exercia `Ocultar` e `Restaurar músicas ocultas`. | Esconde e restaura Ode à Alegria no mesmo percurso, verifica contagem nativa **relativa** em vez de fixar o número de músicas ocultas, e comprova favorito preservado. |
| `tests/c2_d_ui_smoke.py` — Criação→Biblioteca | Verificava adicionar e estudar uma partitura, mas não remover o vínculo sem apagar a gravação/derivado. | Reutiliza o único projeto C2-D já gerado no teste, faz toque prolongado, escolhe exclusão e verifica ausência do vínculo em `addedCreations`, com arquivos originais `.plc`/`.pls` ainda presentes. Sem segundo fixture nem nova jornada. |
| `tests/c2_d_ui_smoke.py` — menu Android | Exigir `clickable=true` em cada item textual de `AlertDialog` pode produzir falha falsa: Android pode expor clique somente no pai. | Seleção por texto visível e coordenadas, com confirmação da transição seguinte; não dispensa teste de presença da opção. |
| `LibrarySelfTest.java` — backup e perfis | Não cobria `hiddenScores` no backup, coleção após desvincular criação ou recuperação pós-commit. | Cenários sintéticos de backup/restauração de ocultas, validação de metadados inválidos, coleção e favorito, limpeza de dois perfis e simulação de crash com limpeza de referências ao recuperar. |
| `ImportedScoreDeletion.java` — crash entre preferências e marcador | A ordem anterior alterava preferências antes de gravar `committed`: ao reiniciar, a recuperação poderia restaurar bytes sem restaurar favoritos e coleções. Testes antigos não simulavam a janela. | Ordem transacional alterada: stage → marcador durável → limpeza idempotente de metadados → remoção dos bytes. `recover` conclui metadados pendentes **antes** de limpar staging. Falhas pós-commit mantêm arquivos para nova tentativa; caminho normal evita gravação dupla. |
| `tests/ImportedScoreDeletionTest.java` | Só simulava rollback de arquivo na falha do callback, sem cruzar a transição do marcador com metadados. | Casos JVM independentes para interrupção antes do marcador (restaura XML/MIDI), depois do marcador (repete metadados e conclui exclusão), callback indisponível, idempotência, chaves inválidas e isolamento. |
| `tests/test_library_removal_contract.py` | Contratos poderiam aprovar nova interface com suites Android ainda antigas. | Verifica que a suíte futura chama os roteiros com exclusão real e que não há remoção direta do fixture MIDI nem restauração destrutiva. |
| `.github/workflows/library-basic.yml` | Testes estáticos não validavam a sintaxe dos roteiros de emulador recém-editados. | Acrescentada validação `py_compile` dos dois roteiros, sem emulador e sem APK. |

## Testes preservados e sem mudança desnecessária

- A suíte principal continua a invocar `emulator_ui.py`, `LibrarySelfTest` e `c2_d_ui_smoke.py` em sua sequência preexistente: não foi acrescentado outro emulador, nem outro workflow completo.
- `emulator_release.py`, `emulator_public_upgrade.py`, `upgrade_fixture.py` e testes de versões antigas não precisam mudar seus dados legados. O campo `hiddenScores` é **opcional**, por isso o teste que compara os JSON antigos continua válido: a simples abertura da Biblioteca não migra nem modifica preferências.
- As verificações de MusicXML, reconhecimento MIDI, notação, assinatura, segurança, histórico, perfis e backups não foram removidas ou enfraquecidas.
- O teste completo continua necessário antes de integração à main; execução em emulador não equivale a aceitação física.

## Protocolo de validação desta entrega

**Permitido:** `library-basic.yml`: auditoria pública, contratos Python, testes JVM de exclusão/recuperação, chaves portáteis, `py_compile` e `:app:compileDebugJavaWithJavac`, comprovando ausência de APK.

**Não executar aqui:** `build.yml` completo, `emulator.yml`, `assembleDebug`, `assembleRelease`, assinatura, Release ou testes físicos. Nenhum merge na main autorizado. Registre o run do HEAD final no PR #102.

**Pendente em fase posterior:** executar a suíte integral no SHA de integração autorizado e confirmar em Android os gestos e diálogos, backup/restauração de ocultação, exclusão entre perfis e retomada pós-crash. Não descrever roteiros que apenas compilaram como se tivessem passado em emulador.

**Próxima ação:** revisar o PR #102, manter o estado rascunho e só planejar integração e APK em solicitação posterior.
