# Política V1 — integração rápida de recursos visuais isolados

## Estado
Proposta técnica implementada nesta branch para avaliação em PR. Nenhuma alteração nesta branch autoriza merge automático, desproteção da main ou publicação. A validação em Actions no SHA final e a revisão da proteção da main são obrigatórias antes de integrar.

## Definição conservadora e verificável
A categoria `visual` somente é atribuída se TODO o diff entre a base do PR e o checkout efetivo contiver exclusivamente inclusão/modificação de arquivos raster `.png`, `.webp`, `.jpg` ou `.jpeg` diretamente em `PianoLivreAndroid/app/src/main/res/drawable*`. Exclusões, XML, Java, JS, recursos mipmap, configurações, documentação, testes e workflows são `full`. Diff vazio também é `full`. Qualquer erro interrompe a classificação. O título ou rótulo do PR não confere privilégios. Mesmo imagens raster exigem revisão humana de proveniência/licenças e possíveis impactos de legibilidade.

O classificador em `PianoLivreAndroid/scripts/classify_pr_risk.py` produz `risk-report.json`. Testes de classificação: `python3 -m unittest discover -s PianoLivreAndroid/tests -p test_classify_pr_risk.py`.

## Integração
- O workflow `emulator.yml` mantém o nome de check obrigatório `emulator`.
- Para `visual`, exige `classify`, `debug-apk` e `ui-fast` com sucesso; `emulator-suite` precisa estar skipped.
- Para `full`, exige `classify`, `debug-apk` e `emulator-suite` com sucesso. Um `ui-fast` adicional é diagnóstico e não substitui a suíte.
- O workflow `build.yml` permanece integral para PRs, incluindo auditorias JVM, JS, MIDI e MusicXML. Portanto a economia inicial está concentrada na suíte longa de emulador.
- A proteção existente da main e seus checks atuais não são removidos por este PR. Verificar a configuração real de branch protection antes de quaisquer mudanças administrativas.

## Publicação separada
O job `public-beta` de `build.yml` somente entra em execução mediante `workflow_dispatch` na main. O próprio build do dispatch é integral, e o publicador existente continua bloqueando publicação por ausência/falha/cancelamento/timeout dos checks exigidos do SHA exato, inclusive `emulator`. Antes do disparo de publicação, executar manualmente `emulator.yml` na main e comprovar sucesso de `emulator-suite` para o SHA que será publicado.

**Atenção:** o check `emulator` após um merge na main executa suíte completa, mas a execução do workflow de emulador é filtrada por caminhos de arquivos. Para publicação quando não houver run completo do SHA, acionar `emulator.yml` manualmente. Não publicar só porque `ui-fast` foi aprovado no PR.

## Testes de aceite antes do merge
1. Executar casos unitários do classificador (raster isolado, arquivo misto, Java, XML, CI, exclusão, vazio).
2. Conferir check `emulator` e o job `classify` no PR desta branch (deve ser `full`, pois altera workflows).
3. Abrir PR experimental separado contendo apenas recurso raster, confirmar `ui-fast` e gate `emulator` verdes e `emulator-suite` skipped, sem Release.
4. Simular alteração mista, falha do `ui-fast` e erro de classificação; a integração deve ser bloqueada.
5. Conferir que merge na main não dispara Release automática; confirmar publicação manual apenas depois de suíte completa no SHA exato.
6. Consultar e preservar required checks na proteção da branch; somente ajustar se estritamente necessário.

## Reversão
Reverter commits desta infraestrutura restaura o roteamento completo do emulador. Antes de restaurar publicação automática, verificar que todas as mudanças presentes no SHA da main possuem a suíte completa exigida; caso contrário, manter `workflow_dispatch`.

## Limites
Nenhum teste automático substitui verificação em Yamaha P-45, Realme 7 5G e Lenovo Tab M9. Este mecanismo inicial NÃO contempla ajustes de cores XML, fontes, espaçamentos ou Java, que continuam como `full` até desenvolvimento de uma análise semântica segura.
