# 102 — Criação C2-A: motor MIDI → MusicXML em memória

**Data:** 01/11/2026. **Branch funcional:** `feat/criacao-c2-a-motor-transcricao`. **Issue:** [#95](https://github.com/atshimura/Piano-livre-aberto/issues/95). **Planejamento:** [docs/101 no PR documental #96](https://github.com/atshimura/Piano-livre-aberto/pull/96).

## Estado exato
- **Implementado em branch, não integrado à main:** `CreationScoreTranscriber.java` — transcritor puro Java; `CreationScoreTranscriberTest.java` — teste JVM direcionado. Nenhuma alteração em `MainActivity`, na Criação C1, na interface, em persistência/backup, permissões, versão, assinatura ou motor MIDI existente.
- **Sem PR funcional por ora:** o usuário determinou “a cada implementação, somente testes básicos; nunca rode a suíte completa”. Porém, o workflow `.github/workflows/build.yml` executa a suíte ampla automaticamente para qualquer PR aberto contra `main` e o classificador conservador de `emulator.yml` atribui `tier=full` a qualquer alteração Java, executando `emulator-suite`. Abrir PR dispararia a suíte completa, contrariando essa regra. **Não enfraquecer nem contornar os gates**. O código permanecerá na branch para revisão futura da estratégia de CI/integração, sem alegar merge ou APK.
- **Validação básica feita localmente:** `javac` JDK 21 + verificação JVM XML DOM, com uma classe de contrato local de `MidiTakeRecorder` reproduzindo a forma dos construtores/campos públicos/package-private; saída `CREATION_C2_A_BASIC_PASS checks=48`. Essa verificação é **parcial**: não equivale a compilar o APK, testar o parser `Score` real do repo, rodar o emulador, ou aceitar hardware. Os dois arquivos reais foram enviados à branch; os mesmos testes dirigidos também estão registrados no repositório.
- **Como executar os testes básicos com os fontes reais do repo, sem suíte ampla:**
  ```sh
  cd PianoLivreAndroid
  mkdir -p build/c2a-basic
  javac -encoding UTF-8 -d build/c2a-basic \
      app/src/main/java/br/org/pianolivre/MidiTakeRecorder.java \
      app/src/main/java/br/org/pianolivre/CreationScoreTranscriber.java \
      tests/CreationScoreTranscriberTest.java
  java -cp build/c2a-basic br.org.pianolivre.CreationScoreTranscriberTest
  ```
  *O comando acima é uma instrução de teste reprodutível; não declarar sua execução efetiva neste registro.*

## Contrato do motor

**Entrada:** lista de `MidiTakeRecorder.Event`, segmentos `MidiTakeRecorder.Segment`, título da composição e objeto `Options`, sem escrita de arquivos nem chamada à API MIDI/Android. Eventos preservam `order`, `epoch`, `timeNs`, status, canal, velocity e indicador de encerramento sintético. A lista é copiada para ordenação e **não é modificada**. `Options.defaults()`: 100 BPM, 4/4, grade 1/16, duas pautas sugeridas a partir dos ataques (não identifica mãos). Oferece 2/4, 3/4, 4/4, 6/8; grades 1/8, 1/16 e 1/32; nota-limiar de separação e sobrescritas por `sourceId` estável.

**Saída `Result`:** MusicXML interno em string, `List<WrittenNote>` com associação estável ao Note On original, começo/fim quantizados, pitch, velocity, canal, pauta/voz e timestamps originais; avisos não fatais; quantidade de compassos. Nada é salvo em C2-A.

**Interpretação musical atual:**
1. Emparelha Note On/Off (inclusive Note On velocity zero) por canal e altura. Reataques encerram nota anterior e deixam aviso; CC120/123 e eventos sintéticos encerram fisicamente as notas necessárias. Pedal CC64 **não** estende durações escritas. Note Off órfão provoca aviso; nota sem Off é encerrada no fim dos segmentos com aviso, sem apagar a origem.
2. Remove os intervalos em que a gravação C1 foi pausada para calcular a linha de tempo musical, usando busca binária por segmento. O início musical padrão é o primeiro ataque; os timestamps originais permanecem nos objetos de saída.
3. Aplica grade rítmica determinística a início/fim; garante duração mínima de uma unidade de grade e mantém cada ataque com sua identidade, mesmo que o arredondamento aproxime ataques. **Limitação:** não é transcrição expressiva de rubato nem reconhecimento automático de quiálteras, anacruses ou tonalidades.
4. Sugere duas pautas, usando separação de registro e agrupamento de acordes compactos no mesmo canal/instante. **Não infere qual mão física tocou**. IDs de eventos são independentes da pauta/quantização e permitem sobrescritas explícitas em blocos posteriores.
5. Distribui notas simultâneas de durações coincidentes como acordes; sobreposições de durações diferentes em vozes independentes. Escreve pausas, tipos rítmicos, pontos, `<backup>`, alterações cromáticas, compassos e `<tie>`/`<tied>` quando uma nota atravessa barras ou é fragmentada.
6. Limites explícitos iniciais: **8.000 notas, 1.024 compassos, 8 milhões de caracteres MusicXML, 24 vozes por pauta**. O erro é explícito e recuperável na futura interface; jamais truncar notas silenciosamente. Esses limites serão reavaliados quando houver ensaios de corpus representativo/OSMD e Android.

## Testes JVM básicos e resultado
O teste dirigido cobre construção XML parseável, identidade estável, melodia, durações em 100 BPM, escape seguro, duas pautas, output determinístico, mudança de pauta sem transposição, 3/4 e 6/8, acorde, altura simultânea em canais diferentes, CC64, nota atravessando compasso com ligadura, pausa removida da linha musical, finalização sintética, sobreposição em vozes, reataque, casos inválidos e parâmetros inválidos.

**Evidência local:** `CREATION_C2_A_BASIC_PASS checks=48`; compilação `javac` e teste DOM aprovados usando contrato local de dados MIDI. **Ainda não se demonstrou funcionamento no `Score.read` real, OSMD, pacote Android ou aparelho físico.**

## Limitações e próximos passos
- **C2-B:** prévia visual, `Score`/OSMD, ajuste por toque da pauta/nota/acorde/compassos e testes de interface. Deve exercitar XML gerado contra parser e renderizador reais antes de habilitar visualização.
- **C2-C:** persistência por derivação versionada e backup seguro; **não salvar derivação enquanto backup não a incluir**; preservar `.plc` canônico e backups v1/v2/v3.
- **C2-D:** validações completas somente se a regra de testes do usuário mudar e/ou houver política de CI aprovada compatível. Nenhum gate existente foi desligado nem configurado artificialmente para aprovação.
- A Release 0.9.60 **não depende** desta branch e não deve incorporar C2-A por engano.
