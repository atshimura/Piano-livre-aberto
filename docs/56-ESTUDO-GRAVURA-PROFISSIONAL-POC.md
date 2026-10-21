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

No PR #49, integrado na `main` pelo commit de merge `535e8ea94d674a886381a5312d71839274ce358b`, passaram o [build assinado e os testes automatizados](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39531053142) e o [roteiro Android no emulador](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39531053402). O emulador confirmou:

- OSMD inicia automaticamente ao abrir e reabrir o Estudo; a alternativa temporal visível renderiza e permite retornar a OSMD.
- Várias partituras e um MusicXML sintético complexo carregam sem falhas; duas pautas simultâneas, símbolos incluídos na fixture e rolagem vertical zero.
- Ajuste proporcional no espaço disponível, inclusive com teclado guia aberto/fechado e visores de teste 1440×2160 e 1600×2560.
- Cursor, reprodução, seleção de mãos, Modo Espera, movimento horizontal e reconciliação das notas gráficas com eventos MIDI nativos.

### Atualização assinada e distribuição

A pré-release aberta está publicada em [v0.9.47-open-beta](https://github.com/atshimura/Piano-livre-aberto/releases/tag/v0.9.47-open-beta); baixar [PianoLivre-0.9.47-open-beta.apk](https://github.com/atshimura/Piano-livre-aberto/releases/download/v0.9.47-open-beta/PianoLivre-0.9.47-open-beta.apk). `aapt`/metadados de build confirmam `br.org.pianolivre.opensource`, `0.9.47-open-beta`, code 56, APK não depurável. `apksigner verify` confirmou o certificado permanente SHA-256 `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`, igual ao APK público anterior.

O teste [Actions 39531053402](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39531053402) instalou o APK real de `v0.9.46-open-beta` code 55, preparou dados sintéticos e executou atualização Android `adb install -r` para code 56. Resultado: `PUBLIC_APK_UPDATE_PRESERVED_DATA_PASS`; manteve o perfil sintético ID7, teclado, andamento 80% e a partitura importada. O hash do arquivo importado foi conferido antes/depois. Essa prova cobre atualização do binário público e dados sintéticos no emulador API29; não acessa nem modifica os dados pessoais do aparelho do usuário.

A instalação pode ser feita por atualização Android normal sobre a edição aberta instalada; não desinstalar. Se o Android mostrar incompatibilidade de assinatura ou pacote, parar e não remover o aplicativo para proteger os dados. Não instalar APK de PR ou chave efêmera. A impressão digital final e os metadados acompanham a pré-release.

## Histórico técnico anterior à Etapa A

OSMD 1.9.8 já era empacotado offline para **Partitura Livre**. A branch `research/estudo-gravura-osmd-poc` acrescentou a renderização horizontal de prova no Estudo, reconciliação de notas MusicXML com eventos nativos, mapa óptico, cursor, entrada de Espera, guia de mãos e instrumentação de layout. Os testes dessa pesquisa não equivalem a aceite visual em celular/tablet. O usuário autorizou o espaçamento gráfico óptico desde que o relógio e os eventos MIDI permanecessem no modelo nativo; essa separação é mantida.

O PR #46 continua uma trilha de pesquisa com mais mudanças e evidências do que a Etapa A precisa. Esta entrega porta somente os componentes requeridos para avaliar OSMD como padrão e os testes correspondentes; seus outros commits e as tarefas posteriores permanecem fora desta integração.
