# 78 — Criação C1: implementação e validação

**Base:** `cf4b24b`, beta pública 0.9.54/code63. **Entrega:** [PR #82](https://github.com/atshimura/Piano-livre-aberto/pull/82), branch `feat/criacao-c1`, candidata 0.9.55-open-beta/code64. Escopo: [76](76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md); autorização de execução e integração: [prompt completo](prompts/CRIACAO_C1_EXECUCAO_COMPLETA.md).

A revisão de 31/10/2026 ampliou o teste inicial de navegação vazia e corrigiu falhas de recuperação, concorrência e saída MIDI. Resultados anteriores à revisão não aprovam o código atual. O registro final no PR contém SHAs, execuções e artefatos aprovados. Integração exige `build` e `emulator` completos. Publicação de Release permanece sem autorização: `pianolivre.beta.publishApproved=false` impede publicação automática na main, preservando compilação, assinatura e testes.

## Entrega por subetapa

| Etapa | Comportamento implementado | Evidência exigida |
|---|---|---|
| C1.1 | Laboratório → Criação; Aprender desabilitado; recentes, gravação, biblioteca, detalhes e Voltar; busca/filtro/ordem/posição conservados; entrada e saída separadas. | Navegação e jornada funcional em telefone/tablet, paisagem e fonte ampliada. |
| C1.2 | Tomada original, relógio na recepção, canais, velocity, simultaneidade, CC64/120/121/123, pausa/retomada segura, interrupção por conexão/perfil. | Recorder, regressões de geração/época e MidiReceiver em thread separada antes da fila da UI. |
| C1.3 | UUID/proprietário imutáveis, escrita atômica, busca sem acentos, ordenação, renomeação, Lixeira, restauração, exclusão confirmada e recuperação de journal. | Persistência, corrupção, limites, reinício do processo e controles reais. |
| C1.4 | Prévia, reprodução na saída MIDI, Pausar/Continuar/Parar, liberação de notas/pedal, ignorar silêncios das pontas, exportação SAF da revisão ou projeto. | Player, roundtrip SMF e seletor Android real com leitura de retorno dos bytes. |
| C1.5 | Backup v3/SHA-256, leitura v1/v2, pré-validação, confirmação, merge conservador de criações e rollback. | BackupArchiveSelfTest, upgrades .54/.52/.46 e suíte completa no SHA integrado. |

## Componentes e decisões

- `MidiTakeRecorder`: fonte imutável com status/dados, canal, ordem, relógio monotônico, época, timestamp Android opcional e ID da entrada; fechamentos derivados identificados. Segmentos excluem pausas, sem quantização.
- `CreationJournal`: fila limitada, thread própria e fsync; formato 3 lê 1/2, CRC do cabeçalho/registros, criação exclusiva. Início de segmento e relógio confirmado preservam notas longas até o último ponto durável. Morte do processo pode perder a cauda ainda não confirmada; não se promete perda zero. Activity recriada não lê journal ativo.
- `CreationProjectStore`: fonte canônica privada `creations/<UUID>.plc`, formato 2 lê 1; CRC, arquivo temporário validado e troca atômica. Metadados/eventos juntos evitam MIDI redundante divergente. UI executa operações de disco em fila de trabalho.
- `MidiTakePlayer`: um agendamento pendente e lotes limitados; cancelamento por geração e saída Android serializada no Handler. Retomada recompõe controladores, teclas e vozes sustentadas. Sem seek livre, overdub ou áudio do telefone.
- `MidiStandardFileWriter`: SMF formato 0, 960 PPQN, mapa de relógio 500.000 µs/semínima. Conversão para ticks tem arredondamento e não impõe ritmo/quantização à fonte. Nota menor que um tick recebe fechamento mínimo de um tick. Off órfão é omitido apenas da derivação. O importador existente tem limites próprios; não se promete reimportar qualquer tomada extensa.
- `MusicCreationScreen`/`MainActivity`: captura não depende de atraso visual; exportação/projetos fora da UI. SAF usa cópia temporária com SHA-256 e restaura estado do seletor; sem permissão ampla de armazenamento. Mudança de perfil preserva a tomada sob o proprietário original.
- `BackupArchive`: v3 inclui criações/SHA-256 e lê v1/v2. Conserva criações locais ausentes do backup e rejeita colisões divergentes/proprietários ausentes antes de mutar. Preferências/importados mantêm substituição confirmada e rollback.

## Limites

100.000 eventos totais por tomada, incluindo reserva de fechamentos; quatro horas ativas; até 200 projetos e 6 MiB de biblioteca, inclusive no merge de backup; teto defensivo de 16 MiB por arquivo. Ao atingir limite, interromper com trecho recuperável. É possível exportar a revisão sem adicioná-la à biblioteca cheia.

Ensaio JVM local com heap de 128 MiB e 97.000 eventos: projeto 3.783.143 bytes, SMF 388.050 bytes; recepção 39–45 ms, salvamento 128–140 ms, exportação 64–83 ms nas execuções locais. São medidas do computador de desenvolvimento, não desempenho de Android físico.

## Verificação

Testes JVM: recorder (24), projetos (10), journal (6), SMF (7), player (9), segurança/recuperação (20), 97.000 eventos e proteção de publicação (16). A CI mantém as regressões musicais, JavaScript, MIDI e auditoria pública.

`CreationSelfTest`/`emulator_creation_functional.py` usa controles Android reais e portas explicitamente SIMULADAS: acordes/canais/pedal, pausa/eco, revisão, salvar, renomear, busca/ordem/Back, Lixeira/restauração/exclusão, ausência de entrada/saída, segundo plano, recuperáveis, perfis e reinício. Exportação abre DocumentsUI e lê o URI retornado; o importador confirma quatro notas. São 18 estados por dispositivo, 36 PNGs e `piano-c1-report.json`, além da navegação e regressões anteriores. Telefone 1280×720; tablet 1920×1080, fonte 1,3; densidade 240 dpi.

Execução falha preserva logs/capturas, sem contar como aceite. A revisão corrigiu IDs repetidos das portas simuladas, leitura de PID antes do processo existir e atualização redundante do contador que impedia a ociosidade do leitor de acessibilidade.

## Limitações e continuidade

Yamaha P-45, Realme 7 5G e Lenovo Tab M9 exigem testes físicos posteriores. TalkBack não foi validado especificamente; CI não mede latência USB física. Offline/GPL-3.0-only preservados, sem novas partituras ou bibliotecas externas. C2 (partitura), C3 (PDF), camadas e sintetizador ficam fora; V1 não está concluída.

APK de PR usa certificado descartável; release da main usa assinatura permanente dos Actions Secrets. Artefatos expiram conforme retenção. Release durável requer autorização específica; não habilitar publicação como decisão técnica rotineira.
