# PIANO LIVRE — PROMPT 2 DE 4
## Reformulação integral da tela Estudar — referência visual obrigatória

**Repositório único:** https://github.com/atshimura/Piano-livre-aberto

**Prompt 1 concluído:** PR #71 — https://github.com/atshimura/Piano-livre-aberto/pull/71

**Referência visual obrigatória:** [tela Estudar aprovada](../references/tela-estudar-aprovada.webp)

**Arquivo da imagem:** docs/references/tela-estudar-aprovada.webp

**Este arquivo:** docs/prompts/PROMPT_02_TELA_ESTUDAR_PIANO_LIVRE.md

> **Missão:** implementar efetivamente a nova interface do modo Estudar com máxima fidelidade à imagem aprovada, ampliar a área útil da partitura, remover controles obsoletos e preservar as funcionalidades musicais e dados. Não considerar concluído sem provas e testes. A imagem é referência visual dos estados normal, em execução e controles restaurados; requisitos textuais posteriores aprovados prevalecem em diferenças específicas.

## 1. Preparação obrigatória e histórico

1. Ler AGENTS.md, README.md, docs/00-INDICE.md, docs/01-VISAO-E-REQUISITOS.md, docs/02-ARQUITETURA.md, docs/03-NOTACAO-TEMPO.md, docs/04-MIDI-ESTUDO.md, docs/05-DADOS-BACKUP.md, docs/06-BUILD-TESTES.md, docs/07-ESTADO-E-ROADMAP.md, docs/08-DECISOES.md, docs/10-CONTINUIDADE.md e demais documentos relevantes.
2. Ler o Prompt 1 e a documentação de sua entrega: docs/prompts/PROMPT_01_TELA_INICIAL_PIANO_LIVRE.md e docs/70-TELA-INICIAL-PROMPT-01.md.
3. Inspecionar a tela Estudar, seus estados e a integração entre PianoScreens.java, MainActivity.java, renderização, janela da partitura, USB-MIDI, controles de reprodução, perfis e preferências. Identificar dependências compartilhadas com Laboratório e Partitura Livre.
4. Abrir e **inspecionar visualmente a imagem obrigatória** em docs/references/tela-estudar-aprovada.webp. Não usar apenas seu nome ou a descrição. Ela tem 3 apresentações: tela normal, durante execução e após toque para revelar controles.
5. Verificar HEADs/PRs atuais e o status dos testes, sem assumir que dados anteriores estejam atualizados.

O Prompt 1 foi concluído tecnicamente no PR #71, branch feat/tela-inicial-piano-livre, SHA 32b83a3b6d37a831900de4bafd5ec8cbd406d0d8. Como esse PR ainda não havia sido integrado à main na preparação deste documento, **basear o Prompt 2 na branch que contém o Prompt 1**, inclusive esta documentação e imagem. Caso o Prompt 1 já tenha sido integrado, atualizar a base cuidadosamente, sem duplicar commits. Criar branch de implementação feat/tela-estudar-piano-livre. Abrir PR dependente, preferencialmente dirigido à branch do Prompt 1 até seu merge; após integração, rebase/retarget sem perda de alterações nem force-push.

Não acessar o repositório privado histórico atshimura/piano-livre. Respeitar a proteção de main, GPL-3.0 e direitos das partituras.

## 2. Fidelidade ao desenho aprovado

Reproduzir **com máxima fidelidade possível** a hierarquia, cores, bordas, tipografia, tamanhos relativos, distâncias, destaque azul e aproveitamento de espaço da imagem, com componentes reais e funcionais.

A imagem apresenta:
- **Vista normal:** barra superior azul-marinho unificada; faixa compacta MODO / MÃOS; partitura grande com duas pautas, claves e cursor; barra inferior de transporte.
- **Vista durante execução:** ocultação das duas faixas superiores, preservando a barra inferior, para ampliar a partitura.
- **Vista após toque:** reexibição dos controles ocultos, sem mudar indevidamente posição, andamento, seleção de mãos ou modo musical.

Adaptar somente para legibilidade, acessibilidade, dimensão/escala real, dados genuínos e compatibilidade. Não converter a imagem em fundo estático, nem copiar partituras como bitmap. A escrita da partitura deve continuar sendo gerada pelo renderizador musical.

O desenho ilustra um instrumento “Yamaha P-45” conectado; **isso não é dado fixo**. Desconectado, mostrar USB-MIDI; conectado, mostrar somente o nome real retornado pelo sistema.

## 3. Estrutura obrigatória

### 3.1. Barra superior única

**Eliminar a faixa separada de título da música**, reunindo as informações em um único cabeçalho compacto azul-marinho:
- voltar para a Biblioteca;
- título da música;
- compositor e metadados somente quando constarem dos dados reais;
- acesso funcional a Partitura Livre;
- BPM e informações essenciais de andamento e/ou zoom, sem confundir velocidade de reprodução com zoom;
- perfil, configurações e botão USB-MIDI/identificação real do instrumento.

O nome Piano Livre pode ser discreto ou compactado quando necessário. Informações secundárias podem recolher-se em menu acessível em telas estreitas; não excluir funcionalidades. **Remover por completo** a frase “Piano conectado · toque aqui para testar”, pois o botão MIDI já é o indicador.

### 3.2. Faixa compacta MODO / MÃOS

Manter numa única faixa secundária, sempre que houver largura:
- MODO: Espera | Contínuo | Demonstração;
- MÃOS: Ambas | Esquerda | Direita.

Somente uma seleção ativa em cada grupo; opção ativa com destaque azul. Conservar o comportamento real do estado musical, inclusive persistência/transições. Em telas compactas, oferecer adaptação que preserve os seis controles acessíveis, sem áreas de toque impraticáveis.

### 3.3. Partitura predominante

A área da partitura deve ocupar o restante do espaço entre os controles e **aumentar em relação ao layout anterior**. Manter duas pautas quando exigidas, claves, armadura, compasso, símbolos MusicXML, cifras, dinâmica, articulações, notas simultâneas, cursor, mapeamento de notas, rolagem suave e contexto fixo conforme documentação. Não reduzir a legibilidade para ganhar altura fictícia. Não modificar arquivos MusicXML originais nem o mapeamento MIDI.

### 3.4. Barra inferior permanente

Manter Início, Iniciar/Pausar, Parar, controle da velocidade (com percentual coerente) e Trecho/repetição. Controles reais, dimensionados para toque. Preservar semântica de Início, transporte, pausa em segundo plano, loop e seleção de trecho diretamente na pauta. A faixa inferior permanece durante a ocultação das superiores.

## 4. Remoções expressamente aprovadas

**Remover da interface do modo Estudar:**
- botão e **funcionalidade** “Teclado guia”;
- “OSMD · nova”;
- “Anterior · temporal”;
- “Partitura Original”/qualquer seletor manual entre renderizadores;
- texto redundante “Piano conectado · toque aqui para testar”.

Não apenas ocultar mantendo espaço reservado: eliminar as faixas, handlers, estados e dependências de layout que consumam área da partitura. Remover caminhos mortos apenas quando comprovadamente exclusivos do Estudar.

**A remoção do teclado guia não significa desligar o USB-MIDI ou o reconhecimento de notas.** Preservar o reconhecimento de notas, acordes e simultaneidade, modos, feedback na pauta, seleção de mãos e demonstração. Não remover partes compartilhadas usadas pelo Laboratório, MIDI ou outras telas. Se houver preferências antigas do guia, manter leitura/backup sem reintroduzir a visualização no Estudar; nunca destruir dados. Atualizar as decisões técnicas antigas D-007/D-008 em documentação, preservando histórico e registrando a aprovação posterior.

**Renderização automática:** retirar a escolha manual de OSMD versus representação temporal. Selecionar internamente o caminho apto à música e ao modo, sem perder os fallbacks tecnicamente necessários. Verificar equivalência do comportamento, fidelidade MusicXML, mapa nota↔partitura, estabilidade de zoom/scroll, desempenho e interação. Não substituir os motores por uma imagem ou simplificação que perca conteúdo.

**Preservar Partitura Livre:** é um modo de leitura, não um botão de renderizador. Deve abrir a música correta sem as restrições do estudo e permitir voltar.

## 5. Ocultação e restauração dos controles

A imagem aprovada contém ocultação durante execução e restauração por toque. **Neste Prompt 2**, implementar a organização de estado e layout para a transição, e, sempre que tecnicamente seguro, implementar o comportamento completo de ocultar automaticamente barra superior + faixa MODO/MÃOS durante a execução e restaurá-las ao toque da partitura, mantendo a barra inferior.

O Prompt 3 fará a **consolidação do comportamento dinâmico, integração USB-MIDI entre telas e casos limítrofes**. Se houver algum comportamento deixado para o Prompt 3, documentar exatamente o que falta; não declarar o resultado visual da segunda e terceira vistas como implementado sem prova.

A transição não pode:
- cancelar toque destinado a selecionar nota ou trecho;
- alterar seleção de mãos, modo, velocidade, posição, loop ou estado MIDI;
- interromper a animação musical;
- mover inesperadamente o cursor ou recortar notas;
- deixar a barra inferior inacessível;
- criar piscadas ou oscilações com sucessivos toques.

Usar estratégia de exibição acessível e previsível; evitar timers arbitrários que prejudiquem o estudo em Espera.

## 6. Mãos, modos e integridade musical

- Seleção Ambas/Esquerda/Direita sincronizada com o motor, sem inferir mão pela altura da nota. Não modificar MusicXML ao trocar a mão.
- Espera: não penalizar demora; notas extras não bloqueiam, mas são indicadas; acordes obedecem à calibração documentada, sem janelas arbitrárias.
- Contínuo: andamento, velocidade, relógio e cursor sincronizados.
- Demonstração: preservar a saída MIDI quando disponível, sem exigir sintetizador externo ou internet.
- Eventos de pausa, retomada, perda de foco, reconexão MIDI, alteração de velocidade, início/parada, mudança de modo, trecho e redimensionamento não devem perder estado indevidamente.
- Manter a biblioteca e o Laboratório do Prompt 1 intactos, salvo mudanças compartilhadas estritamente necessárias.

## 7. USB-MIDI

No modo Estudar, manter botão clicável na barra superior. Sem conexão mostrar “USB-MIDI”; com dispositivo conectado mostrar o nome real e atualizá-lo com os eventos existentes de conexão/desconexão. Não exibir nomes fictícios nem frase redundante. Reaproveitar o comportamento da tela inicial do Prompt 1. O Prompt 3 consolidará a padronização e os testes cruzados extensivos; nesta entrega, não regressar o comportamento existente.

## 8. Responsividade, desempenho e acessibilidade

Usar a composição tablet paisagem da imagem como principal referência. Validar celular paisagem e perfis representativos de Realme 7 5G e Lenovo Tab M9, sem dizer que emulação equivale à experiência física. Não cortar botões, títulos, indicações ou notas. Áreas de toque adequadas, contraste, fonte legível, tratamento de nomes longos e estado MIDI variável.

Mensurar em pelo menos dois viewports a altura em pixels/dp da partitura antes/depois (normal e controles ocultos) e demonstrar ganho real de área útil, sem distorção nem cortes de símbolos. Inspecionar também música longa, duas pautas, regiões extremas e resize.

## 9. Testes obrigatórios

1. Rodar verificador público, suites JVM/JS, corpus MusicXML/MIDI e testes Android aplicáveis.
2. Testar barra superior única, ausência dos controles eliminados, seleção visual/funcional de três modos e três mãos.
3. Testar a barra inferior: Início, Iniciar/Pausar, Parar, Velocidade, Trecho e retorno à Biblioteca.
4. Verificar reconhecimento de notas e acordes por eventos MIDI sintéticos, inclusive em Ambas/Esquerda/Direita, sem guia visual.
5. Testar mudança automática do renderizador/fallbacks, mapeamento gráfico das notas, rolagem e notação sem regressão.
6. Testar ocultação/reexibição dos controles (caso implementada neste prompt), toque em nota versus toque para revelar faixa e preservação do estado.
7. Testar preservação das preferências/backup, inclusive preferência antiga do teclado guia.
8. Capturar imagens **reais do emulador** comparáveis às três vistas da referência aprovada; registrar diferenças e corrigi-las.
9. Compilar APK de validação para o SHA final e publicar artefato no Actions, sem usar assinatura permanente em PR.
10. Exigir sucesso dos workflows relevantes no SHA final. Não omitir ou enfraquecer testes para aprovar CI.

Separar explicitamente testes automatizados de aceite físico pendente no Yamaha P-45, Realme 7 5G e Lenovo Tab M9. Um teste de interface não comprova reconhecimento em instrumento real.

## 10. Documentação e entrega

- Criar docs/71-TELA-ESTUDAR-PROMPT-02.md com decisões, estruturas, diferenças de layout, fluxos, medições, testes, screenshots, limites, riscos e próximos passos.
- Atualizar docs/00-INDICE.md, docs/07-ESTADO-E-ROADMAP.md, docs/08-DECISOES.md, histórico e versão conforme regras do projeto, sem publicar versão oficial.
- Preservar e referenciar **esta imagem obrigatória**, já versionada em docs/references/tela-estudar-aprovada.webp.
- Abrir PR de implementação com dependência clara do PR #71 e anexar links dos dois workflows, APK de validação, comparações visuais e plano de continuidade.
- Não integrar em main, não publicar Release e não começar o Prompt 3 sem primeiro encerrar a implementação e a validação previstas neste Prompt 2.
- Não declarar concluído o que não for efetivamente demonstrado. Em caso de impedimento, registrar causa exata, trabalhos feitos e próxima ação.

## 11. Critérios de aceite

O Prompt 2 só está tecnicamente concluído quando:
- a tela corresponde à imagem aprovada, salvo adaptações justificadas;
- só há uma barra superior unificada, uma faixa compacta MODO/MÃOS e uma barra inferior de transporte;
- a partitura ganhou espaço real medido;
- os controles Teclado guia, OSMD · nova, Anterior · temporal, Partitura Original e frase redundante desapareceram **do Estudar**;
- Partitura Livre, USB-MIDI, reconhecimento, seleção de mãos, modos, BPM/velocidade, trecho e navegação funcionam;
- não há regressão de notação, dados, biblioteca, laboratório ou transporte;
- as evidências visuais, testes, APK e documentação final estão disponíveis e associados ao SHA testado.

**Executar autonomamente todo o Prompt 2 no código, não apenas apresentar plano.** Solicitar intervenção somente por bloqueio real, decisão relevante de produto ou situação de segurança. O PR deverá permanecer aberto para integração posterior.
