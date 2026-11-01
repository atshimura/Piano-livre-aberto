# Implementação E — Bloco E-B (E2 + E3): pauta grande e notas alinhadas

**Estado:** duas alterações funcionais **implementadas no PR #91, ainda sem integração na main**.  
**Etapa coberta:** E2 (aumentar pauta) + E3 (início horizontal comum). **E4/E-C e E5–E6/E-D pendentes.**  
**Base:** após E-A ([documento 97](97-IMPLEMENTACAO-E-E-A-BASELINE.md)); [escopo 95](95-IMPLEMENTACAO-E-DESCOBRIR-PAUTA.md); [blocos 96](96-IMPLEMENTACAO-E-QUATRO-BLOCOS.md); [issue #89](https://github.com/atshimura/Piano-livre-aberto/issues/89).  
**Branch única de trabalho:** `feat/implementacao-e-descobrir-pauta`, [PR funcional draft #91](https://github.com/atshimura/Piano-livre-aberto/pull/91).  
**Referência funcional testada:** commit `180b1442994021a04400046558a76843077caa5d`, Action `build` [#41316106385](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41316106385) **sucesso**. A documentação posterior criará outro SHA; não atribuir a essa revisão automaticamente os checks do SHA mais novo.

## 1. Duas melhorias implementadas, somente na pauta

### E2 — Ampliação proporcional da pauta padrão

No arquivo `PianoLivreAndroid/app/src/main/assets/discovery-notation.js`:
- A câmera **normal** passou de `{top:95,bass:225,height:360}` para `{top:82,bass:185,height:284}`, preservando o desenho nativo de **duas pautas Sol/Fá** e as relações exatas de altura, glifos OSMD/VexFlow, hastes, acidentes e linhas suplementares.
- O limite de escala para a câmera normal passou a **2,1**; faixas de grande extensão **tier 1/2** permaneceram com os parâmetros anteriores, sem antecipar a E4.
- **Nada** foi alterado na barra superior, textos/painel informativo, largura 75%/25%, MIDI, sessão musical, Estudar ou Criação.

### E3 — Início uniforme depois das claves

- Removida a regra antiga de centralização da nota isolada `width*.46`.
- A coluna inicial é definida a partir da largura real das duas **claves originais**, preservando a faixa fixa/clonada durante a rolagem e incluindo margem suficiente para bemóis e outros acidentes.
- **Nota única e acorde**: alinhamento de cada pauta com o mesmo ponto de ataque; **sequência**: deslocamento uniforme dos eventos já espaçados pelo OSMD (sem reordenar tempo nem comprimir espaçamento).
- **Notas reais** são distinguidas de pausas invisíveis: neste MusicXML transitório todas as notas têm cabeças ovais de semínima, enquanto pausas auxiliares têm caixa alta/estreita, apesar de ambas usarem `.vf-notehead`. O filtro de geometria serve apenas ao posicionamento da **pauta Descobrir**; futuras durações ou formatos devem exigir novo teste dessa distinção.
- Cabeças, hastes, acidentes aninhados e grupos irmãos de linhas suplementares seguem juntos. A clave não é redesenhada manualmente.

## 2. Ensaios dirigidos e correções

O teste específico `PianoLivreAndroid/tests/DiscoveryEVisualBrowserTest.js` usa o OSMD offline original com dois viewports de referência (680×350 e 1024×600 CSS px), conferindo:
- aumento gráfico real de pelo menos 18% perante E-A;
- duas pautas com cinco linhas e escala estável entre vazio, nota, acorde e sequência;
- mesma coluna inicial de **notas reais** em Dó4 isolado, tríade Dó-Mi-Sol, primeiro evento da sequência, Dó2, Dó6 e Mi♭4;
- Dó4 e suas linhas suplementares trasladados juntos, preservação de acorde, glifos e limites verticais;
- bemol codificado no MusicXML e renderizado no PNG; o VexFlow não exige uma classe SVG visível `.vf-accidental`, portanto o teste verifica a cabeça engravada depois da faixa de claves e a imagem é inspecionada;
- limites simultâneos MIDI 21+108 continuam presentes no tier ampliado existente (reversibilidade só em E4).

**Rodadas e correções:**
1. A primeira execução detectou que um bemol ficava próximo demais à faixa fixa de claves. A margem horizontal foi ampliada, sem reduzir a pauta.
2. A segunda revelou erro da inspeção de classe SVG para bemol; a imagem mostrava o símbolo visível, mas a classe consultada não possuía tinta; a asserção foi substituída por verificação geométrica apropriada, mantendo o fixture de Mi♭4.
3. A análise do baseline mostrou que `.vf-notehead` também contém pausas invisíveis da outra pauta. O posicionamento e o teste foram corrigidos para considerar somente as cabeças ovais **de notas reais**.
4. No commit `180b144`, o workflow `build`/Chromium terminou em **sucesso**. A coleção `implementacao-e-visual-eb` contém **16 PNGs (8 estados × 2 viewports)** mais `measurements.json`, conferidos por medidas; capturas de Dó4, acorde, sequência, bemol e extremos foram examinadas.

### Comparação com E-A — medidas reais de Chromium

| Medida | Celular 680×350 | Tablet 1024×600 |
| --- | ---: | ---: |
| Espaçamento padrão entre linhas na E-A | 9,28 px CSS | 16,22 px CSS |
| Espaçamento padrão na E-B | **11,76 px CSS** | **20,56 px CSS** |
| Ganho geométrico | **aprox. 26,7%** | **aprox. 26,8%** |
| Centro da primeira cabeça de nota isolada Dó4 | **116,30 px** | **203,35 px** |
| Primeira nota de sequência / primeira cabeça de acorde | **mesma coluna, sem divergência medida** | **mesma coluna, sem divergência medida** |
| Largura da faixa fixa de claves | 69,25 px | 121,09 px |

A medição demonstra **aumento real do tamanho da gravura**, não do contêiner. O início de uma nota fica claramente após o trilho de claves, em vez de no centro do espaço branco. Nenhum corte vertical foi encontrado nas dezesseis situações do teste de navegador; isso não é prova de todos os dispositivos do mundo.

## 3. Arquivos alterados e validação

Produção: apenas `PianoLivreAndroid/app/src/main/assets/discovery-notation.js`.

Testes: `PianoLivreAndroid/tests/DiscoveryNotationBrowserTest.js` (câmera de resize atualizada), `PianoLivreAndroid/tests/DiscoveryEVisualBrowserTest.js` (novo), mantendo os testes e fixtures do E-A; `.github/workflows/build.yml` adiciona a regressão visual e guarda o artefato com métricas/PNG. Outros arquivos próprios do E-A no PR permanecem, sem mudança funcional desta etapa.

**Execução confirmada:** [GitHub Actions build #41316106385](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41316106385) com conclusão **success** no código `180b144`, incluindo asserções de geometria e testes existentes do build. O workflow Android do código correspondente [#41316106584](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41316106584) iniciou `debug-apk`, `ui-fast` e `emulator-suite`; conferir estado efetivo dos últimos jobs antes de atribuir sucesso. Uma alteração documental posterior pode iniciar outra rodada de checks.

**Não feitos/pendentes nesta entrega:** validação USB física no Yamaha P-45, testes físicos no Realme 7 5G e Lenovo Tab M9, ajuste reversível de extremos (E4), fechamento da suíte da revisão final, integração, assinatura permanente de distribuição e nova Release. O APK debug de PR não é versão distribuível.

## 4. Próximos passos inequívocos

- **Próximo bloco: E-C / etapa E4 — Escala Inteligente Reversível.** Substituir a progressão monotônica de `DiscoveryStaffView.setNotes` (`tier=Math.max(...)`) pelo enquadramento baseado no conteúdo visível, restaurando o **tamanho padrão maior da E2** quando uma nova entrada comum couber. Conservar o último resultado e proteger notas extremas simultâneas.
- Depois, E-D deverá validar o **SHA final** com CI completo e emulador, atualizar documentação/versão, e só integrar após os gates exigidos. Não reutilizar o sucesso da E-B como aprovação de E-C/E-D.
- O usuário pode solicitar: **“Implemente o Bloco E-C da Implementação E.”** Não abrir um PR novo: continuar o [#91](https://github.com/atshimura/Piano-livre-aberto/pull/91).

**Atenção:** este documento registra um **bloco implementado na branch funcional**; não significa que a versão instalada nos dispositivos tenha mudado.
