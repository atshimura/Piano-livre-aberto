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
