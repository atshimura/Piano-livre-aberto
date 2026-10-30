# Descobrir — layout estável e validação rápida

Pedido de 30/10/2026: [texto integral](prompts/DESCOBRIR_LAYOUT_ESTAVEL_TESTES_RAPIDOS.md), [issue #77](https://github.com/atshimura/Piano-livre-aberto/issues/77). Branch `feat/descobrir-layout-estavel-e-testes-rapidos`, independente do PR #76, sobre main `f91c27853be576a353628e220d4e716d02507bed`. #76 continuava aberto no início. Não integrar nem publicar Release nesta entrega.

## Organização e gravura

Antes, a instrução ocupava a superfície inteira; resultados reservavam faixas superiores de texto e a pauta alternava entre uma e duas claves, com ajuste contínuo pelo envelope. Agora: barra existente preservada, área esquerda com aproximadamente 75% da largura e painel direito com 25%, fundo azul suave, texto quebrado e rolagem vertical independente. A pauta branca tem rolagem horizontal para sequências, sem reduzir as cabeças para caber todas as notas. Desde a abertura há duas pautas completas, sol e fá, inclusive sem entrada.

O motor permanece o OSMD 1.9.8 já empacotado, com seus glifos VexFlow, igual a Estudar. Não há glifos desenhados à mão nem nova biblioteca. A página específica de Descobrir não altera `notation.html`, o relógio ou a câmera do Estudar. MusicXML transitório sempre declara duas pautas, vozes e backups corretos; pausas invisíveis preservam a pauta sem notas. A divisão por registro é uma escolha de clave para visualização, nunca identificação de mão física.

A câmera translada cada pauta gravada por inteiro até origens fixas e aplica uma escala uniforme. Conserva notas, acidentes, linhas suplementares e sua relação com a clave. Conectores de sistema e fórmula de compasso são omitidos nesta apresentação sem métrica. Três níveis: padrão MIDI 36–84; ampliado 21–108; extremo 0–127. A faixa só cresce durante uma visita: histerese sem temporizador e sem diminuir/respirar ao soltar teclas. Reabrir Descobrir volta ao padrão. Sequências ampliam largura, não comprimem a escala vertical. O próximo SVG fica invisível até estar completo e então substitui o anterior, evitando quadros vazios intermediários.

## Entrada e persistência

A entrada ativa apresenta alturas imediatamente; o resultado confirmado continua na sessão musical existente. A liberação mantém o resultado, e a análise de escala/arpejo segue em paralelo sem consumir a resposta isolada. O painel diferencia Entrada atual e Último resultado. Nome PT/internacional, oitavas, cifras, componentes e detalhes de reconhecimento permanecem acessíveis. Aprender segue desabilitado publicamente; catálogo, prática e demonstração futuros continuam preservados em `MusicLabScreen`.

## Testes rápidos e gate completo

`emulator.yml` mantém os checks `build` (workflow próprio) e `emulator` (suíte completa). A proteção da main foi conferida: só exigia build; esta entrega acrescenta emulator, sem retirar build, enforcement de administradores, revisão ou proibições de force-push/exclusão. Publicador e seus testes continuam exigindo os checks completos do SHA final da main e certificado permanente. `ui-fast` não é substituto desses gates.

`debug-apk` compila uma vez. `ui-fast` e o emulador completo de PR recebem o artefato do mesmo run, nomeado pelo checkout SHA, e verificam SHA de origem e SHA-256 do APK. O certificado debug descartável do runner acompanha esse artefato temporário para que a variante release de PR possa atualizar a instalação sintética com a mesma assinatura. Não é uma chave permanente nem um Secret; nenhum Secret é fornecido a PR. Em push confiável de main, a suíte completa recompila debug/release com a assinatura permanente existente; não reutiliza o APK descartável.

`ui-fast`: Descobrir em telefone/tablet, oito estados por perfil, depois fumaça Home → Estudar → Biblioteca → Descobrir → Voltar, com quatro capturas de navegação. Produz logs, medições e capturas reais; não declara cobertura integral de MIDI, MusicXML, backup ou upgrade. O teste completo conserva todas as regressões anteriores e também executa os novos estados de Descobrir. Coleta em falha usa `if: always()` apenas no upload/diagnóstico, nunca para converter falha em sucesso. Runs substituídos do mesmo PR são cancelados; main não é cancelada por essa regra.

Caches: Gradle setup existente preservado. AVD continua frio API29/x86_64/pixel_3, sem cache de disco mutável compartilhado entre jobs: prioridade à reprodução e separação de dados. Baseline completa continua compilada e ensaiada no fluxo completo. Futuro cache de AVD deve incluir versão de imagem/API/arquitetura e demonstrar ganho antes de adoção. Sem promessa de 5–10 minutos sem medição.

Linha de base medida no PR #76: run 41059977477 iniciou 13:02:40 UTC e terminou 13:39:12 UTC (36m32s de workflow); build 41059977579 levou 4m01s. É amostra histórica identificada, não evidência de aprovação desta branch. Tempos rápidos/completos e links desta entrega serão registrados no fechamento do PR após os Actions efetivos, incluindo dependência de compilação e boot. Sem mediana com apenas uma amostra.

## Arquivos, validação e evidências

Produção: `MusicLabDiscoveryScreen`, `DiscoveryStaffView`, `DiscoveryScoreBuilder`, `discovery.html`, `discovery-notation.js`; `MainActivity` apenas liga a intenção de teste protegida por debuggable. Versão candidata **0.9.54-open-beta / 63**, reservando 0.9.53/62 ao PR #76 independente; não altera beta publicada. Ajustes dos testes de upgrade apenas acompanham a versão alvo.

Testes: `SharedDebugApkTest` rejeita SHA divergente, bytes alterados e configuracao de assinatura permanente, alem de conferir exportacao/restauracao da fixture descartavel. MusicXML/JVM (50 checks), sessão musical, `DiscoveryNotationBrowserTest` (duas pautas, origens/escala estáveis e extremos sem corte), `MusicLabDiscoverySelfTest` (regressões existentes e glifos reais), `DiscoveryLayoutSelfTest` e `emulator_discovery_layout.py` (sessão real da UI com MIDI sintético), `emulator_ui_fast.py` (navegação), release não depurável ignorando flags internas. Medições do SVG e asserções nativas complementam capturas; não usar coordenadas auxiliares como prova de pixels.

Estados de captura telefone/tablet: vazio, nota, nota retida, acorde, sequência curta, MIDI0, MIDI127 e nota comum depois do extremo. Nomes `piano-discovery-layout-{phone,tablet}-{estado}.png`, relatório JSON e logs no artefato rápido e na evidência completa. Testes locais já executados: auditoria pública, 11 testes de publicação, sintaxe dos workflows/Python, MusicXML e navegador. Android e resultados remotos só serão declarados aprovados após execução real.

O registro final no PR contém HEAD, checkout temporário dos Actions, runs, duração, APK/hash, capturas e resultados, sem criar commit recursivo apenas para citar o próprio SHA. Inspecionar visualmente os PNG originais baixados. Limites: emulador usa MIDI sintético; Yamaha P-45, Realme 7 5G e Lenovo Tab M9 exigem validação física. Não há migração de dados, nova permissão, serviço, telemetria, assinatura pública ou mudança no repositório histórico privado. V1 não concluída.

## Correcoes encontradas durante a validacao

O primeiro job compartilhado compilou, mas presumiu um caminho de certificado debug inexistente naquele runner. O helper agora consulta signingReport do Gradle e exporta/restaura apenas a configuracao descartavel. O primeiro teste focado tentou medir o titulo oculto no estado vazio; passou a medir a instrucao efetivamente visivel. A sequencia sintetica mantem a mesma sessao entre ataques, sem reconectar e reiniciar o analisador a cada nota. As claves usam uma faixa fixa clonada da gravura original durante rolagem horizontal. Build e emulador cancelam apenas runs substituidos do mesmo PR. Nenhuma dessas correcoes remove assercoes funcionais ou reduz gates.

As primeiras capturas reais identificaram que o OSMD compacto encerrava as linhas apos o evento: as cinco regras finais foram estendidas ate a largura util, sem esticar glifos. Notas/acordes isolados ficam em posicao horizontal definida; sequencias mantem avanco horizontal. A primeira abertura do tablet ainda concluia o ajuste da janela: o teste exige igualdade entre o viewport CSS e o tamanho nativo final antes de registrar a referencia vazia, mantendo a comparacao estrita de escala/origens entre resultados. O navegador tambem exige que as pautas ocupem a largura principal.

O driver focado aguarda o PID apos configuracao do display em vez de presumir processo imediatamente disponivel no retorno do am start. Em falha, salva log e framebuffer antes da limpeza. O gate emulator executa uma guarda explicita mesmo se a preparacao do APK falhar: dependencias falhas nao podem gerar um check obrigatorio skipped que o GitHub trate como suficiente para merge. A guarda falha fechada; nao oculta falhas nem altera a suite.

A investigacao do primeiro frame no tablet identificou a corrida precisa: OSMD.load e assincrono, e a janela podia mudar depois do calculo da camera e antes do render. A gravura agora descarta o enquadramento se largura/altura mudaram durante o load e calcula um novo quadro antes de substituir o anterior. O relatorio usa as dimensoes efetivamente usadas no calculo; o teste de navegador inclui resize durante renderizacao.

A revisao dos PNG aprovados pelo primeiro teste rapido encontrou uma linha suplementar na posicao horizontal anterior ao centro. O VexFlow grava vf-ledgers como grupo irmao de vf-stavenote; ambos agora recebem o mesmo deslocamento. A regressao de navegador compara os centros reais da linha suplementar e da cabeca em Do4 e nos dois extremos, para impedir que a geometria geral aprove novamente esse defeito.

Os limites do piano de 88 teclas (MIDI21/La0 e MIDI108/Do8) tambem entram nas fixtures do enquadramento ampliado e na fidelidade MusicXML; os extremos MIDI0/127 permanecem ensaiados separadamente. O gerador passa a 50 checks JVM.

Na inspecao das rasterizacoes legadas, algumas imagens ainda mostravam o quadro anterior apesar de onRendered ja ter ocorrido. Cada pedido agora tem revisao verificada na ponte Java/JS; callbacks obsoletos sao descartados. O teste legado tambem aguarda postVisualStateCallback antes de desenhar o bitmap, como o teste focado ja fazia. O navegador confere a revisao recebida. As primeiras imagens legadas nao servem como prova final desses acordes/sequencias; os novos artefatos devem ser conferidos no fechamento.

As rasterizacoes de componente agora fixam os LayoutParams do viewport solicitado, conferem o viewport CSS contra o tamanho nativo e restauram os parametros normais ao terminar. Isso impede que o layout pai substitua silenciosamente os tamanhos de teste e evita atribuir um viewport nominal a um bitmap de outra dimensao.

O driver compartilhado emulator_discovery_components.py executa a mesma regressao de 14 componentes no fluxo rapido/completo. O rapido copia somente duas imagens representativas adicionais (acorde entre pautas e sequencia longa), e o completo conserva as 14. O driver exige dimensoes PNG iguais ao viewport solicitado e registra SHA-256 dos bytes originais. Assim a correcao de frame/viewport recebe feedback antecipado sem novo boot ou recompilacao e sem fingir cobertura musical/MIDI integral no job rapido.
