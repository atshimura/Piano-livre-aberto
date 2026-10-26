# 09 — Segurança e licenças na versão aberta

O aplicativo opera offline e não requer permissão INTERNET em tempo de execução. Os arquivos MusicXML/XML/MXL e backups importados são privados do usuário; não anexar dados reais a issues públicas.

A importação de ZIP e XML não confiável deve continuar recebendo testes adversariais e limites de tamanho; o WebView e a ponte JavaScript–Java são superfícies de risco. O backup v2 inclui pré-validação, confirmação e rollback, mas não criptografa os dados exportados.

A distribuição pública tem o pacote separado `br.org.pianolivre.opensource` e não recebe nenhum certificado histórico. A assinatura persistente futura deve ser configurada em ambiente confidencial pelo mantenedor. PRs externos não podem receber segredos.

O código próprio usa GPL-3.0-only. Componentes de terceiros, incluindo OpenSheetMusicDisplay, conservam suas licenças. Partituras e arranjos precisam de autorização específica; veja [licenças das partituras](LICENCAS-PARTITURAS.md).

Para uma versão estável, verificar importação adversarial, notas MIDI em aparelhos reais, licença das músicas, assinatura/atualizações, isolamento de dados e dependências. Ativar proteção da branch principal nas configurações do GitHub.
