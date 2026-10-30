# PIANO LIVRE — PROMPT 3 DE 4
## Consolidação MIDI, comportamento dinâmico e estabilidade das interfaces

**Repositório oficial:**
https://github.com/atshimura/Piano-livre-aberto

**Prompt 1 — Tela Inicial:**
https://github.com/atshimura/Piano-livre-aberto/pull/71

**Prompt 2 — Tela Estudar:**
https://github.com/atshimura/Piano-livre-aberto/pull/73

**Referências visuais obrigatórias:**
- `docs/references/tela-inicial-aprovada.png`
- `docs/references/tela-estudar-aprovada.png`

**Documentação dos prompts anteriores:**
- `docs/70-TELA-INICIAL-PROMPT-01.md`
- `docs/71-TELA-ESTUDAR-PROMPT-02.md`

## 1. MISSÃO

Concluir a integração funcional entre as novas interfaces Inicial e Estudar, consolidando o reconhecimento e a identificação USB-MIDI, o comportamento dinâmico das barras e a estabilidade das sessões musicais.

Esta entrega deverá preservar integralmente o desenvolvimento dos Prompts 1 e 2.

**Não redesenhar as interfaces nem substituir as soluções já implementadas e aprovadas.**

A implementação deve priorizar:

1. Identificação automática e consistente do instrumento MIDI.
2. Tratamento confiável de conexão, desconexão e reconexão.
3. Gerenciamento compartilhado do estado MIDI entre telas.
4. Consolidação da ocultação e restauração das barras de estudo.
5. Preservação das sessões, configurações e dados.
6. Responsividade e acessibilidade.
7. Testes completos de regressão.
8. Documentação técnica e preparação para o Prompt 4.

O trabalho deve resultar em código funcional e testado, não apenas em planejamento.

## 2. PREPARAÇÃO OBRIGATÓRIA

Antes de modificar o código:

1. Ler integralmente `AGENTS.md`.
2. Ler README, arquitetura, decisões funcionais, roadmap, documentação MIDI, persistência, testes e continuidade.
3. Ler os Prompts 1 e 2 e suas documentações finais.
4. Inspecionar as duas imagens de referência aprovadas.
5. Examinar os PRs #71, #72 e #73.
6. Identificar os componentes compartilhados de interface e conexão MIDI.
7. Conferir os testes e workflows existentes.
8. Verificar o HEAD atual das branches e o estado da `main`.

### Código existente

Inspecionar especialmente:

- `MainActivity.java`
- `PianoScreens.java`
- `HomeMidiIndicator.java`
- `StudyChromeState.java`
- Gerenciamento de sessões MIDI e seus testes.
- Componentes de notação e reprodução.
- Persistência de perfis e configurações.

O Prompt 2 já implementou:

- Cabeçalho superior unificado.
- Modos e seleção de mãos.
- Ocultação das barras durante execução.
- Restauração dos controles por toque.
- Remoção do Teclado guia.
- Remoção dos seletores manuais de renderização.
- Renderização automática com alternativas de recuperação.

**Não implementar novamente essas funcionalidades. Auditar, corrigir e consolidar os comportamentos existentes.**

### Branch

Criar:

`feat/consolidacao-midi-interface-piano-livre`

Utilizar como base a implementação final do Prompt 2, atualmente no PR #73, commit:

`2739bb2223cf7efa0a066ec7fc8380bdcde9430d`

Verificar se houve commits posteriores antes de iniciar.

Abrir PR dependente, com base inicialmente na branch `feat/tela-estudar-piano-livre`.

Não integrar antecipadamente os PRs à `main`.

## 3. BOTÃO USB-MIDI — COMPORTAMENTO DEFINITIVO

O botão USB-MIDI é simultaneamente um controle de gerenciamento e um indicador de conexão.

Deve seguir exatamente as mesmas regras nas telas Inicial e Estudar.

### 3.1. Instrumento desconectado

Exibir:

`USB-MIDI`

Esse deve ser o texto padrão quando nenhum instrumento estiver efetivamente conectado.

### 3.2. Instrumento conectado

Substituir integralmente a legenda USB-MIDI pelo nome real retornado pelo Android.

Exemplo:

`Yamaha P-45`

Exibir esse nome somente quando realmente identificado.

Não fixar o nome Yamaha P-45 no código, pois o aplicativo deve funcionar com instrumentos MIDI compatíveis de diferentes fabricantes.

Não apresentar nomes fictícios.

### 3.3. Dispositivo sem nome identificável

Manter o texto:

`USB-MIDI`

A informação detalhada de que há um dispositivo conectado, mas sem identificação textual, deverá permanecer disponível no gerenciamento ou diagnóstico da conexão.

### 3.4. Ação do botão

O botão deve continuar clicável tanto conectado quanto desconectado.

Ao tocar, permitir acessar os recursos existentes de:

- Seleção do dispositivo MIDI.
- Estado da conexão.
- Informações do instrumento.
- Diagnóstico das portas.
- Mensagens MIDI recebidas, quando essa funcionalidade já estiver disponível.
- Gerenciamento da conexão.

Reutilizar os recursos já existentes, evitando duplicação de telas ou implementação desnecessária.

## 4. ESTADO MIDI COMPARTILHADO

As telas Inicial e Estudar devem refletir o mesmo estado real de conexão.

### Objetivo

Evitar divergências como:

- Tela Inicial apresenta o instrumento conectado, mas Estudar mostra USB-MIDI.
- Uma tela permanece com o nome anterior após desconexão.
- A interface mostra conectado enquanto a abertura da porta falhou.
- O nome do dispositivo muda sem atualização da interface.

### Implementação

Auditar a organização atual e utilizar uma única fonte confiável para os dados da conexão.

Reutilizar a infraestrutura existente sempre que adequada.

A apresentação deverá ser atualizada quando:

1. O aplicativo identificar um dispositivo disponível.
2. A abertura da conexão for iniciada.
3. A conexão for estabelecida.
4. A conexão falhar.
5. O instrumento for desconectado.
6. O usuário selecionar outro dispositivo.
7. A conexão for restabelecida.
8. O aplicativo retornar do segundo plano.
9. O usuário navegar entre as telas.

Disponibilidade física, tentativa de conexão e conexão estabelecida são estados distintos.

Não apresentar o nome como conectado apenas porque o Android encontrou um dispositivo.

Preservar o gerenciamento de portas MIDI de entrada e saída, incluindo dispositivos que exponham somente parte das capacidades previstas.

As atualizações visuais devem ocorrer de forma segura na thread apropriada, sem bloquear a interface.

## 5. CONEXÃO, DESCONEXÃO E RECONEXÃO

Implementar ou aperfeiçoar o tratamento das seguintes situações.

### Cenário A — Conexão com aplicativo aberto

O usuário inicia o aplicativo sem piano conectado e depois conecta o instrumento.

Resultado esperado:

- O Android detecta o dispositivo.
- O mecanismo existente estabelece a conexão conforme suas regras.
- O botão passa a exibir o nome real.
- O reconhecimento MIDI fica disponível.
- Não é necessário reiniciar o aplicativo.

### Cenário B — Aplicativo iniciado com piano conectado

O dispositivo já está conectado antes de abrir o Piano Livre.

Resultado esperado:

- A conexão pode ser identificada corretamente.
- O estado aparece no botão.
- A recepção MIDI funciona sem exigir uma navegação artificial.

### Cenário C — Desconexão inesperada

O cabo é desconectado durante uma sessão de estudo.

Resultado esperado:

- O aplicativo não fecha inesperadamente.
- A sessão musical é interrompida ou pausada de forma segura, segundo as regras existentes.
- Notas pressionadas não permanecem artificialmente ativas.
- Recursos e portas MIDI são liberados adequadamente.
- O indicador retorna a USB-MIDI.
- O usuário não perde desnecessariamente a música ou sua posição.
- O estado de conexão fica claro e acessível.

### Cenário D — Reconexão

O instrumento é conectado novamente.

Resultado esperado:

- Uma nova sessão MIDI válida pode ser estabelecida.
- Mensagens de uma conexão antiga não são aceitas como pertencentes à nova sessão.
- O nome do instrumento reaparece após conexão real.
- O reconhecimento de notas funciona novamente.
- A sessão de estudo não reinicia indevidamente.

### Cenário E — Troca de instrumento

O usuário seleciona outro dispositivo MIDI.

Resultado esperado:

- A conexão anterior é encerrada adequadamente.
- Os recursos anteriores são liberados.
- O novo dispositivo passa a ser a conexão ativa.
- O botão mostra o nome correspondente.
- Não há duplicação de mensagens MIDI.

### Cenário F — Falha ou corrida de conexão

Cobrir abertura assíncrona, desconexão durante a abertura, respostas atrasadas e tentativas sucessivas.

Garantir que callbacks antigos não restabeleçam incorretamente o estado de dispositivos já desconectados.

Reutilizar ou aperfeiçoar os mecanismos existentes de identificação de geração de sessão.

## 6. PRESERVAÇÃO DO RECONHECIMENTO MUSICAL

As alterações MIDI não podem prejudicar:

- Reconhecimento de notas individuais.
- Acordes.
- Notas simultâneas.
- Eventos Note On e Note Off.
- Pedal e controladores suportados.
- Seleção de mão direita.
- Seleção de mão esquerda.
- Ambas as mãos.
- Modo Espera.
- Modo Contínuo.
- Modo Demonstração.
- Calibração existente.
- Feedback visual da partitura.

### Regra pedagógica

No modo Espera, notas extras devem ser indicadas, mas não bloquear indevidamente o avanço.

Não penalizar o tempo de espera do estudante.

Respeitar a janela temporal de acordes conforme as regras de calibração já documentadas.

Não introduzir tolerâncias arbitrárias.

### Demonstração

Preservar o envio MIDI para o instrumento quando suportado.

Garantir encerramento adequado das notas e mensagens de controle ao parar ou desconectar.

Evitar notas presas por eventos perdidos, cancelamento ou troca de sessão.

## 7. BARRAS DINÂMICAS DA TELA ESTUDAR

O Prompt 2 já implementou `StudyChromeState`.

Não substituir esse componente por outro sem justificativa técnica comprovada.

### 7.1. Comportamento aprovado

Na tela normal:

- Barra superior unificada visível.
- Faixa MODO/MÃOS visível.
- Barra inferior visível.

Durante a execução:

- Barra superior e faixa MODO/MÃOS ocultas.
- Barra inferior permanece visível.
- A área da partitura é ampliada.

Ao tocar na partitura:

- Controles superiores reaparecem.
- A interação musical original do toque é preservada.

### 7.2. Consolidação

Auditar os estados e transições existentes.

Verificar:

- Início da reprodução.
- Modo Espera.
- Pausa.
- Parada.
- Retomada.
- Volta ao início.
- Troca de modo.
- Troca de mãos.
- Entrada e saída de Partitura Livre.
- Retorno à Biblioteca.
- Desconexão MIDI.
- Retorno do aplicativo do segundo plano.
- Alteração das dimensões da tela.

As barras não devem piscar, mudar repetidamente de estado ou reaparecer indevidamente.

Não introduzir temporizadores arbitrários de ocultação.

Preservar o comportamento aprovado de revelar os controles mediante interação e recolhê-los em um novo ciclo de execução, salvo correção necessária de inconsistência comprovada.

### 7.3. Toques na partitura

O toque utilizado para revelar os controles não pode impedir:

- Identificação de uma nota.
- Seleção direta de trecho.
- Interações musicais já existentes.
- Reconhecimento da posição musical.

Evitar propagação duplicada ou perda de eventos.

### 7.4. Preservação de estado

Ocultar ou mostrar as barras não pode alterar:

- Beat atual.
- Posição da música.
- Modo selecionado.
- Mão selecionada.
- Velocidade.
- Trecho.
- Loop.
- Instrumento conectado.
- Estado musical da sessão.

## 8. RESPONSIVIDADE E ACESSIBILIDADE

Manter máxima fidelidade às duas imagens aprovadas.

Verificar principalmente:

### Tablets

- Lenovo Tab M9 como referência de uso real futuro.
- Interface horizontal.
- Partitura predominante.
- Barra superior compacta.
- Seletores de mãos e modos acessíveis.
- Texto do instrumento conectado legível.

### Celulares

- Realme 7 5G como referência de validação física futura.
- Orientação horizontal.
- Títulos longos de músicas.
- Nomes longos de instrumentos.
- Menus compactos.
- Controles sem sobreposição.
- Barra inferior sempre acessível.

Tratar nomes longos com estratégias adequadas de largura e truncamento, mantendo o nome completo acessível no gerenciamento ou nas informações de acessibilidade.

Não aumentar novamente o número de barras.

### Critério visual

A área útil da partitura não pode regredir em relação ao Prompt 2 sem justificativa técnica comprovada.

Valores de referência observados no emulador:

| Dispositivo simulado | Normal | Barras ocultas |
|---|---:|---:|
| Celular 1280 × 720 | 417 px | 567 px |
| Tablet 1920 × 1080 | 777 px | 927 px |

Esses valores são referências de regressão nas configurações de teste anteriores, não dimensões absolutas para todos os aparelhos.

Preservar o enquadramento musical e a legibilidade.

## 9. INTEGRIDADE DOS DADOS

Preservar integralmente:

- Biblioteca.
- Partituras importadas.
- Favoritos.
- Coleções.
- Perfis.
- Histórico.
- Preferências musicais.
- Seleção de mãos.
- Modos de estudo.
- Progresso disponível.
- Backup e restauração.
- Compatibilidade de dados anteriores.

Não modificar arquivos MusicXML originais.

Não realizar migrações destrutivas.

Não remover preferências antigas de maneira que comprometa a restauração de backups válidos.

As mudanças de interface não devem provocar recriação de dados ou perda de progresso.

## 10. TESTES OBRIGATÓRIOS

Ampliar os testes existentes sem enfraquecê-los.

### 10.1. MIDI

Testar:

1. Instrumento desconectado.
2. Conexão estabelecida.
3. Dispositivo sem nome.
4. Falha de abertura.
5. Desconexão.
6. Reconexão.
7. Troca de dispositivo.
8. Resposta atrasada de conexão anterior.
9. Sessões MIDI sucessivas.
10. Retorno do segundo plano.
11. Eventos duplicados e encerramento de notas.
12. Estados compartilhados entre Inicial e Estudar.

Utilizar dispositivos simulados e eventos sintéticos quando necessário.

Testar as condições reais de estado, não apenas a função que formata o nome.

### 10.2. Interface

Verificar:

- Mesmo nome MIDI nas duas telas.
- Atualização após desconexão.
- Acesso ao gerenciamento.
- Nome longo.
- Ocultação das barras.
- Restauração por toque.
- Estados de pausa e parada.
- Navegação sem perda de sessão.
- Ausência de controles eliminados.
- Barra inferior permanente.

### 10.3. Motor musical

Manter as suítes de:

- MusicXML.
- OSMD.
- Renderização temporal e fallback.
- Mapeamento gráfico das notas.
- MIDI.
- Acordes.
- Polifonia.
- Modos de aprendizagem.
- Seleção de mãos.
- Trechos.
- Transporte musical.
- Biblioteca e Laboratório Musical.

Não retirar cobertura existente.

### 10.4. Emulador Android

Executar os testes em perfis de celular e tablet.

Produzir capturas reais, incluindo:

- Tela Inicial sem instrumento.
- Tela Estudar sem instrumento.
- Estados conectados simulados de forma verificável, sem apresentá-los como teste físico.
- Tela Estudar com barras visíveis.
- Tela Estudar com barras ocultas.
- Controles restaurados.

Quando a conexão MIDI física não puder ser reproduzida, utilizar testes automatizados adequados e registrar a limitação.

### 10.5. Persistência e atualização

Executar testes de:

- Preservação de preferências.
- Retomada de sessão.
- Backup.
- Restauração.
- Atualização de instalação pública com certificado compatível, quando o ambiente de teste permitir.

Os testes devem ser executados sem dados pessoais reais.

## 11. COMPILAÇÃO E APK

Executar os workflows de build e emulador do GitHub Actions.

Corrigir todas as falhas relacionadas à implementação.

Compilar o APK de validação e disponibilizar seu artefato.

O Prompt 2 utilizou a versão de validação:

`0.9.51-open-beta`, versionCode `60`.

Verificar a versão atual no repositório antes de definir a próxima.

Se essa continuar sendo a versão de origem, preparar a próxima versão de validação como `0.9.52-open-beta`, versionCode `61`, respeitando as regras de versionamento e evitando colisões.

Em PRs, utilizar apenas a assinatura temporária de validação prevista pelo projeto. Não expor nem disponibilizar segredos de assinatura permanente.

**Não publicar Release oficial neste prompt.**

## 12. DOCUMENTAÇÃO

Criar:

`docs/72-CONSOLIDACAO-MIDI-INTERFACES-PROMPT-03.md`

Registrar:

- Objetivo.
- Estado inicial.
- Arquitetura MIDI encontrada.
- Alterações efetuadas.
- Comportamento definitivo do botão USB-MIDI.
- Estados e transições de conexão.
- Tratamento de desconexão e reconexão.
- Comportamento das barras dinâmicas.
- Responsividade.
- Testes executados.
- Resultados e evidências.
- Limitações.
- Próximos passos.

Atualizar também, conforme necessário:

- `AGENTS.md`
- `docs/00-INDICE.md`
- `docs/07-ESTADO-E-ROADMAP.md`
- `docs/08-DECISOES.md`
- `docs/10-CONTINUIDADE.md`
- Histórico de alterações.

Registrar decisões novas sem apagar o histórico das anteriores.

Preservar as duas imagens de referência aprovadas.

## 13. PULL REQUEST

Ao concluir a implementação:

1. Revisar o código modificado.
2. Executar os testes.
3. Corrigir as falhas.
4. Compilar o APK.
5. Registrar o commit final.
6. Abrir o PR de implementação, inicialmente dependente do PR #73.
7. Inserir os resultados e links dos workflows.
8. Disponibilizar as capturas e evidências.
9. Disponibilizar o APK de validação.
10. Documentar limitações e pendências.

Não integrar à `main`.

Não publicar Release.

Não iniciar o Prompt 4 antes do encerramento técnico e da validação automatizada do Prompt 3.

## 14. CRITÉRIOS DE ACEITAÇÃO

O Prompt 3 somente poderá ser declarado tecnicamente concluído quando:

- O indicador USB-MIDI utilizar o mesmo estado nas telas Inicial e Estudar.
- Nomes reais de instrumentos forem exibidos corretamente quando disponíveis.
- Conexões e desconexões atualizarem a interface.
- Reconexões não reaproveitarem sessões MIDI inválidas.
- Eventos MIDI antigos não contaminarem novas sessões.
- O reconhecimento musical continuar funcionando.
- A Demonstração não deixar notas presas em encerramentos controláveis.
- As barras da tela Estudar apresentarem transições estáveis.
- Os controles inferiores permanecerem disponíveis.
- Não houver regressão na área da partitura.
- As funcionalidades dos Prompts 1 e 2 forem preservadas.
- Os dados locais permanecerem íntegros.
- Os testes automatizados aplicáveis forem aprovados.
- Os workflows do commit final estiverem aprovados.
- O APK de validação estiver disponível.
- A documentação estiver completa.
- O PR estiver pronto para revisão.

Os testes físicos no Yamaha P-45, Lenovo Tab M9 e Realme 7 5G permanecerão identificados como pendentes quando não realizados.

Não confundir testes MIDI sintéticos com validação física.

## 15. AUTONOMIA

Executar integralmente o trabalho sem solicitar aprovação para decisões técnicas rotineiras.

Não interromper após apresentar planejamento ou realizar apenas a primeira correção.

Prosseguir até concluir implementação, testes, documentação e PR.

Caso algum comportamento já esteja corretamente implementado no Prompt 2, preservar o código e comprovar seu funcionamento, em vez de reimplementá-lo.

Não modificar funcionalidades musicais fora do escopo sem necessidade comprovada.

Não incorporar serviços externos, servidores, telemetria ou serviços pagos.

Não acessar o repositório histórico privado.

### Resultado final esperado

**Prompt 3 tecnicamente concluído, com as duas interfaces integradas ao mesmo estado MIDI, conexão e reconexão confiáveis dentro dos testes disponíveis, comportamento dinâmico consolidado, regressões aprovadas, APK de validação e PR pronto para integração posterior.**

Ao encerrar, apresentar relatório objetivo com:

- Status.
- Branch e PR.
- Commit final.
- Implementações e correções.
- Testes e resultados.
- APK.
- Evidências.
- Limitações.
- Próxima etapa: Prompt 4 — Consolidação, integração e publicação.