# 25 — Histórico da linhagem e separação dos APKs

A versão privada evoluiu pelas betas 0.9.9, 0.9.10 e 0.9.11, cujos testes de atualização dependiam de um certificado e de um pacote Android históricos. Esses materiais **não integram** a edição aberta.

O repositório público tem histórico Git independente e o pacote `br.org.pianolivre.opensource`, diferente de `br.org.pianolivre`. Não ocorre atualização direta nem transferência automática de dados entre esses aplicativos. A via potencial de migração é backup/restauração manual, após teste específico.

Para manter a continuidade da linha aberta, aumentar a versão efetiva em `PianoLivreAndroid/gradle.properties` ao modificar o APK, assinar versões sucessivas com a mesma chave pública estável e testar atualização real. Sem assinatura persistente, dois artefatos da CI podem ser incompatíveis na instalação incremental.
