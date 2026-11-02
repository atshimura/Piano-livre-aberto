# Alteração 2 — Cabeçalho minimalista da Biblioteca Musical

**Decisão aprovada:** 02/11/2026. Esta implementação acumula a alteração 1 na branch técnica `feat/home-ajustes-acumulados`. Não há nova Release nem integração com a `main`.

## Interface

O título **Biblioteca Musical** é mantido. Logo abaixo, os três únicos controles ficam **na mesma linha e nesta ordem**:

**Coleções | Importar | Busca**

Todos usam largura proporcional idêntica (peso 1) e 44 dp de altura. Espaçamento lateral responsivo de 4–8 dp, com campos mais compactos e redução da altura do painel. Botões preservam a fonte de **13 sp**, busca conserva **14/16 sp**, conforme dimensão disponível, sem reduções adicionais. Ícones são omitidos nos botões quando a tela é estreita, mas as palavras Coleções e Importar permanecem. A busca apresenta **Buscar** em telas compactas e **Buscar partituras...** nas maiores, conservando a descrição acessível completa.

Estética: botão Coleções azul-lavanda, Importar verde-claro e campo Busca neutro. O título e a listagem de partituras permanecem no lugar.

## Preservação de funcionalidades

O seletor **Categoria** foi retirado da interface, sem eliminar biblioteca, favoritos ou dados de coleções. Os antigos filtros foram movidos para o menu **Coleções**, nas opções **Todas as partituras**, **Favoritos** e **Ver coleção**. Permanecem **Nova coleção**, **Renomear coleção** e **Excluir coleção**. A seleção de coleções respeita o perfil atual; a exclusão de uma coleção em uso limpa o filtro temporário. A busca textual e a importação MusicXML/MXL/MIDI seguem seus comportamentos anteriores. A lista informa o filtro ativo no contador.

## Testes e limites

- Regressões de layout e contratos foram atualizados para exigir exatamente três controles alinhados, largura/altura iguais, ordem Coleções → Importar → Busca, fontes preservadas, rolagem e ausência do Spinner Categoria.
- Os testes de navegação antigos foram adaptados para o novo menu Coleções e busca direta, sem eliminar as asserções de catálogo e persistência.
- **Somente testes básicos** no GitHub Actions: contratos Python, compilação sintática de scripts e `compileDebugJavaWithJavac` sem `assemble`, sem geração de APK e sem suíte completa. A validação visual com emulador ou dispositivos físicos fica pendente para uma entrega posterior.
