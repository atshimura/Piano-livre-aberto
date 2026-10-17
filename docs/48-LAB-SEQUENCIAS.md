# 48 — Laboratório: análise delimitada de escalas e arpejos (0.9.39 / code48)

## Escopo desta entrega
Na tela **Descobrir**, disponibilizar **Iniciar sequência** e **Concluir análise**. Enquanto a captura estiver ativa, receber ataques de notas do instrumento USB-MIDI ou de toques individuais no teclado virtual. Mostrar resultados apenas depois que o usuário definir o fim; não presumir tempos em milissegundos e não confundir acorde simultâneo com arpejo.

## Implementação
- `MusicSequenceAnalyzer.java`: comparação determinística de padrões de intervalos de escalas/arpejos **completos** ao longo de **uma oitava**, em ordem estritamente ascendente ou descendente. Escalas maior, menor natural, harmônica, melódica ascendente, modos dórico/frígio/lídio/mixolídio/lócrio, pentatônicas e cromática. Arpejos de tríades e acordes com sétima definidos no código.
- `MusicLabScreen.java`: controle de iniciar/concluir análise e status com tamanho da sequência e resultado. Teclado Tocar e MIDI físico compartilham função de registrar ataques. Toques simultâneos no teclado virtual não geram ordem arbitrária de sequência; em Montar o app não registra ataques individuais para o analisador. A tela Aprender continua independente. Não grava dados nem interfere nos modos normais da partitura.
- `PianoScreens.java` / `MainActivity.java`: encaminham ataques físicos **novos** quando Laboratório estiver visível e atividade ativa.
- `MusicSequenceAnalyzerTest.java`: verificação JVM de sequências completas representativas, ordens, notas inválidas, repetições e melodias não-monotônicas. Acrescentado workflow JVM de CI.
- Os nomes são hipóteses de padrão **iniciado na nota executada**, não afirmações de tonalidade funcional da música. A grafia enharmônica nas sugestões vem de uma tabela cromática genérica, sem análise de armadura.

## Limitações documentadas (não considerar entregue)
- Não identifica padrões incompletos, mudanças de direção, fraseado, escalas fora de um intervalo de oitava, arpejos em inversões, velocidade, quiálteras ou figuracões rítmicas.
- O **reconhecimento espontâneo automático** de escalas/arpejos durante execução livre, que foi aprovado juntamente com a análise delimitada, **ainda não está implementado**. Não tentar atribuir tonalidade ou inventar contexto musical.
- Ainda faltam catálogo pesquisável, escala/arpejo demonstrados, áudio offline, pauta profissional, dedilhados opcionais, prática guiada estendida, metrônomo e histórico por perfil.
- O modo Montar é exclusivamente para seleção de notas/acordes no teclado virtual, não cria sequência ordenada.
- A validação real Yamaha P-45/Realme/Lenovo continua pendente (#11). Não substituir por emulador.

## Retomada e testes
Verificar SHA de PR/Actions (JVM + Gradle + emulador). Se testes passarem, integrar em `main` via merge protegido, confirmar histórico. Referência funcional principal [doc 45](45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md), evolução da issue [#34](https://github.com/atshimura/Piano-livre-aberto/issues/34) e documentação anterior [47](47-NOME-DA-NOTA-TOQUE.md). Não alterar histórico privado ou dados.
