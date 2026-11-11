# ZAI-011 — Laboratório e Criação sem rolagem vertical desnecessária

**Data:** 11/11/2026. **Origem:** relato do mantenedor após instalar a ZAI-010/0.9.71: as novas telas exigem uma pequena rolagem vertical em paisagem.

## Diagnóstico

- O seletor do **Laboratório Musical** somava cabeçalho de 56dp, títulos fixos (43dp e 30dp), espaços e três cartões de **260dp**; em uma janela Android menor que essa soma, o `ScrollView` rolava a tela inteira mesmo sem conteúdo dinâmico.
- Em **Criação**, o painel de duas colunas era filho de um `ScrollView` **global** e tinha altura determinada pelos conteúdos, inclusive o mínimo de 255dp da coluna direita e botões de 152dp/64dp da esquerda. Isso fazia o cabeçalho e controles da página deslizarem juntos.

## Decisão técnica

1. **Laboratório:** dimensionar títulos, espaços, fontes e cartões pela **altura real do componente** após as barras do sistema Android (`onSizeChanged`), não pela altura física do aparelho. Na janela baixa, usar cartões compactos, ainda com três colunas, ação Descobrir/Criação e indicação Aprender — Em breve. Em telas altas, manter cartões mais confortáveis. Preservar `ScrollView` **somente como fallback** se a janela for baixa demais ou a fonte de acessibilidade ampliar os textos; nunca cortar botões para forçar ausência de rolagem.
2. **Criação:** manter barra superior e cabeçalho do painel **fixos**, reservar toda altura restante para as duas colunas com `LayoutParams(-1,0,1)`. Coluna esquerda e coluna direita recebem rolagens **independentes**, apenas quando os controles, listas, revisão ou detalhes realmente excedem suas respectivas áreas. Ajustar as alturas dos cartões principais da esquerda pela altura medida do painel, respeitando áreas mínimas de toque.
3. Preservar **Minhas criações** e seu `listScrollY` na coluna direita, recentes, filtros, diálogo de saída de gravação pendente e recuperação `.plc`/`.pls`. Não alterar gravador, motor de partitura, dados, assinatura ou serviços.

## Testes e critérios

- Teste JVM puro `ZaiAdaptiveHeightsTest`: alturas úteis de **300, 310, 320, 340, 360, 420, 600 e 800dp** sem rolagem do seletor na escala normal; alturas de **270–290dp** e fontes ampliadas liberam rolagem segura, sem clipping. Botões da Criação passam por limites de 150–500dp de altura de coluna.
- Contrato Python `test_zai011_vertical_layout_contract.py`: impede reintrodução de `scroll.addView(body)`, exige duas rolagens contextualizadas, preserva restauração de posição, gravação e exclusão protegida.
- Workflow dedicado `zai-011-verify.yml`: auditorias do repositório, contratos anteriores, JVM e compilação Android debug isolado.
- **Validação real obrigatória** posterior no Realme 7 5G e Lenovo Tab M9, na orientação paisagem e com navegação do sistema ativada. Em caso de escala de fontes acima do espaço disponível, a rolagem **da coluna** e o fallback do Laboratório são intencionais. Emulador/testes automatizados não provam experiência física.

**Distribuição desejada:** após CI, integrar exclusivamente à `zai/lab`, gerar APK principal incremental **0.9.72-open-beta/code81**, assinado com Secrets existentes, publicar os mesmos bytes sob `zai/downloads` com SHA-256 verificável; não alterar `main`, Release oficial, histórico privado, licenças ou direitos das partituras.
