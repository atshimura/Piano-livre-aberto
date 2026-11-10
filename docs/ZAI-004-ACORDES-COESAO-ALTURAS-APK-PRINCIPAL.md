# ZAI-004 — Fidelidade dos acordes MIDI e escrita compartilhada de alturas

**Data:** 10/11/2026. **Escopo:** linha ZAi (`zai/feat/zai004-acordes-fidelidade-20261110` → `zai/lab`), sem alteração da `main`. **Autorização do mantenedor:** planejar, documentar, implementar e gerar um APK para atualizar o aplicativo principal, conforme o fluxo de 0.9.65 (mesmo pacote e assinatura permanente, versão incremental). Sem publicação de Release nem modificação da `main` por esta entrega.

## 1. Falha reproduzida pelo mantenedor

O usuário tocou dois acordes simples, classificados corretamente pelo Descobrir:
1. Dó maior, notas MIDI **[67, 60, 64]** (Sol4/Dó4/Mi4), simultâneas no sentido musical.
2. Dó menor, MIDI **[63, 60, 67]** (Mi♭4/Dó4/Sol4), simultâneas.
A partitura gerada pela Criação apresentou diferentes vozes, vários ataques visuais ligados e pausas em lugar de dois acordes íntegros. O arquivo `acordes.mid` foi analisado. PPQN=960 e tempo 500.000 μs/semínima no SMF **são referências da exportação**, não provas de pulsação musical.

Evento por nota, em ticks absolutos desde início da trilha:
| Nota | On | Off | Duração em ms (referência MIDI de 120 BPM) |
|---|---:|---:|---:|
| G4 67 | 6185 | 7263 | 561,46 |
| C4 60 | 6193 | 7285 | 568,75 |
| E4 64 | 6207 | 7291 | 564,58 |
| E♭4 63 | 9132 | 10402 | 661,46 |
| C4 60 | 9138 | 10396 | 655,21 |
| G4 67 | 9144 | 10394 | 651,04 |

Os grupos têm espalhamento de ataques de **11,46 ms** e **6,25 ms**; solturas de **14,58 ms** e **4,17 ms**. São acordes inequivocamente coesos, não três vozes independentes.

### Causa-raiz

- `MusicDiscoverySession` agrupa os Note On em uma unidade de acorde, `MusicTheory` identifica sua qualidade, `DiscoveryScoreBuilder` escreve todas as alturas juntas como MusicXML com `<chord/>`. O OSMD renderiza.
- `CreationScoreGenerator` chama `ScoreMidiPairer` → `ScoreTempoEstimator` → `ScoreRhythmQuantizer` → `ScoreXmlComposer`. O quantizador agrupa os ataques simultâneos, mas **exige o mesmo término quantizado** para colocar as notas na mesma voz. Diferenças de ~7ms próximas a fronteira de grade causam 4 e 5 semicolcheias, separando um acorde em vozes; uma duração de 5/16 gera fragmentos ligados, que se parecem com ataques extras.
- Com apenas dois acordes não há pulsação determinada; 120 BPM provisórios são aceitáveis **somente como convenção explícita** e não justificam inventar vozes.
- `ScoreXmlComposer.pitchXml` usa `<step>D</step><alter>1</alter>` para MIDI 63 (Ré♯), ao passo que `DiscoveryScoreBuilder` usa `<step>E</step><alter>-1</alter>` (Mi♭). É uma divergência de grafia enarmônica, não de altura.

## 2. Decisões e limites musicais

1. **Priorizar o acorde como unidade de ataque:** início no mesmo agrupamento musical e mesma pauta; múltiplas teclas com Note Off quase coincidentes devem manter a mesma duração escrita, voz e um único ataque vertical por acorde.
2. **Tolerar humanização sem mascarar polifonia:** normalizar pequenas diferenças de soltura (limiar limitado por pulso e teto conservador de ~70 ms). Quando as durações diferirem significativamente, preservar notas sustentadas por vozes independentes. A sincronização de início não significa que necessariamente tenham a mesma duração.
3. **Não apagar ritmos ou encurtar notas por padrão:** os ataques originais e durações físicas continuam no modelo `PairedNote` e nos arquivos `.plc`; somente a duração *notada* pode ser normalizada na faixa musicalmente ambígua.
4. **Ortografia consistente de alturas:** representar teclas pretas como o Descobrir por padrão (C♯, E♭, F♯, A♭, B♭); a tonalidade e cifras podem exigir enarmonia diferente em contexto futuro. Não alterar o número MIDI nem transportar as notas.
5. **Ritmo indeterminado com apenas dois acordes:** avisar que os 120 BPM são provisórios, evitar pausas artificiais entre as notas de cada acorde; respeitar o silêncio genuíno *entre* acordes.
6. **Preservar contratos ZAI-001/002:** pauta dupla, corte C4 MIDI 60, monofonia, staccato, legato, vozes e sobreposição real, ties entre compassos, MusicXML/OSMD/Estudar, projetos e backups. Não alterar entrada MIDI `MidiConnection`.
7. **Não confundir renderização e exatidão:** OSMD desenha o XML recebido, não reinterpreta a captura; verificar seis ataques MusicXML ligados a seis fontes e dois grupos/slots simultâneos, mais regressão independente.
8. **APK principal:** somente após aprovação dos testes, produzir `br.org.pianolivre.opensource`, `versionCode=75`, `versionName=0.9.66-open-beta`, certificado público permanente idêntico a 0.9.65; build `release` não depurável e sem `INTERNET`. A mudança de versão é de **artefato via parâmetros Gradle**, sem mudar o arquivo de versões de `main` ou criar tag/Release. O pacote `.opensource.zai` é proibido no instalador principal.

## 3. Execução planejada

**Z4-P0 — Regressão do caso real:** implementar fixture exclusivamente com notas e ticks sanitizados acima, extraídos do MIDI fornecido (sem distribuir arquivo pessoal e seu metadado). Testar que são 2 acordes, seis Note On sem extras e não seis eventos espalhados por vozes desnecessárias.

**Z4-P1 — Coesão no quantizador:** no mesmo grupo de ataque e pauta, agrupar por solturas próximas e unificar duração notada em figura musical simples; atribuir mesma voz e `<chord/>`, sem truncar sustentação genuinamente distinta. Não depender de igualdade exata de Note Off nem de igualdade acidental de células 4/5.

**Z4-P2 — Grafia de alturas:** harmonizar as tabelas de enarmonia do Descobrir e da Criação (MIDI 63 = E♭4), inclusive testes de todas as doze classes cromáticas. Não inventar tonalidade explícita (além da configuração neutra pré-existente).

**Z4-P3 — Regressão e integração:** testes JVM para MIDI real, pequenas diferenças de soltar, acordes mais longos, nota sustentada dentro de acorde, C4/B3 e comportamento preexistente, parsing no Estudar, testes de contrato, compilação Android; falhar antes do APK se houver alteração indevida de pacote.

**Z4-P4 — Entrega assinada:** integrar código em PR para `zai/lab` (não `main`), gerar 0.9.66 code75 em branch `zai/ci/` dedicada com GitHub Actions Secrets; verificar com `aapt`, `apksigner`, SHA-256 e disponibilizar APK principal diretamente e como artefato GitHub. Registrar resultados e limitações; nenhum teste virtual equivale ao Yamaha P-45 no Realme 7 5G / Lenovo Tab M9.

## 4. Critérios de aceite objetivos

- **AC-REAL-2:** as seis notas MIDI reais preservam alturas e dois ataques de acorde; cada acorde contém exatamente três notas, duas marcas `<chord/>`, tempo provisório avisado, sem redução para duas pautas independentes nem várias vozes espúrias.
- **AC-RELEASE-JITTER:** mesmo ataque e pequenas diferenças nos tempos de Note Off (5 a 30ms) não causam figuras e vozes diferentes.
- **AC-POLY:** uma nota que exceda significativamente a soltura das demais não é encurtada só para permanecer num acorde; as demais mantêm coesão.
- **AC-ENHARMONIC:** MIDI63 = E♭ e os outros acidentes seguem a convenção usada pelo Descobrir. Mesmos números MIDI na Criação e no Estudar.
- **AC-REGRESSION:** ZAI-002/001 (14 notas escala, staccato/legato, sobreposições e 5/16) continuam passando.
- **AC-APK:** arquivo instalável em tese por cima de 0.9.65 code74, package principal, code75, fingerprint permanente, sem INTERNET, não debug. **Testes físicos de instalação/dados/música permanecem pendentes.**

**Observação sobre causalidade:** esta implementação trata a coesão de acordes sem afirmar que duas ocorrências isoladas permitem uma leitura rítmica única; o fallback de 120 BPM continuará explicitamente provisório, com escrita conservadora. A classificação de qualidade do acorde no Descobrir é diferente de representação completa de uma partitura.
