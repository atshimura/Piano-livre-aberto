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
- Importação MIDI como partitura, biblioteca aprimorada (busca/favoritos/coleções), metas/histórico e perfis completos.
- Cobertura de notação complexa, robustez de MusicXML externo e critérios musicais testados editorialmente.
- Migração assistida de dados entre o pacote histórico `br.org.pianolivre` e a edição pública `br.org.pianolivre.opensource`.
- Assinatura durável para atualizar APKs públicos: dependente de quatro GitHub Secrets criados pelo mantenedor.

## Roteiro
1. Manter a CI e a documentação públicas coerentes e sem artefatos privados.
2. Preservar repertório com licenças verificadas e não incluir obras protegidas sem autorização.
3. Configurar proteção de `main`, assinatura durável e fluxo de PR.
4. Testar pessoalmente dispositivos e piano antes de declarar release estável.
5. Desenvolver incrementalmente os recursos V1 pendentes em PRs pequenos e testados.

**Estado legal/técnico:** GPL-3.0-only apenas para o código próprio; direitos das músicas descritos separadamente. Uma execução de CI verde comprova aquele commit e cenário automatizado, não conformidade jurídica integral nem qualidade certificada do produto.
