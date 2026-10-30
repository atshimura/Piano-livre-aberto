# Consolidação e publicação — Prompt 4

Versão alvo: **0.9.52-open-beta (61)**, pacote `br.org.pianolivre.opensource`, tag `v0.9.52-open-beta`. A conclusão exige checks aprovados do merge final, certificado permanente, upgrade automatizado e assets reais da pré-release; este documento não presume publicação antes dessas verificações.

## Estado inicial e integração

`main` inicial: `743608dca09869604f209032a72cc74371eb7765`, beta pública 0.9.50 (59). Main protegida com check obrigatório `build`; proteção não alterada. Não existiam tag ou Release 0.9.52. Nenhum acesso ao repositório histórico privado.

| Entrega | HEAD auditado | Destino |
|---|---|---|
| #71 Inicial | `32b83a3b6d37a831900de4bafd5ec8cbd406d0d8` | Primeiro merge, `fb5a59d39317952d3c4c2a8b22352dadce7d9696` |
| #72 especificação e PNG | `22e193b8f31e8f38ac63b7f6cb6d8561c0c29338` | Retarget para main; registro dos hashes e check em `86eeb73` antes do segundo merge |
| #73 Estudar | `2739bb2223cf7efa0a066ec7fc8380bdcde9430d` | Incorporado por ancestralidade na consolidação final |
| #74 MIDI | `a26a7bebfa3d0a077ff5e3603f63a0b629e08a9d` | Incorporado por ancestralidade na consolidação final |

Branch final: `release/consolidacao-interfaces-0.9.52`, criada sobre #74, com merge do fechamento documental de #72. Preserva todos os commits de #71–#74 e os vínculos de revisão. Não integra #73 isoladamente: **a 0.9.51 (60) não deve ser publicada**. Nenhum force-push, desativação de teste ou bypass da proteção. Nenhum conflito na preparação. A ordem efetiva e o SHA dos merges posteriores ficam no registro final do PR consolidado e na Release, conforme protocolo abaixo.

As referências originais foram inspecionadas e preservadas; hashes em [references/README](references/README.md). Relatórios [70](70-TELA-INICIAL-PROMPT-01.md), [71](71-TELA-ESTUDAR-PROMPT-02.md) e [72](72-CONSOLIDACAO-MIDI-INTERFACES-PROMPT-03.md) conservam o histórico das entregas anteriores; as antigas restrições de merge/publicação foram supersedidas pela autorização explícita do [Prompt 4](prompts/PROMPT_04_CONSOLIDACAO_PUBLICACAO_PIANO_LIVRE.md).

## Auditoria e ajustes finais

Preservados: Inicial com biblioteca real; cabeçalho único do Estudar; modos/mãos; partitura ampliada; ausência de guia/seletor manual; Partitura Livre; transporte inferior; barras dinâmicas sem timer; estado MIDI comum, portas funcionais, gerações/épocas, pausa segura e encerramento controlável. MusicXML, relógio musical, dados, direitos musicais, pacote e identidade de assinatura não foram alterados nesta consolidação.

O publicador aguarda até 40 minutos pelos checks exatos, em vez de 30, dentro do job de 45 minutos, acomodando a suíte ampliada. Timeout/falha continuam bloqueando publicação. Notas específicas da versão são incorporadas ao mecanismo existente com SHA real do merge, certificado, checksum e links. Não há segundo publicador nem upload manual. Testes de recuperação/idempotência/conflito existentes preservados; novos cenários cobrem espera prolongada, timeout e identidade efetivamente publicada. Notas em UTF-8 explícito também passam no Windows.

## Verificação obrigatória do commit integrado

Resultados de PRs não são apresentados como resultados de main. O registro final deve identificar o SHA de main e seus runs de `build.yml` e `emulator.yml`, ambos em push confiável. Exigir:

- Auditoria pública, JVM, JS, Chromium/corpus, MusicXML/MIDI, notação complexa/polifonia, notas/tempos/trechos/longas e fallback.
- Biblioteca, importação, busca, favoritos/coleções, perfis/histórico, navegação e Laboratório; backup, rollback, compatibilidade e preservação de dados.
- `MidiConnectionTest` (226), `MidiProtocolEpochTest` (4.226), reconhecimento Android, portas sintéticas e telas com nome explicitamente SIMULADO; ataques antigos, Note On/Off, pedal, desconexão/troca/reconexão e foreground.
- Home celular/tablet e Estudar normal/oculto/restaurado. Matriz `piano-prompt03-midi-interfaces.json`, manifesto `piano-visual-evidence.json` e PNGs reais no artefato `PianoLivre-public-emulator-evidence` do run final. O job curto `midi-visual` fornece revisão antecipada, sem substituir a suíte completa.
- Pautas completas: dez faixas de tinta nos seis estados de Estudar por viewport. Alturas de regressão @240 dpi: celular 1280×720 **417/567/417 px**, tablet 1920×1080 **777/927/777 px**. Barra inferior acessível; restauração exata e sem perda após diagnóstico.
- APK release sem depuração e sem INTERNET, versão 61, pacote aberto, certificado permanente conferido por `apksigner`. Upgrade desde APK público efetivamente assinado, além dos ensaios sintéticos debug→release. PR não recebe Secrets.

## Registro final sem commit recursivo

O próprio SHA de merge e o hash do APK só existem após esta documentação. Para evitar outro commit que divergiria do APK, o **registro final vinculado a este documento** fica no comentário de fechamento do PR consolidado e nas notas da Release. Deve incluir: ordem/SHAs dos merges, reconciliação #73/#74, ancestralidade e comparação dos arquivos, SHA final, runs/resultados, capturas/artefatos, certificado, checksum baixado e teste da URL. Só declarar concluído depois de preencher e conferir esse registro.

O publicador grava automaticamente o SHA efetivo e o SHA-256 do APK nas notas da Release. `target_commitish` e a tag devem resolver ao mesmo SHA; `SHA256SUMS`, `public-signature.txt` e `public-apk-metadata.txt` devem estar anexados. Certificado esperado: `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`. Download e recálculo local devem conferir com o checksum publicado. Assets existentes não são substituídos.

- [Pré-release oficial esperada e registro efetivo](https://github.com/atshimura/Piano-livre-aberto/releases/tag/v0.9.52-open-beta).
- [APK oficial — validar disponibilidade antes de anunciar conclusão](https://github.com/atshimura/Piano-livre-aberto/releases/download/v0.9.52-open-beta/PianoLivre-0.9.52-open-beta.apk).
- [Notas da versão](releases/0.9.52-open-beta.md).

## Limitações e caminho até V1

Realme 7 5G: paisagem, fluidez/toque, barras, reprodução, background e atualização preservando dados. Lenovo Tab M9: legibilidade, área útil, mãos/controles, fluidez, atualização e backup. Yamaha P-45: USB/OTG e nome real, notas/acordes, pedal, desconexão/reconexão, Demonstração e notas presas. Registrar aparelho, versão, cenário e resultado na issue #11.

Esses testes físicos não foram executados nem inferidos da CI; não bloqueiam por si só a beta. A versão não é V1. Importação MIDI segue aproximada, limitações MusicXML continuam explícitas e adaptação nativa/responsiva não copia metadados ausentes. Após a publicação: aceite físico, correção de regressões observadas em branch/PR, validação de atualização/backup nos aparelhos e revisão dos critérios restantes de V1.
