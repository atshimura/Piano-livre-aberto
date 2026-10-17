# 51 — Agrupamento convencional de vigas sem <beam> explícito (0.9.42)

## Motivação e implementação
A notação temporal já respeitava dados `<beam>` do MusicXML; faltavam barras nas colcheias e semicolcheias quando o arquivo trazia figuras sem agrupamento. Uma passagem sem marcas agora infere **apenas** grupos regulares de notas curtas, por fonte, compasso, pauta, voz, duração e pulso. Nunca agrupa sobre pausa implícita/hiato, mudança de pulso, outra voz ou ligadura de duração transpassando o pulso.

Compasso simples agrupa em unidades do denominador; compassos compostos 6/8, 9/8, 12/8 agrupam em tempos pontuados de três colcheias. Um grupo tem no mínimo duas notas; semicolcheias recebem segundo nível ou gancho onde apropriado. MusicXML com marcas explícitas **não é reescrito nem completado por hipótese**.

A função `inferConventionalBeams` age apenas na memória do renderizador, antes do cálculo das hastes. Não toca alturas, valores temporais, áudio, MIDI, parser Java, nem o arquivo importado. Testes `NotationAdvancedTest.js` cobrem 4/4, 6/8, vozes, hiatos, acordes, níveis e preservação de `<beam>`.

## Limitações e aceite
Ainda faltam revisão editorial/visual das claves e hastes em partituras variadas, subdivisões irregulares, beaming editorial de síncopes e alternância de compasso dentro da mesma medida. Não deduzir que toda partitura visual foi validada. Verificar CI do PR e emulador no SHA correto; testes físicos Lenovo/Realme/Yamaha pendentes (#11). Escopo geral #34.
