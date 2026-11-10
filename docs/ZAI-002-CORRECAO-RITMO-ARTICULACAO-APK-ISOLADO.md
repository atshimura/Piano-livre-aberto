# ZAI-002 — Correção rítmica e articulação a partir de MIDI real

**Data:** 09/11/2026. **Linha exclusiva:** `zai/feat/corrigir-ritmo-articulacao-apk-isolado` → `zai/lab`. **`main` intocada.**

## 1. Problema observado em aparelho físico

A primeira implementação ZAI-001 foi considerada **reprovada em uso real**. Uma execução de duas escalas Dó–Ré–Mi–Fá–Sol–Lá–Si produziu partitura repleta de pausas e notas fragmentadas. A tela exibiu **“Andamento não identificado com segurança — prévia a 120 BPM (provisória)”**. O arquivo `Nova_composição.mid` fornecido pelo mantenedor contém 14 notas efetivas, duas vezes as alturas MIDI **[60,62,64,65,67,69,71]**, sem notas extras.

- Primeiro grupo: intervalos entre ataques próximos de **1,06 s** (mais lento).
- Segundo grupo: intervalos entre ataques próximos de **0,53 s** (mais rápido).
- Proporção perto de 2:1; interpretar o grupo lento como mínimas e o rápido como semínimas, em pulso-base de cerca de 113 BPM, é uma hipótese musical natural.
- A duração física de pressionamento é **menor** que a distância entre ataques; o pianista tocou notas destacadas (staccato). **Não converter automaticamente cada liberação antecipada em pausa musical.**
- A exportação MIDI usa metrônomo de 120 BPM por convenção técnica, **não** como valor inferido da execução.
- O ZAI-001 também fragmentava uma nota longa em duas figuras sem ligadura dentro de um mesmo compasso; isso inventa ataques na notação.
- Os testes anteriores com MIDI sintético regular não cobriram essas condições.

## 2. Contratos musicais

1. **Alturas/ataques invioláveis:** nenhum número MIDI pode ser alterado; não inventar, omitir nem repetir ataques. MusicXML válido e renderizar no OSMD não provam fidelidade musical.
2. **Modelo com três conceitos separados:** (a) instante do Note On; (b) valor musical escrito estimado da sequência de ataques; (c) intervalo físico Note On→Note Off e articulação. Conservar todos no modelo/controle de regressão.
3. **Pulso robusto:** estimar a unidade a partir de **intervalos de ataque** com agrupamento tolerante; permitir proporção 2:1 e mudanças de valores, sem confundir esse padrão com duas velocidades ou cair por padrão nos 120 BPM técnicos. Informar incertezas quando não for possível identificar base.
4. **Staccato:** tecla solta bem antes do ataque seguinte, mas ataques regulares → valor de nota correspondente à distância entre ataques e marcação `<staccato/>`; **não** inserir pausas espúrias a cada liberação.
5. **Legato:** liberação perto do ataque seguinte, inclusive pequena sobreposição → manter valor escrito e acrescentar ligadura de expressão (`<slur>`) quando uma série inequivocamente conectada for detectada. Não confundir com ligadura de prolongamento.
6. **Sobreposição sustentada:** quando a nota permanece pressionada significativamente depois do ataque seguinte, preservar sua duração e alocar vozes polifônicas sem truncar a nota.
7. **Pausas reais:** intervalo entre ataques significativamente maior que o valor local esperado (p.ex. limite entre frases) → escrever a nota de duração local esperada e inserir a pausa restante. Não tratar toda soltura antecipada como pausa.
8. **Última nota de frase:** sem próximo ataque, ponderar duração pressionada e padrão local sem supor que a duração equivale obrigatoriamente ao tempo até o stop da gravação.
9. **Ligaduras de prolongamento:** todo desdobramento de **uma mesma nota MIDI** em duas ou mais figuras, no mesmo compasso ou cruzando barra, deve ligar todos os fragmentos, inclusive notas longas; um único ataque MIDI continua um único ataque na leitura musical.
10. Duas pautas permanentes G/F, divisão fixa C4 MIDI 60, MusicXML/OSMD, backups, biblioteca e gravações `.plc` preservados.

## 3. Identidade Android obrigatoriamente isolada

O APK anterior foi gerado com `br.org.pianolivre.opensource` e **substituiu o app principal** no celular. Isto não pode se repetir.

- **Principal:** `br.org.pianolivre.opensource` (imutável nesta linha).
- **Teste ZAi:** `br.org.pianolivre.opensource.zai` (**diferente e testado via `aapt` antes da distribuição**).
- Rótulo visível de launcher: “Piano Livre ZAi”. Nenhum artefato ZAi pode ser enviado com ID principal. Falhar o workflow se houver qualquer divergência.
- Assinatura permanente por GitHub Actions Secrets permitida; a assinatura idêntica **não** separa apps, apenas o `applicationId` separa.
- Manter `versionCode`/`versionName` da linha principal inalterados (versão da ZAi não implica release), sem interferir em `main`. **Não afirmar que atualiza o ZAi anteriormente instalado** sem comprovar o antigo package ID. A nova instalação isolada não poderá substituir a principal.

## 4. Plano executável

**Z2-P0 — Diagnóstico e regressão:** inserir fixture derivado somente das 14 notas MIDI reais (tempos/alturas, sem dados privados), inspecionar Captura→Pairer→Estimador→Quantização→MusicXML→Estudo, estabelecer expectativas de altura, ritmo e marcações; não apagar gravações.

**Z2-P1 — Pulsação:** substituir estimativa frágil por agrupamento robusto de intervalos entre Note On; procurar unidade mais curta frequente com relação 1:2 entre grupos, tolerar jitter. Sem série adequada → fallback claramente provisório, sem certeza infundada.

**Z2-P2 — Articulação e figuras:** interpretar ataques usando os intervalos locais, separar repouso real de staccato; detectar legato e sobreposição sustentada; corrigir todas as ligaduras entre fragmentos; MusicXML exprime staccato, slur, tie como conceitos distintos.

**Z2-P3 — Testes:** regressão física de 14 notas (alturas e ordem), MIDI com todas semínimas/mínimas de 90/120 BPM, staccato, legato, sobreposição expressiva, pausa genuína, nota de 5/16 com ligação no mesmo compasso, 2 pautas, conservação de notas ao passar por `Score.read` e OSMD. Executar JVM + contratos + compilação Android + verificação assinatura e ID.

**Z2-P4 — APK:** parametrizar exclusivamente o build da ZAi para pacote isolado e rótulo próprio; workflow dedicado `zai-apk-teste.yml` publica somente após verificações. Não publicar Release, não modificar assinatura, histórico ou `main`. Validar com hardware posteriormente; teste emulador não substitui hardware.

## 5. Critérios de aceite

- **R-REAL-14:** duas sequências MIDI [60,62,64,65,67,69,71], sem notas extras; semínimas no trecho rápido, mínimas no lento, alerta de incerteza só quando justificável; poucas pausas reais entre frases, não entre todas as notas.
- **R-STACC:** ataques regularmente espaçados e soltura em 35–60% do intervalo produzem figura inteira com staccato, não várias pausas.
- **R-LEGATO:** soltura ~próximo ataque, com micro-sobreposição, mantém sequência e escreve slur.
- **R-OVERLAP:** overlap acima de limiar musical preserva duração e voz sobreposta.
- **R-TIE:** um Note On sustentado por 5/16 atravessando decompõe-se em figuras ligadas em cada ponto.
- **R-SECURITY:** APK assinado com SHA-256 público permanente, pacote `br.org.pianolivre.opensource.zai`, label “Piano Livre ZAi”, sem INTERNET, release não depurável e nenhuma publicação na main.
- **R-COMPAT:** projetos, backups, biblioteca e Estudar não podem ser destruídos. Testes reais em P-45, Realme 7 5G e Tab M9 permanecem pendentes.

**Limite assumido:** sem metrônomo uma execução arbitrária pode admitir leituras de pulso metade/dobro; neste marco o algoritmo privilegia a leitura mais parcimoniosa com intervalos frequentes e explica ambiguidades. Não há promessa de transcrição universal nem validação física por emulador.
