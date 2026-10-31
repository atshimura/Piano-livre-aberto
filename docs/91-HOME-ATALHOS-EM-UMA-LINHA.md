# Tela inicial — três atalhos sempre na mesma linha

## Objetivo
Na Home, Biblioteca Musical, Laboratório Musical e Histórico devem aparecer sempre lado a lado, em uma única linha horizontal, com cartões de larguras iguais. Não deslocar nenhum dos três para outra linha.

## Implementação
- `PianoScreens.buildNavigationCards()` substitui o critério anterior de uma coluna abaixo de 720 dp por uma única `LinearLayout` horizontal de três filhos com `weight=1`.
- Para largura reduzida ou fonte ampliada, conteúdo interno vertical: ícone acima de título centralizado em até duas linhas; a descrição visual e a seta não são exibidas para liberar espaço. O card mantém a descrição completa para acessibilidade.
- Em telas amplas, manter ícone, título, descrição e seta lado a lado. Tamanho e altura são escolhidos conforme largura lógica da janela, densidade e escala de fontes.
- Ações originais preservadas: Biblioteca rola ao catálogo, Laboratório abre a área musical, Histórico abre os dados do perfil. Sem mudanças em MIDI, gravação, backup e persistência.

## Verificação direcionada
O roteiro existente `PianoLivreAndroid/tests/home_layout_checks.py` agora confirma três cards com mesma coordenada vertical, larguras praticamente iguais e ausência de sobreposição em perfis compactos, paisagem e tablet. Ele também mantém as verificações de texto, acessibilidade básica e navegação da Home.

## Limitações
Telas estreitas podem ocultar subtítulos visuais; o rótulo acessível completo continua presente. A decisão de usar a apresentação compacta é tomada quando a tela é construída ou reconstruída após alteração de configuração do Android. A fonte muito ampliada pode demandar avaliação visual adicional.

Esta mudança é exclusivamente na Home; não implica lançamento de nova versão ou publicação oficial automática.
