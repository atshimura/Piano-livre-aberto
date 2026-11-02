# Retomada — Implementação Criação C2
**Repositório:** `atshimura/Piano-livre-aberto`. Repositório histórico privado: **não tocar**.
**Especificação autoritativa:** [docs/101-CRIACAO-C2-PLANO-EXECUCAO.md](../101-CRIACAO-C2-PLANO-EXECUCAO.md); [issue #95](https://github.com/atshimura/Piano-livre-aberto/issues/95). Anterior: docs 76, 78, 94.

## Comandos de implementação
- **Implemente C2-A**: motor de transcrição MIDI em memória, ritmo/pautas/vozes/compassos/MusicXML válido, IDs imutáveis para notas, testes JVM e corpus.
- **Implemente C2-B**: UX de Gerar/Prévia/Ajustar/Divisão entre pautas (geral, nota, acorde, múltiplas, compassos), OSMD, undo, cancelamento, testes Android dirigidos.
- **Implemente C2-C**: derivação vinculada ao projeto, gravação atômica, perfis/lixeira, backup versionado completo com rollback e compatibilidade v1/v2/v3, abrir em Estudar.
- **Implemente C2-D**: integração final, testes de compatibilidade/notação/backup e emulador, documentação, PRs e APK assinado. Release **só** com autorização explícita e versionada.

## Checklist obrigatório antes de cada bloco
1. Ler `AGENTS.md`, README, docs 101/76/78/94, 05/38 e código/CI atuais. Verificar `main`, versão, PRs, issues e runs; não assumir estado de 01/11/2026.
2. Preservar eventos `.plc` e reprodução SMF sem quantização. Só converter cópia derivada; manter MusicXML interno. Não inferir mão por nota e não estender duração escrita pelo pedal.
3. Testar idempotência, notas repetidas, acordes, 6/8, ligaduras, voz, ID por evento, seleção por compasso, undo e nada de perda silenciosa.
4. Sidecar proposto exige backup novo que realmente o inclua; validar preflight/rollback, legacy backup v1/v2/v3 e perfil/lixeira antes de habilitar gravação definitiva.
5. Trabalhar em branch+PR, CI adequada ao risco, proteger `main`, validar SHA final, atualizar documentação e issue. Jamais anunciar teste ou release não concluídos; testes físicos do Yamaha/Realme/Lenovo ficam para usuário.
6. A saída pode ser parcial só com diagnóstico/documentação se um gate bloquear. Registrar precisamente o bloqueio e próxima ação segura; não mudar o produto por conveniência.

**Decisões de produto já tomadas:** gerar após clique, prévia sem formulário obrigatório, ajustes opcionais BPM/compasso/1-8/1-16/1-32, escolha de pauta automática ou por altura, correção de nota/acorde/seleção múltipla/intervalo de compassos, desfazer/restaurar/salvar, original intocado, Estudar própria composição. C3: exportar MusicXML/PDF; C4: editor de notas; sem rede/sintetizador/overdub na C2.
