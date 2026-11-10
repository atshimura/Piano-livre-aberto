# ZAI-005 — Entrega confiável de APKs via download direto no GitHub

**Data:** 10/11/2026. **Motivação:** o usuário recebeu repetidamente **“Library file not found”** ao tentar baixar APKs por links `sandbox:/mnt/data/...` da interface ChatGPT, mesmo quando os arquivos existiam e tinham checksums corretos no ambiente do agente. **A existência local não garante acesso pelo usuário à biblioteca de arquivos.**

## Diagnóstico e solução

- Os links do ChatGPT são gerenciados pela plataforma e podem falhar por problemas de biblioteca/anexo, expiração ou resolução de link. Não podem ser considerados um canal de distribuição confiável controlável pelo projeto.
- Artefatos do GitHub Actions são válidos para auditoria, mas **expiram** (30 dias no workflow de builds) e podem exigir login GitHub. Também não são o caminho canônico de instalação.
- O usuário solicitou um método que **não recorra mais ao erro Library file not found**. O aplicativo é GPL, e o repositório aberto já contém os fontes; foi criada uma branch pública de arquivos `zai/downloads`, **sem tocar em `main`, sem nova infraestrutura paga, servidor ou serviço externo, sem transformar o APK em uma Release oficial**.
- Cada APK é colocado em `apks/principal/` com **nome de versão imutável** e arquivo de SHA-256. Não substituir versões anteriores silenciosamente. A URL principal deve apontar ao GitHub, de preferência ao **commit específico de publicação** para reduzir o risco de branch alterada.
- O código binário (.apk) é armazenado junto ao histórico Git da branch de downloads; não entra na branch principal e não é compilado ali.
- Os arquivos de instrução e hash devem permanecer no repositório. **Nunca mover keystores/senhas para a branch de downloads**: somente o APK release já assinado sai do GitHub Actions.
- A continuidade da disponibilidade depende de o repositório, o commit e o GitHub permanecerem disponíveis. Não há garantia absoluta de operação contínua de serviços externos, mas não há prazo de expiração de 30 dias para esse arquivo versionado no histórico do Git.

## APK principal 0.9.66 — publicação efetiva

- Código funcional ZAI-004: [PR #118](https://github.com/atshimura/Piano-livre-aberto/pull/118), merge em `zai/lab`.
- Build original com assinatura permanente: [GitHub Actions #42800557154](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42800557154) — **SUCCESS**; artefato temporário ID `13756365992`.
- Reutilização **dos mesmos bytes**, sem novo build ou nova assinatura: [GitHub Actions #42811586606](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42811586606) — **SUCCESS**.
- Branch de download: `zai/downloads`, commit da publicação do APK `f9895ce1d745f333c134a537578112e74f48b9f4`. O workflow baixou o artefato anterior, verificou SHA-256, `apksigner`, `aapt` (identificador/versão/rótulo) e fez push para a branch exclusiva com `GITHUB_TOKEN` de acesso restrito ao repositório.
- Arquivo público versionado: `apks/principal/PianoLivre-Principal-0.9.66-open-beta-assinado.apk`, tamanho **794.540 bytes**.
- SHA-256: `352890f1c5b337964b79de20cd062de4c1ec4afd53de4b5ae9662055766fe8f4`.
- Identificador: `br.org.pianolivre.opensource`, versão `0.9.66-open-beta`, `versionCode=75`.
- SHA-256 da assinatura permanente: `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`.
- A verificação pós-push leu o arquivo **do Git remoto em `refs/remotes/origin/zai/downloads`**, recalculou seu SHA-256 e comparou com o original. O GitHub confirmou também o objeto binário no commit `f9895ce...`, tamanho 794.540 bytes. A execução #42811586606 terminou SUCCESS.
- URL **direta versionada (commit específico)**:
  `https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/f9895ce1d745f333c134a537578112e74f48b9f4/apks/principal/PianoLivre-Principal-0.9.66-open-beta-assinado.apk`
- URL para abrir pelo navegador GitHub, sem depender da biblioteca do ChatGPT:
  `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.66-open-beta-assinado.apk`
- URI curta por branch (pode mudar se a branch for alterada):
  `https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/refs/heads/zai/downloads/apks/principal/PianoLivre-Principal-0.9.66-open-beta-assinado.apk`
- Registro do checksum versionado:
  `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.66-open-beta-SHA256.txt`.

## Contrato obrigatório para as próximas entregas

**Padrão a seguir após cada novo APK**:

1. Gerar por branch e PR da ZAi, sem alterar `main` sem autorização expressa. Testar motor JVM, contratos, compilação APK e identidade Android; usar exclusivamente assinatura permanente de Secrets configurados. Diferenciar explicitamente APK principal `br.org.pianolivre.opensource` de APK experimental `br.org.pianolivre.opensource.zai`.
2. Salvar o APK como artefato temporário no Actions (com hashes e metadados) **e publicar os mesmos bytes em `zai/downloads`** por um workflow dedicado. O workflow de publicação deve usar `permissions: actions:read, contents:write`; confirmar a versão, assinatura e hash **antes e depois** de publicá-lo; não criar uma Release sem autorização. Nunca sobrescrever uma versão já existente com bytes diferentes; gerar novo número de versão.
3. Produzir **link direto GitHub raw por SHA de commit** como o **primeiro link de instalação**, seguido de link para a página do arquivo no GitHub; usar artefato Actions apenas como alternativa secundária e `sandbox:/mnt/data` somente como opção adicional, sem prometer que funcione.
4. Registrar no `ZAI-CONTINUIDADE.md` e na documentação de entrega: SHA do código gerador, número da versão, código, fingerprint, tamanho do APK, SHA-256, links de download e run da publicação. Informar se não há testes físicos. Não afirmar “link testado pelo usuário” sem confirmação no dispositivo.
5. É proibido apresentar um caminho de arquivo do ambiente local, anexos do ChatGPT ou uma URL presumida como se a plataforma garantisse sua entrega. **Verificar o objeto remoto e seu hash, não só o ZIP do runner.** Caso o push ao GitHub não funcione, interromper divulgação do link “permanente” e informar a falha; nunca inventar caminho.

## Restrições de instalação

O APK 0.9.66 principal atualiza o código 74 do 0.9.65, usando o mesmo identificador e a mesma assinatura. Fazer backup validado antes e instalar sobre a versão anterior, **sem desinstalar**. Não presumir que os dados migraram corretamente apenas por a assinatura ser válida.

A solução elimina a dependência dos links Library do ChatGPT para **esta e futuras entregas que seguirem o contrato**, mas não pode garantir que um serviço de terceiros esteja online para sempre. Essa distinção deve continuar explícita.
