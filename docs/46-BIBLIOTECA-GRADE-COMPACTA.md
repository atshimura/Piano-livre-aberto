# 46 — Biblioteca em grade compacta (0.9.37 / code46)

## Decisão funcional
A biblioteca deverá aproveitar melhor a paisagem do Realme 7 5G e a área do Lenovo Tab M9, usando **grade compacta** adaptada à largura útil. Permanecem as ações de Estudar, Partitura Livre, favorito, organização por pressionamento longo, busca/filtros e importação. Não converter em lista única, nem alterar coleções/dados dos perfis.

## Implementação proposta nesta entrega

Arquivo: `PianoLivreAndroid/app/src/main/java/br/org/pianolivre/PianoScreens.java`, métodos `buildLibrary()` e `refreshLibrary()`.

- Redução moderada de espaçamentos internos no cabeçalho, cartões e filtros sem remover funcionalidades.
- Densidade responsiva conforme largura do dispositivo (dp): 2 colunas abaixo de 600 dp; 3 de 600 a 869 dp; 4 de 870 a 1159 dp; 5 a partir de 1160 dp.
- Cartões com 92 dp de altura e linhas de 99 dp; títulos truncados com reticências, estrela acessível e ações visíveis.
- Nenhuma alteração em `LibraryStore`, IDs de músicas, `BackupArchive`, preferências, importações ou direitos das partituras.
- Acesso ao Laboratório previsto na issue #34 preservado.

## Critérios e limitações
- Verificar os builds e a UI no emulador **do commit da entrega**, sem supor equivalência com validação física.
- Verificar que a busca accent-insensitive, contagem de resultados, alternância de perfis, favoritos, coleções e histórico se mantêm funcionais, com regressões existentes.
- A grade define densidade por largura de tela em dp, não mede comprimento de títulos; nomes grandes são truncados.
- No Realme 7 5G e Lenovo Tab M9 físicos, confirmar legibilidade e alvo de toque em paisagem; refinar se necessário.

## Ponto de continuidade
A issue [#34](https://github.com/atshimura/Piano-livre-aberto/issues/34) lista entregas restantes: Espera com preparação visual de 5 s configuráveis e cursor anterior às notas; notação (hastes, claves e vigas); tooltip nome português/internacional e cifra; Lab para escalas/arpejos, dedilhados, reprodução, metrônomo e histórico. [Documento 45](45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md) é referência funcional completa. A issue #11 continua aberta até aceite físico; não declarar 1.0.
