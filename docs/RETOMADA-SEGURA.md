# Retomada segura entre sessões — Piano Livre

Este é o **ponto de entrada operacional** para retomar trabalho interrompido no ChatGPT, Work ou outro agente. O GitHub, não o histórico da conversa, é a fonte de verdade. **Não existe processo de programação contínuo garantido fora de uma sessão ativa**; esta rotina evita depender dele.

## Regras de execução

1. Trabalhar exclusivamente em `atshimura/Piano-livre-aberto`; nunca copiar ou alterar o repositório histórico privado. Ler `AGENTS.md` e os documentos relevantes antes de escrever.
2. Na abertura de **cada** sessão, conferir `main` HEAD, versão em `PianoLivreAndroid/gradle.properties`, PRs e issues abertas, branch/HEAD da tarefa e GitHub Actions do commit efetivo. O resumo desta página pode estar desatualizado.
3. Delimitar uma **unidade verificável de trabalho** por sessão (uma correção, um grupo pequeno de fixtures, uma inspeção visual com relatório, uma integração testada). A missão geral até 1.0 permanece, mas não deve virar um único pedido gigantesco ao agente.
4. Em toda alteração lógica coerente, salvar **commit pequeno na branch de trabalho**, seguido de um **checkpoint no corpo ou comentário do PR**. Fazer checkpoint também antes de testes demorados, troca de tarefa ou término da sessão. Não deixar a única cópia do trabalho em arquivos temporários do ambiente.
5. O checkpoint deve dizer a **próxima ação executável**, não apenas 'continuar'. Depois de falha, primeiro inspecionar branch, commits, checks e comentários no GitHub; não repetir automaticamente instruções antigas.
6. Usar branch e PR; respeitar proteção da `main`, testes, licenças, offline, assinaturas somente por Secrets e preservação de dados. Não fazer merge de PR draft, falho ou sem aceite requerido; não considerar CI/emulador substitutos de aparelhos físicos.
7. Informar claramente quando um comando/teste não foi executado ou quando o resultado é apenas de commit anterior. Não atribuir a si a execução de testes por outro agente sem conferir evidências.

## Modelo obrigatório de checkpoint no PR

Preencher concisamente após cada marco; não deixar campos com resultados inventados.

```text
CHECKPOINT — AAAA-MM-DD HH:MM UTC
Objetivo restrito da sessão:
Repositório / branch / PR:
Base main SHA:       
HEAD da branch SHA:  
Implementado (arquivos e mudança):
Testes locais executados e resultados:
Actions do HEAD exato (links e estado):
Não validado / risco / decisão pendente:
PR apto a integrar? Sim/Não — motivo:
PRÓXIMA AÇÃO ÚNICA: [comando, arquivo ou verificação concreta]
```

Se a conversa congelar **antes** do checkpoint, os commits, estado da branch e runs são a evidência primária; os comentários anteriores não bastam. Estado 'pendente' não é 'aprovado'.

## Ponto de retomada conhecido em 18/10/2026 (confirmar novamente)

- Versão da `main`: `0.9.46-open-beta`, `versionCode 55`.
- Item 1.2, **gravura profissional da partitura no Estudo**, em pesquisa: [PR #46](https://github.com/atshimura/Piano-livre-aberto/pull/46), branch `research/estudo-gravura-osmd-poc`, aberto em draft, com 178 commits e 22 arquivos alterados quando verificado. HEAD observado: `c9e3c2ffb5f2cf46939900bec062f74f05d8f98d`.
- Documento técnico da **branch do PR #46**, não necessariamente da `main`: [docs/56-ESTUDO-GRAVURA-PROFISSIONAL-POC.md](https://github.com/atshimura/Piano-livre-aberto/blob/research/estudo-gravura-osmd-poc/docs/56-ESTUDO-GRAVURA-PROFISSIONAL-POC.md). Ler também [issue #34](https://github.com/atshimura/Piano-livre-aberto/issues/34) e [issue #11](https://github.com/atshimura/Piano-livre-aberto/issues/11).
- O PR informa build e emulador aprovados para o commit de pesquisa indicado **na descrição do próprio PR**, mas falta aceite editorial da gravura, ampliação de casos adversariais, seleção de trecho, medições de desempenho e validação física. **Não fazer merge/release/alterar versão apenas porque CI passou**.
- Próxima unidade de trabalho sugerida: confrontar capturas reais do emulador com referência de notação profissional para o mesmo conteúdo, registrar objetivamente problemas de leitura/tamanho/colisões, corrigir um subconjunto mensurável e atualizar checkpoint. Confirmar qual o último checkpoint antes de iniciar.
- Não reescrever ou dividir retroativamente os 178 commits do PR #46 por conveniência. Para entregas futuras, preferir PRs menores independentes quando possível.

## Prompt de retomada para nova sessão

> Retome o desenvolvimento do Piano Livre no repositório `atshimura/Piano-livre-aberto`. **Primeiro** leia `AGENTS.md`, `docs/RETOMADA-SEGURA.md`, `docs/10-CONTINUIDADE.md`, a documentação técnica da branch ativa e o checkpoint mais recente do respectivo PR. Verifique `main` e HEAD/CI atuais no GitHub. Não presuma que a conversa anterior terminou com sucesso; identifique o que foi realmente commitado. Continue **somente a próxima unidade de trabalho verificável** indicada pelo checkpoint, com commits pequenos, testes adequados e atualização do checkpoint. Preserve decisões pedagógicas, dados, GPL-3.0 e as restrições de assinatura/offline. No item 1.2, não integre o PR #46 nem substitua o modo Estudo antes dos critérios editoriais, funcionais e de desempenho documentados. Ao terminar a unidade, deixe a próxima ação explícita no GitHub.

## Em caso de novo travamento

1. Verificar se um commit novo apareceu no PR e se um Action foi iniciado/concluído; não inferir fracasso pela tela congelada.
2. Abrir **outra conversa curta**, colar o prompt acima e pedir leitura do checkpoint. Não colar toda a conversa antiga.
3. Se o problema também atingir conversas curtas, testar outro navegador/dispositivo/rede, consultar [status.openai.com](https://status.openai.com/) e, persistindo, contatar o suporte com horário, URL do chat e detalhes da falha. O diagnóstico do serviço é separado da continuidade técnica do GitHub.
