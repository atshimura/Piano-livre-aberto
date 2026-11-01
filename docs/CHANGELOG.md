# Histórico de alterações

## 0.9.60-open-beta (code69) — correção de abertura (01/11/2026)

Corrigido o lançamento que abria diretamente Estudar quando o perfil possuía uma partitura salva. Toda nova Activity inicia em Home, preservando dados e preferências; o estudo só abre após escolha explícita. Testes de reabertura com sessão salva adicionados à CI Android. **Release aprovada especificamente pelo usuário; publicação condicionada aos gates do SHA final.** [Issue #92](https://github.com/atshimura/Piano-livre-aberto/issues/92) · [notas 0.9.60](releases/0.9.60-open-beta.md).

## 0.9.59-open-beta / code68 — candidata Implementação E (01/11/2026)

Melhoria exclusiva da pauta Descobrir: ampliação proporcional do padrão, alinhamento de notas/acordes/sequências à esquerda e ajuste reversível de enquadramento conforme alturas visíveis, com retorno automático após nova entrada normal. **E5/E6 em validação no PR #91; versão não publicada, Release não autorizada**. [Protocolo](100-IMPLEMENTACAO-E-E-D-VALIDACAO-ENTREGA.md) · [notas da candidata](releases/0.9.59-open-beta.md).

## Implementação E — E4 (E-C) na branch #91 — 01/11/2026

Câmera do Descobrir agora é automaticamente reversível pela extensão das notas visíveis, preservando último resultado e retornando à pauta ampliada em entrada comum. Testes [build #41318276669](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276669) aprovados (56 verificações JVM, 12 PNGs e seis ciclos por perfil em Chromium); teste Android `ui-fast` [#41318276483](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276483) aprovado (22 estados reais da UI, MIDI sintético). **Somente PR draft #91; E5/E6, integração, APK assinado e testes físicos pendentes**. [Relatório](99-IMPLEMENTACAO-E-E-C-ENQUADRAMENTO-REVERSIVEL.md).

## Implementação E — Bloco E-B (E2 + E3), branch de desenvolvimento — 01/11/2026

A pauta normal de Descobrir foi ampliada em ~27% nas dimensões Chromium verificadas (celular/tablet), e notas, acordes e sequências começam após as claves na mesma coluna. OSMD e MIDI preservados; teste dedicado, 16 capturas e build CI [#41316106385](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41316106385) aprovados no código verificado. **Somente no PR draft #91**, não integrado nem publicado; retorno automático dos extremos (E4) e entrega final pendentes. [Relatório](98-IMPLEMENTACAO-E-E-B-PAUTA-ALINHADA.md).

## Planejamento aprovado — Implementação E (31/10/2026; sem nova versão)

Documentados três ajustes futuros e exclusivos da pauta de Descobrir: pautas maiores, coluna inicial comum de nota/acorde/sequência e enquadramento automático reversível para extremos simultâneos. Definidos plano E0–E6, matrizes de teste e critérios de aceite. **Nenhuma alteração do aplicativo, teste executado para E ou APK é reivindicada.** [Plano técnico](95-IMPLEMENTACAO-E-DESCOBRIR-PAUTA.md) · [Issue #89](https://github.com/atshimura/Piano-livre-aberto/issues/89).

## 0.9.55-open-beta / code64 — candidata Criação C1

Gravação MIDI, projetos por perfil, recuperação, reprodução USB-MIDI, exportação SAF e backup v3. Revisão corrige perda do intervalo de notas longas, pausa, portas, callbacks obsoletos e persistência concorrente. Acrescenta jornada funcional Android, roundtrip SAF, upgrade .54 e limites em heap de 128 MiB. Publicação automática requer aprovação explícita; esta candidata não autoriza Release. [Registro completo](78-CREACAO-C1-IMPLEMENTACAO.md).

## 0.9.54-open-beta / code63

Home refinada, Descobrir e CI rápido; [consolidação](77-CONSOLIDACAO-PUBLICACAO-0954.md).
