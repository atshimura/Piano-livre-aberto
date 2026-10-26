# APK de distribuição — 0.9.35 / code 44

Release passa a usar pacote br.org.pianolivre.opensource, depuração desativada e o mesmo certificado permanente no build confiável da main. Não há mudança de namespace/pasta de dados entre debug aberto e release aberto. A variante release anterior herdava o applicationId histórico; agora ambas as variantes públicas têm sufixo explícito, sem consultar repositório/assinatura privados.

PRs e emuladores usam somente a chave debug descartável do runner para compilar as duas variantes e testar atualização. Main configura assinatura permanente por Secrets; ambas variantes devem apresentar certificado igual, e a distribuição deve conferir com a fingerprint pública registrada. Ausência/divergência impede publicação. Minificação permanece desativada nesta beta para preservar o comportamento já validado.

Workflow compila debug e release. Verifica pacote/versão, certificado coincidente, ausência de depuração e permissão INTERNET no APK distribuído. Só release entra no artefato PianoLivre-open-release-apk e na pré-release durável. Artefatos/URLs das versões anteriores são preservados. Publicação aguarda build/emulador do mesmo merge e testa também rejeição de metadados debuggable; PublishBetaTest tem sete cenários sem rede.

Após regressões debug, o emulador instala release por cima, sem limpar dados. Restaura somente o MXL sintético conhecido removido pelo teste anterior, lança com flags de self-test e verifica que são ignoradas. Confirma teclado, velocidade e perfil ID 7 preservados pela UI, processo vivo e run-as bloqueado por não ser depurável. Exige PUBLIC_RELEASE_UPGRADE_UI_PASS e envia screenshot/JSON. Isso ainda depende da CI desta entrega; não é teste físico nem migração do pacote privado.

Estado anterior confirmado: PR #32 integrado em 175e9e9c0ce5e6acdbc6606318b9f44e73501cad após build 38987484430/emulador 38987484513. BACKUP_SELFTEST_PASS 36, UPGRADE_SELFTEST_PASS 6 e XML_INPUT_SELFTEST_PASS 6. Pré-releases 0.9.31–0.9.33 verificadas; conferir a publicação da 0.9.34 nos Actions do merge.

Próxima ação: confirmar CI deste APK de distribuição e corrigir falhas. Integrar sob proteção; verificar certificado, checks do merge e pré-release. Depois registrar em #11/#4 a pendência de aceite físico Realme 7 5G/Lenovo Tab M9/Yamaha P-45, conforme documento 40. V1 ainda não concluída. Continuar correções independentes constatadas; não presumir hardware a partir do emulador.
