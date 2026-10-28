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

## Validação

O workflow `emulator.yml` foi ampliado para capturar a tela inicial em perfis de paisagem estreito e largo e conferir banner, ajuda local, importação, coleções, favoritos, navegação e acesso USB-MIDI. Um teste JVM cobre os estados de indicador desconectado, nome real retornado pelo Android e dispositivo aberto sem nome identificável. Também se mantêm as regressões existentes da biblioteca e do modo Estudar. As capturas e os resultados deste SHA devem ser anexados/consultados no PR; somente resultados do SHA final contam como evidência.

**Resultados do commit final:** preencher após CI e emulador.

**Ainda não testado fisicamente:** Lenovo Tab M9, Realme 7 5G e conexão Yamaha P-45. O emulador não comprova tamanho de fonte, toque, qualidade da arte nem identificação MIDI em hardware real.

## Limitações visuais deliberadas

- O catálogo não expõe compositor, categoria musical nem dificuldade; as etiquetas correspondentes foram omitidas.
- A miniatura neutra é usada até que o catálogo possua arte própria autorizada.
- Em tela estreita, o conteúdo preserva rolagem vertical e os quatro atalhos ficam em coluna; em larguras intermediárias ficam em grade 2 × 2.
- A identificação do nome MIDI depende de o Android abrir o dispositivo. Uma seleção pendente, falha de abertura ou desconexão mantém a legenda USB-MIDI.
