# 27 — Finalização da migração pública

## Estrutura escolhida
- O projeto antigo permanece em repositório privado, com seu histórico intacto.
- O código aberto é desenvolvido e testado neste repositório, sob licença GPL-3.0-only para código próprio.
- A edição aberta possui pacote Android separado e não depende de chaves, binários ou downloads do repositório privado.
- As composições e partituras têm tratamento de direitos separado do código.

## Automação
Os workflows públicos usam apenas runners padrão Linux do GitHub, executam testes Java, JavaScript, MusicXML, MIDI e UI no emulador e compilam o APK. Os resultados pertencem ao commit/branch executado. O validador verifica catálogo e inventário de materiais musicais e bloqueia ativos privados conhecidos.

## Ações de administração do proprietário
1. Ativar proteção de `main` em Settings → Branches, exigindo PR e checagens do build e do emulador e impedindo sobrescrita/exclusão forçada.
2. Gerar uma chave de assinatura da edição pública em computador confiável e cadastrar os quatro GitHub Actions Secrets descritos em [assinatura](ASSINATURA.md); conferir identidade do certificado entre dois builds.
3. Autorizar uma Release somente após conferir APK, assinatura, versão, inventário de direitos e teste da CI no commit efetivo. O download de artefatos temporários não equivale a Release.
4. Validar pessoalmente Yamaha P-45, Realme 7 5G e Lenovo Tab M9 para a futura aprovação funcional.

## Segurança da biblioteca
Composições protegidas e versões não verificadas ficam fora da árvore pública, ainda que os arquivos MusicXML tenham sido criados pelo mantenedor. Revisar cada item antes de atualizar o manifesto de licenças.

A migração de código e testes pode estar completa sem a V1 funcional estar concluída. Não anunciar produção estável baseada somente em CI de emulador.
