# Importação não destrutiva — 0.9.27 / code 36

A importação MusicXML valida os bytes antes de criar arquivos na biblioteca. Nomes repetidos recebem sufixo; o arquivo existente não é sobrescrito. Extensões .XML/.MUSICXML/.MXL são reconhecidas sem diferenciar maiúsculas. Bytes originais são preservados; arquivos inválidos não entram na biblioteca. Escrita falhada remove apenas o novo arquivo reservado. Encerramento abrupto durante a escrita ainda pode deixar novo arquivo incompleto; nunca sobrescreve o anterior.

Score.read limita entrada e soma da expansão MXL a 16 MiB, até 256 entradas, XML com até 128 níveis e 200 mil elementos. Rejeita caminhos ascendentes, duplicados e contêiner com DTD. Respeita rootfile do META-INF/container.xml; sem container aceita somente candidato XML único. Apenas score-partwise é aceito; timewise não vira partitura vazia silenciosamente.

ScoreImportTest cobre mesmos nomes, bytes originais, falha sem novo arquivo, rootfile após XML não musical, ambiguidade, caminhos ascendentes, DTD, profundidade, tamanho bruto e expansão comprimida. Regressões do catálogo/MIDI/guia devem passar no PR. Android/CI não equivalem a testes físicos.

Continuidade: confirmar CI do PR desta versão e integrar após histórico (#24), atualizar issue #10. Ainda falta leitor MIDI com notação aproximada e atribuição explícita de mãos, auditoria de compatibilidade e testes adversariais de backup. Não declarar Etapa 5 concluída.
