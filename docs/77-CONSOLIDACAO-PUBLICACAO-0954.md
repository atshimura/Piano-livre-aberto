# Consolidação e publicação da beta 0.9.54

Autorização: [instrução integral](prompts/INTEGRACAO_PUBLICACAO_BETA_0954.md). Alvo único: **0.9.54-open-beta / versionCode 63**. Base auditada `f91c27853be576a353628e220d4e716d02507bed`, beta oficial 0.9.52/61. As restrições de merge/publicação das entregas anteriores são históricas e foram supersedidas por esta autorização específica.

## Composição e rastreabilidade

Branch `release/consolidacao-0.9.54`, criada sobre main. Os três HEADs estavam inalterados, abertos e aprovados quando conferidos:

| Entrega | PR | HEAD incorporado por merge |
| --- | --- | --- |
| Home, biblioteca, responsividade e testes | #76 | `72d1ee77412b7929b41fdaa94521cd3676cbe97f` |
| Descobrir profissional e CI rápida/completa | #78 | `8cfc5db53d9f0f7bec0796192d2c1fc7b561578e` |
| Documentação futura da Criação C1 | #80 | `11a8579ce23f023875299993a225dc8b10743d82` |

Não integrar os PRs individualmente à main: somente o PR consolidado, após aprovação do conjunto. Preservar os commits originais por ancestralidade. Não publicar 0.9.53, não fazer force-push, não alterar proteção ou Secrets, não acessar o repositório privado. Arquivos locais gerados e não rastreados permanecem preservados e fora dos commits.

## Resolução dos conflitos

Versão final 0.9.54/63 em Gradle e expectativas de upgrade. `emulator.yml` conserva `debug-apk`, `ui-fast`, `emulator-suite` e gate obrigatório `emulator`. Home passa a executar também no job rápido; sua jornada completa, doze capturas e relatório permanecem no fluxo integral. Descobrir conserva 18 estados e 14 componentes no fluxo completo. Filtros documentais da C1 foram mantidos. Flags internas Home/Descobrir são conjuntamente verificadas como ignoradas no APK release. Índice, decisões e continuidade preservam as três entregas.

Nenhuma implementação da C1 é incorporada. O código de produção da Home e Descobrir deve coincidir com as respectivas entregas. A única ampliação de Java nesta consolidação é a asserção de histórico sintético em `UpgradeSelfTest`, exclusiva de debug. Estudar, fontes MusicXML, dados reais, motor MIDI, assinatura e licenças permanecem preservados.

## Atualização e publicação

Mantidos os ensaios da base 0.9.46. Acrescentado ensaio de código público 0.9.52 → 0.9.54 na CI do PR, com chave descartável, perfis ID estáveis, biblioteca/importado, posição/velocidade e histórico de sessão. Na main, executar também os APKs oficiais realmente publicados 0.9.46 e 0.9.52 → release atual com certificado permanente; confirmar conteúdo das preferências e bytes do importado. Só dados sintéticos em emulador isolado.

Publicador existente `publish_public_beta.py`: 90 minutos de espera dentro de job de 100 minutos, para acomodar suíte e filas. Falha, cancelamento, skipped, checks ausentes ou timeout continuam bloqueando. Testes de segurança cobrem espera superior a 40 minutos, timeout, check ausente, identidade exata do SHA, certificado, pacote, depuração, INTERNET e idempotência. Não substituir assets existentes. As notas específicas estão em [0.9.54](releases/0.9.54-open-beta.md).

## Protocolo do registro efetivo

O fechamento do PR consolidado e a Release registrarão o próprio SHA de main e o checksum do APK, sem criar outro commit apenas para citar esses valores posteriores. Antes de concluir: conferir `build` e `emulator` do SHA integrado, proveniência e dimensões das capturas Home/Estudar/Descobrir, versão/pacote/assinatura do APK, SHA256SUMS, tag, target, assets e download real. Os resultados individuais dos PRs originais não aprovam a consolidação.

Conferir que #76/#78/#80 foram incorporados por ancestralidade e registrar seus estados finais. Encerrar #77 somente após incorporação efetiva da otimização; manter #79 aberta. Nenhuma outra issue ou PR histórico deve ser alterado.

## Limitações e próximos passos

Hardware físico permanece pendente: Yamaha P-45 (USB/OTG, notas/acordes, pedal e reconexão), Realme 7 5G e Lenovo Tab M9 (legibilidade, toque, responsividade, atualização e backup). Emulador/MIDI sintético não equivalem a esse aceite. A V1 não está concluída. C1 será implementada somente em tarefa futura, conforme [planejamento](76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md) e issue #79.
