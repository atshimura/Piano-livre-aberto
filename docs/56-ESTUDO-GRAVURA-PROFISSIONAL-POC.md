# 56 — Gravura OSMD no modo Estudo

## Estado atual — Etapa A (0.9.47-open-beta / code 56)

**Decisão de produto:** OSMD passa a ser o renderizador padrão do modo Estudo nas próximas versões de teste. O renderizador temporal permanece disponível por escolha visível do usuário. Esta entrega não declara o aplicativo estável, não inicia a antiga Tarefa 07 e não encerra as tarefas 07–20.

O código de prova OSMD da branch de pesquisa foi portado seletivamente para uma branch baseada na `main`; o PR #46 não é integrado em bloco. O APK usa o mesmo `applicationId` público (`br.org.pianolivre.opensource`), sem nova variante ou segundo aplicativo. A versão anterior de teste publicada é `0.9.46-open-beta` / `versionCode 55`; esta entrega usa `0.9.47-open-beta` / `versionCode 56`.

## Uso e preservação

- Ao abrir ou trocar uma partitura no Estudo, o aplicativo carrega OSMD automaticamente.
- Na faixa de controles, **OSMD · nova** e **Anterior · temporal** alternam o desenho. A seleção é apenas de apresentação e não é gravada no perfil.
- A alternância usa o beat atual; não edita o MusicXML nem modifica relógio MIDI, andamento, perfil, histórico, arquivos ou dados de estudo.
- **Partitura Livre** conserva sua função normal de leitura OSMD convencional. Não há gesto oculto para ativar o OSMD do Estudo.
- OSMD usa SVG horizontal, mapeamento beat→posição gráfica ligado ao relógio nativo, cursor de estudo, Modo Espera, marcas de mão e redimensionamento com teclado guia. O render temporal continua acessível como recuperação/comparação.

## Layout e limites

O SVG completo é redimensionado proporcionalmente ao espaço disponível do WebView. O viewport do Estudo permite movimento horizontal e bloqueia rolagem vertical; ambas as pautas são ajustadas ao espaço útil, com margem para o envelope gráfico. A geometria musical do OSMD não é comprimida por remoção de símbolos. Ao abrir/fechar o teclado guia, o ajuste vertical preserva o mapa horizontal quando a largura não muda.

Essas garantias são instrumentadas no emulador; legibilidade percebida, desempenho em aparelhos reais e comportamento no Yamaha P-45 ainda precisam de avaliação do usuário. A compatibilidade MIDI e o espaço visual não podem ser confirmados por emulador.

## Testes e atualização

### Validação automatizada da Etapa A

Na branch `feat/etapa-a-osmd-padrao`, PR #49, SHA `2213f6f0777f1fff4cd59cd45751865449f21d88`, passaram o [build e testes automatizados](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39528005169) e o [roteiro Android no emulador](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39528005167). O emulador confirmou:

- OSMD inicia automaticamente ao abrir e reabrir o Estudo; a opção temporal visível renderiza a alternativa e o retorno a OSMD funciona.
- Partituras diferentes e um MusicXML sintético complexo carregam sem falha; o arquivo musical, as duas pautas e os símbolos avaliados pelo roteiro permanecem no SVG.
- Ajuste proporcional ao espaço disponível e `scrollTop=0`, com teclado guia aberto/fechado. Também passou em visores de teste de 1440×2160 e 1600×2560.
- Cursor, reprodução, seleção de mãos, Modo Espera, mapeamento gráfico para eventos MIDI nativos e atualização da partitura sem recarga indevida.
- O ensaio de interface do APK de release no emulador mostrou perfil sintético, teclado e velocidade preservados (`PUBLIC_RELEASE_UPGRADE_UI_PASS`).

O teste de atualização com o APK público anterior assinado permanentemente é uma etapa separada do workflow confiável na `main`; os workflows de PR não recebem essa assinatura. O teste de interface acima usa dados sintéticos no emulador e não prova sozinho a atualização binária assinada. A confirmação final deve registrar o resultado do teste Android que instala `v0.9.46-open-beta` code 55 e atualiza com o novo APK code 56, verificando o mesmo certificado e os dados sintéticos preservados.

O beta público da edição aberta é assinado somente nos workflows confiáveis da `main`, com a chave permanente do repositório. O APK público anterior conferido é `br.org.pianolivre.opensource`, `0.9.46-open-beta` / code 55, com fingerprint SHA-256 `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`. A versão proposta é `0.9.47-open-beta` / code 56. Não distribuir APK de PR assinado com chave efêmera. Após a integração, conferir pacote, versão e o mesmo certificado com `aapt` e `apksigner`; baixar o APK da pré-release da edição aberta e instalá-lo por cima com atualização Android normal. Não desinstalar o app. Se pacote ou certificado diferirem, interromper antes da instalação para não arriscar os dados.

## Histórico técnico anterior à Etapa A

OSMD 1.9.8 já era empacotado offline para **Partitura Livre**. A branch `research/estudo-gravura-osmd-poc` acrescentou a renderização horizontal de prova no Estudo, reconciliação de notas MusicXML com eventos nativos, mapa óptico, cursor, entrada de Espera, guia de mãos e instrumentação de layout. Os testes dessa pesquisa não equivalem a aceite visual em celular/tablet. O usuário autorizou o espaçamento gráfico óptico desde que o relógio e os eventos MIDI permanecessem no modelo nativo; essa separação é mantida.

O PR #46 continua uma trilha de pesquisa com mais mudanças e evidências do que a Etapa A precisa. Esta entrega porta somente os componentes requeridos para avaliar OSMD como padrão e os testes correspondentes; seus outros commits e as tarefas posteriores permanecem fora desta integração.
