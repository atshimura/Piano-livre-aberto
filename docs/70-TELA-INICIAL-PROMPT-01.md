# Tela inicial — Prompt 1

**Estado:** implementação em revisão no PR aberto para `main`. Esta entrega não integra nem publica uma versão. A imagem e o prompt aprovados do PR #70 foram preservados na branch de implementação.

## Escopo implementado

- Cabeçalho exclusivo da tela inicial com marca, seleção/criação de perfil, opções e botão USB-MIDI. O cabeçalho anterior do modo Estudar permanece separado.
- Indicador USB-MIDI mostra essa legenda quando não há conexão aberta; após a abertura da porta, mostra o nome retornado pelo Android para o dispositivo selecionado. O botão abre a seleção/diagnóstico MIDI já existente.
- Banner responsivo com título, texto, atalhos para a Ode à Alegria e ajuda local. A ilustração é um recorte da área artística aprovada, otimizado como WebP local; o screenshot completo não é usado como interface.
- Cartões clicáveis para biblioteca, Laboratório, Histórico e Opções. A biblioteca mantém a posição e os dados ao receber foco.
- Biblioteca local em painel próprio: pesquisa por título e nome de arquivo, filtros existentes de favoritos/coleções, criação/exclusão de coleções, botão de importação, favoritos, contagem, estados vazio/sem resultado e cartões gerados a partir do catálogo e dos arquivos importados.
- Cartões usam miniatura musical neutra. O catálogo atual fornece nomes e arquivos, mas não compositor, nível ou gênero; esses metadados não são inventados.
- Orientação de quatro cartões por linha em tablets largos, duas colunas em larguras médias e coluna única em celulares. Busca e filtros empilham em larguras compactas.

## Compatibilidade e preservação

Os arquivos, preferências, favoritos, coleções, perfis, histórico e rotas de estudo existentes continuam usando os stores e telas já presentes. A mudança da apresentação não recria nem migra dados. A seleção de perfil no cabeçalho novo sincroniza com o seletor mantido no cabeçalho de Estudar. Nenhuma dependência remota, permissão de internet ou serviço novo foi adicionado.

## Correções da auditoria do Prompt 1

Na versão inicial do PR #71, o recurso `home_hero_art.webp` ainda incluía um fragmento do botão antigo da referência aprovada. O recorte foi refeito na área artística à direita: agora só contém piano, ambiente e notas. Título, descrição e ações continuam sendo controles nativos. No modo paisagem de baixa altura, o banner passou de 138 para 178 dp, reduziu o preenchimento e o tamanho do texto e reserva largura para os dois botões; a ilustração fica confinada ao lado direito com transição suave atrás do texto.

Os três cartões que têm miniaturas individuais na referência agora usam recortes locais próprios daquela arte aprovada, vinculados pelos nomes de arquivo MusicXML já existentes. Os demais itens, inclusive importados, continuam sendo dinâmicos e mantêm a miniatura neutra de fallback. Nenhum compositor, nível, gênero ou categoria é criado.

A falha `Created collection did not appear in library filters` veio da inspeção do teste, não de perda de dados: a árvore de acessibilidade de um `Spinner` fechado expõe a opção selecionada, não a lista inteira de opções. A criação já atualizava o `LibraryStore` e reconstruía o adaptador; o teste procurava uma opção não selecionada sem abrir a lista. A regressão agora abre o filtro e consulta a opção real, aguarda as atualizações nativas e exercita criar, renomear, confirmar persistência após reiniciar o processo e excluir a coleção.

## Validação

O workflow `emulator.yml` mantém as capturas de tela de home estreita (720 × 540) e larga (1920 × 1080) e agora exige que os botões “Começar pela Ode à Alegria” e “Como usar” constem no layout estreito. O teste de coleções inspeciona o dropdown verdadeiro após salvar/renomear, reinicia o app para conferir persistência e comprova a remoção após excluir. As regressões existentes da biblioteca, navegação, modos de estudo e MIDI continuam habilitadas. O build geral continua cobrindo o teste JVM do indicador USB-MIDI.

Comparação visual esperada: o banner preserva a composição aprovada, removendo apenas o conteúdo antigo que estava embutido no recorte; o cabeçalho mantém a exceção textual aprovada para USB-MIDI; atalhos e biblioteca permanecem componentes reais e dinâmicos. As miniaturas individuais seguem os elementos ilustrados vistos na referência, enquanto as limitações de metadados do catálogo continuam explícitas.

**Verificações locais desta correção:** `verify_public_snapshot.py`, `py_compile` de `tests/emulator_ui.py` e `git diff --check` passaram. Este ambiente não contém Gradle, JDK/`javac` nem Android SDK; APK, testes JVM/Android e capturas finais dependem dos workflows do commit final.

**Resultados dos workflows e evidências do commit final:** registrar depois da execução do Actions desta revisão; os links da execução anterior em `c88b256` não comprovam este código.

**Ainda não testado fisicamente:** Lenovo Tab M9, Realme 7 5G e conexão Yamaha P-45. O emulador não comprova tamanho de fonte, toque, qualidade da arte nem identificação MIDI em hardware real.

## Limitações visuais deliberadas

- O catálogo não expõe compositor, categoria musical nem dificuldade; as etiquetas correspondentes foram omitidas.
- A miniatura neutra é usada até que o catálogo possua arte própria autorizada.
- Em tela estreita, o conteúdo preserva rolagem vertical e os quatro atalhos ficam em coluna; em larguras intermediárias ficam em grade 2 × 2.
- A identificação do nome MIDI depende de o Android abrir o dispositivo. Uma seleção pendente, falha de abertura ou desconexão mantém a legenda USB-MIDI.
