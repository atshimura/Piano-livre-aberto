# 103 — Criação C2-B: prévia de partitura e ajustes temporários

**Data:** 01/11/2026. **Estado:** implementação parcial em branch funcional `feat/criacao-c2-b-previa-ajustes`, dependente da [C2-A](102-CRIACAO-C2-A-MOTOR-IMPLEMENTACAO.md); **não integrada na `main` e não disponível em APK**. **Issue:** [#95](https://github.com/atshimura/Piano-livre-aberto/issues/95). Contrato: [plano 101 no PR #96](https://github.com/atshimura/Piano-livre-aberto/pull/96). Este documento registra código real e limites, não substitui testes no Android.

## O que existe na branch

- **`CreationScoreReview.java`**: sessão de revisão **em memória**, cópia imutável das listas C1, derivação `CreationScoreTranscriber`, parâmetros 100 BPM/4/4/1/16 ajustáveis, corte por altura/sugestão, IDs de nota preservados, seleção individual/chord/intervalo de compassos, movimentação para pauta superior/inferior, histórico limitado de 48 ações, desfazer/refazer e restaurar sugestão. Ao falhar a geração, a edição não substitui o estado anterior.
- **`CreationScorePreviewScreen.java`**: prévia no app a partir de uma composição salva; diálogo de tela ampla, renderização do MusicXML **offline** via parser `Score.read` e `NotationView`/OSMD existente, cálculo e leitura em executor de um thread, mensagens de erro e cancelamento sem tocar nos dados. Controles de BPM, compasso 2/4–6/8, grade 1/8–1/32, duas pautas, limite por altura e seleção textual acessível de notas, acordes e compassos com paginação. Navegação Voltar/Back confirma descarte de ajustes não salvos.
- **`MusicCreationScreen.java`**: nova seção “Partitura” com **Gerar partitura** nos detalhes de gravação **salva e não recuperável**; não aparece para arquivos na Lixeira. Criação C1 e exportação MIDI anteriores não são modificadas.
- **`CreationScoreReviewTest.java`**: cenários dirigidos para IDs, alteração de pauta, acordes, compasso, quantização, desfazer/refazer, seleção, restauração e preservação do original. Não há alterações no workflow CI, no certificado, na versão ou no formato de dados.

## Diferenças deliberadas do escopo completo C2-B

1. **Seleção pelo desenho SVG/OSMD ainda não implementada.** O OSMD atual não oferece uma associação segura dos cabeçotes tocados ao `sourceId` original depois de quantização, polifonia e ligaduras. Por segurança e acessibilidade, a edição é feita pela lista de notas que acompanha a pauta, com opções de acorde, grupo e intervalo de compassos. É uma entrega **parcial da UX C2-B**. Criar bridge de hit-test com mapeamento comprovado nota-fonte → notação e teste de toque real antes de liberar o toque direto no cabeçote.
2. **Não é possível Salvar ou Estudar a partitura ainda.** Esses botões não afirmam sucesso: “Salvar partitura — C2-C” permanece **desabilitado**; a prévia e os ajustes desaparecem ao fechar o diálogo. Persistência, sidecar, backup v4 com preflight/rollback e Estudar pertencem ao C2-C. A gravação C1 original nunca muda.
3. **Não houve confirmação de geometria OSMD em Android real ou emulador.** `Score.read` é chamado no worker de geração, mas não se executou o APK Android nesta etapa.
4. Controles por slider, reinicialização do diálogo e redimensionamento estão sujeitos à futura revisão de usabilidade em celular/tablet; a seleção por lista evita afirmar equivalência com toque no cabeçote.
5. A C2-A ainda tem limitações declaradas (limites de 8000 notas/1024 compassos, redução de rubato, sem quiálteras inferidas) e não foi integrada à main. A C2-B depende dela como base de branch.

## Testes básicos dirigidos efetivamente realizados

**Resultados locais:**
- `CREATION_C2_A_BASIC_PASS checks=48` — recompilação da cópia local do motor da C2-A e do teste de regressão respectivo via `javac` JDK 21.
- `CREATION_C2_B_BASIC_PASS checks=27` — `CreationScoreReviewTest.java` (mesmo corpo do teste commitado na branch) com uma **cópia local equivalente** de `CreationScoreReview`, de `CreationScoreTranscriber` e do contrato `MidiTakeRecorder` reconstruído localmente, compilados em JVM JDK 21. Inclui desfazer/refazer, corte por altura, seleção e preservação da fonte. Resultado não demonstra compilação do exato conjunto de arquivos Android do SHA GitHub.
- Inspeção lexical dos quatro arquivos alterados (`CreationScoreReview`, `CreationScorePreviewScreen`, `MusicCreationScreen`, teste): chaves equilibradas e nenhuma string/comentário aberto no texto verificado.

**Não realizados:** suíte completa, workflow GitHub, gradle Android, testes em emulador, teste de toque de notas OSMD, validação real de `Score.read`/OSMD, teste físico Yamaha/Realme/Lenovo, APK/release. Portanto, **não afirmar que a UI foi validada ou que o C2-B está concluído para integração**.

### Comandos básicos reprodutíveis com arquivos reais, em ambiente Android de desenvolvimento

```sh
cd PianoLivreAndroid
mkdir -p build/c2-basic
javac -encoding UTF-8 -d build/c2-basic \
  app/src/main/java/br/org/pianolivre/MidiTakeRecorder.java \
  app/src/main/java/br/org/pianolivre/CreationScoreTranscriber.java \
  app/src/main/java/br/org/pianolivre/CreationScoreReview.java \
  tests/CreationScoreTranscriberTest.java \
  tests/CreationScoreReviewTest.java
java -cp build/c2-basic br.org.pianolivre.CreationScoreTranscriberTest
java -cp build/c2-basic br.org.pianolivre.CreationScoreReviewTest
```

**Importante:** os comandos acima usam os fontes reais e devem ser executados antes de considerar revisão de código aprovada; esta seção não declara que eles foram executados contra o checkout real.

## Política de CI e integração

O usuário determinou **somente testes básicos em cada implementação; nunca rodar a suíte completa**. A CI do repositório dispara testes amplos automaticamente em qualquer PR de Java: `.github/workflows/build.yml` e `classify_pr_risk.py` (`tier=full`) em `emulator.yml`. **Não abrir PR funcional nesta entrega**, pois isso dispararia o job proibido. **Não** desabilitar o workflow, alterar o classificador ou falsificar checks sem uma decisão apropriada de proteção. A `main` permanece no SHA anterior e a Release 0.9.60 não pode absorver C2-A/B sem autorização/integração.

## Próximas ações técnicas

1. Em ambiente com Android SDK, executar apenas compilação básica/assembleDebug e testes de `CreationScoreReviewTest` com arquivos reais; corrigir incompatibilidades de API e testar `Score.read` com MusicXML derivado. Sem suíte completa.
2. Para concluir integralmente C2-B, implementar seleção direta na pauta com hit-test estável, comprovando tocabilidade e preservação do ID original, sem sacrificar a acessibilidade textual.
3. Resolver a incompatibilidade processual entre a nova regra de testes e gates de PR antes de abrir PR/merge, respeitando a proteção de `main`.
4. C2-C: armazenamento derivado versionado + backup integral com compatibilidade/rollback + Estudar. Não publicar a funcionalidade parcialmente.

**Regra de produto:** nada do fluxo acima altera a fonte MIDI original, mesmo quando o usuário escolhe outra pauta, tempo ou compasso. Nenhuma dependência externa, serviço pago ou telemetria incorporada.
