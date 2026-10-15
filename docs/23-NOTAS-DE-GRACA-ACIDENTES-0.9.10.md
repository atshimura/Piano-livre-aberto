> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 23 — Notas de graça e acidentes simultâneos (Piano Livre 0.9.10-beta)

**Data:** 15/10/2026. **Versão:** 0.9.10-beta, versionCode 19. Etapa incremental do renderer temporal; não equivale a suporte completo de ornamentos nem validação física com Yamaha P-45.

## 1. Notas de graça

O leitor MusicXML temporal agora conserva o atributo slash do elemento grace, além da ordem XML da nota. Em uma sequência de graça que compartilha o beat da nota principal, arrangeVoices desloca os ornamentos para a esquerda em passos de 21 unidades SVG, mantendo at/beat inalterado. drawNote gera o grupo temporal-grace-note em escala 67% e acrescenta traço para acciaccatura quando slash=yes. A nota principal permanece musical-event para seleção e realce.

**Limitação expressiva:** a nota de graça é apenas impressa. O parser Java já não gera evento musical dela, para não inventar antecipação, dinâmica, roubo de tempo ou duração MIDI. Para execução futura será necessária política musical própria e validação com o P-45. O beaming especial de notas de graça e agrupamentos extensos ainda não estão implementados; a função collectBeamGroups evita misturar grace com notas temporais normais.

## 2. Acidentes simultâneos de vozes

Antes sustenidos, bemóis e bequadros próximos apareciam sobrepostos. arrangeVoices agora examina notas da mesma pauta/beat e distribui sinais com distância vertical inferior a 26 unidades em colunas horizontais separadas por 19 unidades. A posição também considera o deslocamento da cabeça de nota já calculado para polifonia.

Quando vozes distintas repetem exatamente o mesmo sinal na mesma altura e alteração cromática, apenas o símbolo impresso duplicado é omitido; as duas notas permanecem. Quando duas vozes exigem alturas diferentes (por exemplo F natural e F sustenido na mesma linha), ambos os sinais são preservados.

**Limites:** colisões em beats muito próximos, sinais de cortesia/editoriais, microtonalidade, armaduras especiais e polifonia acima de quatro vozes não receberam certificação editorial.

## 3. Regressão e compatibilidade

**NotationOrnamentsTest.js** comprova grupo de graça menor, slash, ordenação antes da nota principal, beat intacto, acidentes conflitantes ou duplicados e isolamento das vigas temporais. **musicxml_visual_crosscheck.js** contém exemplo real MusicXML com duas notas de graça, uma acciaccatura, e vozes simultâneas com E natural e E sustenido. Regressões existentes de claves, quiálteras, ligaduras, MIDI, Espera, teclado ambas as mãos, backup e as dez peças da biblioteca continuam obrigatórias.

Para distribuir APK da 0.9.10, verificar CI de compilação/assinatura/testes JVM+JS e emulador no commit exato. O teste de emulador confirma interface, não a execução de MIDI no Yamaha P-45 físico.

## 4. Próximos problemas

Fidelidade do beaming próprio de grace, mais de quatro vozes densas, colisões entre acidentes de ataques próximos, vigas atravessando pautas, espaçamento editorial horizontal independente do tempo e ornaments com realização MIDI calibrada são trabalhos futuros.

Preservar o acordo pedagógico: notas extras não bloqueiam Espera, simultaneidade de acordes depende de calibração real, duração/pedal orientativos, teclado virtual sempre com duas mãos.