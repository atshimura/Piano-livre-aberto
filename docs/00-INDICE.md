# Documentação do Piano Livre aberto

- [74 — Ajustes da Tela Inicial após a beta 0.9.52](74-AJUSTES-TELA-INICIAL-POS-BETA-0952.md): banner 30% menor, três atalhos ampliados, biblioteca reorganizada e grade de duas colunas; entrega em PR, sem merge/Release.

- [73 — Consolidação, integração e publicação, Prompt 4](73-CONSOLIDACAO-PUBLICACAO-PROMPT-04.md): precedência sobre os estados históricos de PR aberto abaixo; versão 0.9.52, gates finais, assinatura, APK e aceite físico pendente.
- [Notas da 0.9.52-open-beta](releases/0.9.52-open-beta.md).

**Esta documentação trata do repositório público.** O código provém de um desenvolvimento prévio privado, mas não depende do histórico ou de arquivos restritos para compilar.

## Entregas em preparação
- [72 — Consolidação MIDI e interfaces, Prompt 3](72-CONSOLIDACAO-MIDI-INTERFACES-PROMPT-03.md): estado compartilhado, reconexão, encerramento de notas, lifecycle e provas sintéticas nas duas telas; branch dependente do Prompt 2, sem merge/Release.
- [71 — Tela Estudar, Prompt 2](71-TELA-ESTUDAR-PROMPT-02.md): cabeçalho unificado, renderização automática, ocultação/restauração, medições e evidências no PR #73, dependente do PR #71; sem merge ou Release e com aceite físico pendente.
- [70 — Tela inicial, Prompt 1](70-TELA-INICIAL-PROMPT-01.md): implementação técnica concluída, evidências e APK verificados; PR #71 aberto para integração posterior, sem Release desta entrega e com aceite físico pendente. Próxima etapa: Prompt 2 — Tela Estudar.
- [66 — Laboratório Descobrir 1.0](66-LABORATORIO-DESCOBRIR-1.0.md): interface mínima pública, reconhecimento USB-MIDI, pauta nativa, testes e pendências de aceite.

## Documentos vigentes
- [65 — Auditoria e reconciliação da Entrega B](65-ENTREGA-B-AUDITORIA-E-RECONCILIACAO.md): falha da jornada Android, reconciliação de PRs e estado da prova B1+B.
- [01 — Visão e requisitos](01-VISAO-E-REQUISITOS.md): decisões de produto e critérios para V1.
- [02 — Arquitetura](02-ARQUITETURA.md): Android, MIDI, MusicXML e renderização.
- [03 — Notação e tempo](03-NOTACAO-TEMPO.md): renderizador, rolagem, claves e representação.
- [04 — MIDI e aprendizagem](04-MIDI-ESTUDO.md): regras pedagógicas e MIDI.
- [05 — Dados e backup](05-DADOS-BACKUP.md): perfis, sessões e persistência.
- [06 — Compilação e testes](06-BUILD-TESTES.md): fluxos públicos verificáveis.
- [07 — Estado e roadmap](07-ESTADO-E-ROADMAP.md): entregue versus pendente.
- [08 — Decisões](08-DECISOES.md): histórico funcional (ler à luz do AGENTS atual).
- [09 — Segurança e licenças](09-SEGURANCA-LICENCAS.md): prevenção e restrições.
- [10 — Continuidade](10-CONTINUIDADE.md): instruções para desenvolvedor/IA.
- [11 — Manual de teste](11-MANUAL-DE-USO-BETA.md): uso do APK público.
- [Assinatura pública](ASSINATURA.md): criação local e GitHub Secrets.
- [Licenças das partituras](LICENCAS-PARTITURAS.md): inventário de redistribuição.
- [25 — Linhagem histórica e separação de pacotes](25-CONTINUIDADE-0.9.9-0.9.11.md).
- [27 — Migração pública e critérios finais](27-PREPARACAO-PUBLICACAO.md).

- [28 — Plano sequencial até V1 e entrega da Etapa 1](28-ETAPAS-V1.md).

## Histórico técnico (versões anteriores)
Arquivos de auditoria e evolução, preservados **como referências históricas, não declarações de testes atuais**:
- [12 — Auditoria MIDI](12-AUDITORIA-RECONHECIMENTO-MIDI-MAOS.md)
- [13 — Correções MIDI](13-CORRECOES-MIDI-0.9.1.md)
- [14 — Teclado de duas mãos](14-TECLADO-DUAS-MAOS.md)
- [16 — Backup e consistência](16-BACKUP-MIDI-CONSISTENCIA.md)
- [17 — Espera e calibração](17-REGRAS-ESPERA-CALIBRACAO.md)
- [18 — Motor musical](18-MOTOR-MUSICAL-0.9.5.md)
- [19 — Percurso musical](19-PERCURSO-MUSICAL-0.9.6.md)
- [20 — Claves](20-NOTACAO-CLAVES-0.9.7.md)
- [21 — Quiálteras](21-QUIALTERAS-LIGADURAS-VOZES.md)
- [22 — Polifonia](22-POLIFONIA-QUIALTERAS-VIGAS-0.9.9.md)
- [23 — Notas de graça](23-NOTAS-DE-GRACA-ACIDENTES-0.9.10.md)
- [24 — Auditoria 0.9.11](24-AUDITORIA-NOTACAO-MIDI-ETAPAS-1-3.md)

O índice do histórico privado, instruções de assinatura beta antiga e configuração de runner pessoal **não** são documentos operacionais deste repositório. O `AGENTS.md` público prevalece quando houver divergências.

- [29 — Diagnóstico local de desempenho e lifecycle](29-DESEMPENHO-LIFECYCLE.md).

- [30 — Fixtures compartilhadas do motor musical](30-FIXTURES-MOTOR-MUSICAL.md).

- [31 — Guia visual expressivo](31-GUIA-EXPRESSIVO.md).

- [32 — Continuidade da sessão MIDI](32-SESSAO-MIDI.md).

- [33 — Perfis com identidade estável](33-PERFIS-ESTAVEIS.md).

- [34 — Biblioteca local, busca e organização](34-BIBLIOTECA-LOCAL.md).

- [35 — Histórico e metas locais](35-HISTORICO-E-METAS.md)

- [36 — Importação não destrutiva](36-IMPORTACAO-SEGURA.md)

- [37 — Importação MIDI](37-IMPORTACAO-MIDI.md)

- [38 — Compatibilidade de dados](38-COMPATIBILIDADE-DE-DADOS.md)

- [39 — Limites MusicXML](39-LIMITES-MUSICXML.md)

- [40 — Aceite e atualização](40-ACEITE-E-ATUALIZACAO.md)

- [41 — Desempenho do guia com notas longas](41-DESEMPENHO-GUIA-SUSTENTADO.md)

- [42 — Codificação MusicXML](42-CODIFICACAO-MUSICXML.md)

- [43 — Nomes legados](43-NOMES-LEGADOS.md)

- [44 — APK de distribuição](44-APK-DISTRIBUICAO.md)

- [45 — Laboratório Musical: escopo aprovado, primeira entrega e continuidade](45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md)
- [46 — Biblioteca em grade compacta](46-BIBLIOTECA-GRADE-COMPACTA.md)
- [47 — Nome da nota ao tocar na partitura](47-NOME-DA-NOTA-TOQUE.md)
- [48 — Laboratório: escalas e arpejos por sequência](48-LAB-SEQUENCIAS.md)

- [49 — Cursor fixo antes das notas no estudo](49-CURSOR-ANTES-NOTAS.md)

- [50 — Modo Espera: iniciar e preparação visual](50-ESPERA-PREPARACAO.md)

- [51 — Vigas inferidas por métrica](51-VIGAS-INFERIDAS.md)
- [52 — Reconhecimento livre de padrões](52-LAB-PADROES-LIVRES.md)
- [53 — Catálogo pesquisável do Laboratório](53-LAB-CATALOGO.md)
- [54 — Demonstração musical USB-MIDI](54-LAB-DEMONSTRACAO-MIDI.md)
- [55 — Prática guiada sequencial no Laboratório](55-LAB-PRATICA-SEQUENCIAL.md)

- [59 — Análise automática e Estudo adaptável](59-ESTUDO-ADAPTAVEL.md).

- [75 ? Descobrir: layout estavel e validacao rapida](75-DESCOBRIR-LAYOUT-ESTAVEL-E-PAUTA-PROFISSIONAL.md)
