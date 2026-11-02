# Alteração 4 — Barras superiores permanentes na tela Estudar

**Decisão do produto:** as duas barras superiores — título/BPM/posição/USB-MIDI e modos/mãos — permanecem visíveis durante toda a permanência em **Estudar**. Iniciar/pausar reprodução, modo Espera, tocar uma nota, retorno de segundo plano e controles de MIDI não podem escondê-las. A barra inferior continua fixa.

## Implementação

- `StudyChromeState` deixa de alternar a visibilidade por início de transporte: `update`, `reveal` e `visible` retornam sempre verdadeiro.
- `PianoScreens.applyStudyChrome` força ambas as barras a `View.VISIBLE` quando Estudar está aberto, registra o estado/debug e só redimensiona a partitura se alguma barra precisou ser restaurada (por exemplo após navegar de outra tela). O tamanho da pauta não oscila por iniciar/pausar execução.
- As transições a **Tela Inicial, Laboratório, Criação ou Partitura Livre** continuam ocultando a barra superior de Estudar fora dessa tela. Não há alteração de conteúdo musical, reprodução, reconhecimento MIDI, preferências ou geometria compacta da alteração 3.
- Testes da lógica de estado e de geometria foram atualizados: o comportamento antigo de ocultar/revelar não é mais esperado, enquanto continuam obrigatórias a estabilidade do viewport, identificação de notas e integridade musical.

## Validação autorizada nesta rodada

Somente contratos Python, teste Java puro do estado e compilação Android Java sem `assemble` ou APK. Não executar suíte completa nem teste visual em emulador. A navegação e a geometria reais serão validadas quando um APK de teste for autorizado. Commit na branch `feat/home-ajustes-acumulados`, sem integração à `main`.
