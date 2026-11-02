# 112 — Criação: Adicionar à Biblioteca para estudar

**Decisão aprovada em 02/11/2026.** Sexta alteração da branch técnica `feat/home-ajustes-acumulados`, preservando as cinco anteriores. Não integrar à main nem gerar APK nesta fase.

## Fluxo aprovado

1. Em **Criação → Gerar / Visualizar e ajustar partitura**, o usuário produz a prévia, ajusta e **Salva partitura** normalmente.
2. **Somente depois de salvar** aparece **Adicionar à Biblioteca** no lugar de **Estudar esta partitura**. A tela de detalhes da composição exibe a mesma opção para partituras já salvas e ainda não adicionadas. Nenhuma geração é adicionada automaticamente.
3. A operação confirma propriedade do perfil, status da composição, hash da origem MIDI e integridade da derivação MusicXML, fora da thread de UI. A adição ao catálogo por perfil é idempotente e realizada na thread de UI para serializar as preferências.
4. Após adicionar, a tela informa **Na Biblioteca Musical**; a partitura aparece pelo título atualizado na Biblioteca, com **Estudar, Partitura livre, Favoritos e Coleções**. O botão Voltar de Estudar retorna à Tela Inicial, como ocorre com as demais partituras.
5. Se a partitura for ajustada e salva novamente, a referência continuará apontando para a versão vigente, sem criar duplicatas nem reescrever o MIDI original.

## Preservação de dados

- O registro na Biblioteca é um vínculo portátil `creation-score:<UUID>`; o MusicXML permanece no arquivo privado versionado `creation-scores/<UUID>.pls`, não é duplicado em `imported/`.
- No JSON `user_<perfil>_library` versão 1, novo campo **opcional** `addedCreations` com UUIDs únicos. Arquivos anteriores sem esse campo continuam válidos. Identificadores malformados ou a chave com tipo errado são rejeitados. Favoritos e coleções aceitam essas chaves portáteis.
- O backup v4 existente já inclui tanto o JSON completo da Biblioteca quanto as partitura derivadas verificadas; a restauração anterior continua compatível com versões antigas. Nenhuma migração destrutiva.
- Partituras vinculadas cujos projetos estejam na Lixeira, indisponíveis, definitivamente excluídos ou pertencentes a outro perfil não são exibidas, sem apagar silenciosamente o marcador armazenado. Restaurar o projeto pode tornar o item visível novamente.
- A leitura para estudar revalida o perfil, o estado do projeto e a derivação. A funcionalidade MIDI, MusicXML, motor de estudo e seleção de mãos não mudam.

## Validação desta etapa

Somente testes de contratos Python, teste JVM de chave portátil e compilação Java Android **sem assemble/APK**, usando o workflow básico da branch. O roteiro C2-D foi atualizado para passar por Adicionar → Biblioteca → Estudar, mas sua execução gráfica fica pendente de autorização para APK. Testes físicos com Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 continuam pendentes.
