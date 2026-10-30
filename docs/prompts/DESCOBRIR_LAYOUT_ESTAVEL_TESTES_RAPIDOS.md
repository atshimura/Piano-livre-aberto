# PIANO LIVRE — REFORMULAÇÃO DO “DESCOBRIR” + OTIMIZAÇÃO DOS TESTES RÁPIDOS

## Missão

Implementar uma reformulação da tela **Laboratório Musical → Descobrir** do Piano Livre, com foco em:

1. **estabilidade visual da notação**;
2. **melhor separação entre pauta e informações escritas**;
3. **qualidade profissional do desenho das pautas e claves**, no mesmo nível visual do modo **Estudar**;
4. **melhoria da usabilidade pedagógica**;
5. **adoção do fluxo de testes rápidos** aprovado pelo mantenedor para acelerar iterações de interface, sem enfraquecer a validação completa antes do merge/publicação.

Esta entrega deve ser executada **a partir da próxima tarefa de desenvolvimento**, conforme orientação expressa do usuário.

---

## 1. Repositório

**Repositório oficial:**  
https://github.com/atshimura/Piano-livre-aberto

**Branch principal atual:**  
`main`

**Commit publicado da beta 0.9.52:**  
`f91c27853be576a353628e220d4e716d02507bed`

**Ajustes recentes da Home:**  
PR #76 — ainda separado da `main`, sem merge/release no momento desta solicitação.

**Issue obrigatória a implementar nesta entrega ou no início dela:**  
Issue #77 — aceleração dos testes Android com fluxo rápido + fluxo completo.

---

## 2. Preparação obrigatória

Antes de modificar qualquer código:

1. Ler `AGENTS.md`.
2. Ler:
   - `docs/45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md`
   - `docs/57-INVENTARIO-FUNCIONAL-V1.md`
   - `docs/08-DECISOES.md`
   - `docs/10-CONTINUIDADE.md`
   - documentação da publicação beta e da continuidade da V1
3. Ler a **issue #77** e implementar o que for necessário para que esta entrega já use o novo fluxo de validação rápida.
4. Inspecionar a implementação atual do Descobrir, especialmente:
   - `MusicLabDiscoveryScreen.java`
   - `DiscoveryStaffView.java`
   - `DiscoveryScoreBuilder.java`
   - `MusicLabScreen.java`
   - eventuais testes do Descobrir e do emulador
5. Confirmar o estado atual da `main` e do PR #76 antes de abrir a nova branch.

Se o PR #76 tiver sido integrado antes do início efetivo desta tarefa, partir da `main` atualizada.  
Se não tiver sido integrado, partir da `main` e manter esta entrega independente, salvo necessidade técnica comprovada.

---

## 3. Branch e PR

Criar uma branch específica:

`feat/descobrir-layout-estavel-e-testes-rapidos`

Ao final, abrir um **PR para a `main`**.

Não fazer merge direto na `main`.  
Não publicar Release oficial nesta etapa.

---

## 4. Objetivo funcional da nova tela Descobrir

A nova tela Descobrir deve deixar de exibir as informações escritas sobre a própria pauta e passar a organizar o conteúdo da seguinte forma:

### Estrutura geral

- **Barra superior:** manter a organização já existente do Laboratório.
- **Área principal abaixo da barra:**
  - **3/4 à esquerda:** área da notação musical;
  - **1/4 à direita:** área de informações escritas.

### Regra visual geral

A pauta deve se tornar a referência visual principal e estável do Descobrir.

**As pautas ficam estáveis; as notas mudam.**

---

## 5. Layout obrigatório

### 5.1. Área esquerda — pautas

A área esquerda deverá:

- ocupar aproximadamente **75% da largura**;
- exibir **sempre duas pautas vazias**, mesmo antes de tocar qualquer nota;
- mostrar:
  - **clave de sol** na pauta superior;
  - **clave de fá** na pauta inferior;
- utilizar desenho **profissional e musicalmente correto** das pautas, claves, linhas suplementares, acidentes, cabeças de nota e demais elementos gráficos.

### Regra obrigatória de fidelidade visual

A renderização das pautas deve ter a **mesma linguagem visual e qualidade profissional do modo Estudar**.

Não aceitar uma solução esquemática grosseira, simplificada ou visualmente inferior.

A pauta não deve parecer um desenho auxiliar improvisado.

---

### 5.2. Área direita — informações escritas

A área direita deverá:

- ocupar aproximadamente **25% da largura**;
- mostrar as informações textuais do resultado;
- poder ter **fundo visual diferente** da área das pautas, desde que harmonize com o restante do aplicativo.

### Informações possíveis

Exibir, conforme o caso:

- nome da nota em português;
- nome internacional;
- oitava;
- cifra;
- MIDI;
- nome do acorde;
- notas componentes;
- nome da escala ou arpejo, quando reconhecido;
- detalhes auxiliares já disponíveis pela lógica atual.

### Estado inicial

Quando nada estiver identificado ainda, mostrar nessa área a instrução equivalente a:

`toque uma nota, acorde ou escala musical`

Essa instrução não deve ficar centralizada sobre a pauta como principal superfície da tela; o foco visual deve permanecer na estrutura com pautas já visíveis.

---

## 6. Comportamento da pauta

### 6.1. Tamanho estável por padrão

Na maioria dos casos, a pauta **não deve mudar de tamanho** ao tocar notas comuns.

Isso significa que:

- uma nota isolada não deve ampliar a pauta;
- acordes comuns não devem redimensionar a pauta;
- pequenas sequências não devem provocar mudanças constantes de escala;
- tocar Dó4, Mi4, Sol4 etc. deve manter a geometria estável.

### 6.2. Ajustes somente quando realmente necessários

Como o teclado pode cobrir grande extensão, haverá casos em que a pauta precisará se adaptar.

Implementar uma estratégia de ajuste **excepcional**, e não contínua.

#### Requisitos

- usar linhas suplementares normalmente para notas fora da pauta;
- só ajustar a escala/altura útil quando a extensão realmente exigir;
- evitar redimensionamento a cada nota;
- evitar efeito visual de “piscando” ou “respirando” de tamanho.

### Sugestão obrigatória de abordagem

Implementar **faixas ou níveis estáveis de enquadramento**, com histerese suficiente para evitar mudanças bruscas. Por exemplo:

- enquadramento padrão;
- enquadramento ampliado para extensão maior;
- enquadramento extremo para notas muito graves/agudas.

Não é obrigatório usar exatamente três níveis, mas é obrigatório evitar redimensionamento contínuo nota a nota.

---

## 7. Comportamento das notas

### 7.1. Nota isolada

Quando o usuário tocar uma nota isolada:

- ela deve aparecer corretamente na pauta;
- com posição vertical musicalmente correta;
- com qualidade gráfica equivalente ao Estudar;
- preferencialmente em posição horizontal estável/central ou em posição definida pelo layout.

### 7.2. Acordes

Quando o usuário tocar acorde:

- as notas devem aparecer simultaneamente;
- com empilhamento musicalmente correto;
- preservando legibilidade;
- sem distorcer o tamanho geral das pautas.

### 7.3. Sequências, escalas e arpejos

Quando a entrada for uma sequência:

- as notas podem avançar horizontalmente na área esquerda;
- sem que as cabeças de nota fiquem diminuindo excessivamente;
- sem comprimir a pauta de maneira desproporcional;
- mantendo legibilidade.

A análise de sequência deve continuar funcionando como já aprovado.

---

## 8. Persistência visual do último resultado

Implementar a melhoria pedagógica aprovada:

**o último resultado reconhecido deve permanecer visível até ser substituído por um novo resultado.**

### Requisitos

- a nota, acorde ou sequência reconhecida não deve desaparecer imediatamente ao soltar a tecla;
- o estudante deve poder observar a posição na pauta com calma;
- uma nova entrada substitui o resultado anterior;
- se necessário, diferenciar visualmente:
  - entrada atualmente ativa,
  - último resultado mantido.

Essa diferenciação pode ser sutil, mas deve permanecer clara e elegante.

---

## 9. Continuidade da identificação imediata

Preservar o comportamento aprovado anteriormente:

- notas isoladas devem aparecer imediatamente;
- o analisador de frase/escala continua em paralelo;
- o reconhecimento de acordes, escalas e arpejos não deve regredir;
- não perder a integração com a sessão MIDI existente.

---

## 10. Fundos diferentes

Foi aprovado que:

- a área das pautas;
- e a área das informações escritas

**podem ter fundos diferentes**.

### Regras

- a diferenciação deve ajudar a leitura e a organização;
- não usar contraste agressivo ou aparência desconexa;
- manter consistência com a identidade visual do Piano Livre;
- preservar acessibilidade e legibilidade.

---

## 11. Exigência de qualidade gráfica musical

### Obrigatório

Garantir que o desenho das pautas, especialmente:

- clave de sol;
- clave de fá;
- linhas da pauta;
- linhas suplementares;
- notas e acidentes;

tenha aparência **profissional**, equivalente ao padrão do modo Estudar.

### Isso implica

- não usar ícones pobres ou desenhos simplificados de baixa fidelidade;
- não aceitar uma pauta meramente esquemática se isso piorar a qualidade;
- reutilizar, adaptar ou alinhar a tecnologia de renderização já usada no Estudar sempre que isso preservar ou elevar a qualidade.

Se houver necessidade de reorganizar o uso do OpenSheetMusicDisplay ou de seus contêineres para garantir esse resultado, fazê-lo.

---

## 12. Responsividade

Validar a nova organização no mínimo em:

- tablet em paisagem;
- celular em paisagem.

### Requisitos

- o painel direito deve continuar utilizável;
- a área esquerda deve preservar protagonismo das pautas;
- os textos do painel direito devem continuar legíveis;
- a divisão 3/4 × 1/4 pode sofrer pequenas adaptações responsivas justificadas;
- não esmagar as pautas em telas pequenas.

Em casos extremos de largura reduzida, admitir pequena adaptação proporcional, mas preservar a lógica:
**pautas predominantes à esquerda, informações à direita**.

---

## 13. Testes rápidos e completos (Issue #77)

Esta entrega deve ser a primeira a adotar o princípio aprovado pelo mantenedor:

### 13.1. Validação rápida durante o desenvolvimento

Criar/usar um fluxo rápido para iterações visuais, com objetivo de reduzir o tempo de feedback.

Esse fluxo deve:

- compilar o necessário;
- iniciar emulador;
- validar a tela Descobrir;
- gerar capturas reais;
- exercitar o layout principal;
- não fingir cobertura completa de regressão.

### 13.2. Validação completa antes do encerramento do PR

Antes de considerar a entrega concluída, continuar exigindo:

- build completo;
- testes automatizados relevantes;
- regressões do Laboratório/Descobrir;
- regressões musicais e MIDI pertinentes;
- testes completos de emulador exigidos pelo projeto.

### Regra obrigatória

**O fluxo rápido não substitui o fluxo completo para merge/publicação.**

---

## 14. Testes funcionais obrigatórios

Validar no mínimo:

1. abertura do Laboratório Musical;
2. entrada em Descobrir;
3. exibição das duas pautas vazias com claves corretas;
4. instrução inicial no painel direito;
5. exibição de nota isolada;
6. exibição de acorde;
7. exibição de sequência;
8. persistência visual do último resultado;
9. coexistência da resposta imediata com o analisador de sequência;
10. comportamento das pautas em notas comuns;
11. ajuste excepcional em notas muito graves/agudas;
12. ausência de redimensionamento contínuo indesejado;
13. legibilidade do painel direito;
14. aparência profissional das claves e pautas;
15. preservação da sessão MIDI do Descobrir.

---

## 15. Evidências visuais obrigatórias

Produzir capturas reais da nova tela, incluindo pelo menos:

- estado inicial sem nota;
- nota isolada;
- acorde;
- sequência curta;
- situação com nota grave/extrema;
- situação com nota aguda/extrema;
- tablet em paisagem;
- celular em paisagem.

As capturas devem demonstrar claramente:

- divisão entre pauta e painel textual;
- pautas estáveis;
- claves bem desenhadas;
- persistência do último resultado;
- fundos diferenciados, se aplicados.

---

## 16. Documentação

Criar um documento específico, por exemplo:

`docs/75-DESCOBRIR-LAYOUT-ESTAVEL-E-PAUTA-PROFISSIONAL.md`

Registrar:

- objetivo da mudança;
- problema do layout anterior;
- nova organização 3/4 + 1/4;
- decisão de manter pautas estáveis;
- estratégia para extensão do teclado;
- persistência do último resultado;
- adoção do fluxo rápido de testes;
- arquivos alterados;
- testes executados;
- capturas e limitações.

Atualizar também a documentação/índice pertinentes.

---

## 17. O que não deve ser alterado

Não alterar desnecessariamente:

- a tela Estudar;
- a Home, salvo dependência técnica inevitável;
- o fluxo de publicação beta;
- a assinatura permanente;
- os dados do usuário;
- o repositório histórico privado.

Não degradar funcionalidades já aprovadas do Laboratório.

Não remover a futura base de Aprender/Praticar que permanece fora do escopo público atual.

---

## 18. Entrega

Ao concluir:

1. revisar o código;
2. executar validação rápida durante os ajustes;
3. executar a validação completa final;
4. corrigir falhas;
5. registrar documentação;
6. abrir PR para `main`;
7. informar:
   - link do PR,
   - commit final,
   - testes rápidos e completos executados,
   - capturas,
   - limitações,
   - próximos passos.

---

## 19. Critérios de aceite

A entrega só poderá ser considerada concluída quando:

- a área principal do Descobrir estiver dividida em painel de pautas à esquerda e painel textual à direita;
- as duas pautas vazias com clave de sol e fá aparecerem desde o início;
- a pauta tiver aparência profissional compatível com o modo Estudar;
- as informações escritas deixarem de sobrepor a pauta;
- o tamanho das pautas permanecer estável na maioria das notas;
- o ajuste de escala ocorrer apenas quando necessário para extensões maiores;
- o último resultado reconhecido permanecer visível até ser substituído;
- notas, acordes e sequências continuarem funcionando;
- houver evidências visuais reais;
- houver PR aberto e pronto para revisão;
- o novo fluxo rápido de validação tiver sido adotado sem remover a suíte completa obrigatória.

---

## 20. Autonomia

Executar toda a tarefa com autonomia técnica.

Não parar após apenas planejar ou criar a branch.

Implementar efetivamente:
- a nova organização do Descobrir;
- a estabilidade da pauta;
- a persistência do último resultado;
- a qualidade profissional das claves e pautas;
- e a adoção do fluxo rápido de testes.

Solicitar intervenção somente diante de impedimento real.

**Resultado esperado:**  
Tela Descobrir reformulada, mais didática e estável, com pauta profissional, painel textual lateral e PR pronto para revisão.