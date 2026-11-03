# 116 — Implementação 2: exclusão de partitura derivada na Criação

**Data:** 03/11/2026. **Branch:** `feat/creation-score-separate-delete-20261103`. **PR #103** (rascunho, base `feat/library-safe-remove-20261103`/PR #102). A `main` e a Release 0.9.62 não recebem alterações aqui.

## Decisão funcional aprovada

Na tela **Laboratório Musical → Criação → Minhas Criações → Detalhes da composição** há três ações diferentes:

1. **Excluir somente a partitura…** aparece **apenas quando existe partitura gerada salva**. Confirmar **Excluir partitura** apaga definitivamente a derivação `creation-scores/<UUID>.pls`, inclusive os ajustes manuais; o projeto `creations/<UUID>.plc`, a gravação MIDI canônica e seus eventos permanecem byte a byte intactos. Após a operação, a mesma tela apresenta **Gerar partitura**. Cancelar não modifica nada.
2. **Mover composição para a Lixeira** mantém o comportamento reversível anterior, apenas esclarecendo o objeto da ação no rótulo: composição MIDI e eventual partitura derivada ficam preservadas, mas indisponíveis para estudo até **Restaurar**.
3. **Excluir definitivamente…** só está disponível na Lixeira, com confirmação; conserva o comportamento anterior de excluir a composição e sua derivação. Não confundir com o comando exclusivo da partitura.

**Diferença da Implementação 1:** o menu **Excluir da biblioteca** desvincula uma partitura criada **sem apagar** seu arquivo. Na Implementação 2, **Excluir somente a partitura…** apaga o arquivo `.pls` e desvincula seus metadados.

## Consistência e recuperação de dados

- Exclusão só é disponibilizada quando a derivação existe. Antes da operação, `MusicCreationScreen` confere usuário/perfil proprietário, projeto não recuperável/não enviado à Lixeira e a integridade da partitura vinculada ao MIDI através de `CreationScoreStore.read`.
- `CreationScoreDeletion` usa identificador UUID validado, recusa symlinks, verifica a existência do arquivo original e move somente o `.pls` para `creation-score.delete-pending`.
- Antes de mudar os metadados grava o marcador durável `committed`: interrupção **antes** dele restaura o `.pls`; interrupção **depois** dele repete, de forma idempotente, a desvinculação dos perfis **antes** de descartar os bytes da derivação. Se a atualização falhar, o arquivo permanece no staging para recuperação posterior.
- `LibraryStore.removeCreationEveryProfile` limpa atomicamente `addedCreations`, favoritos, coleções e `user_N_file` de todos os perfis que referenciem a partitura, sem apagar o histórico de estudo. A sessão carregada em memória também é encerrada se apontar para a derivação excluída.
- A recuperação é chamada na inicialização do aplicativo, antes de `makeBackup` e antes da exportação manual do backup. Não há alteração de formato de backup ou de projetos; backups anteriores continuam válidos. Restaurar um backup anterior que contenha a derivação pode permitir recuperá-la.
- Sem serviços externos, telemetria, dependências pagas, permissões adicionais ou alterações no arquivo privado histórico.

## Testes básicos realizados

Workflow dedicado `.github/workflows/creation-deletion-basic.yml`, acionado somente na branch da Implementação 2:

- Contratos Python da UI, Criação e Biblioteca, incluindo a exclusividade das duas ações.
- JVM: `CreationScoreDeletionTest.java` verifica preservação do original, exclusão só da derivação, falhas antes/depois do marcador, recuperação com metadados indisponíveis e idempotência; mantém regressão JVM da Implementação 1.
- Compilação `:app:compileDebugJavaWithJavac` e `py_compile` de roteiros Android; verificação explícita de que **nenhum APK foi gerado**.

**Resultado oficial:** consultar execução do SHA final no PR #103. Não transferir aprovação de commits anteriores para a entrega final.

## Atualização da suíte completa — sem execução

- `CreationSelfTest.java`: ajustado ao nome **Mover composição para a Lixeira** mantendo os testes de Lixeira, restauração e exclusão definitiva.
- `c2_d_ui_smoke.py`: aproveita a **mesma composição C2-D existente** no roteiro. Após exercitar geração, adição à Biblioteca, estudo e desvinculação da Implementação 1, reabre Detalhes, testa **Cancelar** (o `.pls` continua), confirma **Excluir partitura**, verifica `.pls` ausente, projeto `.plc` preservado **byte a byte**, vínculo `addedCreations` removido e botão **Gerar partitura** novamente visível. Sem novo AVD ou novo fixture.
- `LibrarySelfTest.java`: cenário sintético com dois perfis confere remoção do vínculo, favoritos/coleções/sessões e preservação dos bytes originais do projeto.
- `test_creation_responsive_contract.py` e `test_creation_score_deletion_contract.py` incluem guardas contra perda ou troca da cobertura futura.

**Não realizado nesta etapa:** `build.yml`, `emulator.yml`, execução de scripts Android, `assembleDebug`/`assembleRelease`, assinatura permanente, APK, Release, testes físicos ou integração à main. Testes Java/Python que verificam código não comprovam o funcionamento de gestos/diálogos em aparelho.

## Próxima etapa

Após autorização para os gates necessários, executar a suíte completa no SHA de integração, corrigir eventuais problemas reais de interface e validar em dispositivos Realme 7 5G, Lenovo Tab M9 e Yamaha P-45. PR #103 depende funcionalmente do PR #102.
