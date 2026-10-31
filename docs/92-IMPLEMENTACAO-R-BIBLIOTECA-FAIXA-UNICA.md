# Implementação R — controles da Biblioteca Musical em faixa única

## Escopo aprovado (31/10/2026)
Reorganizar a seção da Biblioteca na Home em duas faixas: o título e, logo abaixo, **uma única linha horizontal** contendo campo de busca expansível, seletor de categoria e, à direita, dois botões menores empilhados (Importar e Coleções).

Em telas grandes, a **busca deve ser o controle mais largo** e utilizar todo o espaço restante. O seletor de categoria mantém largura limitada e suficiente para exibir “Categoria”, com margens internas e seta; as opções do menu permanecem completas.

## Implementação
- Somente `PianoScreens.buildLibraryPanel()` na estrutura da Home, com pequeno ajuste em `refreshLibraryFilters()` para apresentar “Categoria” no valor padrão do seletor. O menu aberto preserva “Todas as categorias”, “Favoritos” e as coleções, e o texto completo é preservado para acessibilidade.
- A faixa usa `LinearLayout` horizontal, com busca `weight=1`, categoria de 100–148 dp conforme largura e coluna de ações de 52–134 dp. Os dois botões têm 44 dp de altura e intervalo de 4 dp; em telas estreitas passam a usar símbolos, mantendo descrições acessíveis “Importar partitura” e “Coleções”.
- O título da Biblioteca permanece acima dos controles. A pesquisa, importação, criação e gestão de coleções, resultados, filtros, perfis e armazenamento não foram alterados.
- Os componentes são nativos/offline. Não há serviços externos nem mudanças em MIDI, MusicXML ou persistência.

## Versão e assinatura
Candidata **0.9.56-open-beta, versionCode 65**, derivada da 0.9.55/code64. O APK instalável para uso contínuo **precisa ser um release assinado pelo certificado permanente** já configurado em GitHub Actions Secrets, validado pela fingerprint de distribuição do workflow. APK debug não é entrega válida.

## Verificações básicas e resultados
O roteiro direcionado `PianoLivreAndroid/tests/home_layout_checks.py` contém verificações de geometria para quatro perfis: pesquisa/categoria na mesma linha, larguras não sobrepostas, ações empilhadas, botões >=44 dp e pesquisa maior nas telas largas. As validações anteriores de busca, categorias, botão importar, coleções, cartões e banner foram preservadas. Registrar resultados reais do PR no GitHub; não assumir sucesso até o término das execuções.

## Integração e distribuição
Trabalhar por PR para `main`. A política atual do classificador classifica código Java e versão como `full`: os workflows existentes podem iniciar automaticamente a suíte completa e a branch protegida pode bloquear o merge até aprovação. Não modificar ou contornar os checks por causa desta entrega. A publicação de Release pública permanece separada e sujeita ao protocolo de publicação; fornecer APK assinado de Actions não significa publicar Release.

## Aceite físico
A validação em Realme 7 5G e Lenovo Tab M9 permanece pendente e não é substituída pelo emulador.
