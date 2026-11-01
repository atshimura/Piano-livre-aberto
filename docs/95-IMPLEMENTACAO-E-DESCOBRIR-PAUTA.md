# Implementação E — Descobrir: pauta ampliada, notas no início e enquadramento reversível

**Situação atual:** E0–E1 diagnóstico concluído e **E2+E3 implementados e validados por Chromium na branch do PR funcional #91**, ainda não integrados à main. **E4 (enquadramento reversível) foi implementado e testado no PR funcional #91; E5–E6 (validação final/entrega) continuam pendentes.** [Registro E-C](99-IMPLEMENTACAO-E-E-C-ENQUADRAMENTO-REVERSIVEL.md). [Relatório E-B](98-IMPLEMENTACAO-E-E-B-PAUTA-ALINHADA.md). A situação histórica da entrega documental inicial permanece registrada no PR #90.  
**Data da decisão:** 31/10/2026. **Origem:** aprovação do usuário, consolidada na [issue #89](https://github.com/atshimura/Piano-livre-aberto/issues/89).  
**Repositório:** atshimura/Piano-livre-aberto, branch estável `main`.  
**Referência de inspeção:** `main` em `47d44eb7db8bf5e9a335f781bd810ba43fb9d11e`; `PianoLivreAndroid/gradle.properties` informa 0.9.58-open-beta/code67 nessa referência. É somente uma fotografia do estado inicial, **não** versão reservada para E.  
**Precedência:** estas três decisões posteriores substituem somente o comportamento de tamanho/posição/retorno descrito em [75](75-DESCOBRIR-LAYOUT-ESTAVEL-E-PAUTA-PROFISSIONAL.md), não seu restante. A identificação musical e a navegação continuam regidas pelos documentos [66](66-LABORATORIO-DESCOBRIR-1.0.md) e [93](93-LABORATORIO-NAVEGACAO-COMPARTILHADA.md).

## 1. Finalidade da tela e escopo imutável

O **Laboratório Musical → Descobrir** permite tocar livremente no piano USB-MIDI e visualizar notas, acordes e sequências em **duas pautas**, com claves de Sol e Fá. O painel direito informa o nome em português/internacional, oitava, cifras e, quando reconhecidos pelo motor, acorde, inversão, escala ou arpejo. Entrada ativa e último resultado conservam a lógica existente; a análise de sequências não é reiniciada por uma mudança visual.

**Somente a pauta da região esquerda muda.** Preservar a mesma barra superior, navegação Voltar/Descobrir/Aprender/Criação (com Aprender indisponível enquanto não implementado), posição e largura relativas dos painéis (~75% de pauta / ~25% de informações), fundo, textos, rolagem independente do painel, dados e todas as ações já existentes. Não introduzir nova configuração, botão de zoom, teclado virtual, gravação, metrônomo, catálogo, serviço, permissão, telemetria ou dependência de rede. Não modificar Estudar, Criação, Home ou o MusicXML original de músicas importadas.

O aumento e a redução automática da pauta tratam **visualização**, nunca reconhecimento de mãos, teoria, alturas MIDI, duração, pedal ou julgamento pedagógico.

## 2. Três mudanças aprovadas — comportamento observável

### E1 — Pauta padrão visualmente maior

- Ampliar as duas pautas musicais (linhas, espaço entre linhas, claves, cabeças, hastes, acidentes e linhas suplementares) de modo proporcional e uniforme **dentro da área branca já existente**.
- Aproveitar melhor a altura disponível, eliminando reservas verticais excessivas quando ambas as pautas e as notas efetivamente exibidas couberem. Manter margem segura para a tinta, distância entre pautas e boa leitura.
- O tamanho padrão é estável ao tocar notas comuns, acordes usuais e sequências curtas. Não esticar apenas as linhas, não ampliar texto do painel direito nem deformar glifos.
- O ganho exato em pixels/porcentagem **não foi medido nem aprovado**; ele será definido e justificado com capturas e geometria real em telefone/tablet paisagem, sem inventar um percentual.

### E2 — Nota única começa no mesmo alinhamento da sequência

- A primeira cabeça de nota isolada fica numa **coluna inicial consistente logo após a faixa fixa das claves**. Acordes usam a mesma coluna, com suas alturas empilhadas corretamente entre as duas pautas.
- A primeira nota de uma sequência também nasce dessa coluna; notas seguintes avançam para a direita, com espaçamento suficiente para alterações/acidentes e linhas suplementares.
- Não centralizar uma nota isolada para, na sequência, deslocá-la bruscamente à esquerda. A mudança de nota→acorde→sequência não deve provocar salto horizontal desnecessário da primeira coluna.
- Em sequências longas, manter scroll horizontal, largura suficiente e claves fixas à esquerda, sem encolher cabeças para fazer tudo caber. A faixa das claves não pode cobrir a primeira nota, acidentes ou linhas suplementares.
- A pauta permanece de Sol/Fá em todos os estados, inclusive sem MIDI.

### E3 — Ajuste automático **reversível** nos registros extremos

- No estado normal e para conteúdo comum, usar a **pauta padrão ampliada** de E1.
- Se o **conteúdo atualmente exibido** ultrapassar o espaço seguro, selecionar temporariamente enquadramento adequado que contenha todas as notas e sua tinta — inclusive linhas suplementares, acidentes, hastes e claves — com escala uniforme, sem deformação.
- Devem caber **ao mesmo tempo** os extremos do piano de 88 teclas: **Lá0/MIDI 21 e Dó8/MIDI 108**. Testar adicionalmente a faixa técnica MIDI 0+127, sem prometer igual legibilidade em todos os dispositivos sem medir o resultado.
- Ao tocar **nova entrada** que caiba no enquadramento normal, **voltar automaticamente ao tamanho padrão ampliado**. A redução não fica travada até sair e reabrir a tela.
- Ao soltar teclas, **não** restaurar prematuramente uma pauta grande se isso cortar o último resultado retido. Reter o resultado legível; recalcular quando a nova entrada efetivamente exibida permitir.
- Evitar pisca-pisca, quadro vazio, efeito de “respiração” ou alternância rápida ao receber várias mensagens Note On de um mesmo acorde. Não incluir botão manual de ajuste: o “opcional” aprovado significa **adaptação automática excepcional**, e não uma nova opção de interface.
- Uma nova nota comum enquanto uma nota extrema continua **visível na mesma entrada ativa** não autoriza reduzir o enquadramento a ponto de cortar a extrema. A transição de escala depende do **conjunto apresentado**, não apenas da última mensagem MIDI.
- O nível normal deve prevalecer sempre que for seguro; o nível extremo só deve ser ativado quando necessário, com retorno oportuno ao padrão.

## 3. Análise do código existente e causas concretas

| Arquivo (caminhos relativos ao projeto Android) | Estado examinado | Alteração planejada |
| --- | --- | --- |
| `app/src/main/java/br/org/pianolivre/MusicLabDiscoveryScreen.java` | layout horizontal com `staff` de peso 3 e informações de peso 1; `showState()` escolhe entre notas pressionadas e resultado persistido | **Preservar** dimensões/controles. Conferir que o renderer recebe exatamente o conjunto **visível**, inclusive prévia e resultado retido, sem mudar a sessão. Ajustar somente se necessário para orquestrar renderização visual atômica. |
| `app/src/main/java/br/org/pianolivre/DiscoveryStaffView.java` | `setNotes()` usa `tier=Math.max(tier, ...)`; uma vez ampliado o enquadramento, não volta ao normal até `resetFraming()` | Remover o nível monotonicamente acumulado, selecionar enquadramento conforme conteúdo visível, reverter ao normal na nova entrada compatível e manter revisão/geração contra callbacks atrasados. |
| `app/src/main/assets/discovery-notation.js` | câmera com alturas de referência 360/510/700; escala calculada do viewport, sem piso de escala 1 (o `Math.max(1,...)` limita apenas a altura antes da divisão), e teto de 1,8; nota isolada/acorde deslocados para `width*.46`; conteúdo rolável e rail fixo de claves | Projetar tamanho padrão maior; medir envelope real de glifos e bounds do viewport; definir posição inicial depois das claves; ajustar a câmera vertical e a escala de maneira proporcional e reversível; evitar cortes em viewports baixos e clones defasados. |
| `app/src/main/assets/discovery.html` | viewport horizontal e rail à esquerda | Preferencialmente **não modificar**. Alterar somente se exigido por posicionamento/alinhamento/corte, sem trocar tema ou alterar a tela ao redor. |
| `app/src/main/java/br/org/pianolivre/DiscoveryScoreBuilder.java` | MusicXML transitório com duas claves e divisão por região para visualização | **Preservar** sem mudanças de semântica; investigar somente se fixture comprovar falha. Clave por registro não equivale à mão física. |
| `app/src/main/java/br/org/pianolivre/MusicDiscoverySession.java` | recebe eventos MIDI e retém/análise resultados | **Não alterar** o motor nem critérios temporais; ajustes de câmera devem ser independentes da lógica de reconhecimento. |

**Falhas/riscos a comprovar** antes de declarar correção: não há ensaio específico, nos fixtures de navegador atuais, do par MIDI 21+108 simultâneo; MIDI 21/108 e 0/127 são testados isoladamente. A escala pode ser inferior a 1 nas câmeras altas; por isso validar a tinta dos extremos em PNG/SVG renderizados, sem inferir sucesso apenas do MusicXML. O E-A adicionou o ensaio simultâneo na [documentação 97](97-IMPLEMENTACAO-E-E-A-BASELINE.md) e coletou referências em ambos os perfis. Os testes atuais de browser têm expectativa de escala numérica antiga que precisará ser atualizada com novas invariantes, não removida.

## 4. Estratégia técnica — desenho da solução

### 4.1. Referência visual e espaço vertical

1. Medir em pixels CSS (e conferir com dp nativos) a área branca realmente disponível, regiões das duas pautas, glifos e margens em celular/tablet paisagem.
2. Definir uma câmera **normal ampliada**, ancorada em Sol e Fá, com espaçamento e margem legíveis. O critério é caber o envelope **inteiro** de ambos os sistemas; preservar as relações musicais e o padrão OSMD/VexFlow já empacotado.
3. Para conteúdo além do normal, aplicar enquadramento amplo/extremo ou escala calculada a partir de *bounds* reais, com margem de segurança. Nunca impor piso de ampliação que resulte em tinta fora do viewport. Escala sempre uniforme nos dois eixos.
4. Se a área física for insuficiente para uma representação confortável dos limites técnicos, priorizar **integridade e ausência de cortes** e registrar a perda de legibilidade; não ocultar nota, linhas suplementares nem afirmar qualidade não validada.
5. Reusar os glifos OSMD, sem desenhar claves artificiais ou alterar a notação do Estudar.

### 4.2. Coluna horizontal estável

1. Determinar largura da faixa fixa a partir das claves gravadas no SVG; reservar uma folga para acidentes/linhas suplementares.
2. Posicionar o evento isolado e o primeiro evento sequencial na **mesma coluna inicial útil**, independente da largura total.
3. Mover o grupo completo de notas/acidentes/linhas suplementares juntos. Manter acordes na mesma coluna por ataque, sem deslocar notas uma a uma.
4. Garantir que o rail clonado não reintroduza elementos móveis sobre a coluna inicial nem masque notas. Testar em ambas as claves.
5. Manter rolagem horizontal e espaçamento das sequências; resize só deve recalcular o alinhamento, não mudar a semântica dos eventos.

### 4.3. Seleção reversível de enquadramento e estabilidade

- Separar **entrada/resultado musical** de **modo de enquadramento gráfico**. Calcular primeiro as notas **visíveis agora** (prévia de tecla pressionada, acorde corrente ou resultado retido).
- Escolher o **menor enquadramento necessário** capaz de conter a tinta musical, com critérios explícitos e testáveis. Se a nova entrada couber no modo normal, restaurar o normal imediatamente ao novo quadro completo.
- Não recalcular a partir de lista vazia em Note Off se o resultado permanece na tela. Não misturar extremos de um resultado histórico que já foi substituído com os da entrada atual.
- Coalescer atualizações visuais do mesmo lote/quadro sem sacrificar a resposta ao toque, e conservar revisão monotônica para descartar renderizações antigas. Não acrescentar janela de acorde arbitrária nem atraso MIDI pedagógico.
- Manter o quadro completo anterior até novo SVG consistente estar pronto e publicá-lo de forma atômica; preservar a política atual de re-render ao mudar viewport.
- Verificar ida e volta repetidas (normal→extremo→normal), redimensionamento/orientação e pausa/retomada, inclusive sequência que cruza o limite de um enquadramento.

### 4.4. Escolha técnica aberta, sem criar decisão de produto

A implementação poderá manter três faixas fixas ou usar cálculo por envelope dentro das faixas existentes. Preferir solução mais simples que **demonstre**, em imagens e testes, ausência de cortes, ampliação real no modo normal e ausência de oscilação. Os números de escala, offsets e margens são **parâmetros técnicos a medir**, não exigências novas do usuário. Não substituir o desenho aprovado por layout alternativo.

## 5. Plano executável de desenvolvimento

| Ordem | Trabalho | Saída/checagem de encerramento |
| --- | --- | --- |
| **E0 — Preparação** | Ler `AGENTS.md`, docs 66/75/93/esta E, issue #89; conferir `main`, PRs recentes, versão e CI; criar branch de código baseada na `main` atual, independente do PR documental | Estado inicial/HEAD e dependências registradas; sem mexer no histórico privado |
| **E1 — Baseline** | Capturar PNGs reais dos estados vazio, nota comum, acorde, sequência, extremos isolados e **par simultâneo** em telefone/tablet; coletar geometria SVG | Medidas de tamanho normal, coluna inicial, envelope, clipping e repro dos problemas; nenhuma hipótese apresentada como teste concluído |
| **E2 — Ampliação** | Ajustar câmera padrão/espacamento/margens nas pautas com OSMD | Duas pautas visivelmente maiores; outras áreas idênticas; sem corte em notas comuns e acordes |
| **E3 — Alinhamento** | Unificar coluna inicial de nota/acorde/primeira nota da sequência, respeitando claves fixas e rolagem | Notas após as claves sem salto horizontal; acordes, acidentes e ledgers alinhados |
| **E4 — Adaptação reversível** | Implementar cálculo por conteúdo exibido, incluindo retorno automático e retenção do último resultado | Normal→extremo→normal passa; 21+108 simultâneo sem cortes; sem pisca, sem reversão indevida no Note Off |
| **E5 — Regressão e otimização** | Testes unitários Java, Playwright/Chromium, UI Android rápida, depois gates completos; corrigir regressões e analisar PNGs | Testes por SHA de commit, logs, evidências e comparação com baseline; não enfraquecer checks |
| **E6 — Entrega** | Atualizar documentação, ADR, changelog, versão **somente se APK mudar**, abrir PR de implementação; revisar CI, integração e distribuição pela política vigente | PR rastreável; merge só com gates obrigatórios aprovados, proteção da main preservada, sem Release automática não autorizada; hardware pendente anotado |

Não considerar E1/E2/E3 isoladas como conclusão. A **Implementação E funcional** somente estará concluída quando as três melhorias coexistirem com regressões aprovadas e evidências. O **presente PR é exclusivamente documental**: não equivale a nenhuma fase de código E1–E6.

## 6. Matriz de ensaios/critério de aceite

| Cenário | Exigência observável |
| --- | --- |
| Abertura sem MIDI | Duas pautas maiores, Sol/Fá corretamente gravadas, painel direito intacto |
| Uma nota Dó4/MIDI 60 | Cabeça na primeira coluna útil após as claves; enquadramento normal grande; texto e pauta corretos |
| Mi4 e Sol4, notas comuns, acordes Dó-Mi-Sol | Escala e origem da pauta estáveis; acordes verticais sem desalinhamento de ledgers/acidentes |
| Uma nota grave/aguda fora da pauta | Linhas suplementares não cortadas; enquadramento ajustado somente se necessário |
| Lá0/MIDI 21 + Dó8/MIDI 108, pressionados juntos | Ambas as notas e sua tinta inteira visíveis, com duas claves legíveis e escala uniforme, nos dois perfis |
| MIDI 0 + 127 simultâneos (teste técnico) | Sem perda de pitch ou tinta silenciosamente cortada; registrar se o tamanho ainda fica pouco confortável |
| Extremo retido após Note Off | Último resultado permanece visível e enquadramento não volta prematuramente |
| Nova nota Dó4 após extremo | Retorno **automático** ao tamanho normal grande; nota posicionada no começo, sem precisar reabrir |
| Extremo sustentado + outra nota comum | Não restaurar tamanho normal enquanto o extremo ainda integra a entrada visível |
| Escala/arpejo de uma oitava | Ordem e início horizontal preservados, sem compressão da pauta; motor musical continua reconhecendo como antes |
| Sequência longa com mudanças de registro | Rolagem horizontal, claves fixas, pitch/ordem e acidentes preservados; sem saltos ou cortes |
| Repetir 10 ciclos normal↔extremo (teste de stress sugerido) | Quadro consistente e sem alternância espúria, vazamentos, chamadas obsoletas ou travamento |
| Resize, background/retorno e reconexão | Pauta e último resultado corretos; recursos MIDI e UI não reativam observações antigas |

**Evidências exigidas:**
- Testes JVM e fixtures de `DiscoveryScoreBuilder`/`MusicDiscoverySession` preservando notas, oitavas, simultaneidade e distinção sequência/acorde.
- `DiscoveryNotationBrowserTest.js` expandido: conferir valores geométricos do **tamanho real** comparados à baseline, primeira coluna igual nos estados, limites de tinta por glifo (não apenas bounding box de noteheads), primeiro/último quadro, extremos simultâneos e resize. Atualizar assertivas ligadas ao tamanho antigo sem reduzir sua cobertura.
- `DiscoveryLayoutSelfTest` e roteiros de emulador `emulator_discovery_layout.py`, `emulator_discovery_components.py` e `emulator_ui_fast.py`: capturas de **telefone e tablet em paisagem**, estados vazio/nota/acorde/sequência/21+108/normal-restaurado, comparação do painel e barra invariáveis. Conferir timestamps/revision dos SVGs.
- Verificação de que uma nota comum passou a ter escala visual maior na pauta de E1; não basta trocar dimensões do contêiner.
- Check rápido pode apoiar iterações, **não substitui** `build` + `emulator` completos para integrar/publicar mudanças Java/JS. Registrar SHA, links de runs, status real, arquivos de evidência, falhas e correções.
- Ensaios físicos **pendentes do usuário**: Yamaha P-45 (extremos simultâneos e retorno), Realme 7 5G e Lenovo Tab M9 (legibilidade e fluidez). Emulador e MIDI sintético não os substituem.

## 7. Riscos, proteções e reversão

- **Tinta extrema cortada:** medir todas as partes do glifo, inclusive ledgers/acidentes/hastes; recalcular câmera com margem real.
- **Sobreposição de pautas ao ampliar:** validar distância e envelopes **de ambas**, não aproximá-las cegamente.
- **Zoom oscilando com acordes/Note Off:** seleção guiada pelo conjunto atualmente apresentado; resultado anterior retido e renderização por revisão.
- **Pauta inicia de novo no centro ou sob a clave:** centralização legada deve ser substituída em todos os caminhos (nota, acorde, prévia, sequência).
- **Regressões no reconhecimento e no Estudar:** não alterar motor MIDI/teoria/renderer de Estudar; manter o OSMD compartilhado como biblioteca, com página Descobrir independente.
- **Performance/WebView:** evitar render extra por evento redundante e manter último quadro válido até o novo estar completo.
- **Proteção de dados:** nenhum esquema/backup migra nesta melhoria; sem serviços externos; respeitar GPL-3.0-only e direitos de partituras.
- **Rollback:** mudanças reversíveis de câmera/posicionamento confinadas aos arquivos do Descobrir; se não houver garantia visual em tamanhos-alvo, não integrar, registrar captura falha e corrigir sem desabilitar asserções.

## 8. Critérios de conclusão e continuidade

**Concluído nesta entrega documental:** decisões da implementação E consolidadas, diagnóstico do código atual, plano de execução, arquivos alvo, matriz de testes, critérios de aceite, riscos e política de integração. Links no índice/ADR/issue devem permitir retomada por outro desenvolvedor.

**Não concluído:** mudança de código, novos PNGs, teste automatizado da E, APK, validação em aparelhos físicos, merge do futuro PR de código e versão 1.0. O planejamento não aumenta `versionCode` nem aprova publicação.

**Próxima ação de desenvolvimento:** iniciar E0/E1 na `main` vigente (sem presumir que o SHA de referência ainda é o atual), criar branch de implementação própria e seguir E2→E6 até concluir as três mudanças juntas. Usar a issue #89 e **este documento como fonte de escopo**, respeitando decisões anteriores que não foram substituídas.


## 9. Plano operacional otimizado — quatro blocos solicitáveis (01/11/2026)

O usuário aprovou organizar as sete etapas técnicas E0–E6 deste documento em **quatro blocos com nomes fixos para retomada independente**. A fonte operacional é [**96 — Implementação E: quatro blocos**](96-IMPLEMENTACAO-E-QUATRO-BLOCOS.md), que especifica entradas, ações, testes direcionados, critérios de saída e comandos prontos para cada bloco:

- **E-A — Diagnóstico Direto** (E0+E1); solicitação: **“Implemente o Bloco E-A da Implementação E.”**
- **E-B — Pauta Grande e Alinhada** (E2+E3); solicitação: **“Implemente o Bloco E-B da Implementação E.”**
- **E-C — Escala Inteligente Reversível** (E4); solicitação: **“Implemente o Bloco E-C da Implementação E.”**
- **E-D — Validação e Entrega** (E5+E6); solicitação: **“Implemente o Bloco E-D da Implementação E.”**

**Meta agregada:** 8–12 horas de esforço técnico, sujeita aos resultados de testes e CI. Trabalhar em **uma branch/PR funcional** com commits por bloco, testes rápidos nos blocos intermediários e validação completa antes de merge. O plano em quatro blocos é reorganização operacional, **não é redução de escopo** das três melhorias nem declaração de execução. [Issue #89](https://github.com/atshimura/Piano-livre-aberto/issues/89).
