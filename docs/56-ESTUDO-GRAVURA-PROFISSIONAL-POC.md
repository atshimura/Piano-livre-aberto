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

As regressões selecionadas para esta entrega verificam abertura OSMD, troca para o render temporal e retorno, fixtures MusicXML, duas pautas, rolagem vertical, teclado guia, cursor/posição, reprodução, Espera e conteúdo do usuário após atualização. Os links dos workflows e o resultado do ensaio de atualização devem ser anotados aqui após a conclusão do PR e dos Actions deste SHA.

O beta público da edição aberta é assinado somente nos workflows confiáveis da `main`, com o segredo permanente do repositório. Não distribuir APK de PR assinado com chave efêmera. Para instalar, baixar o artefato ou pré-release gerado após os checks do commit integrado e usar atualização Android normal; não desinstalar o app. A compatibilidade depende do pacote e do certificado conferidos no APK final.

## Histórico técnico anterior à Etapa A

OSMD 1.9.8 já era empacotado offline para **Partitura Livre**. A branch `research/estudo-gravura-osmd-poc` acrescentou a renderização horizontal de prova no Estudo, reconciliação de notas MusicXML com eventos nativos, mapa óptico, cursor, entrada de Espera, guia de mãos e instrumentação de layout. Os testes dessa pesquisa não equivalem a aceite visual em celular/tablet. O usuário autorizou o espaçamento gráfico óptico desde que o relógio e os eventos MIDI permanecessem no modelo nativo; essa separação é mantida.

O PR #46 continua uma trilha de pesquisa com mais mudanças e evidências do que a Etapa A precisa. Esta entrega porta somente os componentes requeridos para avaliar OSMD como padrão e os testes correspondentes; seus outros commits e as tarefas posteriores permanecem fora desta integração.
