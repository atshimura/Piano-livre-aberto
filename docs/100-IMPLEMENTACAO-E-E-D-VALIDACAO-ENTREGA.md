# Implementação E — E-D (E5 + E6): validação final e entrega

**Status no momento do commit de preparação:** E5/E6 em execução; não afirmar conclusão, merge ou publicação antes de checar os resultados.  
**Data:** 01/11/2026 · [PR funcional #91](https://github.com/atshimura/Piano-livre-aberto/pull/91) · [issue #89](https://github.com/atshimura/Piano-livre-aberto/issues/89).  
**Branch de trabalho:** `feat/implementacao-e-descobrir-pauta`; base `main` protegida. Não tocar no repositório histórico privado.

## 1. Escopo integral em uma única entrega

- **E2** — a pauta com Sol/Fá cresce ~27% nos viewports de referência (espaço de linha 9,28→11,76 CSS px no telefone e 16,22→20,56 no tablet).
- **E3** — nota isolada, acorde e começo de sequência ficam na mesma coluna após as claves, sem pulo do centro para a esquerda; glifos e ledgers permanecem associados; rolagem/claves fixas preservadas.
- **E4** — câmera selecionada pelo conjunto de notas *visível*, com tiers normal (36–84), ampliado (21–108) e extremo (0–127); nota comum após extremos restaura automaticamente a pauta grande, mas soltar as teclas mantém o último resultado legível.
- **Fora do escopo:** alteração da barra superior, painel direito, análise MIDI, modos Estudar/Criação, persistência, dados, permissões, rede, teclado virtual, zoom manual ou nova dependência.

## 2. Inspeção técnica de E5 e regressões

Arquivos de produção alterados: `PianoLivreAndroid/app/src/main/assets/discovery-notation.js` (câmera normal e coluna inicial), `PianoLivreAndroid/app/src/main/java/br/org/pianolivre/DiscoveryStaffView.java` (câmera reversível) e `PianoLivreAndroid/app/src/main/java/br/org/pianolivre/DiscoveryFraming.java` (função pura nova). Testes adicionais e captura no workflow `build.yml`, `DiscoveryNotationBrowserTest.js`, `DiscoveryEVisualBrowserTest.js`, `DiscoveryEReversibleBrowserTest.js`, `DiscoveryFramingTest.java`, `DiscoveryLayoutSelfTest.java`, `DiscoveryNotationFixtures.java` e `emulator_discovery_layout.py`.

**Evidências já aprovadas de E-C (anteriores ao último commit documental):**
- [build #41318276669](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276669): compilação, testes de notação OSMD/Chromium, 56 checks JVM de enquadramento e 12 capturas de normal→extremo→normal, sucesso. Repetição de seis ciclos por perfil e sem corte vertical nas imagens analisadas.
- [Android #41318276483](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276483): `classify`, `debug-apk` e `ui-fast` sucesso, incluindo 22 capturas da interface (11 estados por perfil). A `emulator-suite` dessa revisão foi cancelada após um commit de documentação; portanto, **não** conta como aprovação integral.
- [E-A](97-IMPLEMENTACAO-E-E-A-BASELINE.md), [E-B](98-IMPLEMENTACAO-E-E-B-PAUTA-ALINHADA.md) e [E-C](99-IMPLEMENTACAO-E-E-C-ENQUADRAMENTO-REVERSIVEL.md) registram baseline, aumento, alinhamento, extremos simultâneos e retorno.

**Condição obrigatória de E5:** obter `build=success` e gate agregador `emulator=success` com `emulator-suite=success` do **mesmo HEAD final** do PR #91. Conferir asserções do Android para 11 estados por perfil (pauta vazia, Dó4, acorde, sequência, extremos simultâneos e retidos, nota normal restaurada) e regressões de atualização de versões anteriores. Inspecionar capturas efetivas, evitar atribuir sucesso de commits antigos a novo SHA, não silenciar falhas nem remover testes. Revisar árvore diff contra `main` e registrar qualquer limitação.

## 3. Versão candidata e segurança da distribuição

Versão candidata para integrar quando aprovada: **0.9.59-open-beta / code68** (última versão pública conferida: 0.9.58-open-beta/code67). Fonte de versão: `PianoLivreAndroid/gradle.properties`. `pianolivre.beta.publishApproved=false`, autorização de publicação anterior permanece explicitamente restrita à versão 0.9.58; **não aprovar/taguear/publicar automaticamente v0.9.59**. O pedido E5/E6 autoriza integrar após os gates e gerar **APK assinado**, não habilitar publicação permanente de Release.

O workflow de push confiável na `main` (nunca PR) usa GitHub Actions Secrets já configurados para assinar **release não depurável** e exigir fingerprint conhecida, mesmo pacote `br.org.pianolivre.opensource` e ausência de permissão Internet. Após merge, inspecionar job, log de assinatura e artefato `PianoLivre-open-release-apk` do **commit de main**; não apresentar o APK de depuração de PR como distribuível nem revelar credenciais. O artefato temporário expira, portanto registro de link de run e SHA é obrigatório. Release durável exige autorização específica posterior e novo gate versionado.

## 4. Protocolo de integração e conferência de E6

1. Concluir checks integrais do HEAD de PR com `build` e `emulator` verdes, inclusive `emulator-suite`. Se falhar, diagnosticar, corrigir em branch e repetir.
2. Marcar o PR como pronto para revisão, sem contornar proteções. Verificar regras de merge, SHA exato e ausência de novos commits externos. Integrar via squash permitido apenas se GitHub aceitar os checks/proteção.
3. Conferir o novo HEAD da `main` e seus próprios workflows, inclusive assinatura permanente da variante release e artefato `PianoLivre-open-release-apk`. **Só** chamar E6 concluído quando o APK assinado do SHA de main tiver sido efetivamente confirmado.
4. Atualizar PR #91 e issue #89 com links, resultados efetivos, problemas corrigidos e limitações; fechar a issue só depois do aceite técnico comprovado. Se falhar qualquer gate, **manter PR aberto**, com falha e próxima ação registradas.

## 5. Validação física e limites

Testes com **Yamaha P-45** (Lá0+Dó8 reais, retorno da pauta e MIDI), **Realme 7 5G** e **Lenovo Tab M9** permanecem pendentes do usuário. Não considerar emulador ou MIDI sintético equivalentes ao hardware real. A versão 1.0 não está concluída. Nenhuma migração de dados nem novo serviço de rede foi introduzido.

**Estado ao criar este arquivo:** registro pré-integração e definição de gates, sem declaração fictícia de execução final. O fechamento factual e o SHA final devem estar no PR e na issue, que permanecem a trilha viva de execução.
