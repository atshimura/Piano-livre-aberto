# PROMPT — IMPLEMENTAÇÃO COMPLETA DO LABORATÓRIO MUSICAL: CRIAÇÃO C1

## 1. Missão

Continuar o desenvolvimento do aplicativo Android **Piano Livre**, implementando integralmente o módulo **Laboratório Musical → Criação C1**, com o máximo de autonomia técnica.

**Repositório oficial:**
https://github.com/atshimura/Piano-livre-aberto

**Versão de referência:** 0.9.54-open-beta, versionCode 63.

**Issue de acompanhamento:** https://github.com/atshimura/Piano-livre-aberto/issues/79

A Criação C1 deve permitir que o usuário toque no piano conectado por USB-MIDI, grave sua execução, salve composições, organize projetos, reproduza a gravação no instrumento e exporte arquivos MIDI.

**Implementar efetivamente o módulo completo, não apenas documentar, desenhar telas ou criar protótipos.**

Prosseguir automaticamente entre as subetapas, corrigindo problemas encontrados e realizando testes.

## 2. Documentação obrigatória

Antes de modificar o código, ler:

- `AGENTS.md`
- `README.md`
- `docs/00-INDICE.md`
- `docs/10-CONTINUIDADE.md`
- `docs/76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md`
- `docs/prompts/CRIACAO_C1_IMPLEMENTACAO.md`
- `docs/05-DADOS-BACKUP.md`
- `docs/33-PERFIS-ESTAVEIS.md`
- `docs/37-IMPORTACAO-MIDI.md`
- `docs/38-COMPATIBILIDADE-DE-DADOS.md`
- `docs/45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md`
- `docs/66-LABORATORIO-DESCOBRIR-1.0.md`
- `docs/77-CONSOLIDACAO-PUBLICACAO-0954.md`

A especificação do documento 76 é a **fonte principal e autoritativa**. Este prompt complementa o planejamento, sem substituir suas decisões detalhadas.

Conferir o estado atual da `main`, releases, PRs, issues e GitHub Actions.

O PR #81 consolidou a beta 0.9.54, incorporando os PRs #76, #78 e #80. Confirmar que esta continua sendo a base adequada antes de iniciar.

Não acessar o repositório histórico privado.

## 3. Funcionalidades obrigatórias

### C1.1 — Interface e navegação

Adicionar a opção **Criação** dentro do Laboratório Musical, mantendo:

- Descobrir: funcional e sem regressões.
- Aprender: visível, mas desabilitado.
- Criação: habilitada quando a implementação estiver funcional.

A página inicial da Criação deverá apresentar:

- Nova gravação.
- Minhas Criações.
- Composições recentes, quando existirem.
- Perfil ativo.
- Situação real da conexão MIDI, diferenciando entrada e saída.

Não adicionar um novo cartão à tela inicial geral do aplicativo.

Implementar navegação coerente entre início, gravação, projetos, detalhes, Lixeira e recuperação, respeitando o botão Voltar do Android.

Adaptar todas as telas para celulares e tablets, especialmente em orientação horizontal.

### C1.2 — Motor de gravação MIDI

Implementar gravação livre de uma única execução por composição.

Controles obrigatórios:

- Gravar.
- Pausar.
- Continuar.
- Parar.
- Reproduzir gravação.
- Salvar.
- Descartar, com confirmação.

A gravação deverá preservar:

- Note On e Note Off.
- Número e canal das notas.
- Velocidade de execução.
- Ordem cronológica dos eventos.
- Tempo real de recepção com relógio monotônico.
- Pedal sustain (CC64), quando transmitido.
- Repetições e rearticulações da mesma nota.
- Acordes e simultaneidade musical.

Não aplicar quantização à gravação original.

Tratar corretamente Note On com velocidade zero, canais distintos, pedal pressionado e teclas ainda sustentadas no momento de parar.

Utilizar gerações/epochs para impedir que mensagens antigas sejam misturadas com uma nova sessão.

Na pausa, preservar os eventos recebidos e registrar separadamente os ajustes necessários à reprodução segura.

Não gravar mensagens originadas pela própria reprodução do aplicativo.

**Segurança da gravação:** perda de conexão, troca de perfil, fechamento da tela, interrupção do aplicativo ou passagem para segundo plano não podem descartar silenciosamente o trabalho já registrado.

Não manter gravação oculta em segundo plano nesta versão.

### C1.3 — Projetos e biblioteca de composições

Implementar armazenamento local permanente de projetos, cada um com identificador único e imutável.

Cada composição deverá conter título, perfil proprietário, datas, duração, dados MIDI e demais metadados pertinentes.

Implementar **Minhas Criações**, com:

- Lista de projetos.
- Pesquisa por título, sem diferenciação de maiúsculas ou acentos.
- Ordenação por nome, data ou duração.
- Filtros: Todas, Salvas, Recuperáveis e Lixeira.
- Abertura de detalhes.
- Renomeação.
- Exclusão para Lixeira.
- Restauração da Lixeira.
- Exclusão definitiva mediante confirmação.

Permitir títulos repetidos sem confundir os projetos.

Preservar busca, filtros e ordenação ao retornar dos detalhes.

Associar projetos aos IDs estáveis dos perfis, sem vincular os arquivos ao nome do usuário.

A exclusão ou troca de perfil não pode apagar composições silenciosamente.

**Recuperação:** implementar journal incremental e persistência segura, com escrita temporária, validação de integridade e recuperação de trechos íntegros após falhas.

Um projeto incompleto não deve aparecer como salvo. Arquivos corrompidos não devem substituir versões válidas.

Estabelecer limites técnicos de armazenamento, tamanho e quantidade de eventos com base em testes, evitando bloquear a interface.

### C1.4 — Reprodução e exportação MIDI

Permitir reproduzir a composição no piano conectado via USB-MIDI.

A reprodução deverá respeitar:

- Duração original das notas.
- Intervalos temporais.
- Velocidade.
- Canais.
- Pedal sustain.
- Repetições.
- Acordes.

Implementar controles Reproduzir, Pausar, Continuar e Parar, com posição e duração.

Permitir ignorar silêncio inicial/final como opção de reprodução, sem alterar o arquivo original nem eliminar pausas internas.

Ao pausar, parar, desconectar ou sair da tela, interromper os agendamentos e liberar notas e pedal com segurança.

Não retomar automaticamente depois de reconectar o instrumento.

Reproduzir somente quando existir porta MIDI de saída funcional. Não simular áudio pelo celular.

**Exportação:**

Implementar exportação `.mid` pelo seletor de arquivos do Android (SAF), sem exigir acesso amplo ao armazenamento.

Gerar Standard MIDI File válido, com mapa de tempo e resolução documentados, preservando a fidelidade da execução dentro dos limites técnicos do formato.

Testar exportação seguida de reimportação com o importador MIDI existente, comparando notas, canais, velocidades e tempos.

Não modificar a composição original ao exportar.

### C1.5 — Backup, restauração e compatibilidade

Integrar todas as composições ao sistema de backup do Piano Livre.

O backup deverá preservar:

- Projetos salvos.
- Arquivos MIDI.
- Metadados.
- Identificadores dos projetos.
- Associação aos perfis.
- Projetos na Lixeira.
- Dados recuperáveis válidos.

Evoluir o formato de backup de maneira versionada, mantendo compatibilidade de leitura com as versões anteriores v1/v2.

**Regra crítica:** restaurar um backup antigo não pode eliminar silenciosamente composições criadas posteriormente.

Implementar pré-validação, verificação de integridade, staging e rollback, abrangendo todos os dados envolvidos.

Testar backups válidos, corrompidos, incompletos, com colisões de identificadores, com perfis ausentes e com interrupções simuladas.

Preservar a biblioteca de partituras, os perfis, favoritos, coleções, sessões e preferências existentes.

## 4. Funcionalidades que NÃO devem ser implementadas

A C1 não inclui:

- Conversão automática de gravações para partitura MusicXML.
- Visualização ou edição de partitura derivada da gravação.
- Exportação PDF.
- Gravação em múltiplas camadas ou overdub.
- Áudio por microfone.
- Sintetizador interno.
- Servidores, contas online ou telemetria.

Essas capacidades pertencem às etapas posteriores.

Os valores propostos de 100 BPM, compasso 4/4 e quantização 1/16 são apenas sugestões para a futura C2. **Não aplicar esses valores à gravação original da C1.**

## 5. Estratégia de desenvolvimento

Executar sequencialmente C1.1, C1.2, C1.3, C1.4 e C1.5, ajustando dependências quando necessário para não expor funções sem armazenamento seguro.

Para cada subetapa:

1. Criar branch de desenvolvimento a partir da base atual correta.
2. Implementar código funcional.
3. Criar ou atualizar testes automatizados.
4. Executar testes direcionados.
5. Corrigir falhas identificadas.
6. Registrar decisões e alterações na documentação.
7. Abrir PR e verificar os checks.
8. Integrar pelo procedimento autorizado da `main` somente quando as condições de segurança e testes estiverem satisfeitas.
9. Avançar para a próxima subetapa sem solicitar autorização técnica rotineira.

Utilizar a arquitetura rápida de CI introduzida no PR #78, mas exigir `build` e `emulator` completos antes de qualquer integração.

Não enfraquecer os gates de segurança.

Não executar limpeza destrutiva, force-push ou alterações indevidas dos GitHub Actions Secrets.

## 6. Testes obrigatórios

### Motor MIDI

Testar notas isoladas, acordes, rearticulações, diferentes canais, velocidades, CC64, pausas, retomadas, perda de conexão, relógio, ordenação e finalização segura.

### Interface Android

Testar emuladores de celular e tablet, em paisagem, incluindo:

- Navegação e botão Voltar.
- Gravação e revisão.
- Biblioteca vazia e com diversos projetos.
- Busca, filtros e ordenação.
- Renomeação e títulos longos.
- Lixeira e recuperação.
- Troca de perfil.
- Entrada MIDI ausente.
- Saída MIDI ausente.
- Segundo plano e interrupções.
- Responsividade e legibilidade.

### Persistência e compatibilidade

Testar reinício do aplicativo, recuperação de journal, corrupção de arquivos, limites de tamanho, backup/restauração e atualização da beta pública anterior.

### Regressão do aplicativo

Manter funcionamento de Tela Inicial, Biblioteca, Estudar, Descobrir, reconhecimento MIDI, importação MusicXML/MIDI, histórico e perfis.

Executar a suíte completa no SHA efetivamente integrado e registrar os resultados.

Os testes físicos no **Yamaha P-45, Realme 7 5G e Lenovo Tab M9** serão realizados posteriormente pelo usuário. Não declarar validação física apenas por resultados de emulador.

## 7. Versionamento e publicação

Não reutilizar versionCode 63 para uma nova entrega.

Determinar a próxima versão de acordo com o histórico atual do repositório.

Não publicar APK oficial com funcionalidades parciais apresentadas como C1 concluída.

Após completar toda a C1, compilar a versão release e validar pacote, ausência de INTERNET, depuração desativada e compatibilidade.

A publicação oficial somente poderá ocorrer após integração aprovada à `main`, checks completos e autorização de release. Utilizar exclusivamente a assinatura permanente dos GitHub Actions Secrets, sem expor credenciais.

Se a publicação ainda não estiver autorizada, disponibilizar o PR e os artefatos de validação adequados, deixando explicitamente registrada essa pendência.

## 8. Documentação e continuidade

Atualizar:

- Documento 76 da Criação C1.
- Índice da documentação.
- Documento de continuidade.
- Roadmap.
- Histórico de alterações.
- Documentação técnica dos novos componentes.
- Issue #79.

Registrar continuamente a evolução das subetapas, commits, PRs, testes e limitações.

Não encerrar a execução somente porque C1.1 ou outra entrega intermediária foi concluída.

Prosseguir até C1.5 enquanto houver condições técnicas seguras.

Se houver impedimento real, registrar no GitHub o estado preciso da implementação, o problema encontrado, as evidências e a próxima ação necessária.

## 9. Critérios de conclusão

A Criação C1 somente estará tecnicamente concluída quando o usuário puder:

1. Acessar a Criação pelo Laboratório.
2. Gravar uma execução MIDI real ou sintética validada.
3. Pausar, continuar e parar.
4. Salvar o projeto e reabri-lo após reiniciar o aplicativo.
5. Encontrar e organizar composições.
6. Renomear, excluir e restaurar projetos.
7. Recuperar gravações interrompidas quando houver journal íntegro.
8. Reproduzir a gravação por saída MIDI funcional.
9. Exportar um arquivo `.mid` válido.
10. Realizar backup/restauração sem perda silenciosa de dados.
11. Utilizar diferentes perfis sem misturar projetos.
12. Passar nos testes integrados e nas regressões obrigatórias.

A comprovação funcional automatizada não substitui os testes físicos pendentes.

## 10. Relatório final

Ao terminar, apresentar objetivamente:

- Estado de cada subetapa C1.1 a C1.5.
- Funcionalidades efetivamente implementadas.
- Branches e PRs.
- Commit final da `main`, se integrado.
- Versão do aplicativo.
- Links dos GitHub Actions e resultados.
- APK de validação ou Release oficial, conforme aplicável.
- Verificações de assinatura e integridade, quando publicadas.
- Limitações conhecidas.
- Testes físicos pendentes.
- Próxima etapa de desenvolvimento, C2.

**Autonomia:** executar o trabalho completo sem solicitar decisões técnicas rotineiras. Preservar os dados existentes, a GPL-3.0, o funcionamento offline e as decisões aprovadas.

**Objetivo final: entregar a Criação C1 funcional, testada, documentada e integrada com segurança ao Piano Livre, sem antecipar os recursos de C2/C3 nem declarar a versão 1.0 concluída.**