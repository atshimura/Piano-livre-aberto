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

Testes: MusicXML/JVM (42 checks), sessão musical, `DiscoveryNotationBrowserTest` (duas pautas, origens/escala estáveis e extremos sem corte), `MusicLabDiscoverySelfTest` (regressões existentes e glifos reais), `DiscoveryLayoutSelfTest` e `emulator_discovery_layout.py` (sessão real da UI com MIDI sintético), `emulator_ui_fast.py` (navegação), release não depurável ignorando flags internas. Medições do SVG e asserções nativas complementam capturas; não usar coordenadas auxiliares como prova de pixels.

Estados de captura telefone/tablet: vazio, nota, nota retida, acorde, sequência curta, MIDI0, MIDI127 e nota comum depois do extremo. Nomes `piano-discovery-layout-{phone,tablet}-{estado}.png`, relatório JSON e logs no artefato rápido e na evidência completa. Testes locais já executados: auditoria pública, 11 testes de publicação, sintaxe dos workflows/Python, MusicXML e navegador. Android e resultados remotos só serão declarados aprovados após execução real.

O registro final no PR contém HEAD, checkout temporário dos Actions, runs, duração, APK/hash, capturas e resultados, sem criar commit recursivo apenas para citar o próprio SHA. Inspecionar visualmente os PNG originais baixados. Limites: emulador usa MIDI sintético; Yamaha P-45, Realme 7 5G e Lenovo Tab M9 exigem validação física. Não há migração de dados, nova permissão, serviço, telemetria, assinatura pública ou mudança no repositório histórico privado. V1 não concluída.
