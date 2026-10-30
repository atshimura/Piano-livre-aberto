# Ajustes da Tela Inicial após a beta 0.9.52

Base: `main` publicada em `f91c27853be576a353628e220d4e716d02507bed`. Branch: `feat/ajustes-tela-inicial-pos-beta-0952`, com PR próprio para revisão. Esta entrega não autoriza merge ou nova Release.

## Referência e objetivo

[Imagem anotada](references/tela-inicial-ajustes-20261030.png), original encontrado como `Tela inicial.png`, preservado sem edição: SHA-256 `9bb96073cc853335b4b1a50929a088e3939ab77cc6999063c446b86463a69953`. Contém o banner demarcado, pedido de ampliação dos três atalhos, remoção de Opções, reorganização dos controles e grade em duas colunas. [Instruções integrais](prompts/AJUSTES_TELA_INICIAL_POS_BETA_0952.md). A referência original do Prompt 1 continua preservada; esta rodada refina a implementação existente.

## Implementação

- Banner com altura multiplicada por 0,70, arredondada ao dp inteiro: paisagem baixa **220→154 dp**, tablet amplo **215→151 dp**, configuração compacta de maior altura **285→200 dp**. A variação de até 0,5 dp vem do arredondamento. Título, descrição e os dois botões existentes preservados; título mais horizontal e ações lado a lado, com 44 dp de altura. Arte passa a 64% da largura interna do banner e se aproxima do texto, com transição para legibilidade.
- Três atalhos: Biblioteca Musical, Laboratório Musical e Histórico. Cartão Opções removido; engrenagem existente continua abrindo o mesmo diálogo. Atalhos com 96/108 dp de altura, títulos 16/18 sp, subtítulos 12/14 sp, ícones 28/34 sp e mais espaço interno. Três por linha a partir de 720 dp; abaixo disso, uma coluna, com rolagem.
- Biblioteca com título e ações separados da busca/filtro. Controles com 48 dp e tipografia de 14/16 sp. Em telas amplas, título/Coleções/Importar ficam na primeira linha, busca/filtro na segunda. Em larguras menores, título, ações, busca e filtro têm faixas próprias, evitando os controles de 16 dp da beta anterior.
- Grade limitada a **duas colunas a partir de 600 dp**, e uma abaixo disso. A largura usada no limiar considera a escala de fonte, evitando insistir em duas colunas com texto ampliado. `LibraryGridLayout`, antes sem uso na Home, passa a fornecer a decisão real. Nenhuma paginação ou restrição ao catálogo/importados.
- Cartões musicais maiores, título 17/19 sp com até duas linhas, descrição 13/14 sp, miniaturas ampliadas, favoritos com alvo de 44 dp e botões Estudar/Partitura livre de 14 sp e 44 dp. Stores, callbacks e conteúdo das partituras preservados.

Não há alteração na estrutura do Estudar, USB-MIDI, modos, reconhecimento, Laboratório, perfis, histórico ou formato dos dados. Ajustes em `PianoScreens` se restringem à construção da Home e a medições debug; os métodos compartilhados de controles e da tela Estudar não foram redesenhados.

## Arquivos e versão

`PianoScreens.java`, `LibraryGridLayout.java`, teste JVM da grade, driver/medições de Home, navegação dos testes UI pela engrenagem, workflow de evidências e documentação. As medições `HOME_ITEM`/`HOME_LAYOUT` são exclusivas de APK depurável e não escrevem preferências; o ensaio de release exige sua ausência.

Como o APK mudou, `gradle.properties` identifica o candidato como **0.9.53-open-beta (62)**, conforme AGENTS. A beta publicada 0.9.52 (61), sua tag e seus assets permanecem intactos. O APK gerado no PR é temporário e usa certificado descartável; não é nova publicação oficial. Os testes de upgrade apenas atualizam a versão esperada do candidato, preservando a base pública e os critérios de integridade.

## Validação e evidências

Verificações locais iniciais aprovadas: `verify_public_snapshot.py`, compilação Python dos drivers e `LibraryGridLayoutTest` (11 casos, incluindo larguras limítrofes e rejeição de zero). Não equivalem a build Android local.

O job curto existente roda `emulator_home_refinements.py` antes do MIDI, gerando evidências antecipadas sem retirar a suíte completa. A mesma verificação é usada pela jornada UI completa. Configurações: **720×540**, **1280×720** e **1920×1080**, densidade 240, e **2160×1080 em paisagem**, densidade 440 (display físico 1080×2160 rotacionado), que cobre a largura lógica intermediária do emulador padrão com navegação lateral. O tamanho da captura vem do PNG e os gestos usam os limites visíveis, sem confundir orientação física e coordenadas da Activity. Exige redução medida de 30% (tolerância de arredondamento de 1 dp), aumento da arte, exatamente três atalhos e dez partituras, uma/duas/duas/duas colunas, tipografia maior e ausência de texto/ações cortadas. Exercita engrenagem e preservação dos controles da biblioteca.

Capturas reais por configuração: `piano-home-refined-{phone,landscape,tablet,compact-tablet}.png`, `...-library.png` e `...-grid.png`. O JSON `piano-home-refined-report.json` registra dimensões nativas, fontes, contagem e colunas. O manifesto visual completo exige as doze imagens. Home estreita/ampla também conserva os nomes de captura anteriores. A inspeção na densidade 440 revelou a borda do botão Como usar cortada pelo contêiner de texto; a faixa agora reserva no mínimo 428 dp nas larguras não compactas, e o teste compara os limites dos controles com esse contêiner, além dos limites do banner.

A suíte existente mantém os testes de banner/Ode/ajuda, busca/filtros, favoritos, coleções com reinício, importação, navegação Biblioteca/Laboratório/Histórico, engrenagem/perfis, Estudar e Partitura Livre. Mantidas as regressões musicais/MIDI, dados/backup/upgrade, notação, responsividade e release sem depuração. A rolagem de retorno ao topo foi ampliada para acomodar a lista maior em uma coluna.

Na primeira suíte completa, a Gymnopédie estava desenhada e o processo atual confirmou 469 notas/mapeamentos, mas o teste falhou por comparar a quantidade global de eventos com uma contagem anterior do buffer circular do logcat. As medições da Home aumentavam esse histórico durante toda a jornada. A instrumentação agora exige o intent debug explícito `home_layout_selftest`, usado somente nas capturas; o ensaio de release exige que seja ignorado. O teste da partitura longa confere o evento do PID recém-iniciado e exige as 469 notas/mapeamentos atuais, sem aumentar timeout nem retirar critérios musicais. O retorno ao topo termina após alcançar o cabeçalho e realizar o último gesto, evitando vinte varreduras quando já está no início. Renderizador e motor musical permanecem intactos.

A revisão seguinte passou a renderização longa e chegou às jornadas finais, que procuravam a busca após rebobinar a Home inteira. Como os controles maiores ficam abaixo da primeira dobra em telas baixas, o roteiro agora preserva o posicionamento feito pelo atalho Biblioteca Musical e o usa novamente se necessário. O seletor é localizado por sua descrição de acessibilidade, com regressão para controles empilhados; as verificações de pesquisa e das sessões curta/longa continuam exigidas. Não houve mudança no funcionamento da busca, nos dados ou no Estudar para acomodar o teste.

A jornada reconhece também o placeholder da busca com seu ícone e readquire o campo real após fechar o teclado, em vez de reaproveitar coordenadas anteriores à mudança de altura dos controles.

O job antecipado também executa as mesmas jornadas curta/longa após Home/MIDI, permitindo revisar essa navegação antes do término da suíte completa. Seu limite é 25 minutos para acomodar essa cobertura; o job completo de 60 minutos continua executando todas as verificações existentes, inclusive as jornadas e a variante release.

**Resultado final e proveniência:** a descrição e o comentário de fechamento do PR registrarão HEAD final, checkout efetivo do Actions, runs, resultados, APK, hashes, capturas e expiração. Não declarar concluído antes de build/emulador aprovados e inspeção dos PNGs desta revisão. Esse registro evita um commit adicional apenas para citar o próprio SHA. Resultados anteriores da 0.9.52 não validam estes ajustes.

## Limitações e próximos passos

O conteúdo da Home continua rolável: em celular compacto os três atalhos e a biblioteca não cabem todos na primeira dobra. Uma coluna é deliberada abaixo do limiar de leitura; tablet usa duas colunas maiores. A ilustração continua sendo recorte local da arte aprovada, com controles nativos reais. Metadados ausentes não são inventados.

Após aprovação automatizada/visual: revisar o PR e validar legibilidade/toque no Realme 7 5G e Lenovo Tab M9. Nenhum ensaio físico foi presumido. A assinatura pública e a publicação seguem o processo existente somente em eventual integração autorizada. Yamaha P-45 e os critérios de V1 continuam pendentes.
