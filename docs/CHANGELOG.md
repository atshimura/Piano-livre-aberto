# Histórico de alterações

## Implementação E — Bloco E-B (E2 + E3), branch de desenvolvimento — 01/11/2026

A pauta normal de Descobrir foi ampliada em ~27% nas dimensões Chromium verificadas (celular/tablet), e notas, acordes e sequências começam após as claves na mesma coluna. OSMD e MIDI preservados; teste dedicado, 16 capturas e build CI [#41316106385](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41316106385) aprovados no código verificado. **Somente no PR draft #91**, não integrado nem publicado; retorno automático dos extremos (E4) e entrega final pendentes. [Relatório](98-IMPLEMENTACAO-E-E-B-PAUTA-ALINHADA.md).

## Planejamento aprovado — Implementação E (31/10/2026; sem nova versão)

Documentados três ajustes futuros e exclusivos da pauta de Descobrir: pautas maiores, coluna inicial comum de nota/acorde/sequência e enquadramento automático reversível para extremos simultâneos. Definidos plano E0–E6, matrizes de teste e critérios de aceite. **Nenhuma alteração do aplicativo, teste executado para E ou APK é reivindicada.** [Plano técnico](95-IMPLEMENTACAO-E-DESCOBRIR-PAUTA.md) · [Issue #89](https://github.com/atshimura/Piano-livre-aberto/issues/89).

## 0.9.55-open-beta / code64 — candidata Criação C1

Gravação MIDI, projetos por perfil, recuperação, reprodução USB-MIDI, exportação SAF e backup v3. Revisão corrige perda do intervalo de notas longas, pausa, portas, callbacks obsoletos e persistência concorrente. Acrescenta jornada funcional Android, roundtrip SAF, upgrade .54 e limites em heap de 128 MiB. Publicação automática requer aprovação explícita; esta candidata não autoriza Release. [Registro completo](78-CREACAO-C1-IMPLEMENTACAO.md).

## 0.9.54-open-beta / code63

Home refinada, Descobrir e CI rápido; [consolidação](77-CONSOLIDACAO-PUBLICACAO-0954.md).
