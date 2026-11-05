# ZAI-CONTINUIDADE.md — Linha de desenvolvimento Z (Z.ai/GLM)

Criada em **2026-11-06**, por decisão do mantenedor **Alexandre Shimura**.

## Propósito

Esta é a linha de desenvolvimento **paralela** conduzida com o agente Z.ai (GLM),
preservando **integralmente** a linha original de desenvolvimento (conduzida com
ChatGPT), documentada em [AGENTS.md](AGENTS.md). Nenhuma decisão, regra ou registro
do AGENTS.md é alterado por esta carta; as duas linhas convivem em namespaces
separados.

## Relação com a linha original

- A linha original (`main` + branches `feat/`, `fix/`, `ci/`, `docs/`, releases)
  permanece **intocada** pela linha Z. Nenhum push da linha Z se dirige a essas
  branches sem autorização expressa do mantenedor.
- **Sincronização unidirecional**: `main` → `zai/lab`, por merge periódico. A linha
  Z sempre contém tudo o que a linha original já entregou.
- O caminho reverso (Z → `main`) ocorre **somente** via pull request expressamente
  autorizado pelo mantenedor, sujeito aos gates `build`/`emulator` da CI.
- A linha Z **não** altera `versionCode`, `versionName`, releases, assinatura ou
  quaisquer publicações; numeração e releases permanecem sob comando da linha
  original.

## Regras da linha Z

1. Toda branch da linha Z usa o prefixo `zai/` (ex.: `zai/lab`, `zai/feat/...`).
2. Commits da linha Z usam o prefixo `[zai]` na mensagem, mantendo o histórico
   identificável linha por linha.
3. Sem force-push; sem reescrita de histórico público; sem tocar em branches da
   linha original.
4. Sem alterar workflows, testes ou proteções existentes; sem desativar gates.
5. Trabalho em **português do Brasil**; licença **GPL-3.0-only** do código próprio
   preservada; licenças de terceiros respeitadas (ver
   [LICENCAS-PARTITURAS.md](docs/LICENCAS-PARTITURAS.md) e `score-rights.json`).
6. Nunca enviar credenciais, chaves, certificados ou dados privados de alunos ao
   repositório; segredos apenas via GitHub Actions Secrets.
7. Validações da linha Z executadas localmente (JVM/JS/Python) e, quando
   aprovadas pelo mantenedor, via workflow dedicado próprio (ex.: `zai-basic.yml`),
   sem alterar os workflows existentes. Gates completos `build`/`emulator`
   permanecem exigidos **somente** antes de qualquer integração futura à `main`,
   sempre com autorização expressa.
8. Produto permanece **local/offline**: sem login, servidor, telemetria ou
   sincronização automática; sem permissão `INTERNET`; sem dependências externas
   novas sem autorização.
9. Regras de produto do [AGENTS.md](AGENTS.md) (modo Espera, teclado guia, MIDI,
   backup, dados `.plc`/`.pls`) valem integralmente para a linha Z.

## Estado

- Base inicial: `main` em **0.9.64-open-beta** (versionCode 73), commit `55426cf`.
- Ambiente de trabalho do agente Z: sandbox com histórico completo, identidade
  de commits `Alexandre Shimura <70826980+atshimura@users.noreply.github.com>`.

## Registro de tarefas da linha Z

| Data | Branch | Descrição | Estado |
|---|---|---|---|
| 2026-11-06 | `zai/lab` | Criação da base da linha Z a partir de `main` (0.9.64). | Concluído |
| 2026-11-06 | `zai/lab` | Criação desta carta de protocolo (`ZAI-CONTINUIDADE.md`). | Concluído |
