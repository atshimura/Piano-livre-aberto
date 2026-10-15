> **Arquivo de referência técnica transferido para a edição aberta.** Parte das afirmações sobre versão, CI, aplicativo privado ou certificados descreve apenas o histórico de desenvolvimento. Para a situação vigente no repositório público, consulte [AGENTS.md](../AGENTS.md), [build e testes](06-BUILD-TESTES.md) e [estado](07-ESTADO-E-ROADMAP.md). Links para execuções privadas podem estar inacessíveis a terceiros.

# 01 — Visão do produto e requisitos funcionais

**Natureza:** referência de produto e decisões já discutidas. **Não é atestado de implementação.** Referência histórica: especificação funcional “Aplicativo Android de aprendizagem de piano inspirado no Flowkey — Rev. 1.0”, de 13/10/2026, elaborada antes do primeiro protótipo. Status contemporâneo em [07-ESTADO-E-ROADMAP.md](07-ESTADO-E-ROADMAP.md).

## 1. Objetivo, público e plataformas

Aplicativo **independente** para estudar piano com uma experiência de acompanhamento visual comparável pedagogicamente ao Flowkey. Hardware primário: **Yamaha P-45** ligado por **USB-MIDI**. Dispositivos Android prioritários para teste: **Lenovo Tab M9** (tablet) e **Realme 7 5G** (celular). Android e uso em paisagem primeiro; Windows e sincronização entre dispositivos são etapas posteriores.

O foco é **aprendizagem**, não apenas visualização de partitura. O aluno deve poder estudar lentamente, aguardar notas corretas, separar mãos, selecionar passagens e usar demonstração no instrumento físico. Teclado desenhado é recurso de orientação e **não precisa emitir notas pelo toque na tela**. O aplicativo não precisa sintetizar áudio: o Yamaha é a referência principal, podendo o app permanecer silencioso.

## 2. Convenção de decisão versus implementação

- **Aprovado:** escolha explícita de produto. Pode estar implementada ou pendente.
- **Proposto:** solução técnica/operacional a validar.
- **A validar:** depende de arquivo complexo, prova musical, hardware ou medição.
- **Futuro:** intenção conhecida fora da primeira versão.
- **Implementado/parcial/ausente:** exclusivo do inventário de código em docs/07.

Não transformar proposta em decisão aprovada e não aplicar tolerâncias arbitrárias como se tivessem aprovação.

## 3. V1 desejada (escopo aprovado)

1. USB-MIDI P-45 bidirecional, diagnóstico e tratamento seguro de interrupções.
2. Biblioteca musical integrada de dez arquivos inicialmente selecionados; importação MusicXML/MXL e MIDI prevista no escopo; organização e busca, progresso local.
3. Modos Espera, Contínuo e Demonstração; mãos direita, esquerda e ambas; partitura legível e temporalmente sincronizada.
4. Controle de andamento em **% e BPM** e estudo de trechos, inclusive seleção diretamente na partitura.
5. Metas **básicas**, perfis individuais, biblioteca compartilhada, favoritos/coleções e histórico.
6. Retomada de sessão; backup e restauração **manuais completos**, sem servidor obrigatório e sem sincronização automática.
7. Testes de regressão musical e dispositivos-alvo para declarar V1 concluída.

O primeiro protótipo **não atinge todos esses itens**; consulte docs/07. Fora da V1: Windows, edições musicais avançadas, avaliações completas de ritmo e articulação, planejamento pedagógico automático, sincronização em nuvem e vídeos sem licença.

## 4. Modos e comportamento pedagógico

| Modo | Progressão | Papel da execução do usuário |
|---|---|---|
| Espera | Trava na posição exigida até o reconhecimento correto | Tocar notas acordes das mãos selecionadas; espera não pune demora |
| Contínuo | Avança segundo o tempo musical e velocidade configurada | Tocar acompanhando a partitura; não atribuir automaticamente nota negativa |
| Demonstração | Avança e envia Note On/Off ao instrumento MIDI | Ouvir/observar a execução; sem avaliar o aluno |

**Mãos:** direita, esquerda ou ambas. Mão não selecionada **não é avaliada**; eventualmente poderá ser tocada automaticamente pelo P-45 com acompanhamento MIDI configurável. **Não inferir mão apenas pelo registro/altura das notas**.

### Espera: regras aprovadas

- Nota errada não libera a posição; nota correta libera.
- Acordes exigem novos ataques das notas corretas dentro de janela **configurável e calibrável no instrumento real**, nunca arbitrada em milissegundos. Antes de calibrar, a Aprendizagem não deve impor prazo presumido.
- Reconhecer e **orientar** duração, ligaduras e pedal na Aprendizagem sem bloqueio ou reprovação; somente na Avaliação futura aplicar critérios específicos após aprovação. Não confundir tecla física mantida, som por pedal e duração escrita.
- Posição sem eventos exigidos da(s) mão(s) em estudo não deve bloquear arbitrariamente o aluno.
- Casos especiais a validar: notas extras, repeated notes, mesma tecla atravessando posições, vozes independentes, pedal CC64, acordes sobrepostos e ties.

**Situação atual 0.9.11:** a antiga implementação limitada a `downKeys.containsAll` foi substituída pelo `WaitRecognizer` com novos ataques, sinalização de notas extras sem bloqueio e calibração opcional da janela de acorde por perfil. **Duração e pedal apenas orientam a Aprendizagem e não impedem avanço**. A avaliação estrita é futura e requer decisão separada. Sem calibração física ou valor de ms presumido. Ver [17](17-REGRAS-ESPERA-CALIBRACAO.md), [24](24-AUDITORIA-NOTACAO-MIDI-ETAPAS-1-3.md).

## 5. Visualização, fluxo e interação

- Partitura temporal legível com nota correspondente ao instante e cursor de referência, deslocamento suave sem microtravadas.
- **Claves, armaduras e fórmula de compasso permanecem à esquerda, imóveis**, mudando de conteúdo quando ocorrer mudança musical. Acidentes pontuais, notas, pausas e demais sinais vinculados ao trecho seguem a partitura.
- Modo de estudo com **teclado guia opcional**: mostra notas esperadas e teclas pressionadas. Com teclado aberto, ocultar automaticamente cabeçalhos e barra superior de modos/mãos; manter barra inferior, partitura e teclado visíveis; restaurar ao fechar.
- **Início** reposiciona na abertura, pausa e cancela repetição anterior.
- **Trecho:** seleção por toque e arraste diretamente na pauta, ajuste dos limites, repetir de modo livre, manual ou demonstração conforme evoluções; manter via manual quando útil.
- Se houver vídeo demonstrativo autorizado e sincronizado, usar; caso contrário, teclado virtual ilustrativo. **Não existe licença de vídeos atualmente**.
- Visualização convencional livre separada da experiência temporal para estudo.

## 6. Velocidade, repetição e metas

- Ajustes por porcentagem e BPM; mudança de andamento dentro da partitura deve ser interpretada **na posição correta**.
- Permitir execução única, repetição livre e manual, estudo por partes e demonstração do trecho.
- Metas configuráveis: acertos apenas de notas, notas + ritmo ou avaliação completa; execuções corretas consecutivas ou acumuladas.
- Regime **Aprendizagem** separado de **Avaliação de desempenho**. Na avaliação futura mostrar métricas distintas (notas, ritmo, duração/articulação), **sem nota global obrigatória**.
- Tolerâncias pré-configuráveis iniciante/intermediário/avançado, com ajuste técnico em milissegundos; limiares numéricos a calibrar e aprovar, não inventar.

## 7. Importação, correções e notação

- Prioridade: MusicXML **.xml/.musicxml** e comprimido **.mxl**, preservando vozes, notas, durações, pausas, pautas, tempos, repetições e retornos.
- Importação **MIDI** desejada, com reconstrução aproximada de notação e possível atribuição de mãos; **não está implementada no protótipo**.
- MSCZ nativo e links do MuseScore são futuros/condicionados; nunca prometer acesso automatizado a conteúdo protegido. **PDF/OMR fora do escopo inicial**.
- Permitir correções de **atribuição de mãos/vozes, repetições/retornos e pedal** como **camadas separadas**, sem modificar o MusicXML/MXL original. Não construir editor geral de notas/compassos na V1.
- Distinguir **ties** (prolongamento da nota) de **slurs** (fraseado/ligadura de expressão), dinâmicas, vozes, grace notes, coda/segno e alternativas.

## 8. Pedal e avaliação

- Reconhecer CC64 e distinguir pedal físico, manutenção da tecla e duração escrita.
- Só avaliar uso de pedal quando o arquivo ou uma marcação pedagógica contiver indicação de pedal.
- Permitir futuramente editar marcações de pedal na camada pedagógica sem alterar partitura importada.
- Qualquer avaliação interrompida involuntariamente é **incompleta**, não reprovação.
- Não penalizar o modo Aprendizagem por tempo de reflexão no Espera.

## 9. Biblioteca musical inicial

Os dez arquivos abaixo estão em **app/src/main/assets/scores/** e indexados por **assets/catalog.json**. Os arquivos foram fornecidos para **uso pessoal/testes**; presença no repositório não demonstra licença para publicação pública.

| Peça | Arquivo no catálogo | Escopo |
|---|---|---|
| Ode à Alegria | ode-to-joy-ludwig-van-beethoven.mxl | V1 |
| Für Elise — Easy | fur-elise-easy-ver.mxl | V1 |
| Lago dos Cisnes | lago-dos-cisnes.mxl | V1 |
| Canon em Ré | canon-in-d.mxl | V1 |
| Asa Branca | asa-branca-luiz-gonzaga-humberto-texeira-arr-andre-melhor.mxl | V1 |
| Bella Ciao | bella-ciao-piano.mxl | V1 |
| Minueto em Sol maior | minuet-in-g-major-christian-petzold.mxl | V1 |
| Minueto em Sol menor | minuet-in-g-minor-bwv-anh-115-christian-petzold.mxl | V1 |
| Aquarela | aquarela-toquinho.mxl | V1 |
| Gymnopédie nº 1 | gymnopedie-no-1-satie.mxl | V1 |

Candidatas **posteriores** na auditoria original: Gymnopédie alternativa/compacta, Prelúdio em Dó maior de Bach, Hallelujah e Carinhoso. **Não adicionar automaticamente sem testes/licença**. A classificação V1 indica seleção, **não certificação musical**.

## 10. Perfis, privacidade e continuidade

- Biblioteca compartilhada entre perfis **locais**, histórico e preferências por usuário, identificação simples (nome/avatar), sem PIN, senha, conta ou administrador.
- CRUD de perfis, favoritos, coleções, situação em estudo/concluída e busca por nome/compositor são **requisitos**, não todas funcionalidades implementadas.
- Pausar e preservar música, posição, trecho, modo, mão, velocidade e configurações quando houver chamada, segundo plano, encerramento ou desconexão MIDI; **interrupção não equivale a erro**.
- Backup/restauração manual deve abranger **todos** os perfis, históricos, partituras importadas, correções, metas e configurações; sem servidor nem mesclagem automática.
- Usar armazenamento local com minimização de dados, validar XML/ZIP importados e confirmar substituição na restauração.

## 11. Condições mínimas para declarar a V1 pronta

Aceite somente mediante evidências de: instalação nos dois dispositivos; USB-MIDI P-45 (entrada, saída, acorde, pedal, notas presas); dez partituras sem erros musicais críticos; Espera com acordes e durações; velocidade BPM/% e trecho; perfis, favoritos/coleções, metas, histórico; retomada íntegra e backup/restauração recuperáveis; regressão e medição de fluidez em hardware real.

**Status da beta:** algumas dessas condições não passaram por implementação ou validação. Consulte [07](07-ESTADO-E-ROADMAP.md) e [06](06-BUILD-TESTES.md).

## 12. Governança de decisões

Mudanças pedagógicas (critérios de acerto, avaliação, tolerâncias, arquivos permitidos, retenção de dados) exigem decisão explícita do responsável pelo produto. Registre decisões em [08-DECISOES.md](08-DECISOES.md), divergências em [07](07-ESTADO-E-ROADMAP.md) e testes em [06](06-BUILD-TESTES.md).
