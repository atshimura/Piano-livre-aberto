# PIANO LIVRE — INTEGRAÇÃO FINAL E PUBLICAÇÃO DA BETA 0.9.54

## Objetivo

Integrar na `main` as melhorias concluídas da Tela Inicial, a reformulação do Descobrir, a otimização dos testes rápidos e a documentação da futura Criação C1.

Após a integração, gerar, assinar e publicar oficialmente o APK **Piano Livre 0.9.54-open-beta, versionCode 63**, fornecendo o link direto para download.

**Executar efetivamente a integração e publicação, não apenas planejar.**

## 1. Repositório

https://github.com/atshimura/Piano-livre-aberto

Ler obrigatoriamente `AGENTS.md`, `README.md`, `docs/00-INDICE.md`, `docs/10-CONTINUIDADE.md`, a documentação de assinatura, publicação e os registros específicos de cada PR.

Nunca acessar ou modificar o repositório histórico privado.

## 2. Pull requests a integrar

### PR #76 — Tela Inicial

https://github.com/atshimura/Piano-livre-aberto/pull/76

HEAD verificada:

`72d1ee77412b7929b41fdaa94521cd3676cbe97f`

Preservar integralmente:

- Banner 30% menor, com ilustração ampliada.
- Três atalhos grandes: Biblioteca, Laboratório e Histórico.
- Acesso às Opções pela engrenagem.
- Melhorias da biblioteca, busca, filtros e coleções.
- Duas colunas de partituras em tablets e adaptação a celulares.
- Aumento da tipografia.
- Testes de regressão e evidências visuais.

### PR #78 — Reformulação do Descobrir e testes rápidos

https://github.com/atshimura/Piano-livre-aberto/pull/78

HEAD verificada:

`8cfc5db53d9f0f7bec0796192d2c1fc7b561578e`

Preservar integralmente:

- Duas pautas profissionais, Sol e Fá, com renderização OSMD.
- Área de pautas à esquerda, ocupando cerca de 75% da largura.
- Painel textual de identificação à direita, com cerca de 25%.
- Fundos visualmente diferenciados.
- Pautas estáveis, com níveis discretos de enquadramento e histerese.
- Claves e elementos de notação profissionalmente desenhados.
- Notas MIDI apresentadas imediatamente.
- Persistência do último resultado após soltar as teclas.
- Reconhecimento de notas, acordes, escalas e arpejos.
- Rolagem horizontal das sequências.
- Validação rápida `ui-fast` e suíte completa `emulator-suite`.
- Check obrigatório `emulator`, que só aprova quando a suíte completa é bem-sucedida.

### PR #80 — Documentação da Criação C1

https://github.com/atshimura/Piano-livre-aberto/pull/80

HEAD verificada:

`11a8579ce23f023875299993a225dc8b10743d82`

Integrar exclusivamente a documentação de planejamento e continuidade:

- Escopo completo da Criação C1.
- Navegação entre projetos musicais.
- Roteiro de implementação futura.
- Referências no índice, README, AGENTS e continuidade.

**Não implementar a Criação C1 neste trabalho.**

O usuário decidiu deixá-la para depois. A área Criação não deve aparecer como funcionalidade disponível no APK 0.9.54.

## 3. Estratégia de integração

Antes de modificar qualquer coisa:

1. Conferir o estado atualizado da `main`, dos três PRs e de seus checks.
2. Confirmar a versão oficial atual e as Releases existentes.
3. Identificar possíveis alterações posteriores aos commits registrados neste prompt.
4. Revisar os arquivos modificados em comum.
5. Confirmar que não existe trabalho mais recente que torne algum PR obsoleto.

### Evitar publicações intermediárias

**Não integrar simplesmente o PR #76 diretamente na main e depois publicar o PR #78.**

A automação existente pode criar uma Release a cada alteração de versão na `main`.

Em vez disso:

1. Criar uma branch de consolidação a partir da `main` atual.
2. Incorporar as alterações de #76, #78 e #80, preservando a rastreabilidade dos commits e PRs.
3. Resolver conflitos.
4. Ajustar a versão final para 0.9.54-open-beta / 63.
5. Executar os testes do conjunto integrado.
6. Abrir um PR único de consolidação para a `main`.
7. Integrar somente após os checks obrigatórios.
8. Permitir a publicação oficial de uma única beta final.

Não publicar separadamente a 0.9.53, a menos que já exista alguma publicação efetiva que precise ser preservada.

Não fazer force-push nem contornar a proteção da `main`.

## 4. Atenção especial aos conflitos

Os PRs modificam arquivos em comum, especialmente:

- `.github/workflows/emulator.yml`
- `PianoLivreAndroid/gradle.properties`
- `PianoLivreAndroid/tests/emulator_public_upgrade.py`
- `PianoLivreAndroid/tests/emulator_release.py`
- `docs/00-INDICE.md`
- `docs/08-DECISOES.md`
- `docs/10-CONTINUIDADE.md`

Resolver os conflitos preservando **o conteúdo funcional das três entregas**, não simplesmente escolhendo um lado inteiro.

### Regras específicas

**Versão:** manter 0.9.54-open-beta / 63.

**Home:** preservar todos os ajustes do PR #76 e seus testes.

**Descobrir:** preservar todos os ajustes do PR #78.

**CI:** usar a arquitetura de testes rápidos e completos introduzida no PR #78, incorporando também os testes específicos da Home adicionados pelo PR #76.

**Documentação:** incluir a documentação do PR #80 sem alterar a situação da Criação C1, que permanece planejada.

### Atenção ao workflow de emulador

O PR #78 substituiu o antigo fluxo `midi-visual` pela estrutura `debug-apk`, `ui-fast`, `emulator-suite` e gate `emulator`.

Ao integrar o PR #76:

- Manter os testes de refinamento da Home.
- Garantir sua execução efetiva na suíte adequada.
- Não restaurar inadvertidamente o workflow antigo.
- Preservar a coleta de capturas da Home e do Descobrir.
- Não permitir que `ui-fast` substitua `emulator-suite`.
- Não enfraquecer o gate obrigatório.

O PR #80 também altera o filtro de caminhos do workflow. Preservar sua intenção sem prejudicar os filtros e gatilhos atuais.

## 5. Revisão funcional antes do merge

Conferir que o aplicativo integrado mantém:

- Tela Inicial refinada.
- Biblioteca Musical funcional.
- Tela Estudar.
- Partitura Livre.
- Laboratório Descobrir reformulado.
- Aba Aprender visível e desabilitada.
- Perfis e histórico.
- Importação MusicXML e MIDI.
- Reconhecimento e saída USB-MIDI.
- Modos de estudo.
- Backup e restauração.
- Compatibilidade dos dados anteriores.

Não introduzir funcionalidades novas da Criação C1.

Corrigir problemas de integração encontrados antes de solicitar o merge.

## 6. Testes obrigatórios

### Durante as correções

Utilizar o modo de desenvolvimento rápido aprovado na issue #77:

- Compilação compartilhada do APK de debug.
- Testes direcionados de interface.
- Capturas reais em emulador.
- Validação das alterações da Home e do Descobrir.

### Antes da integração

Executar a suíte completa do commit consolidado:

- Build Android.
- Testes JVM, JavaScript e MusicXML.
- Motor MIDI e reconhecimento musical.
- Biblioteca e navegação.
- Perfis e histórico.
- Backup/restauração.
- Testes de atualização.
- Emulador Android completo.
- Regressões visuais de Home, Estudar e Descobrir.
- Validação dos checks `build` e `emulator`.

Não utilizar os resultados individuais anteriores de #76, #78 e #80 como prova suficiente da integração.

Se houver falha, corrigir, atualizar o PR de consolidação e repetir os testes pertinentes até obter aprovação do commit efetivo.

### Atualização de versão

Além das regressões existentes, testar a atualização da beta pública **0.9.52 para 0.9.54**, preservando dados sintéticos de perfis, biblioteca, sessões e preferências.

Manter os demais ensaios de compatibilidade existentes.

## 7. Integração à main

Depois que o PR de consolidação estiver aprovado:

1. Confirmar HEAD e checks.
2. Integrar usando o mecanismo permitido pela proteção da `main`.
3. Verificar o commit efetivamente integrado.
4. Confirmar a inclusão das três entregas.
5. Conferir a versão 0.9.54 / 63.
6. Confirmar a rastreabilidade dos PRs originais.
7. Registrar o estado final de cada PR, sem fechar ou alterar indevidamente PRs históricos não relacionados.

Não considerar concluída a integração apenas porque os PRs individuais estão aprovados.

## 8. APK oficial assinado

O APK deve ser construído a partir da `main` integrada.

**Requisitos obrigatórios:**

- Nome: `PianoLivre-0.9.54-open-beta.apk`
- VersionName: `0.9.54-open-beta`
- VersionCode: `63`
- Pacote: `br.org.pianolivre.opensource`
- Variante: release
- Depuração: desativada
- Permissão INTERNET: ausente
- Assinatura: certificado público permanente existente

Fingerprint SHA-256 esperada do certificado:

`7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`

Usar exclusivamente o mecanismo confiável do GitHub Actions com Secrets existentes.

Nunca expor o keystore ou credenciais.

**Não utilizar o APK de validação dos PRs como APK oficial**, pois ele foi assinado temporariamente.

## 9. Publicação da versão 0.9.54

Utilizar o mecanismo existente de publicação:

`PianoLivreAndroid/scripts/publish_public_beta.py`

Antes da publicação, verificar:

- Tag `v0.9.54-open-beta` ainda não publicada.
- `build` aprovado na `main` final.
- `emulator` aprovado no mesmo commit.
- Assinatura permanente correta.
- Integridade dos arquivos.
- APK release correto.
- Compatibilidade da versão anterior.

O publicador aguarda até 40 minutos pelos checks. Como a suíte completa já levou aproximadamente 37 minutos, revisar a margem operacional de timeout e o tratamento de filas, sem reduzir testes nem permitir publicação antecipada.

Se o tempo disponível for insuficiente, corrigir o mecanismo de espera com testes de segurança, preservando a exigência de checks aprovados do SHA exato.

Preparar notas específicas da versão em `docs/releases/0.9.54-open-beta.md`, se necessário para o fluxo existente.

Incluir nas notas:

- Ajustes da Tela Inicial.
- Reformulação do Descobrir.
- Melhoria da renderização das pautas e claves.
- Otimização do feedback de testes.
- Correções de estabilidade e responsividade.
- Limitações conhecidas.
- Testes físicos pendentes.

Não anunciar a Criação C1 como implementada.

## 10. Conferência da Release

Depois de publicada, conferir efetivamente:

1. Existência da Release oficial.
2. Tag correta.
3. Commit correto.
4. APK disponível para download.
5. Tamanho do arquivo.
6. SHA-256 correspondente ao arquivo `SHA256SUMS`.
7. Certificado de assinatura permanente.
8. Package ID e versionCode corretos.
9. Arquivos de assinatura e metadados anexados.
10. Possibilidade real de baixar o APK.

Não inventar URL nem declarar publicação sem confirmação.

## 11. Preservação dos arquivos de trabalho

Durante a revisão anterior, a limpeza opcional de arquivos locais não rastreados foi bloqueada.

Respeitar essa proteção.

Não executar `git clean -fdx`, exclusões indiscriminadas, resets destrutivos ou qualquer limpeza que possa apagar arquivos locais úteis.

Não incluir arquivos de build, screenshots temporárias ou artefatos não rastreados indevidamente no PR.

A limpeza não é requisito para finalizar a integração.

## 12. Registro e relatório final

Atualizar a documentação de continuidade, o índice, o roadmap e as notas da versão.

Registrar:

- PRs efetivamente incorporados.
- PR de consolidação.
- Commit final da `main`.
- Versão final.
- Checks e resultados do SHA integrado.
- Referências das capturas.
- Arquivo APK.
- SHA-256 do APK.
- Certificado.
- Release oficial.
- Limitações e próximos passos.

Manter a issue #79 e a implementação da Criação C1 para depois.

A issue #77 poderá ser encerrada somente depois de confirmar que a otimização dos testes realmente foi incorporada à `main`.

## 13. Resposta final ao usuário

Ao terminar, apresentar de forma objetiva:

**Integração:** concluída ou impedimento real encontrado.

**Versão:** 0.9.54-open-beta (63).

**Testes:** links e resultados.

**Release:** link oficial.

**APK:** link direto para baixar.

**Assinatura:** permanente, conferida.

**Testes físicos:** pendentes no Yamaha P-45, Realme 7 5G e Lenovo Tab M9.

Não considerar a versão 1.0 concluída.

## Autonomia

Executar a integração, resolver conflitos, realizar testes, publicar e verificar o APK, sem pedir autorização para decisões técnicas rotineiras.

Solicitar intervenção somente diante de impedimento real que não possa ser resolvido com segurança.

**Objetivo final: uma única versão pública 0.9.54 que reúna os ajustes da Home, a reformulação do Descobrir, a otimização dos testes e a documentação da Criação C1, sem implementar ainda o módulo Criação.**
