# Implementação E — execução E-A (E0 + E1): diagnóstico e baseline

**Estado:** instrumentação do baseline preparada; resultados de CI precisam ser confirmados no SHA do PR funcional.  
**Escopo:** somente **E0 + E1 / Bloco E-A — Diagnóstico Direto**. E-B, E-C e E-D **não foram executados**.  
**Especificação:** [95](95-IMPLEMENTACAO-E-DESCOBRIR-PAUTA.md); [quatro blocos: 96](96-IMPLEMENTACAO-E-QUATRO-BLOCOS.md); [issue #89](https://github.com/atshimura/Piano-livre-aberto/issues/89).  
**PR documental integrado:** [#90](https://github.com/atshimura/Piano-livre-aberto/pull/90), squash `05b2d2d59bbb9877549e757ecf0dad440654d29d`.  
**Branch funcional única:** `feat/implementacao-e-descobrir-pauta`, baseada nesse commit; PR funcional é separado do #90.

## E0 — conferências e escopo

- Lidos `AGENTS.md` e docs 66, 75, 93, 95, 96. Issue #89 preserva três mudanças aprovadas: **aumentar exclusivamente a pauta**, **alinhar notas ao início**, **escala automática reversível** para extremos.
- `main` protegida; após integrar docs #90, a base é `05b2d2d59bbb9877549e757ecf0dad440654d29d`. Na base, `gradle.properties`: `0.9.58-open-beta` / `versionCode=67`, `publishApprovedVersion=0.9.58-open-beta` para aquela beta, **sem nova aprovação para a E**.
- Sem alterações de assinatura, dados, perfil, licença, lógica USB-MIDI, renderizador do Estudar, painel direito ou navegação.
- Políticas: branch/PR; sem acesso ao repositório histórico privado; nenhuma declaração de validação física em Realme 7 5G, Lenovo Tab M9 ou Yamaha P-45.

## E1 — diagnóstico dirigido confirmado por código

### 1. Pauta padrão ainda pequena

Em `PianoLivreAndroid/app/src/main/assets/discovery-notation.js` a câmera base é `{top:95,bass:225,height:360}`, com `scale=Math.min(1.8,Math.max(1,inputHeight-16)/camera.height)`. O desenho é centralizado verticalmente pela sobra de altura. Esse cálculo **não garante ampliação suficiente** da pauta normal em grandes viewports e pode ainda não acomodar uma câmera extrema de 700 unidades em altura reduzida devido ao piso 1. **Hipótese técnica, a medir com SVG real**; não presumir falha efetiva em hardware sem captura.

### 2. Centralização da nota isolada

No mesmo JS, quando `!ordered&&count`, a cabeça musical é deslocada usando `dx=width*.46-(box.x+box.width/2)`. Sequências **não** recebem esse deslocamento. Assim, há duas colunas de referência diferentes para uma nota isolada e o início sequencial. Na etapa E-B, alterar apenas a regra de posição horizontal, preservando as claves fixas, os acidentes e os ledgers.

### 3. Enquadramento extremo que não retorna automaticamente

Em `DiscoveryStaffView.setNotes`, cada nota aplica `tier=Math.max(tier,...)`, de modo que o nível nunca é reduzido enquanto a tela permanecer aberta. A sequência de teste já existente “extremo → comum” confirma **por especificação de teste** que o tier permanece extremo; não confundir isso com a nova regra aprovada. Na etapa E-C, escolher câmera pelo **conteúdo efetivamente exibido**, com retorno automático ao padrão assim que a nova entrada couber.

### 4. Lacuna dos extremos simultâneos

A fixture do navegador cobria MIDI 21, 108, 0 e 127 **separados**, e a jornada Android cobria MIDI 0 e 127 em sucessão. Nenhum dos dois casos garantia antes a captura do par 21+108 **juntos**. E1 adiciona uma fixture simultânea e um estado focado no Android, **sem mudar o renderizador ou a classificação musical**.

## Instrumentação introduzida apenas para E-A

| Arquivo | Mudança |
| --- | --- |
| `PianoLivreAndroid/tests/DiscoveryNotationFixtures.java` | Adiciona entrada MusicXML **simultânea** `[21,108]`, preservando índices anteriores. |
| `PianoLivreAndroid/tests/DiscoveryEBaselineBrowserTest.js` | Renderiza OSMD sem rede em 680×350 e 1024×600 CSS px; coleta screenshot e JSON para vazio, Dó4, sequência, 21+108 simultâneos e Dó4 após enquadramento extremo ainda retido. Mede `scale`, `staffSpace`, `tier`, `firstNoteX`, `railWidth`, envelope de notas/ledgers/stems/claves e clipping. **Diagnóstico, sem impor aceitação do comportamento atual.** |
| `PianoLivreAndroid/app/src/main/java/br/org/pianolivre/DiscoveryLayoutSelfTest.java` | Insere estágio **extremes-together** com dois eventos MIDI sintéticos no mesmo lote e captura de tela em ambos os perfis; a política legada de tier **não é alterada**. |
| `PianoLivreAndroid/tests/emulator_discovery_layout.py` | Passa a esperar **10 estados por perfil, total 20**; imagem `piano-discovery-layout-{phone,tablet}-extremes-together.png`. |
| `.github/workflows/build.yml` | Executa o teste Playwright dirigido após os testes já existentes; anexa arquivo de medidas e PNGs em `implementacao-e-baseline-browser`. |

A instrumentação nativa de emulador já grava `piano-discovery-layout.json` e 10 PNGs por perfil no artefato `PianoLivre-ui-fast-<SHA>`; não criar um segundo driver de inicialização de AVD. O teste Java/Android usa exatamente a tela real `MusicLabDiscoveryScreen` e eventos MIDI **sintéticos**, não hardware físico.

## Referências visuais e resultados a extrair da CI

Os arquivos de referência devem ser associados **ao SHA efetivamente executado**, não a builds antigos:
- **Navegador/OSMD:** artefato `implementacao-e-baseline-browser`, dez PNGs e `measurements.json`; a saída do teste também registra `E_A_BASELINE` com medidas por caso.
- **Emulador Android, captura de interface real:** artefato `PianoLivre-ui-fast-<SHA>`, `piano-discovery-layout.json` e imagens por estágio. Perfis da infraestrutura existente: “phone” 1280×720 e “tablet” 1920×1080 com densidade 240; verificar viewport CSS efetivo da WebView no relatório.
- **Critério de diagnóstico:** relatar tamanho real da pauta, posição da primeira cabeça e distância após claves, clips e aparência das duas notas extremas. **Não transformar possível corte da baseline em “teste aprovado” por omissão.**

### Quadro de evidências

| Verificação | Estado neste registro inicial |
| --- | --- |
| Código/decisões e arquivos alvos | **Inspecionados** |
| `main` com documentação E integrada via #90 | **Confirmada** |
| Fixture 21+108 e teste navegador | **Adicionados**, ainda sujeitos a execução/CI |
| Captação Android 21+108 por telefone/tablet | **Instrumentada**, ainda sujeita a execução/CI |
| PNGs e medições de baseline gerados e inspecionados | **Pendente de execução/inspeção** |
| Alteração visual E-B e câmera E-C | **Não iniciadas** |
| Suíte completa, merge funcional, APK e hardware | **Não pertencem à conclusão de E-A** |

## Retomada e passagem ao E-B

Após confirmar execução das provas, incluir no registro do PR: SHA, links dos jobs e artefatos, medidas reais obtidas e quaisquer falhas. Se uma captura não for acessível ao executor atual, registrar a limitação em vez de assumir que foi inspecionada. **O bloco E-A só está totalmente comprovado após a coleta de referência em ambos os perfis**; o próximo pedido do usuário é **“Implemente o Bloco E-B da Implementação E.”** (pauta maior e notas alinhadas), mas não é necessário aguardar testes físicos.

Não modificar `versionCode`, não integrar o PR funcional e não publicar APK no diagnóstico E-A.
