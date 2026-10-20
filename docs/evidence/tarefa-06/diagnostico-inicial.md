# Diagnóstico inicial — Tarefa 06/20

Base auditada: 0d529794b116b663032a4741a6dd705c2a6629e9; PR #46 aberto em draft, confirmado antes de alterações. OSMD 1.9.8; versão pública code55 preservada.

- A/B: a instrumentação existente em notation.html já adverte que seletores CSS não identificam todos os glifos. Contagem zero de acidentes/quiálteras não é defeito comprovado. Nenhuma correção gráfica foi autorizada por essa contagem.
- Semântica já implementada: Score.java não insere grace em events nem em writtenEvents; mantém o XML para gravura. OpticalTimeMap exclui IsGraceNote da reconciliação principal. Isso é conservador e não representa execução de ornamentos.
- D: a fixture complexa anterior contém três colcheias 3:2 com divisions=4 e duration=2 cada. Colcheia tercinada exigiria 4/3 divisões, e não 2. A soma escrita dessas três notas é 1 beat; a duração declarada soma 1,5 beat. Essa amostra permanece como regressão histórica de 27 cabeças, mas NÃO serve de oráculo temporal de tercinas. Criar fixture independente com divisions=60, sem mascarar o arquivo antigo.
- C: nenhuma ausência ou colisão gráfica nova foi comprovada antes da execução WebView. Inspeção de código não equivale a inspeção visual.
- Cobertura faltante: enarmonia completa, duplos acidentes, precaução, vigas secundárias, 5:4, grace com/sem barra e combinação válida.
- Ambiente local: JDK17 e Node disponíveis; SDK/Gradle/ADB e /dev/kvm ausentes. Compilação/WebView dependem de Actions; resultados históricos não serão atribuídos à Tarefa 06.

Este diagnóstico foi registrado antes de modificar código do aplicativo ou criar as novas fixtures. Etiqueta transitória permanece reservada à Tarefa 08; testes físicos pendentes.
