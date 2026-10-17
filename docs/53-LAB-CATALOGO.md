# 53 — Catálogo pedagógico pesquisável local (0.9.44)

## O que foi implementado
O Laboratório passou a oferecer busca textual offline nas categorias Nota, Intervalo, Acorde (tríades, suspensos, acordes de sétima, com inversões), Escala (maiores, menores, modos, pentatônicas, cromática), Arpejo (tríades e sétimas). Há entradas para as 12 classes cromáticas a partir da quarta oitava, além dos cinco exercícios já existentes. Cada item descreve notas de referência e permite exploração no teclado virtual/USB-MIDI, mantendo a pauta esquemática.

## Limites
Os nomes cromáticos se baseiam em notação padrão de sustenidos e **não constituem grafia harmônica contextualizada** pela armadura. Inversões são voicings abertos por subida da nota grave à oitava superior. A conferência atual de prática compara conjuntos de classes de altura, não ordem, ritmo, dedilhado, oitava ou mão para escalas/arpejos. Não afirmar que prática guiada sequencial está concluída. Apenas os dados de catálogo estão em código, sem rede. A GUI busca títulos/categorias e reduz spinner às coincidências.

## Verificações
Teste puro JVM `MusicLabCatalogTest` checa conjuntos, existência dos tipos, filtros, imutabilidade de lista, limites de notas e preservação dos cinco exemplos. Exigir CI e emulador no SHA exato antes de integrar. Testes físicos continuam na issue #11.
