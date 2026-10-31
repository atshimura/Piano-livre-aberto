# Barra superior do Laboratório Musical — navegação unificada

## Decisão de interface
Barra azul-marinho `#101C31` com botão **Voltar** separado e abas **Descobrir / Aprender / Criação** agrupadas em uma superfície arredondada. A aba atual usa azul `#286BCB`, as demais branco suave. **Aprender permanece indisponível** em todas as telas: não se deve afirmar que a função já foi implementada.

## Implementação
- Novo componente Java `MusicLabNavigationBar` com apenas uma fonte de layout: `populate(bar, selected, back, goDiscover, goCreation)`.
- `MusicLabDiscoveryScreen` utiliza o componente com **Descobrir** selecionado.
- `MusicCreationScreen.showHome()` utiliza o mesmo componente com **Criação** selecionada; os subfluxos de gravação, lista e detalhes conservam seus cabeçalhos e ações de retorno seguras, para não perder uma tomada não salva.
- Quando Aprender for implementado, o mesmo componente poderá ser utilizado com `LEARN`, após a aprovação funcional separada.
- Em tela compacta (< 460 dp) Voltar é apresentado como seta acessível; em tela ampla (>= 650 dp) exibe “Laboratório Musical” e botão “← Biblioteca”. A faixa tem 56 dp de altura e alvos com no mínimo 40 dp de altura dentro de uma superfície de 48 dp; evitar alterações de semântica de navegação.
- Mantém rotas, perfis, armazenamento local, lógica MIDI e recuperação de gravações sem modificações.

## Teste básico dirigido
`PianoLivreAndroid/tests/emulator_ui_fast.py` verifica a presença das abas, Aprender desabilitado, seleção correta, ação Voltar e posições idênticas das três abas ao alternar Descobrir → Criação → Descobrir. Capturas `piano-fast-discovery.png` e `piano-fast-creation.png`. Os testes são executados apenas quando o GitHub Actions os aciona; **não representam validação em hardware real**.

## Integração e APK
Candidata 0.9.57-open-beta / code 66. O APK distribuível é o `app-release.apk` do commit integrado na `main`, assinado com o certificado permanente configurado em Actions Secrets e verificado pelo workflow. Nenhum APK debug é considerado entrega. O projeto permanece offline e GPL-3.0-only. O `publishApproved` continua `false`, sem publicação automática de Release.

## Restrições
Alterações Java são classificadas `full` pela política CI atual. Workflows `build`/`emulator` podem disparar automaticamente em PR e push; não suprimir checks obrigatórios ou contornar proteção para implementar uma alteração visual. Testes físicos Realme 7 5G / Lenovo Tab M9 / Yamaha P-45 seguem pendentes.
