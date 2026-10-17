# 07 — Estado atual e prioridades da linha aberta

## Implementado no código-base público
- Android em paisagem, funcionamento offline, importação local MusicXML/XML/MXL.
- Partitura temporal SVG em tiles, claves Sol/Fá ancoradas, leitura livre via OSMD, teclado guia **visual** para ambas as pautas.
- Modos Espera/Contínuo/Demonstração, rotas de repetição, partes por mão, entrada/saída MIDI USB e diagnóstico.
- Espera com indicação não bloqueante de notas extras; janela de acordes calibrável; duração e pedal orientativos.
- Perfis locais, retomada básica de sessão e backup v2 com pré-validação e rollback.
- CI com testes JVM, JavaScript, inventário de partituras, MIDI, compilação e emulador.

## Ainda pendente na aplicação (não confundir com migração GitHub)
- Validação em Yamaha P-45 e celulares/tablets reais; latência, rolagem, conexão MIDI e notas presas.
- Confirmar os PRs de compatibilidade/importação em curso. Busca/favoritos/coleções, perfis estáveis e histórico/metas locais foram implementados; importação MIDI aproximada integrada no PR #26.
- Cobertura de notação complexa, robustez de MusicXML externo e critérios musicais testados editorialmente.
- Migração entre pacotes apenas por backup manual validado, com caminhos portáteis; ensaio físico ainda pendente. Nenhum acesso direto ao pacote/repositório privado.
- Assinatura persistente confirmada na 0.9.13/code22 pelo build público 38938312341; validar o certificado em cada entrega. Atualização com preservação de dados ainda requer ensaio físico.

## Roteiro vigente

O desenvolvimento segue as **seis etapas aprovadas do Projeto**, detalhadas em [28 — Etapas até V1](28-ETAPAS-V1.md) e acompanhadas na [issue #4](https://github.com/atshimura/Piano-livre-aberto/issues/4). Etapa 1 com escopo técnico automatizado concluído no PR #14; aceite físico permanece na Etapa 6. Etapa 2 com escopo técnico concluído e limites MusicXML explícitos; Etapa 3 com escopo técnico automatizado concluído; Etapa 4 com escopo técnico concluído no PR #24; Etapa 5 com escopo técnico concluído nos PRs #25–#28; Etapa 6 em desenvolvimento no PR #29, sem aceite físico. A entrega inicial 0.9.13 reduz custo de animação e evita reconstrução de SVG ao abrir o teclado guia; resultados atuais e pendências estão no documento 28.

**Estado legal/técnico:** GPL-3.0-only apenas para o código próprio; direitos das músicas descritos separadamente. Uma execução de CI verde comprova aquele commit e cenário automatizado, não conformidade jurídica integral nem qualidade certificada do produto.
