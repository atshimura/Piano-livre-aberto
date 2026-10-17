# 47 — Identificação temporária de notas na partitura (0.9.38 / code47)

## Decisão de produto
No **modo Estudo temporal**, tocar em uma cabeça de nota mostra por aproximadamente dois segundos uma etiqueta com nome musical em português, oitava e letra internacional: `Dó4 · C4`, `Fá♯5 · F♯5`, `Si♭3 · B♭3`. Mesmo em acordes, a identificação é **da nota tocada**, não de todos os sons simultâneos. Não interromper acompanhamento visual ou música.

Além disso, quando houver cifra confiável explicitamente presente no MusicXML, acrescentar em segunda linha a identificação harmônica (por exemplo, `Am — Lá menor`); não adivinhar cifras a partir de notas isoladas.

## Implementação nesta entrega
Arquivo `PianoLivreAndroid/app/src/main/assets/temporal-notation.js`.

- Cada cabeça desenhada obtém a grafia da nota original `<pitch><step>/<alter>/<octave>`. A armadura e o acidente são preservados pela altura explicitada em MusicXML. As letras são **as escritas**, não enarmonia inferida de MIDI; oitava é a do MusicXML.
- Um delegador `click` na raiz do renderizador visualiza etiqueta temporária somente se o clique atingir a elipse da cabeça de nota. Ignora clique fora da nota ou durante edição de trecho. Timer e popup são descartados no teardown/re-render, sem armazenamento ou intervenção no relógio.
- Processa `<harmony>` com raiz cromática de -2 a +2 e `<kind>` explicitamente reconhecido: maior, menor, aumentado, diminuto, dominante, sétimas, meio-diminuto e sus2/sus4. Não extrapola a harmonia para compassos posteriores ou pautas incompatíveis; alternativas desconhecidas são omitidas em vez de inventadas. Trechos sem `<harmony>` continuam exibindo apenas a nota.
- O cálculo temporal e a geometria de deslocamento das notas não são modificados. Não afeta a leitura livre OSMD, pontuação nem dados persistidos.

## Testes
`PianoLivreAndroid/tests/NoteIdentificationTest.js` cobre: Dó4/C4, Si♭3/B♭3, Fá♯5/F♯5, dobrado, entradas inválidas/microtons não rotulados, dois acordes MusicXML sucessivos e ausência de extrapolação entre compassos. Incluído no workflow JS após carregar a dependência de teste DOMParser. A validação em emulador exige o mesmo SHA do PR; a interação de toque no dispositivo físico ainda precisa de confirmação.

## Restrições conhecidas e próximos passos
- A etiqueta só contém cifras quando `<harmony>` com qualidade suportada é fornecido pela partitura, não quando o usuário toca um acorde via MIDI ou quando o arquivo não traz acorde. No Laboratório, a identificação por alturas utiliza motor separado de `MusicTheory.java`.
- Notas de graça continuam com glifo `temporal-grace-note` e podem não ser selecionáveis nesta entrega; evitar mudar a classificação de realce sem regressão própria.
- A acessibilidade por teclado externo e diâmetro de área de toque de cabeças pequenas ainda precisam de avaliação.
- A edição de trecho continua prioritária em seu modo próprio.
- Próximo trabalho na issue [#34](https://github.com/atshimura/Piano-livre-aberto/issues/34): Espera de 5 segundos ajustáveis/cursor antes de notas, correção de hastes/vigas/claves, catálogo e análise de sequências do Lab, progresso. V1 ainda requer hardware (#11).

A documentação [45](45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md) continua sendo a referência de escopo global; esta página registra **apenas** o recorte implementado e como retomá-lo.
