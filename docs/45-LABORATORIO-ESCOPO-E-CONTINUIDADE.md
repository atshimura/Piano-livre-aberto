# 45 — Laboratório Musical: decisões, primeira entrega e continuidade

Data: 17/10/2026. Referência: [issue #34](https://github.com/atshimura/Piano-livre-aberto/issues/34). Este documento especifica o **escopo aprovado**, distinguindo rigorosamente as funcionalidades já implementadas daquelas ainda pendentes. Não confundir implementação de uma base com Laboratório completo nem CI com aceite físico.

## Decisões aprovadas para o produto

### Melhorias dos modos atuais
1. **Espera:** botão "Iniciar"; após acionamento, partitura avança gradualmente até a posição de estudo por **5 segundos inicialmente**, configuráveis. Sem contagem regressiva textual e sem prazo para tocar após a preparação. Cursor/referência **antes** das cabeças de nota, nunca as cobrindo; manter extras indicadas mas não bloqueantes, duração/pedal orientativos e acordes com janela somente após calibração.
2. **Notação temporal:** preservar proporção horizontal do tempo musical; aperfeiçoar geometria/ancoragem de claves Sol e Fá, tamanho/direção de hastes e vigas. Respeitar beaming MusicXML; **inferir agrupamentos convencionais** por compasso, pulso, voz e figura quando faltar `<beam>`. Não unir cegamente colcheias adjacentes nem alterar fonte importada.
3. **Biblioteca:** grade compacta responsiva, reduzindo vazios e mantendo busca, filtros, favoritos, coleções e ações acessíveis.
4. **Toque na partitura:** ao tocar **uma cabeça** de nota no modo Estudo, mostrar por ~2 segundos "Dó4 · C4" ou equivalente; usar altura escrita, clave, armadura e acidentes. Se houver cifra harmônica explicitamente informada ou confiavelmente contextualizada no MusicXML, exibir "Am — Lá menor" de forma secundária; **não inventar acordes a partir de notas isoladas**. Em acordes, mostrar a nota individual selecionada; não interromper rolagem ou áudio.

### Laboratório Musical
- Nova área com **Descobrir** e **Aprender**; entrada por USB-MIDI e teclado virtual com **Tocar** (multitouch momentâneo) ou **Montar** (seleção travada).
- **Descobrir:** nomes PT/internacionais com oitava, intervalos, acordes, inversões e variantes com alternativas relevantes. Notas e acordes simultâneos são identificáveis instantaneamente; escalas/arpejos/padrões precisam de análise de sequência, automática e delimitada por início/fim. Distinguir classe de altura/coleção modal de uma tonalidade funcional deduzida apenas pelo contexto.
- **Aprender:** catálogo livre, sem bloqueio por níveis; notas, intervalos, acordes, escalas, arpejos. Consulta por nome/cifra, estrutura, posição/inversão, região e dedilhados **sugeridos** por mão. Pauta musical e teclado **sempre visíveis juntos**.
- **Demonstração:** reprodução local com velocidade/repetição e destaque na pauta/teclado. O teclado guia visual não é sintetizador.
- **Prática:** livre por padrão; opção de mão direita/esquerda/ambas, observação de acertos/faltas/extras, sem impor dedilhado ou velocidade. Metrônomo e avaliação rítmica opcionais, calibrados em hardware real **sem janela de tolerância presumida**.
- **Histórico:** por perfil, offline, preservado em backup sem migrar/destruir dados anteriores. Avaliação musical não se confunde com nota global.

## Primeira entrega implementada nesta branch (a verificar nos Actions)

Arquivos:
- `MusicTheory.java`: nomes Dó4/C4, intervalos cromáticos, tríades maiores/menores/diminutas/aumentadas/suspensas, acordes com sétima definidos no código, inversões por nota de baixo; apresenta interpretações alternativas exatas. **Não** interpreta tonalidade, notas omitidas, grafia enarmônica dependente de armadura, poliacordes, escalas ou arpejos. As cifras deste motor são cromáticas com sustenidos, não equivalem à grafia de um MusicXML específico.
- `MusicLabScreen.java`: tela acessível pelo botão **Laboratório** da biblioteca; abas Descobrir e Aprender/Praticar; teclado virtual em 3 oitavas com mudança de faixa, toques simultâneos, seleção travada e limpeza; entrada física usa o estado MIDI já existente. Um mapa simplificado da altura em duas pautas fica visível junto ao teclado. **Não é um gravador de partitura nem representação rítmica/musicológica completa**; claves desenhadas como legendas "Sol/Fá" nesta entrega, sem pretensão de notação final.
- O modo Aprender oferece **cinco acordes exemplares** (Am, C, F, G7, Dm); prática livre compara classes de altura executadas às esperadas e indica faltas/extras. Não verifica dedilhado, mão, oitava, ritmo, pedal ou duração. Nenhuma pontuação ou histórico é criado nesta entrega.
- `PianoScreens.java`: acesso, exibição exclusiva da tela e ligação da entrada física com o Laboratório; não altera persistência existente.
- `MusicTheoryTest.java` e `.github/workflows/build.yml`: novos testes JVM ligados à compilação do mesmo PR.

## Pendências em ordem sugerida

1. Confirmar build Android/JVM/JS e smoke no emulador do **SHA exato** da branch. Corrigir falhas antes de integrar. Testar multitouch e foco de `ScrollView` no emulador e depois fisicamente.
2. Concluir melhorias pontuais aprovadas: Espera/preparação; biblioteca compacta; identificação por toque com semântica MusicXML; notação revisada (cada uma com fixtures e testes). Evitar alterar os dois parsers independentemente sem testes cruzados.
3. Evoluir Laboratório com representação musical correta, catálogo pesquisável e acordes adicionais, dedilhados fundamentados, sequências de escala/arpejo e identificação de ambiguidades.
4. Demonstração offline sincronizada, prática guiada para escalas/arpejos, metrônomo opcional/calibração e persistência por perfil com backup v2 e ensaios de atualização.
5. Revalidar Yamaha P-45, Realme 7 5G, Lenovo Tab M9; não fechar #11 ou declarar V1 somente por teste de CI.

## Desenvolvimento seguro

Ler `AGENTS.md`, `docs/10-CONTINUIDADE.md`, `docs/17-REGRAS-ESPERA-CALIBRACAO.md`, `docs/34-BIBLIOTECA-LOCAL.md` e este documento. Branch + PR, publicar somente pela main após checks, assinatura permanente exclusivamente via GitHub Actions Secrets. Sem acesso ao repositório histórico privado, Internet no APK, telemetria ou alteração destrutiva. A primeira entrega não altera schema de perfis, backup ou assets de partituras.

A versão do APK desta entrega é **0.9.36-open-beta / code45**, caso o PR correspondente seja integrado e o build assinado da main tenha sucesso. Artefato/release só podem ser declarados existentes depois de sua verificação.

## Registro de validação

- **Testes adicionados:** `MusicTheoryTest` (JVM): notas, intervalos, acordes, inversões, alternativas, oitavas dobradas, intervalos/entradas inválidas e proibição de inferir acorde a partir de 1–2 notas.
- **Estado inicial ao escrever este documento:** arquivos enviados para a branch; execução da CI deste PR **ainda não verificada**.
- **Pendência física:** entrada/saída real Yamaha e usabilidade Android Realme/Lenovo continuam sem ensaio desta entrega.
