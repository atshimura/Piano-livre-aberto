# Tela inicial — Prompt 1

**Estado:** implementação técnica do Prompt 1 concluída no escopo automatizado e visual documentado abaixo, pronta para integração posterior. O [PR #71](https://github.com/atshimura/Piano-livre-aberto/pull/71) permanece aberto e separado da `main`; nenhuma Release oficial foi publicada nesta entrega. A V1 e o aceite físico continuam pendentes. A imagem e o prompt aprovados do PR #70 foram preservados na branch `feat/tela-inicial-piano-livre`.

## Escopo implementado

- Cabeçalho exclusivo da tela inicial com marca, seleção/criação de perfil, opções e botão USB-MIDI. O cabeçalho anterior do modo Estudar permanece separado.
- Indicador USB-MIDI mostra essa legenda quando não há conexão aberta; após a abertura da porta, mostra o nome retornado pelo Android para o dispositivo selecionado. O botão abre a seleção/diagnóstico MIDI já existente.
- Banner responsivo com título, texto, atalhos para a Ode à Alegria e ajuda local. A ilustração é um recorte da área artística aprovada, otimizado como WebP local; o screenshot completo não é usado como interface.
- Cartões clicáveis para biblioteca, Laboratório, Histórico e Opções. A biblioteca mantém a posição e os dados ao receber foco.
- Biblioteca local em painel próprio: pesquisa por título e nome de arquivo, filtros existentes de favoritos/coleções, criação/exclusão de coleções, botão de importação, favoritos, contagem, estados vazio/sem resultado e cartões gerados a partir do catálogo e dos arquivos importados.
- Miniaturas ilustradas locais para Ode à Alegria, Für Elise — Easy e Lago dos Cisnes, contempladas na referência; outras partituras usam miniatura neutra quando necessário. O catálogo atual fornece nomes e arquivos, mas não compositor, nível ou gênero; esses metadados não são inventados.
- Orientação de quatro cartões por linha em tablets largos, duas colunas em larguras médias e coluna única em celulares. Busca e filtros empilham em larguras compactas.

## Compatibilidade e preservação

Os arquivos, preferências, favoritos, coleções, perfis, histórico e rotas de estudo existentes continuam usando os stores e telas já presentes. A mudança da apresentação não recria nem migra dados. A seleção de perfil no cabeçalho novo sincroniza com o seletor mantido no cabeçalho de Estudar. Nenhuma dependência remota, permissão de internet ou serviço novo foi adicionado.

## Correções da auditoria do Prompt 1

Na versão inicial do PR #71, o recurso `home_hero_art.webp` ainda incluía um fragmento do botão antigo da referência aprovada. O recorte foi refeito na área artística à direita: agora só contém piano, ambiente e notas. Título, descrição e ações continuam sendo controles nativos. No modo paisagem de baixa altura, o banner passou de 138 para 178 dp, reduziu o preenchimento e o tamanho do texto e reserva largura para os dois botões; a ilustração fica confinada ao lado direito com transição suave atrás do texto.

Os três cartões que têm miniaturas individuais na referência agora usam recortes locais próprios daquela arte aprovada, vinculados pelos nomes de arquivo MusicXML já existentes. Os demais itens, inclusive importados, continuam sendo dinâmicos e mantêm a miniatura neutra de fallback. Nenhum compositor, nível, gênero ou categoria é criado.

A falha `Created collection did not appear in library filters` veio da inspeção do teste, não de perda de dados: a árvore de acessibilidade de um `Spinner` fechado expõe a opção selecionada, não a lista inteira de opções. A criação já atualizava o `LibraryStore` e reconstruía o adaptador; o teste procurava uma opção não selecionada sem abrir a lista. A regressão agora abre o filtro e consulta a opção real, aguarda as atualizações nativas e exercita criar, renomear, confirmar persistência após reiniciar o processo e excluir a coleção.

## Validação

O workflow `emulator.yml` mantém as capturas de tela de home estreita (720 × 540) e larga (1920 × 1080) e agora exige que os botões “Começar pela Ode à Alegria” e “Como usar” constem no layout estreito. O teste de coleções inspeciona o dropdown verdadeiro após salvar/renomear, reinicia o app para conferir persistência e comprova a remoção após excluir. As regressões existentes da biblioteca, navegação, modos de estudo e MIDI continuam habilitadas. O build geral continua cobrindo o teste JVM do indicador USB-MIDI.

Comparação visual realizada em 29/10/2026 entre a referência preservada e as duas capturas do Actions: o tablet mantém cabeçalho, banner sem texto/botões duplicados, quatro atalhos alinhados e biblioteca com três cartões por linha. No celular os dois botões do banner estão visíveis, com ações empilhadas e conteúdo restante acessível por rolagem. As miniaturas individuais reproduzem os elementos ilustrados da referência. A frase redundante de conexão foi removida.

### Proveniência dos testes e do APK

- Commit de implementação/testes auditado: `31258025aa470fb2128b73369ebce747d9484630`.
- Base `main`: `743608dca09869604f209032a72cc74371eb7765`.
- Checkout efetivo de **ambos** os workflows abaixo, confirmado nos logs de checkout: `b387a0381318b60dd610b13dbaf9a561e0394523`, merge temporário do GitHub de `31258025` sobre `743608dc`; isso não é integração à `main`.
- [Build, JVM, JavaScript, Chromium, auditorias MusicXML/MIDI e APK — sucesso](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40765535697). Inclui `HomeMidiIndicatorTest` (desconectado, conectado com nome e dispositivo sem nome), busca, grade responsiva, persistência e regressões musicais. Compila debug e verifica APK de distribuição sem depuração; publicação/assinatura permanente são ignoradas em PR.
- [Emulador Android API 29 — sucesso](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40765535392). Confirma banner/ajuda, quatro atalhos, biblioteca dinâmica, favoritos, pesquisa sem acentos, importação MIDI com mão explícita/fonte preservada, seletor de importação, criar/renomear/excluir coleções e persistência após reinício. Também passaram backup (36 asserções), upgrade (6), sessão MIDI sintética (13), perfis (14), biblioteca (10), histórico, navegação, leitura livre, notação e jornadas de estudo curtas/longas.
- [APK de validação efetivamente compilado](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40765535392/artifacts/12850659345): artefato `PianoLivre-prompt-01-validation-apk`, pacote `br.org.pianolivre.opensource`, versão mantida `0.9.50-open-beta` / código `59`. É debug de PR, com chave descartável, sem garantia de atualização de outra instalação.
- [Evidências Android completas](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40765535392/artifacts/12851602406), incluindo `piano-visual-evidence.json` que registra o checkout `b387a038…`. APK e ZIP visual estavam disponíveis e não expirados na conferência; expiração informada pela API: 11/11/2026 (UTC). Não são uma Release durável.

### Capturas preservadas e critérios de aceite

As duas capturas foram extraídas sem edição do artefato acima e versionadas para que a comparação continue disponível após sua expiração:

- [Tablet horizontal, 1920 × 1080](evidence/prompt-01/piano-home-tablet.png).
- [Celular horizontal compacto, 720 × 540](evidence/prompt-01/piano-home-phone.png).
- [Referência aprovada, 1672 × 941](references/tela-inicial-aprovada.png), preservada com SHA-256 `eadf8dd3b255c9ddffeee1c5eabc8508b3e33a303bff7ef3da2ed08e81e981fd`.

| Critério | Evidência / resultado |
|---|---|
| Referência preservada e composição implementada com controles reais | PNG original, capturas e `PianoScreens.java`; conferidos |
| Banner do tablet sem duplicação e ambos os botões no celular | Capturas acima e teste responsivo; aprovados |
| Quatro cartões e biblioteca dinâmica | Jornada UI: biblioteca (10 de 10 partituras no catálogo de teste), Laboratório, Histórico e Opções; aprovada |
| Busca, favoritos, coleções e importação | Testes JVM, selftests Android e jornada UI; aprovados, incluindo reinícios |
| USB-MIDI e remoção da frase redundante | JVM com estados sintéticos e UI desconectada/diagnóstico; aprovados. Nome real em hardware pendente |
| Navegação, persistência e regressão | Dois workflows acima aprovados para o código auditado |
| APK e capturas disponíveis | Artefatos conferidos; duas capturas também versionadas |

### Fechamento documental e HEAD final

No fechamento de 29/10/2026, `verify_public_snapshot.py`, `py_compile` de `tests/emulator_ui.py` e `git diff --check` passaram localmente. Não houve recompilação Android local; a evidência de build é o Actions identificado acima.

O commit de fechamento altera somente documentação e cópias das capturas em `docs/`; não altera fonte, recursos do APK, testes, workflows nem `gradle.properties` em relação a `31258025`. Seu SHA e os checks executados após o push ficam registrados na descrição e no comentário de fechamento do PR #71, sem criar um novo commit apenas para citar o próprio SHA. Conferir a identidade com `git diff 31258025 HEAD -- PianoLivreAndroid .github/workflows` (saída vazia). Assim, o APK e as capturas acima correspondem ao código final; seus runs permanecem identificados pelo SHA original e não são apresentados como execuções do commit documental. Os checks do HEAD documental são registrados separadamente no PR.

**Ainda não testado fisicamente:** Lenovo Tab M9, Realme 7 5G e conexão Yamaha P-45. O emulador não comprova tamanho de fonte, toque, qualidade da arte nem identificação MIDI em hardware real.

## Limitações visuais deliberadas

- O catálogo não expõe compositor, categoria musical nem dificuldade; as etiquetas correspondentes foram omitidas.
- Partituras sem uma das três ilustrações locais usam miniatura neutra. A biblioteca não fica limitada às três músicas da referência.
- Em tela estreita, o conteúdo preserva rolagem vertical e os quatro atalhos ficam em coluna; em larguras intermediárias ficam em grade 2 × 2.
- A identificação do nome MIDI depende de o Android abrir o dispositivo. Uma seleção pendente, falha de abertura ou desconexão mantém a legenda USB-MIDI.
- Componentes nativos, ícones e tipografia diferem da arte estática. O banner separa área de texto/ações da arte para garantir legibilidade; os cartões mostram descrição neutra em vez de compositor/dificuldade não disponíveis. Importar partitura e criar perfil continuam explicitamente acessíveis, embora não destacados na referência. Filtros usam apenas favoritos/coleções reais, sem inventar categorias musicais.
- A configuração compacta do emulador comprova responsividade automatizada, não corresponde a uma validação física do Realme. Escala de fonte, conforto de toque, fluidez e nome real do instrumento exigem os aparelhos previstos.

## Próxima etapa

Prompt 2 — reformulação da tela Estudar, em trabalho posterior. Não foi implementado neste fechamento. A integração à `main` e a publicação consolidadas permanecem para a etapa prevista no Prompt 4; não há falha impeditiva conhecida no escopo técnico do Prompt 1. Lenovo Tab M9, Realme 7 5G e Yamaha P-45 continuam pendentes de testes físicos.
