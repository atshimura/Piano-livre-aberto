# Alteração 1 — Atalhos do banner, Histórico e Como usar

**Estado:** implementação em branch técnica, sem APK, merge ou nova Release.

## Decisão aprovada (02/11/2026)

- Substituir **Começar pela Ode à Alegria** e **Como usar** do banner por botões compactos **Biblioteca Musical** e **Laboratório Musical**.
- Dois botões lado a lado, 44 dp de altura e larguras iguais usando pesos idênticos; Biblioteca **azul** (`#286BCB`), Laboratório **verde** (`#128263`); sem remover o título/arte do banner.
- Biblioteca faz rolagem para a seção de busca/partituras; Laboratório abre o módulo atual.
- Remover os três grandes cards inferiores (Biblioteca, Laboratório, Histórico), preservando toda a biblioteca e as partituras.
- Colocar **Histórico** e **Como usar** no menu da engrenagem / Ferramentas, sem alterar funções, persistência ou permissões. A Ode à Alegria continua acessível pela biblioteca.
- Alinhar os testes de Home, navegação, estudo, release e migração aos novos destinos.

## Validação autorizada

Somente testes básicos/contratos estáticos e compilação Java Android **sem tarefa assemble nem empacotamento de APK**. Não executar a suíte completa, não publicar e não integrar à `main` até autorização posterior. Avaliação gráfica com APK/emulador e validação em Realme 7 5G/Lenovo Tab M9 ficam pendentes.
