# 63 — Entrega B: cenário Android de estudo curto e longo

## Objetivo

Provar no emulador Android API 29 um ciclo de estudo completo, em complemento aos testes puros de MusicXML e MIDI e sem equivaler a teste em aparelhos reais.

A execução `tests/emulator_study_session.py` reutiliza os auxiliares de UI do projeto e exercita, em sequência, `Estudo livre 01` e `Gymnopédie nº 1`, ambas já incluídas no catálogo público e sem adicionar música nova.

Cada sessão verifica a abertura OSMD e título, controles visíveis, seleção Direita/Esquerda/Ambas, teclado guia aberto e recolhido, Contínuo iniciar/pausar/retomar/parar, voltar ao início, alternância temporal↔OSMD, recusa de Demonstração sem saída MIDI (emulador sem Yamaha), preparação Espera até sua conclusão, cancelamento e retorno do Android ao foreground. O script salva JSON e três capturas por partitura e é executado pelo workflow de emulador.

**Limitações e evidência:** a nota do piano real, reconhecimento de ataques USB-MIDI, saída física e conforto de leitura no Realme 7 5G e Lenovo Tab M9 são *indeterminados* neste ambiente. A câmera adaptativa da B1 pertence ao PR #51 e não foi integrada a esta branch, que parte diretamente da `main` 0.9.47. O PR é só ampliação de validação da Entrega B, não aprovação da versão 1.0. Conferir os resultados do SHA no GitHub Actions antes de registrar as sessões como aprovadas.

Não efetuar merge, release ou distribuição de APK.
