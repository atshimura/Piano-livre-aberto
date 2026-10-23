# 60 — Entrega B: Estudo utilizável de ponta a ponta

## Escopo e dependências

Este registro acompanha os PRs incrementais da **Entrega B** do [Plano Geral](58-PLANO-GERAL-V1.md). A Entrega A está na `main` (0.9.47/code56). A B1 de câmera/legibilidade ainda depende do PR #51, `feat/analise-estudo-adaptavel`, e **não** integra a `main` automaticamente. Preservar as propostas #46, #47, #48 e não publicar Release/APK nem efetuar merge sem autorização.

## B.1 — Etiqueta de notas OSMD (PR independente da B1)

**Defeito reproduzido por inspeção do código da main:** `notation.html` posicionava a etiqueta junto ao toque, sem prazo de desaparecimento, independentemente de a nota estar na pauta inferior; também apresentava apenas a grafia portuguesa, sem nome internacional paralelo. A etiqueta do renderizador temporal já usava temporizador de aproximadamente dois segundos.

**Correção proposta:** o módulo `study-note-tip.js` gera a notação internacional **a partir da grafia escrita reconciliada**, sem inferir enarmonia pelo MIDI, e calcula uma posição dentro do viewport penalizando ocultar cabeças de nota, claves, contextos fixos, inscrições e hastes. O OSMD mostra `Dó4 · C4` ou grafia equivalente, evita expor identificadores longos na frente da escrita e fecha automaticamente após dois segundos. Toques consecutivos renovam o temporizador; carregamento de outra partitura, toque vazio e cancelamento de seleção removem a etiqueta anterior. Em caso de fonte não reconciliada, não inventa grafia e mostra `Grafia a conferir`. A seleção de trecho continua tendo suas próprias instruções.

**Validação:** `tests/StudyNoteTipTest.js`, inserido na suíte JS de `.github/workflows/build.yml`, cobre grafia internacional, enarmonia escrita, limites em viewports compactos, proteção de nota e contexto, vínculo real de integração e limpeza do estado. O workflow Android também executará suas regressões existentes sobre toque e mapeamento; declarar resultados somente depois que o SHA do PR receber os resultados dos Actions. O algoritmo minimiza cobertura da notação onde há espaço disponível; uma tela inteiramente preenchida de tinta musical não oferece área vazia garantida para qualquer sobreposição. Avaliação editorial e hardware real continuam pendentes.

## Matriz operacional a completar na Entrega B

| Operação | Evidência necessária | Estado neste PR |
| --- | --- | --- |
| Nota por toque OSMD e temporal | Nome correto, tempo finito, pautas livres | Correção B.1 proposta; CI pendente |
| Curta/longa, mãos e guia | Cenário de sessão Android/Chromium | Pendente |
| Espera, Contínuo, Demonstração | Preparação, MIDI determinístico, pausa, fim | Pendente |
| Retorno, loop, troca, fallback | Início/fim/retomada, sem notas presas | Pendente |
| B1 com câmera e contexto | CI da branch #51 após integração autorizada | Pendente |
| Teste físico | Realme 7 5G, Lenovo Tab M9, Yamaha P-45 | Exclusivo do usuário |

Não confundir uma correção isolada com a conclusão da Entrega B; outros PRs e a prova de sessão ponta a ponta permanecem necessários.
