# PIANO LIVRE — PROMPT 4 DE 4
## Consolidação final, integração dos PRs, testes e publicação da versão beta

### MISSÃO

Concluir integralmente o ciclo de reformulação das interfaces do Piano Livre, integrando os Prompts 1, 2 e 3 ao repositório principal, validando a versão consolidada e publicando um APK oficial de distribuição beta com a assinatura permanente.

**Executar efetivamente todas as tarefas, não apenas apresentar planejamento.**

O resultado esperado é uma nova versão beta pública, instalável e atualizável, com APK disponível no GitHub Releases e link direto para download.

## 1. REPOSITÓRIO

**Repositório oficial:**

https://github.com/atshimura/Piano-livre-aberto

**Branch principal protegida:** `main`

**Entregas existentes:**

| Entrega | PR | Commit de referência |
|---|---|---|
| Prompt 1 — Tela Inicial | #71 | `32b83a3` |
| Prompt 2 — Referência e documentação | #72 | `22e193b` |
| Prompt 2 — Tela Estudar | #73 | `2739bb2` |
| Prompt 3 — Consolidação MIDI | #74 | `a26a7be` |

Links:

- https://github.com/atshimura/Piano-livre-aberto/pull/71
- https://github.com/atshimura/Piano-livre-aberto/pull/72
- https://github.com/atshimura/Piano-livre-aberto/pull/73
- https://github.com/atshimura/Piano-livre-aberto/pull/74

Conferir o HEAD real de cada PR antes de iniciar, pois os commits podem ter mudado.

**Não acessar nem modificar o repositório histórico privado `atshimura/piano-livre`.**

## 2. PREPARAÇÃO OBRIGATÓRIA

Antes de qualquer integração:

1. Ler integralmente `AGENTS.md`.
2. Ler `README.md` e a documentação de arquitetura, MIDI, MusicXML, renderização, biblioteca e dados.
3. Ler `docs/ASSINATURA.md`.
4. Ler `docs/40-ACEITE-E-ATUALIZACAO.md`.
5. Ler `docs/44-APK-DISTRIBUICAO.md`.
6. Examinar `.github/workflows/build.yml` e `.github/workflows/emulator.yml`.
7. Examinar `PianoLivreAndroid/scripts/publish_public_beta.py`.
8. Verificar versões existentes em GitHub Releases.
9. Conferir todos os PRs e seus checks.
10. Conferir as dependências entre branches.

Ler os relatórios:

- `docs/70-TELA-INICIAL-PROMPT-01.md`
- `docs/71-TELA-ESTUDAR-PROMPT-02.md`
- `docs/72-CONSOLIDACAO-MIDI-INTERFACES-PROMPT-03.md`

Inspecionar as duas imagens aprovadas:

- `docs/references/tela-inicial-aprovada.png`
- `docs/references/tela-estudar-aprovada.png`

Preservar integralmente essas referências.

## 3. AUDITORIA FINAL ANTES DA INTEGRAÇÃO

Revisar as implementações dos quatro PRs.

Confirmar que:

- A Tela Inicial corresponde à referência aprovada.
- A Tela Estudar corresponde à referência aprovada.
- O cabeçalho superior do Estudar está unificado.
- A partitura ganhou espaço útil.
- O Teclado guia foi removido do Estudar.
- Os seletores manuais antigos de renderização foram removidos.
- Partitura Livre continua funcional.
- Ambas, Esquerda e Direita funcionam.
- Espera, Contínuo e Demonstração funcionam.
- O botão USB-MIDI apresenta corretamente o estado do instrumento.
- Inicial e Estudar compartilham o mesmo estado MIDI.
- Desconexão e reconexão são tratadas com segurança.
- As barras superiores são ocultadas e restauradas corretamente.
- A barra inferior permanece acessível.
- A renderização musical não sofreu regressões.
- O Laboratório Musical continua funcionando.
- Biblioteca, perfis, favoritos, coleções e histórico são preservados.

Quando houver evidência de regressão, corrigir antes de prosseguir.

Não alterar decisões aprovadas por conveniência técnica.

## 4. ESTRATÉGIA DE INTEGRAÇÃO

**A integração deve respeitar a proteção da `main`, a dependência entre PRs e o mecanismo automático de publicação.**

### 4.1. Atenção à publicação automática

Na verificação inicial:

- A última beta pública era `0.9.50-open-beta`, versionCode `59`.
- O Prompt 2 introduziu `0.9.51-open-beta`, versionCode `60`.
- O Prompt 3 introduziu `0.9.52-open-beta`, versionCode `61`.

O workflow da `main` possui publicação automática de pré-release quando identifica alteração de versão e os checks necessários são aprovados.

Portanto, integrar isoladamente o PR #73 pode publicar a 0.9.51 antes da consolidação do PR #74.

**Evitar essa publicação intermediária não solicitada.**

### 4.2. Integração recomendada

Executar:

**Primeira integração:** PR #71 — Tela Inicial.

**Segunda integração:** PR #72 — Referência e documentação do Estudar.

Depois, consolidar as alterações dos PRs #73 e #74 em uma única integração final à `main`, com a versão `0.9.52-open-beta (61)`.

A implementação do PR #74 já contém, por ancestralidade, as alterações dos Prompts 2 e 3.

Uma estratégia aceitável é atualizar a base do PR #74 para a `main`, após integrar os PRs #71 e #72, verificando que seu diff inclui integralmente o conteúdo dos PRs #73 e #74.

Se necessário, utilizar um PR específico de consolidação, preservando os commits, conteúdos e vínculos de revisão.

### 4.3. Cuidados obrigatórios

- Não duplicar arquivos ou commits.
- Não aplicar alterações já presentes na `main`.
- Não perder a imagem aprovada da tela Estudar.
- Não eliminar documentação de nenhum prompt.
- Não fazer force-push.
- Não desabilitar checks.
- Não contornar proteções da `main`.
- Não executar merges inseguros.
- Não publicar uma beta intermediária apenas para satisfazer a ordem numérica dos PRs.

Se os PRs #73 e #74 forem incorporados por um PR consolidado, encerrar ou atualizar os PRs originais com referências explícitas ao merge que incorporou integralmente seu conteúdo.

Não declarar um PR como integrado sem verificar os arquivos e commits efetivamente presentes na `main`.

### 4.4. Verificação do conteúdo integrado

Confirmar que a `main` contém:

- Todo o Prompt 1.
- Imagem e documentação do Prompt 2.
- Toda a reformulação da tela Estudar.
- Toda a consolidação MIDI.
- Todos os testes novos.
- Todos os recursos de notação.
- Todas as correções de responsividade.
- Todas as atualizações de compatibilidade e persistência.

Verificar também que não existem alterações inesperadas em arquivos de assinatura, permissões, direitos musicais ou configuração de publicação.

## 5. VERSÃO FINAL

A versão pretendida para publicação é:

**Nome:** `0.9.52-open-beta`

**VersionCode:** `61`

**Pacote Android:** `br.org.pianolivre.opensource`

**Tag esperada:** `v0.9.52-open-beta`

Antes de publicar, conferir se essa versão já existe em Releases e se o código da `main` corresponde ao conteúdo final pretendido.

Não reutilizar uma tag existente para distribuir outro código.

Se houver conflito de versão, escolher a próxima versão válida, atualizar a documentação e utilizar o processo normal de publicação.

Não alterar o pacote Android.

Não alterar a identidade de assinatura.

Não criar nova chave de assinatura.

## 6. TESTES DA VERSÃO CONSOLIDADA

Os testes aprovados nos PRs são evidências importantes, mas não substituem a validação do código efetivamente integrado.

Executar novamente as suítes relevantes na `main`.

### 6.1. Build e verificações gerais

Exigir aprovação de:

- Auditoria do repositório público.
- Compilação Android.
- Testes JVM.
- Testes JavaScript.
- Testes Chromium.
- Testes de MusicXML.
- Testes de importação MIDI.
- Testes de reconhecimento USB-MIDI.
- Testes de persistência.
- Testes de backup e restauração.
- Testes de compatibilidade.
- Testes de segurança da distribuição.

Não desativar testes antigos para obter aprovação.

### 6.2. Tela Inicial

Validar:

- Banner.
- Navegação.
- Biblioteca.
- Busca.
- Favoritos.
- Coleções.
- Importação.
- Perfis.
- Histórico.
- Opções.
- Botão USB-MIDI.

### 6.3. Tela Estudar

Validar:

- Barra superior unificada.
- Faixa de modos e mãos.
- Partitura ampliada.
- Renderizador automático.
- Partitura Livre.
- Reconhecimento de notas.
- Seleção de mãos.
- Modos de aprendizagem.
- Controles de reprodução.
- Velocidade.
- Trecho e repetição.
- Ocultação das barras.
- Restauração por toque.
- Responsividade.

### 6.4. Conexão MIDI

Validar:

- Dispositivo desconectado.
- Dispositivo conectado.
- Nome real ou nome simulado claramente identificado.
- Dispositivo sem nome.
- Abertura com falha.
- Desconexão.
- Reconexão.
- Troca de dispositivo.
- Mensagens antigas.
- Eventos Note On e Note Off.
- Pedal.
- Notas simultâneas.
- Demonstração.
- Preservação da sessão musical.

Usar os testes sintéticos existentes e manter a separação entre simulação e hardware real.

### 6.5. Regressão de notação

Verificar:

- Duas pautas.
- Claves e armaduras.
- Polifonia.
- Acordes.
- Ligaduras.
- Quiálteras.
- Acidentes.
- Dinâmica e articulação suportadas.
- Mapeamento gráfico das notas.
- Rolagem.
- Cursores.
- Partituras longas.
- Resize.
- Fallback de renderização.

Não modificar MusicXML original para facilitar os testes.

## 7. VALIDAÇÃO VISUAL FINAL

Executar os testes no emulador Android em configurações representativas de celular e tablet.

Utilizar as duas imagens aprovadas como referência.

Gerar capturas reais de:

**Tela Inicial**
- Celular.
- Tablet.

**Tela Estudar**
- Estado normal.
- Barras ocultas durante a execução.
- Controles restaurados.

**MIDI**
- Desconectado.
- Conectado simulado.
- Reconexão simulada.
- Nome de instrumento longo.

Confirmar que não há áreas vazias, controles cortados, pautas incompletas ou sobreposição.

### Área útil da partitura

Preservar os resultados do Prompt 3, nas mesmas configurações de emulador:

| Viewport | Normal | Barras ocultas | Restaurado |
|---|---:|---:|---:|
| Celular 1280 × 720 | 417 px | 567 px | 417 px |
| Tablet 1920 × 1080 | 777 px | 927 px | 777 px |

Uma eventual diferença deverá ser medida e justificada.

**Não aceitar regressão visual relevante sem correção.**

## 8. COMPILAÇÃO E ASSINATURA PERMANENTE

### Regra obrigatória

O APK destinado à publicação deve ser compilado por um workflow confiável da `main`, usando a assinatura permanente configurada nos GitHub Actions Secrets.

Não usar a assinatura descartável dos PRs para publicar a beta.

### Verificações

Confirmar:

1. `applicationId` correto.
2. `versionCode` correto.
3. `versionName` correto.
4. Variante release.
5. APK não depurável.
6. Ausência de permissão INTERNET indevida.
7. Assinatura permanente.
8. Mesma identidade de certificado das betas públicas anteriores.
9. Integridade do APK.
10. Compatibilidade de atualização.

O certificado SHA-256 esperado está registrado em `docs/ASSINATURA.md` e é exigido pelo workflow existente.

Verificar o certificado real com `apksigner`, sem revelar senhas, chaves ou conteúdo do keystore.

Não modificar os Secrets.

Não imprimir valores confidenciais nos logs.

Se a assinatura permanente estiver ausente ou divergente, bloquear a publicação e registrar o impedimento.

### Atualização

Preservar a possibilidade de atualizar instalações públicas assinadas com o mesmo certificado.

Não confundir o pacote público com o aplicativo histórico privado.

Executar os ensaios automatizados disponíveis de atualização e preservação de dados.

Os ensaios de emulador com certificado descartável não comprovam, isoladamente, atualização física com o certificado permanente; distinguir as evidências.

## 9. PUBLICAÇÃO NO GITHUB RELEASES

Depois de:

- Integrar o código correto à `main`.
- Aprovar os testes do commit final.
- Confirmar a assinatura permanente.
- Confirmar pacote e versão.
- Confirmar a integridade do APK.

Publicar a beta pelo mecanismo oficial existente do repositório.

Priorizar o workflow de publicação automática já implementado, evitando criar uma segunda Release ou contornar seus bloqueios de segurança.

Se a publicação automática falhar, diagnosticar e corrigir a causa; não realizar upload manual que ignore checks obrigatórios.

### Release esperada

**Piano Livre 0.9.52-open-beta**

A Release deverá conter:

- APK oficial de distribuição.
- SHA256SUMS.
- Metadados do APK.
- Informações do certificado público.
- Notas de versão.
- Identificação do commit publicado.
- Limitações conhecidas.

Manter a Release como **pré-lançamento (prerelease)**.

Não apresentar a versão como 1.0.

### Link direto do APK

Após a publicação:

1. Acessar a Release criada.
2. Confirmar que o APK está realmente anexado.
3. Obter a URL direta do asset.
4. Verificar que o link funciona.
5. Disponibilizar esse link ao usuário.

**Não apresentar um link de artefato temporário do GitHub Actions como substituto do APK publicado.**

O link deve apontar para o arquivo oficial no GitHub Releases.

## 10. NOTAS DA VERSÃO

As notas da Release devem explicar, de forma acessível:

### Nova Tela Inicial

- Interface renovada.
- Biblioteca integrada.
- Acesso ao Laboratório.
- Favoritos e coleções.
- Perfis e histórico.
- Indicador USB-MIDI.

### Nova Tela Estudar

- Interface mais compacta.
- Partitura ampliada.
- Barras dinâmicas.
- Seleção de mãos.
- Modos de aprendizagem.
- Controles simplificados.
- Renderização automática.

### USB-MIDI

- Identificação do instrumento.
- Estado de conexão compartilhado.
- Melhorias de desconexão e reconexão.
- Tratamento de eventos antigos e notas presas.

### Compatibilidade

- Dados preservados.
- Recursos offline.
- Atualização de versões públicas anteriores com certificado compatível.
- Limitações musicais conhecidas.

Informar que os testes físicos ainda estão pendentes.

## 11. DOCUMENTAÇÃO FINAL

Criar:

`docs/73-CONSOLIDACAO-PUBLICACAO-PROMPT-04.md`

Registrar:

- Estado inicial das entregas.
- Estratégia de integração.
- Ordem efetiva dos merges.
- PRs e commits integrados.
- Eventuais conflitos.
- Correções adicionais.
- Commit final da `main`.
- Versão publicada.
- Testes executados.
- Resultado dos workflows.
- Assinatura verificada.
- SHA-256 do APK.
- Link da Release.
- Link direto do APK.
- Evidências visuais.
- Limitações.
- Pendências físicas.
- Próximas etapas até a V1.

Atualizar:

- `README.md`
- `AGENTS.md`, se necessário.
- `docs/00-INDICE.md`
- `docs/07-ESTADO-E-ROADMAP.md`
- `docs/08-DECISOES.md`, quando necessário.
- `docs/10-CONTINUIDADE.md`
- Histórico de versões.

Evitar commits documentais posteriores que gerem uma publicação duplicada ou façam parecer que a Release contém código de outro SHA.

Preservar o histórico das decisões aprovadas.

## 12. TESTES FÍSICOS PENDENTES

Registrar expressamente que os seguintes aparelhos ainda exigem aceite:

### Realme 7 5G

- Layout horizontal.
- Fluidez da partitura.
- Toques e botões.
- Ocultação das barras.
- Reprodução.
- Segundo plano.
- Atualização e preservação de dados.

### Lenovo Tab M9

- Layout e legibilidade.
- Área útil da partitura.
- Seleção de mãos.
- Controles de estudo.
- Responsividade.
- Fluidez.
- Atualização e backup.

### Yamaha P-45

- Conexão USB-MIDI.
- Nome real do instrumento.
- Reconhecimento de notas e acordes.
- Pedal.
- Desconexão e reconexão.
- Demonstração.
- Ausência de notas presas.

Esses testes poderão ser executados pelo usuário posteriormente.

**Não impedir a publicação beta somente pela ausência desses testes, desde que não exista falha crítica conhecida e que todas as verificações automatizadas e de distribuição estejam aprovadas.**

Não declarar a V1 concluída.

## 13. CRITÉRIOS DE CONCLUSÃO

Considerar o Prompt 4 concluído somente quando:

- Os Prompts 1, 2 e 3 estiverem integralmente presentes na `main`.
- As referências visuais estiverem preservadas.
- Os PRs estiverem corretamente reconciliados.
- Não houver conflitos pendentes.
- Os testes finais da `main` estiverem aprovados.
- A variante release estiver compilada.
- O pacote e a versão estiverem corretos.
- A assinatura permanente estiver comprovada.
- A compatibilidade automatizada de dados estiver aprovada.
- O APK estiver publicado no GitHub Releases.
- A Release estiver marcada como beta.
- O arquivo SHA256SUMS estiver disponível.
- O link direto do APK estiver funcionando.
- A documentação estiver atualizada.
- Os testes físicos pendentes estiverem registrados.

Se algum critério obrigatório não for satisfeito, não declarar publicação concluída.

## 14. AUTONOMIA E CONTINUIDADE

Você está autorizado a executar integralmente a consolidação, os merges previstos e a publicação beta, respeitando as proteções do repositório e os critérios de segurança.

Não solicitar autorização adicional para decisões técnicas rotineiras.

Não interromper a execução depois de integrar apenas um PR.

Não parar após compilar o APK.

Não considerar a tarefa concluída apenas porque os testes passaram.

Prosseguir até a disponibilização do APK oficial e de seu link direto.

Em caso de falha:

1. Investigar.
2. Corrigir.
3. Executar novamente os testes.
4. Confirmar o resultado.
5. Continuar a entrega.

Não mascarar falhas nem desativar mecanismos de proteção para publicar.

Não executar modificações destrutivas.

Não acessar o repositório privado histórico.

## 15. RELATÓRIO FINAL OBRIGATÓRIO

Ao concluir, apresentar:

**PIANO LIVRE — PROMPT 4 CONCLUÍDO**

- Status: concluído ou pendente.
- Versão beta publicada.
- Commit final da `main`.
- PRs integrados ou reconciliados.
- Resultado dos workflows.
- Resultado dos testes automatizados.
- Certificado permanente verificado.
- SHA-256 do APK.
- Link da Release.
- **Link direto para baixar o APK.**
- Limitações conhecidas.
- Testes físicos pendentes.
- Próximas atividades até a versão 1.0.

### Objetivo definitivo

**Entregar uma nova beta oficial do Piano Livre contendo a nova Tela Inicial, a nova Tela Estudar e a consolidação MIDI, integralmente integrada à `main`, testada, assinada com o certificado permanente e publicada no GitHub Releases.**

Não encerrar antes de comprovar o resultado efetivo.