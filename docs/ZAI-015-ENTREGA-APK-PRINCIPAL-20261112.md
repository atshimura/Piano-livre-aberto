# ZAI-015 — Entrega do modo Aprender e APK principal 0.9.76

**Data:** 12/11/2026. **Repositório:** `atshimura/Piano-livre-aberto` (público). **Linha:** `zai/lab`. A `main` protegida, o repositório histórico privado e Releases/tags **não** foram alterados.

## O que foi implementado

Conforme as quatro telas aprovadas, **Laboratório Musical → Aprender** agora abre uma escolha entre **Escolher e tocar** e **Ler partitura**. O antigo cartão «Em breve» foi habilitado, inclusive a aba contextual de Descobrir e o acesso a partir de Criação sem abandonar uma gravação MIDI pendente. Voltar retorna em níveis: exercício → configuração ou trilha → entrada Aprender → Laboratório → Biblioteca.

- **Escolher e tocar:** filtros por nota/acorde/sequência; pesquisa no catálogo de nomes PT/internacionais, acordes, escalas, intervalos e arpejos; seleção de mão como referência, sem alegar identificação da mão real; prévia musical OSMD; botão Iniciar prática. Nota exata, acorde com conjunto completo de alturas sustentadas (sem janela arbitrária) e sequência de Note On individuais na ordem; nenhuma exigência de velocidade/tempo de execução. Teclado virtual multitouch + USB-MIDI; realimentação de acertos e extras, recomeçar e ouvir exemplo tonal sintetizado **localmente**.
- **Ler partitura:** trilha de cinco módulos (Notas na pauta, Movimentos/intervalos, Ritmo, Acordes, Leitura livre), com **28 exercícios originais** nesta versão, desbloqueados progressivamente. Exercício de nota fornece nome/guia no início e retira progressivamente as pistas; dica pode ser solicitada. Avanço de exercício **somente após acerto**. Em ritmo, o aluno **identifica a figura escrita** escolhendo entre opções, sem afirmar que o app mede execução rítmica. OSMD local desenha as partituras dos exercícios. O catálogo permanece acessível livremente na outra jornada.
- **Progresso por perfil:** bitmasks de exercícios concluídos, sem pontos artificiais por repetir a mesma lição, em `SharedPreferences` existentes. `BackupArchive` já exporta/restaura preferências tipadas e `LocalProfiles` exclui as chaves por prefixo ao eliminar o perfil. Não foi alterado o formato de `.plc`, `.pls`, importações nem banco. Dados anteriores preservados.
- **Interface:** quatro telas em paisagem, cabeçalhos e cartões no estilo atual, partitura OSMD com espaço prioritário, teclado guia opcional/gradual, botões de ajuda/áudio/próximo e saída segura ao mudar de modo ou colocar o app em segundo plano. Sem rede, servidor, telemetria ou conteúdo musical externo.

## Verificações reais

1. **Implementação:** [PR #158](https://github.com/atshimura/Piano-livre-aberto/pull/158), integrado a `zai/lab` pelo merge `2d846bcb8d54cfd1816a104c8a2ac80dceb9d5cd`; cabeça `8c3022535b4c87a840c69df5e19ac0deaf6cba31` com **18/18 verificações GitHub aprovadas**, incluindo regressões anteriores, Chromium OSMD e GUI Android.
2. **Motor:** `MusicLearningEngineTest`: **138 verificações JVM** para lições, nomes, desbloqueio, ataques exatos, acorde, sequência, identificação de figura e MusicXML. `test_zai015_learning_contract.py`: **78 verificações** de interface e integração. Smoke `emulator_zai015_learning.py` aprovado em execução GitHub: abertura dos quatro estados, resposta por teclado virtual e progressão da primeira lição. Testes de regressão ZAI-010/011/012/013/014 repetidos após habilitar Aprender.
3. **Assinatura:** [PR #159](https://github.com/atshimura/Piano-livre-aberto/pull/159), integrado a `zai/lab` pelo merge `cd09d684b1cce1bf3c39743704c9f4180dbf8302`, cabeça `7ca45d41180914c7cc50f440d5fdfef5311e3bc8` com **17/17 checks aprovados**; [Actions #43303073521](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43303073521) concluído `success` e com artefato `PianoLivre-PRINCIPAL-0.9.76-APK-assinado-7ca45d41180914c7cc50f440d5fdfef5311e3bc8`. Build release Android `br.org.pianolivre.opensource`, `versionCode=85`, `versionName=0.9.76-open-beta`, **sem INTERNET e sem debuggable**. Assinatura com GitHub Actions Secrets permanentes; não se utilizou chave provisória.
4. **Distribuição:** [Actions #43306502122](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43306502122) concluiu `success`, baixou o artefato original sem recompor nem reassinar, validou novamente versão, pacote, certificado e hash, copiou os **mesmos bytes** para `zai/downloads` e leu de volta o Git remoto para conferir integridade. Workflow e revisão em [PR #160](https://github.com/atshimura/Piano-livre-aberto/pull/160).

## Download e integridade

- **APK principal:** [PianoLivre-Principal-0.9.76-open-beta-assinado.apk](https://github.com/atshimura/Piano-livre-aberto/raw/refs/heads/zai/downloads/apks/principal/PianoLivre-Principal-0.9.76-open-beta-assinado.apk)
- **Metadados públicos:** [PianoLivre-Principal-0.9.76-open-beta-SHA256.txt](https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.76-open-beta-SHA256.txt)
- **SHA-256 dos bytes originais e publicados:** `be9b2244f6cffd0bcf5974f9adfe0010bda0e08ec3def7b44220da8a99d45b75`
- **SHA-256 do certificado público permanente:** `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`

A assinatura e o identificador são os mesmos do APK principal anterior, permitindo atualização sobre a série principal. Faça backup antes de instalar por prudência. O novo módulo permanece em fase beta.

## Limitações e próximos passos

**Não há teste físico:** Realme 7 5G, Lenovo Tab M9 e Yamaha P-45; emulador e Chromium não equivalem à verificação de hardware USB-MIDI, latência, multitouch ou legibilidade nos dispositivos reais. A aplicação ainda não avalia ritmo tocado, dinâmica, pedal, dedilhado real, leitura com duas mãos, fluência ou repertório protegido. O exemplo sonoro usa senoides locais simples e **não reproduz timbre de piano**. Evoluir didática (pausas, compassos, claves, intervalos, síncopes, pontos, pequenas peças com ambas as mãos) em entregas futuras mantendo separadas as regras simplificadas da Criação. Fazer testes físicos quando oportuno, sem interromper outras etapas técnicas.
