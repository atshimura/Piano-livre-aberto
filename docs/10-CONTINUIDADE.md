# 10 — Como continuar o Piano Livre aberto

**Repositório autoritativo para mudanças futuras:** `atshimura/Piano-livre-aberto`. Não fazer novos commits em `atshimura/piano-livre` como parte do fluxo aberto; aquele repositório histórico permanece privado.

1. Verifique a `main`, a versão em `PianoLivreAndroid/gradle.properties`, PRs abertos e os últimos runs do GitHub Actions.
2. Leia `AGENTS.md`, `docs/01`, `docs/07`, `docs/08` e o arquivo pertinente à alteração. Documentos marcados históricos não anulam o estado verificado no código atual.
3. Crie uma branch para cada correção; inclua testes compatíveis e atualize os documentos afetados. Use PR. Não realize rebase de histórico privado, nem importe `signing/`.
4. Rode `python3 PianoLivreAndroid/scripts/verify_public_snapshot.py` e as regressões adequadas. Valide CI do **próprio commit** antes de considerar a alteração aprovada.
5. Ao modificar o aplicativo distribuído, aumente versionCode e versionName efetivos, mantendo o appId público. Pacote público e privado são instalações independentes; backup e restauração exigem prova no dispositivo.
6. Cada partitura precisa de licença/justificativa documental e registro no manifesto `score-rights.json`; não presumir que uma transcrição própria afasta direitos da composição.
7. Se faltar teste no Yamaha P-45, Lenovo ou Realme, declare expressamente. Emulador não mede latência USB-MIDI.

**Assinatura:** segredos cadastrados pelo mantenedor, apenas em workflows de push confiável. Não divulgar JKS, senhas ou artefatos de assinaturas antigas. Ver [assinatura](ASSINATURA.md).

Relatório de cada intervenção: objetivo, HEAD e PR/commit, alterações, testes com links e conclusões, limitações, riscos e próxima etapa. O guia visual do teclado reproduz posições e duração **visualmente**; demonstração sonora é função de saída MIDI, não de sintetizador embutido.

## Retomada do desenvolvimento por etapas

Consultar [28 — Etapas até V1](28-ETAPAS-V1.md) e a issue #4 antes de escolher a próxima tarefa. Registrar o estado do PR e os Actions do próprio commit. Etapa 1 iniciada na 0.9.13; não marcar as etapas completas apenas por existir implementação parcial.

## Retomada 0.9.14

A integração e a assinatura da 0.9.13 foram confirmadas; não repetir o PR #5. A segunda entrega elimina callbacks visuais ociosos em pausa/fim e amplia TemporalRenderingTest. Consulte a seção final do documento 28 e a issue #6 para os Actions do commit efetivo. Próxima implementação: custo de updateInfo no ticker; depois carga longa e lifecycle. A Etapa 1 ainda não foi encerrada.

## Continuação 0.9.15

Implementado cache de apresentação StudyInfoText; ticker musical mantém frequência original, rótulo usa 100 ms durante movimento e atualizações imediatas nos comandos/término. Conferir PR e CI da entrega na issue #6. Próxima implementação: diagnóstico de carga longa/frames e lifecycle; depois reavaliar critérios de fechamento da Etapa 1.

## Continuação 0.9.16

Diagnóstico local e suspensão/retomada de ticker/WebViews implementados; procedimento no documento 29. Confirmar CI/PR e assinatura na issue #6. Depois concluir critérios técnicos automatizados da Etapa 1, mantendo aceite físico pendente, e iniciar Etapa 2 (#8): fixtures de diferenças de interpretação Java/JS. Não encerrar execução apenas por terminar uma versão; continuar enquanto houver trabalho independente e o ambiente permitir.

## Continuação 0.9.17

Fixtures e correções de decimais/vozes em desenvolvimento (documento 30). Confirmar PR #14 de lifecycle e CI da entrega musical antes de avançar. Depois: ligaduras incompletas e fixtures de percurso. Não confundir cobertura musical automatizada com certificação editorial ou Yamaha físico.

## Continuação 0.9.18

Tratamento explícito de ligadura incompleta: novo ataque com aviso, fonte preservada. Fixtures também consultam Score.at (contrato do Espera). Validar dependências/CI e depois auditar percurso e limitações MusicXML conforme documento 30. Persistir em execução entre entregas; não aguardar hardware quando houver tarefas independentes.

## Continuação 0.9.19

Etapa 1 encerrada apenas no escopo técnico (#6); aceite físico em #11. Etapa 2 amplia sound/offset com sete fixtures. Acompanhar PRs #15/#16 e a entrega sound em sequência; validar CI/assinatura do commit efetivo. Depois auditar expressão explícita e limites para avançar à Etapa 3. Não parar por concluir uma versão.

## Continuação 0.9.20

Dinâmica numérica explícita implementada; ver documento 30. Confirmar PRs/CI e assinatura de cada push. Em seguida reavaliar fechamento técnico da Etapa 2 e continuar Etapa 3 com guia visual de mãos/dinâmica/duração, preservando regras de aprendizagem. Validações físicas permanecem na issue #11.

## Continuação 0.9.21

Guia expressivo preparado com amarelo/verde preservados; documento 31. Confirmar PR #18 de dinâmica e CI da entrega de guia. Próxima tarefa da Etapa 3: modos/troca de mão/desconexão/notas presas, sem alterar regras pedagógicas. Depois avançar biblioteca/perfis/progresso. Não encerrar a execução somente por concluir essas entregas.

## Continuação 0.9.22

Etapas 1/2 encerradas no escopo técnico (#6/#8), sem aceite físico. Guia integrado no PR #19. Correção de sessão MIDI/foreground preparada; confirmar MIDI_SESSION_SELFTEST_PASS e CI do próprio commit antes de integrar. Próxima implementação: CC120/121/123 e notas presas (documento 32), depois biblioteca/perfis/progresso. Não interromper automaticamente após a versão.

## Continuação 0.9.23

Controles MIDI de canal implementados e testados em JVM. Confirmar PR de sessão MIDI e PR de CC120/121/123 com MIDI_SESSION_SELFTEST_PASS; conferir assinatura permanente e CI dos commits. Próxima implementação independente: biblioteca/perfis/progresso da Etapa 4, com compatibilidade dos IDs existentes e backup.

## Continuação 0.9.24

Perfis estáveis preparados; confirmar PROFILES_SELFTEST_PASS, UI e backup nos Actions do commit. Etapas 1–3 encerradas apenas tecnicamente; aceite físico em #11. Próxima implementação: busca/favoritos/coleções e histórico/progresso por perfil, preservando IDs e backup. Não encerrar porque uma versão ficou pronta.
