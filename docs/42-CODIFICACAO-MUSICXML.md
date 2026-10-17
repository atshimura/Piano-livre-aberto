# Codificação MusicXML — 0.9.33 / code 42

O leitor anterior convertia toda entrada para UTF-8 antes de respeitar a declaração XML. Isso podia alterar títulos em Latin-1 ou impedir UTF-16 válido. Agora detecta BOM/padrão UTF-16 e declaração de codificação, decodifica estritamente e fornece caracteres ao parser, sem reinterpretar bytes com outra codificação. Suporta UTF-8, UTF-16/LE/BE, ISO-8859-1 e US-ASCII; demais codificações são informadas como incompatíveis. Bytes malformados são rejeitados, sem substituição silenciosa. Arquivo original continua intacto.

O mesmo caminho é aplicado ao container MXL antes de verificar DTD/entidades e limites. Um container UTF-16 não passa por verificação feita sobre bytes tratados como UTF-8. DTD no container é rejeitada antes de construir objetos XML. Fonte da partitura mantém a declaração textual; parsing/renderização recebem a String decodificada. Não há modificação do arquivo importado.

ScoreImportTest acrescenta título Canção em Latin-1, UTF-16 com/sem BOM, entrada UTF-8 inválida e container UTF-16 com DTD. XML_INPUT_SELFTEST_PASS exige seis verificações equivalentes no parser Android, inteiramente sintéticas e sem gravar dados. CI do próprio commit ainda deve ser confirmada antes de integrar. Nenhum teste físico declarado.

Próxima ação: confirmar e integrar PRs #29/#30 e esta correção sob proteção. Verificar atualização offline no emulador, certificado e pré-releases dos merges. Manter issue #11 aberta para Realme/Lenovo/Yamaha; não declarar V1 por CI.
