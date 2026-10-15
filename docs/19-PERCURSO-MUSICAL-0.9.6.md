> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 19 — Percurso musical avançado: Piano Livre 0.9.6-beta

Data: 14/10/2026. Versão: **0.9.6-beta (versionCode 15)**. Etapa incremental de implementação da ordem musical; **a validação física Yamaha P-45 permanece pendente**.

## Objetivo e arquitetura

O modelo musical anterior só avançava linearmente ou repetia barras simples. Agora, **Score.MeasureInfo** preserva o posicionamento original dos compassos, as barras de repetição, casas alternativas e as instruções D.C., D.S., Segno, To Coda, Coda e Fine.

**PerformanceRoute.build(score)** produz um percurso finito de visitas aos compassos originais. As notas e os beats da partitura **não são duplicados**. **PerformanceRoute.Cursor** acompanha as visitas e seus saltos, de modo que Espera, Contínuo e Demonstração usem os mesmos compassos escritos. Os limites de segurança são **512 compassos e 4.096 visitas**; uma estrutura inválida ou ambígua não é expandida sem controle.

## Recursos implementados e casos aceitos

1. **Casas de primeira e segunda vez**, com retorno após a primeira passagem e seleção correta da segunda.
2. **Repetições simples e aninhadas**, com contagem explícita de passagens, desde que não combinem casas aninhadas ambíguas.
3. **D.C. e D.C. al Fine**: retorno ao início, terminando no destino Fine apenas na segunda passagem.
4. **D.S. e D.S. al Coda**: retorno ao Segno, depois salto de To Coda para Coda quando ambos estão presentes.
5. No modo Espera, o percurso procura o **próximo ataque exigido da mão selecionada**, incluindo saltos para trás e casas puladas. No Contínuo e na Demonstração, usa o relógio musical e aplica os saltos.
6. A **repetição manual A–B** prevalece sobre a ordem escrita; não perde a preferência pedagógica do aluno.
7. Ao pausar, não reinicia o percurso. A posição de visita é salva por perfil em **user_ID_route_visit** e volta na reabertura/backup junto do beat e demais preferências.

## Correção MIDI crítica

Foi encontrado por teste integrado um defeito preexistente no método **MidiPlayback.seek**: ao avançar diretamente para outra região, o aplicativo enviava Note On/Off de **todas as notas anteriores** para reconstruir seu estado. Assim, após pular a primeira casa, a Demonstração podia tocar notas de compassos que deveriam ser omitidos.

A rotina agora **reconstrói internamente e em silêncio** os eventos anteriores, desliga as teclas antigas e transmite **apenas as notas ativas no ponto de chegada**. Isso conserva sobreposição de alturas e evita notas fantasma em saltos D.C./D.S./Coda e na navegação manual.

## Partitura temporal

O renderer **temporal-notation.js** foi atualizado para desenhar **colchetes identificados das casas 1ª e 2ª**, além de legendas legíveis **Segno e Coda**. A posição gráfica continua sendo o beat original: a partitura e o teclado guia não devem duplicar notas em função das visitas. O **teclado virtual sempre representa as duas pautas**, independente da mão praticada.

## Combinações ainda recusadas

- Misturas de D.C./D.S. com barras de repetição ou finais alternativos que exijam convenções adicionais de execução.
- Casas aninhadas, numeração acima da segunda, repetições parcialmente sobrepostas ou destinos Segno/Fine/Coda ausentes/múltiplos.
- Saltos no interior do compasso, múltiplas estruturas de retorno e marcadores MusicXML de formatos não reconhecidos.
- Notação complexa adicional como quiálteras, ornamentos, dinâmica detalhada, transposição ou vozes cruzadas, ainda não certificada.

Se o percurso não puder ser interpretado com segurança, o aplicativo **avisa e toca a partitura linearmente**, sem alegar fidelidade musical.

## Testes e evidências

- **PerformanceRouteTest.java:** casas 1ª/2ª, repetições aninhadas, D.C./Fine, D.S./Coda, limite de percurso, recusa de destinos inválidos, salto no Espera e recuperação do número de passagem.
- **PerformanceRoutePlaybackTest.java:** usa Score, MidiPlayback e KeyboardGuide em conjunto. Confere a ordem real de Note On/Off em Ambas, Direita e Esquerda, e a concordância do Espera com o mesmo percurso.
- **MidiSmokeTest.java:** regressão adicional impede Note On fantasma ao saltar para uma posição posterior.
- **musicxml_visual_crosscheck.js:** testa etiquetas de casas, Segno e Coda e preserva a comparação entre os dois leitores MusicXML nas dez partituras existentes.
- **Emulador Android:** instalação, atualização, backup, menu MIDI, visualização temporal, teclado e interface de estudo; esse teste **não substitui** o Yamaha real.

A entrega requer workflows verdes de compilação/assinatura, testes Java/JS e emulador no **mesmo commit**.

## Próximos passos

Completar a interpretação dos casos combinados recusados; avaliar a fidelidade dos símbolos MusicXML com partituras mais difíceis; aproximar os modelos de eventos Java e JS; e realizar ensaios com Yamaha P-45 no Realme 7 5G e Lenovo Tab M9.

**Decisões pedagógicas inalteradas:** no Espera, notas extras são indicadas sem impedir progresso, a janela de acordes é calibrável e sem milissegundos arbitrários, e duração/pedal são orientativos; o guia virtual mostra sempre duas mãos.
