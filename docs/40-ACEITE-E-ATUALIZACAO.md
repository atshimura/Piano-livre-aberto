# Consolidação e aceite — 0.9.31 / code 40

A V1 ainda não está concluída. Esta entrega prepara atualização em emulador e distribuição durável da beta. Validar a própria CI antes de integrar; os cenários novos abaixo ainda não constituem evidência aprovada.

| Área | Evidência automatizada já confirmada | Limite de aceite |
|---|---|---|
| Fluidez, pausa, seek, loop, resize, background | PRs #12–#14, TemporalRenderingTest e emulador | Microtravamentos em Realme/Lenovo não confirmados como eliminados |
| Modelo musical/guia/MIDI | Corpus público, fixtures Java/JS e PRs #15–#21 | USB/latência/pedal Yamaha físico pendente |
| Perfis/biblioteca/histórico | PRs #22–#24; histórico Android 8+1 verificações | Tempo de tela não é nota ou medida de esforço |
| Importação MIDI e fontes | PR #26, 17 verificações JVM + 4 Android; PR #27 seletor de mão no emulador | Notação aproximada; sem reconstrução de pedal/programa/compasso original |
| Backup/retomada | PR #27, build 38985550165 e emulador 38985550170; BACKUP_SELFTEST_PASS 28 | Restauração/atualização em aparelhos reais pendentes |
| Limites de MusicXML | PR #28 e documento 39 | Transposição/microtons/compassos aditivos não implementados; rejeição explícita |

## Novo ensaio de atualização

O emulador compila a base pública fixa f50b2b266a95b253faaa7ce5c9e7548ec9ec0c0f (0.9.25/code34), instala-a e prepara dados exclusivamente sintéticos: perfis IDs 0/7, avatar, favorito, mão, modo, velocidade, posição, teclado e um MXL do catálogo. Abre a base, instala o novo APK com adb install -r e verifica os dados sem alterá-los no self-test. O teste de UI remove apenas os arquivos sintéticos conhecidos na instalação nova do emulador.

Ambos APKs usam a mesma chave debug descartável do runner. Isso testa o mecanismo Android de atualização e compatibilidade de dados, não uma instalação física com a assinatura permanente. A assinatura permanente é verificada separadamente no build da main.

## Distribuição

Main passa a falhar se faltar assinatura permanente ou o certificado divergir do SHA-256 registrado. PRs continuam sem acesso a segredos. Artefatos passam a incluir APK, metadados e certificado por 30 dias. Uma mudança de versão na main, após build e emulador aprovados no mesmo commit, cria pré-release pública com APK assinado e SHA256SUMS; docs sem alteração de versão não criam nova release. O job de publicação usa apenas GITHUB_TOKEN com permissão de escrita no job confiável da main; não manipula Secrets. Se os checks falharem, não publica. Se já existir release daquele commit, preserva os assets; versão existente de outro commit exige incremento.

PublishBetaTest valida seis caminhos sem rede/publicação real: sucesso com target exato/prerelease, check falho, certificado errado, docs sem versão, repetição idempotente e versão conflitante. Publicação real só poderá ser confirmada pelos Actions após integrar esta entrega.

## Aceite físico que impede declarar 1.0

No Realme 7 5G e Lenovo Tab M9: instalar beta pública, atualizar preservando perfis/coleções/histórico/importações, abrir partituras longas e verificar rolagem/resize/background. Fazer backup de teste, restaurar e comparar dados. No Yamaha P-45: conexão/desconexão/reconexão, Espera com ataques/acordes/extras, calibração, teclado de duas mãos, pedal e Demonstração sem notas presas. Registrar versão, aparelho, cenário e resultado na issue #11. Nenhum desses resultados foi presumido.

Próxima ação: confirmar CI de #28 e desta entrega, integrar sob proteção, conferir assinatura e pré-release do merge. Resolver falhas antes de avançar. Etapa 6 permanece aberta enquanto faltar hardware; continuar qualquer correção independente identificada.
