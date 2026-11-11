# ZAI-010 — Laboratório Musical: escolha de modo e Criação em painel horizontal

**Data:** 10/11/2026 — decisões funcionais aprovadas pelo mantenedor por imagens e diálogo. **Escopo:** branch `zai/feat/zai010-navegacao-criacao-figuras-simples-20261110`, PR para `zai/lab`; a `main` de GitHub e o repositório histórico privado permanecem **intocados**. Seguir `AGENTS.md`, `ZAI-CONTINUIDADE.md`, GPL-3.0, offline sem `INTERNET`, arquivos MIDI `.plc`, derivadas `.pls`, backups e assinatura permanente via Secrets. Nenhuma Release/tag/Play Store nesta entrega.

## Decisões e telas aprovadas

**Entrada do Laboratório Musical:** clique na Biblioteca não abre mais diretamente Descobrir. Abre a **tela intermediária paisagem** com três grandes cartões, um por coluna, título `Escolha um modo` e descrições:
- **Descobrir** — "Explore notas, acordes e ideias musicais de forma livre"; abre Descobrir.
- **Aprender** — "Modo voltado ao estudo guiado e acompanhamento da prática"; indicação **Em breve**, cartão desabilitado e sem navegação funcional (não inventar módulo disponível).
- **Criação** — "Grave ideias musicais e transforme suas gravações em partitura"; abre Criação Musical.
Botão `← Biblioteca` retorna à Biblioteca. Navegação de volta de Descobrir e Criação retorna ao seletor de modos, sem abandonar gravação não salva. Cabeçalho mantém o estilo azul-marinho atual.

**Criação Musical — layout em paisagem:** duas colunas, esquerda para ações/transporte e direita para conteúdo; coluna direita mais larga para listas e detalhes. Topo compacto: título `Criação Musical`, `Perfil ativo · Nome` e indicador `USB-MIDI: conectado/indisponível`, ao lado de **Informações USB-MIDI** em vez de um cartão grande. Essa função exibirá o diagnóstico real disponível, não prometerá gerenciamento que não existe. Não remover opção de conectar/dispositivos MIDI em outros pontos existentes do app.

**Estados da tela (sem trocar de página Android):**
1. **Início:** esquerda grande cartão azul `+ Nova gravação` e abaixo `Minhas criações`; direita título **Gravações recentes**, ação `Ver todas`, itens reais por perfil.
2. **Gravando:** o cartão azul esquerdo **transforma-se em painel de gravação**, relógio, notas/eventos, estado, Gravar/Pausar/Continuar/Parar; o painel direito continua com Gravações recentes. Reutilizar `MidiTakeRecorder`/`CreationJournal`, checkpoint, limites, transferência USB, recovery e proteção contra perda sem mudar a gravação.
3. **Parou:** painel direito troca a lista por **Revisar gravação**: ações existentes Salvar composição, Reproduzir gravação, Exportar tomada MIDI, Descartar com confirmação; botão **← Voltar às gravações recentes** no topo direito. Se a tomada não estiver salva, pedir confirmação; não apagar dados ao retornar à lista e permitir reabrir revisão. O cartão esquerdo permanece com tempo/resumo e opção para voltar à revisão. Se não houver entrada MIDI, mostrar diagnóstico claro em vez de iniciar captura indevida.
4. **Minhas criações** ou **Ver todas:** conteúdo da direita passa à biblioteca completa **do perfil**, com busca, ordenação e filtros funcionais **Todas / Com partitura / Só gravação**; manter recursos existentes (recuperáveis, lixeira, restauração, renomear, exportar, gerar/visualizar/excluir só partitura, estudar, player), sem descartar gravação pendente. O título e botão `← Voltar às gravações recentes` ficam no topo direito. Selecionar criação exibe **seus detalhes e ações na mesma coluna direita**, sem trocar a tela principal. As listas recentes e completas consultam a mesma fonte de projetos locais, sem dados sintéticos.
5. **Retornos:** o botão voltar do Android e as ações de navegação respeitam confirmações de mudanças não salvas, recuperação local e estado de reprodução. Perfil ativo e conexão USB-MIDI são atualizados sem limpar projetos.

## Gerador de partituras — sem pontos de aumento

- **Nenhum ponto de aumento junto às notas geradas.** Escolher a **figura simples** mais próxima de cada duração quantizada entre semibreve (4 tempos), mínima (2), semínima (1), colcheia (1/2) e semicolcheia (1/4), limitada ao espaço restante do compasso como em ZAI-007. Ao empate preferir a figura menor, de forma determinística.
- **Não alterar o MIDI `.plc` nem os ataques e alturas**: uma tecla segurada por cinco minutos continua um único Note On/Off original; a figura escrita sem ligaduras e sem pontos pode abreviar intencionalmente a duração. Preservar estimativa BPM, acordes C/Cm (Mi♭), corte C4=60, duas pautas Sol/Fá, OSMD adaptativo.
- Pausas continuam **invisíveis mas temporais** no MusicXML para manter cronologia (ZAI-009), e o gerador não cria automaticamente staccato, ligaduras ou slurs. Pontos de pausas ocultas são internos, não aparecem na partitura.
- Elevar marcador de revisão do motor no MusicXML para `ZAI-010-simple-figures`. Derivadas PLS2 de geradores antigos devem exigir **regeneração explícita** (não alterar `.pls` até Salvar).

## Plano

**P10-0:** ler instruções e inventariar telas, rotas, gravação, biblioteca, scripts e CI; registrar este documento antes do código.

**P10-1:** figura simples e testes MIDI: escolha nearest entre 16/8/4/2/1 células, sem ponto de nota nem reataques, pontos ocultos de pausas não entram no desenho; regressões C/Cm, S1, sustain, 4/4, Estudar.

**P10-2:** tela intermediária de Laboratório Musical em três cartões horizontais; Descobrir e Criação funcionando; Aprender indisponível sem clique; rota voltar correta com instrumentos e perfis intactos.

**P10-3:** painel Criação em duas colunas com mesmo cabeçalho e indicador USB compacto. Consolidar **gravação, revisão, listagem, filtros, detalhes** em coluna contextual mantendo os handlers e repositórios existentes. Listas reais e eventos MIDI, não dados de demonstração. Confirmar saídas de contexto com gravação pendente.

**P10-4:** testes Android/JVM/contratos Python de navegação, state machine, filtro e derivados; teste browser OSMD sem pontos junto às notas; build debug isolado; não afirmar validação física.

**P10-5:** PR e CI dedicada sem tocar workflows globais; corrigir até verde, integrar somente à `zai/lab`.

**P10-6:** APK principal versionCode **80**, versão **0.9.71-open-beta** (superior a 0.9.70/code79), `br.org.pianolivre.opensource`, mesma assinatura permanente em Secrets, não debuggable, sem permissão INTERNET. Workflow separado publica **mesmos bytes** em `zai/downloads`, valida SHA-256 remoto e entrega URL GitHub raw fixada ao commit para evitar `Library file not found`.

## Aceite e limitações

- 3 cartões na orientação paisagem, navegação correta, Aprender **Em breve** sem comportamento enganoso; Android back previsível.
- Cabeçalho Criação e USB compacto, botões ações esquerda e **Gravações recentes** direita, ver todas e minhas criações abrem mesmo painel, ações existentes preservadas.
- Controles de gravação não mudam atividade; revisão surge na direita ao parar; retorno protegido de uma gravação não salva; projetos, `.plc/.pls`, journaling/recovery, backups preservados.
- Não há **ponto de aumento nas notas geradas**, 22 notas S1 preservadas, posições e alturas corretas, nenhum staccato/pausa visível/ligadura; MIDI fonte intacto.
- Sem testes físicos no Realme 7 5G, Lenovo Tab M9 e Yamaha P-45: a validação em CI/emulador não substitui hardware.
