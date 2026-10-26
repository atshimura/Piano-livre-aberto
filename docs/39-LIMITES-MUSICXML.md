# Limites MusicXML explícitos — 0.9.30 / code 39

Acidentes integrais escritos em decimal, como -1.0, são preservados corretamente no modelo. A origem textual não é alterada. Microtons e alterações fora de duplo acidente são rejeitados com explicação; o motor MIDI não implementa afinação fracionária. Transposição não nula e transposição dobrada exigem exportar versão em som real para piano. Compassos aditivos, mistos, senza-misura e unidades fora da lista suportada são rejeitados, evitando duração/ataque silenciosamente incorretos. Isso não equivale a implementar esses recursos; são limitações explícitas.

Preflight SAX limita profundidade e quantidade de elementos antes do DOM tanto da partitura quanto do container MXL, sem entidades externas. ScoreImportTest acrescenta container profundo, transposição, microtom, compasso aditivo e alteração decimal integral. Regressões JVM/JS locais passaram, incluindo catálogo completo, MIDI, reconhecimento e teclado guia. Android e emulador precisam ser confirmados na CI do PR antes de integrar. Nenhum teste físico declarado.

Próxima ação: confirmar e integrar sequencialmente PRs #25–#27 e esta entrega; corrigir qualquer falha dos emuladores. Depois consolidar Etapa 5/issue #10, preparar matriz de aceite e candidato 1.0 na Etapa 6. Aceite físico continua na issue #11; não declarar V1 concluída apenas com CI.
