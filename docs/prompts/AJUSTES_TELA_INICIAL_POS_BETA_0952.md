# PIANO LIVRE — AJUSTES DA TELA INICIAL APÓS A BETA 0.9.52

## Missão

Aplicar uma nova rodada de ajustes na **tela inicial** do Piano Livre, com base na imagem anotada pelo usuário após a publicação da beta `0.9.52-open-beta`.

Essas mudanças são **correções de layout, proporção e legibilidade**, e devem ser implementadas sobre o estado atual da `main`, preservando o restante das funcionalidades já consolidadas.

**Não tratar esta tarefa como um redesenho completo.**  
Trata-se de um refinamento da tela inicial já implementada.

---

## 1. Repositório

**Repositório oficial:**  
https://github.com/atshimura/Piano-livre-aberto

**Branch principal atual:**  
`main`

**Commit publicado da beta atual:**  
`f91c27853be576a353628e220d4e716d02507bed`

**Release atual:**  
`v0.9.52-open-beta`

---

## 2. Preparação obrigatória

Antes de modificar qualquer arquivo:

1. Ler `AGENTS.md`.
2. Ler a documentação da tela inicial já implementada:
   - `docs/70-TELA-INICIAL-PROMPT-01.md`
3. Ler também, para contexto geral:
   - `docs/00-INDICE.md`
   - `docs/07-ESTADO-E-ROADMAP.md`
   - `docs/08-DECISOES.md`
   - `docs/10-CONTINUIDADE.md`
4. Inspecionar a implementação atual da tela inicial na `main`.
5. Utilizar a **imagem anotada pelo usuário** como referência obrigatória para os ajustes desta tarefa.

Se a imagem anotada ainda não estiver salva no repositório, salvá-la em caminho apropriado, por exemplo:

`docs/references/tela-inicial-ajustes-20261030.png`

e mencioná-la na documentação da entrega.

---

## 3. Branch e PR

Criar uma nova branch de implementação a partir da `main` atualizada:

`feat/ajustes-tela-inicial-pos-beta-0952`

Ao final, abrir um **novo pull request para a `main`**.

Não fazer merge direto na `main`.

---

## 4. Ajustes obrigatórios da tela inicial

Implementar exatamente os ajustes abaixo.

### 4.1. Banner de boas-vindas

Ajustar o banner principal da tela inicial.

#### Requisitos

- **Diminuir a altura do banner em 30%** em relação ao layout atual.
- **Aumentar a imagem/ilustração da direita**, para que ela passe a ocupar melhor parte do espaço vazio do banner.
- Preservar:
  - o texto de boas-vindas,
  - o botão “Começar pela Ode à Alegria”,
  - o botão “Como usar”.

#### Objetivo visual

O banner deve continuar tendo destaque, mas com **menos altura vertical** e **melhor aproveitamento horizontal**, ficando mais equilibrado.

Não deixar uma grande área vazia entre o texto da esquerda e a imagem da direita.

---

### 4.2. Cartões principais da faixa superior

Na faixa de atalhos principais, **remover o cartão “Opções”** e manter apenas:

- Biblioteca Musical
- Laboratório Musical
- Histórico

#### Requisitos

- Aumentar os **três cartões restantes**.
- Aumentar também:
  - fontes,
  - ícones,
  - subtítulos,
  - espaçamentos internos,
  - área útil clicável.
- Redistribuir os três cartões para aproveitar melhor a largura da tela.

#### Regra funcional

O acesso a “Opções” continuará existindo pelo **ícone de engrenagem** no topo.  
Não criar outro botão alternativo para substituir o cartão removido.

---

### 4.3. Reorganização da área “Biblioteca Musical”

A região logo abaixo dos três cartões precisa ser reorganizada para melhorar clareza e espaçamento.

#### Ajustar:

- título “Biblioteca Musical”,
- campo de busca,
- seletor/filtro de categorias,
- botão “Importar partitura”,
- botão “Coleções”.

#### Objetivo

Evitar sensação de aperto visual e melhorar a hierarquia dos elementos.

A organização deve ficar mais limpa e coerente, com melhor alinhamento e distribuição.

#### Regras

- Preservar integralmente as funcionalidades existentes.
- Não remover:
  - busca,
  - filtros,
  - importação,
  - coleções.

Pode reorganizar posição, espaçamento, alinhamento e largura dos controles, desde que a usabilidade melhore.

---

### 4.4. Grade de partituras

A grade de músicas da biblioteca deve mudar de **três colunas para duas colunas**.

#### Requisitos

- Exibir os cartões das partituras em **duas colunas**.
- Aproveitar a largura para deixar os cartões maiores.
- Aumentar as fontes dos cartões.
- Melhorar legibilidade de:
  - título da música,
  - subtítulo/descrição,
  - botões “Estudar” e “Partitura livre”.

#### Objetivo

Tornar os cartões mais fáceis de ler e tocar, especialmente em tablet.

Não reduzir o número de partituras exibidas por erro de paginação ou carregamento.

---

### 4.5. Tipografia e legibilidade

Aplicar ajuste de legibilidade na tela inicial revisada.

#### Aumentar, quando necessário:

- títulos dos cartões principais,
- subtítulos dos cartões principais,
- títulos dos cartões musicais,
- textos auxiliares dos cartões musicais.

Evitar aumento exagerado que provoque truncamentos desnecessários ou quebras ruins.

A intenção é **melhorar leitura**, não inflar a interface.

---

## 5. O que não deve ser alterado

Não alterar nesta tarefa:

- a estrutura da tela Estudar;
- a lógica do USB-MIDI;
- os modos musicais;
- o Laboratório Musical, exceto navegação já existente;
- histórico, perfis, favoritos, coleções ou importação, além de ajustes estritamente visuais/posicionais na tela inicial;
- versão publicada ou processos de assinatura, exceto se for necessário gerar uma nova beta de validação ao final do fluxo normal do projeto.

Não reabrir decisões já consolidadas dos Prompts 2, 3 e 4, salvo se algum ajuste visual da tela inicial exigir pequena adaptação compartilhada claramente justificada.

---

## 6. Responsividade

Testar e ajustar o layout para pelo menos:

- tablet em paisagem;
- celular em paisagem.

A nova organização deve:

- manter o banner proporcional,
- manter os três cartões principais bem distribuídos,
- preservar boa leitura na biblioteca,
- manter a grade de partituras em duas colunas sempre que a largura permitir.

Se em telas menores duas colunas não forem viáveis sem comprometer a leitura, documentar o comportamento responsivo adotado.  
Porém, em tablet, o objetivo é claramente **duas colunas maiores**.

---

## 7. Testes obrigatórios

Executar os testes relevantes após a implementação.

### Validar:

1. abertura da tela inicial;
2. funcionamento do banner;
3. botão “Começar pela Ode à Alegria”;
4. botão “Como usar”;
5. navegação para:
   - Biblioteca Musical,
   - Laboratório Musical,
   - Histórico;
6. funcionamento da engrenagem para Opções;
7. funcionamento da busca;
8. funcionamento dos filtros;
9. funcionamento de Coleções;
10. funcionamento de Importar partitura;
11. abertura das partituras em:
   - Estudar,
   - Partitura livre;
12. responsividade da grade em duas colunas;
13. regressão geral da tela inicial.

Executar também:

- build Android,
- testes automatizados existentes,
- testes de emulador, se aplicável ao fluxo do repositório.

---

## 8. Evidências visuais

Produzir novas capturas reais da tela inicial após os ajustes, em pelo menos:

- tablet em paisagem,
- celular em paisagem.

As capturas devem comprovar:

- banner mais baixo,
- imagem da direita maior,
- apenas três cartões superiores,
- ausência do cartão Opções,
- reorganização da Biblioteca Musical,
- grade em duas colunas,
- fontes maiores e melhor legibilidade.

---

## 9. Documentação

Criar ou atualizar um documento específico da entrega, por exemplo:

`docs/74-AJUSTES-TELA-INICIAL-POS-BETA-0952.md`

Registrar:

- objetivo;
- imagem de referência utilizada;
- ajustes implementados;
- arquivos modificados;
- decisões de responsividade;
- testes executados;
- capturas geradas;
- limitações;
- próximos passos.

Atualizar também os índices e registros necessários do projeto, se aplicável.

---

## 10. Entrega

Ao concluir:

1. revisar o código;
2. executar os testes;
3. corrigir falhas encontradas;
4. registrar evidências;
5. fazer commit na branch;
6. abrir o PR para `main`;
7. informar:
   - link do PR,
   - commit final,
   - testes executados,
   - capturas,
   - limitações.

---

## 11. Critérios de aceite

Esta tarefa só poderá ser considerada concluída quando:

- o banner estiver 30% mais baixo;
- a imagem da direita do banner estiver maior;
- existirem apenas três cartões principais;
- o cartão “Opções” tiver sido removido;
- os três cartões restantes estiverem maiores;
- a área da Biblioteca Musical estiver reorganizada;
- a grade de partituras estiver em duas colunas;
- as fontes dos cartões estiverem maiores e mais legíveis;
- as funcionalidades existentes estiverem preservadas;
- os testes relevantes estiverem aprovados;
- houver capturas reais comprovando o resultado;
- o PR estiver aberto e pronto para revisão.

---

## 12. Autonomia

Executar toda a tarefa com autonomia técnica.

Não parar após apenas salvar a imagem, criar a branch ou fazer um ajuste parcial.

Implementar efetivamente os ajustes, validar, documentar e abrir o PR.

Solicitar intervenção somente diante de impedimento real.

**Resultado esperado:**  
Tela inicial refinada conforme as anotações do usuário, com melhor aproveitamento visual, melhor legibilidade e PR pronto para revisão.