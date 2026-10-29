> **Arquivo de referência técnica transferido para a edição aberta.** Parte das afirmações sobre versão, CI, aplicativo privado ou certificados descreve apenas o histórico de desenvolvimento. Para a situação vigente no repositório público, consulte [AGENTS.md](../AGENTS.md), [build e testes](06-BUILD-TESTES.md) e [estado](07-ESTADO-E-ROADMAP.md). Links para execuções privadas podem estar inacessíveis a terceiros.

# 08 — Registro de decisões de produto e arquitetura (ADRs)

**Objetivo:** manter a intenção do usuário disponível sem acesso à conversa original. **Decisões de produto** não devem ser alteradas silenciosamente por conveniência técnica. Este registro combina escolhas aprovadas com decisões técnicas do código, identificando claramente o tipo e o status.

## D-001 — Aplicativo independente, baseado em formatos abertos
**Tipo:** aprovado pelo produto. O Piano Livre busca uma experiência de aprendizagem comparável à do Flowkey, **sem cópia de código, visual proprietário, vídeos ou ativos**. Arquivo musical prioritário MusicXML/MXL. MIDI de entrada/saída por APIs Android. Não assumir direitos de redistribuição dos arranjos MXL.

## D-002 — Android local primeiro; Windows e nuvem depois
**Tipo:** aprovado. Primeiro Android com Yamaha P-45 USB-MIDI, Lenovo Tab M9 e Realme 7 5G; orientação paisagem. Dados locais, sem sincronização automática, sem servidor obrigatório; backup/restauração exclusivamente manuais na fase atual. Windows e sincronização eventual são futuros. **Não implementar backend ou login sem aprovação.**

## D-003 — Núcleo pedagógico e modos
**Tipo:** aprovado. **Espera** sem penalizar demora e sem avançar com erro; **Contínuo** orientado pelo andamento; **Demonstração** executada no instrumento via MIDI. Seletor mão direita/esquerda/ambas; não inferir mãos por altura da nota. **Regra vigente da Aprendizagem:** notas extras devem ser indicadas mas não bloqueiam; acordes usam janela ajustável **só após calibração real por perfil**, sem milissegundos arbitrários; duração e pedal **orientam** e não bloqueiam. Avaliação rigorosa de duração/pedal é futura e separada. Implementação e testes em [17](17-REGRAS-ESPERA-CALIBRACAO.md).

## D-004 — Avaliação separada de aprendizagem
**Tipo:** aprovado conceitualmente, não implementado. Aprendizagem sem avaliação rigorosa, avaliação voluntária com indicadores **notas, ritmo, duração/articulação** separados, sem nota global obrigatória. Metas podem pedir notas, notas+ritmo ou avaliação completa. Tolerâncias iniciante/intermediário/avançado com ajuste em ms; **nenhum número concreto aprovado**. Não criar “pontuação” arbitrária.

## D-005 — Partitura temporal com contexto fixo
**Tipo:** aprovado/solicitado em 14/10/2026. A partitura desliza suavemente sob cursor de referência. **Claves, armaduras e fórmula de compasso ficam fixas à esquerda**; seu conteúdo atualiza em mudanças musicais; sinais locais acompanham as notas. **Implementação atual:** renderer temporal SVG, tiles, painel fixo; exige validação de exatidão e fluidez real.

## D-006 — Controle Início e seleção direta de trecho
**Tipo:** aprovado/solicitado. Botão **Início** retorna ao começo, pausa e suspende repetição anterior. Seleção de trecho **diretamente na pauta por toque e arraste**, ajustando limites; repetição disponível. **Implementado básico** com bridge JS→Android; duração, persistência e formas manuais de repetição requerem extensão.

## D-007 — Teclado guia opcional e ampliado
**Atualização aprovada em 29/10/2026, Prompt 2:** o Estudar remove o teclado guia e seus controles; preserva a preferência antiga no armazenamento/backup e mantém reconhecimento MIDI, seleção de mãos e demonstração. As barras superiores recolhem no início da execução e voltam por toque na pauta; a barra inferior permanece. Esta decisão substitui no Estudar o comportamento histórico abaixo, sem remover componentes usados por outras telas. [Documento 71](71-TELA-ESTUDAR-PROMPT-02.md).

**Tipo:** aprovado/solicitado em 14/10/2026. Teclado visual orienta notas esperadas e MIDI recebido; não precisa ser um teclado tocável. Ao abrir o teclado, **esconder automaticamente as três faixas superiores** (cabeçalho global, título da música, modos/mãos) para ampliar a partitura, preservando a barra inferior para fechar. Restaurar ao fechar. **Implementado na HEAD 59112581f... e aprovado em emulador**, ainda necessita confirmação com usuário e aparelhos reais.

## D-008 — Sem sintetizador Android obrigatório
**Atualização aprovada em 29/10/2026, Prompt 2:** a ausência de áudio/vídeo obrigatório permanece; o teclado guia deixa de ser alternativa visual no Estudar. A orientação musical continua na pauta real, com entrada/saída USB-MIDI preservadas. Não há novo sintetizador.

**Tipo:** aprovado. Demonstração pode enviar MIDI bidirecional ao P-45; o som principal pode vir inteiramente do piano. Eventual vídeo é condicionado a mídia autorizada e sincronizada. Sem vídeo usar teclado guia. **Não adicionar áudio/vídeo obrigatório para funcionar offline.**

## D-009 — Fonte musical importada preservada
**Tipo:** aprovado. Correções de mão, voz, retornos e pedal devem ficar em **camadas pedagógicas editáveis separadas**, sem sobrescrever arquivo MusicXML/MXL original. Editor integral de notas/compassos fora da V1. **Ainda não implementado**.

## D-010 — Biblioteca pessoal compartilhada; progresso por perfil
**Tipo:** aprovado. Dez músicas iniciais de teste, biblioteca compartilhada, favoritos/coleções e histórico individual. Perfis locais simples (nome e avatar, sem senha/PIN), com operações criar/renomear/excluir e troca. **Parcial:** dez músicas, nomes e troca; recursos avançados ainda ausentes.

## D-011 — Recuperação e backup abrangente
**Tipo:** aprovado. Pausar em chamada/segundo plano/desconexão; persistir música, posição, velocidade, mãos, modo, trecho e demais parâmetros. Backup/restauração manuais cobrem perfis, dados, partituras, metas, correções e histórico; sem sincronização automática ou mesclagem. **Implementado parcialmente:** backup JSON **v2** inclui preferências tipadas e arquivos importados com SHA-256, aceita backups v1, valida antes de restaurar e usa estágio/rollback com recuperação transacional e confirmação. Funcionalidades futuras (metas, histórico completo, correções etc.) ainda não existem e portanto não são recuperadas. Ver [05](05-DADOS-BACKUP.md) e [16](16-BACKUP-MIDI-CONSISTENCIA.md).

## D-012 — Diferenciação de leitores musicais
**Tipo:** decisão técnica existente, **não imutável**. O modo de estudo usa renderer temporal SVG próprio; leitura livre usa OpenSheetMusicDisplay offline. Alteração pode ser aprovada se preservar notação, interação e desempenho, além de testes. **Problema técnico conhecido:** Score.java e renderer JS interpretam MusicXML separadamente.

## D-013 — Um relógio musical, animação desacoplada
**Tipo:** decisão técnica atual. MainActivity mantém beat musical; NotationView entrega amostras e JS usa requestAnimationFrame para movimento. Qualquer otimização deve preservar Note On/Off, pausa, velocidade, loop e regras pedagógicas. **Desempenho em emulador não garante fluidez real**.

## D-014 — Beta assinada com chave de teste estável
**Tipo:** decisão técnica provisória. Múltiplas betas usam mesmo certificado de teste para atualizações sem desinstalação. **Não empregar em produção**; verificar versionCode antes de distribuir. Linha de betas verificadas: 0.9.9/code18, 0.9.10/code19 e APK privado 0.9.11/code20. O Gradle na branch da 0.9.11 ainda declara 0.9.10/code19 **no arquivo versionado**; o script de preparação atualiza a versão apenas no workspace de build. Unificar essa declaração sem mexer na assinatura continua pendente. Ver [25](25-CONTINUIDADE-0.9.9-0.9.11.md).

## D-015 — V1 deve ser experiência de aprendizagem completa
**Tipo:** aprovado. Núcleo já funcional não significa V1 concluída. Primeira versão deverá incluir biblioteca organizada, perfis, andamento BPM/%, repetição, metas básicas, recuperação consistente, backup/restauração e testes de aceitação. Análises avançadas e edição complexa podem aguardar etapa posterior.

## Como atualizar

**A numeração D-016 não tem registro neste arquivo; não inventar aprovação retroativa.** Para próximas decisões, usar D-018 e seguintes, preservando D-017 já formalizada. Adicione **D-018, D-019...** com: data, decisão (produto/técnica), motivo, opções analisadas quando houver, consequências, arquivos impactados, evidência de aprovação do produto e situação implementada/parcial/futura. **Não invente decisões do usuário a partir de uma suposição de uma IA.** Em divergência entre requisito e código, o requisito continua registrado e o código é classificado como parcial até que haja aprovação de mudança de escopo.

Veja [01](01-VISAO-E-REQUISITOS.md), [07](07-ESTADO-E-ROADMAP.md) e [10](10-CONTINUIDADE.md).

## D-017 — Notas extras, simultaneidade, duração e pedal no Espera (14/10/2026)

**Aprovado explicitamente pelo usuário:** notas extras não bloqueiam avanço com todas as notas esperadas, mas devem ser indicadas; acordes exigem janela temporal ajustável, **sem escolher ms antes da calibração**; duração e pedal durante Aprendizagem apenas orientam e não são critérios de avanço. O aplicativo oferece coleta de intervalos reais de acordes e edição da janela; quando não calibrado, registra estado sem limite temporal e avisa. Futuras regras de Avaliação são outro assunto. Código e testes em [17](17-REGRAS-ESPERA-CALIBRACAO.md).
