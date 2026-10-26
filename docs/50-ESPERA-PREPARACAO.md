# 50 — Espera: iniciar e preparação visual gradual (0.9.41)

## Decisão funcional
O botão inferior mostra **Iniciar** quando o modo Espera ainda não está armado. Após o toque, notas deslizam durante **5 segundos por padrão**, ajustáveis de 1 a 30 segundos em Ferramentas > Preparação do Espera (ou toque prolongado em Iniciar). Não há contagem regressiva textual nem limite de tempo para o aluno executar depois da chegada. Não se escolhe tolerância MIDI arbitrária.

## Implementação
- `MainActivity`: cancela reconhecimento durante a entrada, espera o relógio monotônico completar a preparação e só então arma as notas esperadas. Trocas de modo, perfil, partitura, início, saída da tela e pausa cancelam a execução por geração; mensagens MIDI prévias não avançam o exercício.
- `WaitPreparation`: duração configurável por perfil com entrada validada, padrão 5s, persistida nas preferências existentes e incluída pelo backup v2 sem alterar esquema.
- `TemporalNotation.waitPrepare`: animação visual interpolada em requestAnimationFrame sobre as tiles existentes, deslocando o início da pauta musical do lado direito até o alinhamento da primeira nota; cursor, claves e contexto fixos. Termina sem continuar animando. O relógio musical do aluno permanece parado até a conclusão.
- `NotationView`: envia preparação apenas quando a WebView estiver renderizada, permite cancelar, e mantém comportamento existente de pausa/teardown. Fallback nativo preserva a regra temporal se não houver desenho da WebView.

## Testes
`WaitPreparationTest` valida duração, início, cancelamento e conclusão monotônica. `TemporalRenderingTest.js` valida entrada parcial, final, parada do RAF e cancelamento. Verificar Actions e emulador da head do PR (não inferir aprovação da main ou do hardware). Yamaha P-45, Realme 7 5G e Lenovo Tab M9 ainda precisam de validação real e ajuste de ergonomia.

## Limites
Preparação é efeito visual local; não reproduz áudio e não conta tempo como avaliação. Se alterar a tela durante a preparação, a sessão exige novo Iniciar ao retornar. O limite de 1 a 30 segundos é de configuração da interface, não tolerância de notas, acordes ou pedal.

## Dependência resolvida
O PR #39 do cursor passou pela CI de build e emulador e foi integrado à `main` em `d37fafafca71ecf40e148a6280c37269d30e0201`. Esta entrega deve ser validada novamente em PR contra `main` antes de integração própria. A preparação não altera a semântica de reconhecimento nem a janela temporal dos acordes.
