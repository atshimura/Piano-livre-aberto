# Alteração 3 — Mais espaço vertical para as pautas em Estudar

Solicitação: 02/11/2026, com referência visual fornecida pelo mantenedor. Branch `feat/home-ajustes-acumulados`.

## Alterações

- Cabeçalho superior passa de 54 para **44 dp**, barra de modo/mãos de 46 para **40 dp**, barra de transporte/velocidade/trecho de 54 para **44 dp**, economizando **26 dp verticais** destinados ao renderizador de partituras, sem deslocar os controles para cima da pauta.
- Botões da segunda barra passam de 44 dp para **38 dp** de altura, com largura horizontal mais enxuta (inclusive Demonstração, mãos e espaçamentos).
- Botões da barra superior e inferior passam de 44 dp para **40 dp** de altura, com larguras reduzidas, preservando callbacks, entrada USB-MIDI, retroceder, Iniciar/Pausar, Parar, velocidade e repetição de trecho.
- **Todas as fontes mantêm seus tamanhos anteriores** (título 15/20 sp, botões compactos 11 sp, botões especiais 20/22 sp, velocidade 10/12 sp). Informações de música BPM, porcentagem e posição ficam agora na **mesma linha** do título também no layout compacto. Foram usados dois `TextView` com linha única e reticências responsivas, mantendo descrição acessível completa para títulos longos.
- A distribuição vertical da partitura continua via `LinearLayout.LayoutParams(-1,0,1)` e a lógica de atualização de geometria e renderização não foi alterada. Não muda motor musical, arquivos, configurações, MIDI ou persistência.

## Verificação

Executar somente testes básicos em código-fonte (incluindo contratos de dimensões, ausência de quebra de linha e preservação dos comandos), `compileDebugJavaWithJavac`, verificação de snapshot público e asserção de **nenhum APK produzido**. Não executar suíte completa nem emulador. A comprovação visual em aparelho e emulador permanece pendente até autorização de APK.

**Limitação:** altura de botões compactos abaixo de 44–48 dp pode reduzir a área de toque recomendada em alguns dispositivos; validar conforto e acessibilidade no teste físico futuro.
