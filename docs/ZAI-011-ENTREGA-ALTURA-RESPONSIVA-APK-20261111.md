# ZAI-011 — Entrega de altura adaptativa do Laboratório/Criação e APK 0.9.72

**Data:** 11/11/2026. **Escopo:** correção da pequena rolagem vertical relatada pelo mantenedor nas novas telas ZAI-010. Implementação integrada somente à branch `zai/lab`; `main`, repositório histórico privado, tag, Release e Play Store não modificados.

## Funcionalidade e limites

- **Laboratório Musical:** a altura das três opções (Descobrir, Aprender em breve e Criação) e dos cabeçalhos é calculada a partir da altura útil do componente Android na orientação paisagem. Cartões reduzem a altura automaticamente na tela baixa e crescem moderadamente em tablets. Scroll residual só aparece quando a janela é insuficiente ou com fonte de acessibilidade ampliada, para não cortar informações.
- **Criação Musical:** o topo e a faixa com perfil/MIDI ficam fixos. As colunas esquerda e direita usam toda a área restante e têm scroll independente somente quando necessário. A coluna direita preserva o scroll de Minhas criações ao voltar dos detalhes. O cartão de nova gravação e o acesso a Minhas criações reduzem altura para caber em telas menores.
- **Dados:** nenhuma mudança no gravador/diário, arquivos `.plc`/`.pls`, perfil, backup, gerador MusicXML, reconhecimento USB-MIDI ou comportamento de saída de tomadas não salvas. ZAI-010 (figuras simples e MIDI original intacto) permanece integrado.

## Evidências verificadas

- [PR funcional #142](https://github.com/atshimura/Piano-livre-aberto/pull/142), merge `043f22efe84f9f6299d00266b26024e4bfad1832`; [documentação de diagnóstico](ZAI-011-TELAS-LABORATORIO-CRIACAO-ALTURA-ADAPTATIVA.md).
- [Actions ZAI-011 #43006469820](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43006469820) **SUCCESS**: `ZAI011_ADAPTIVE_HEIGHTS_PASS checks=89`, `ZAI011_VERTICAL_LAYOUT_CONTRACT_PASS`, contrato ZAI-010, auditoria pública e `ZAI011_ANDROID_BUILD_PASS`. Testes calculam alturas úteis de 300–800dp, exceções de 270–290dp e escala de fonte 1,4; botões da Criação de 150–500dp. Os 9 checks do PR #142 ficaram **SUCCESS**.
- [Actions principal #43006916126](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43006916126) **SUCCESS**, commit `71ed650738bf5ffea4d3bf4734abeeb02dc13adb`: regressões JVM do motor e do layout, contratos Python e compilação APK release assinado por Secrets, pacote principal `br.org.pianolivre.opensource`, versão `0.9.72-open-beta`/`versionCode 81`, minSdk 26, targetSdk 36, não depurável, sem permissão INTERNET. Certificado permanente SHA-256 `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`.
- [Actions de distribuição #43007398252](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43007398252) **SUCCESS**: baixou o artefato assinado sem recompilar, confirmou integridade SHA-256, nome, versão, certificado e ausência de INTERNET/debug, gravou os mesmos bytes em `zai/downloads` e conferiu o hash após o push. Commit de arquivo `4ed29b628795bd6abf555c8625f11009e505b6f9`.

## APK para teste físico

- **Versão:** 0.9.72-open-beta (81), 802.232 bytes.
- **SHA-256:** `6f4cb0f9c6e0646b85cef576a0683bf691e840ba75abd1db1686c0b0790b248a`.
- **Link direto fixado ao commit do GitHub:** https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/4ed29b628795bd6abf555c8625f11009e505b6f9/apks/principal/PianoLivre-Principal-0.9.72-open-beta-assinado.apk
- **Página do APK:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.72-open-beta-assinado.apk
- **Comprovante:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.72-open-beta-SHA256.txt

## Testes físicos pendentes e próximos passos

1. Fazer backup antes de instalar por cima da 0.9.71, **sem desinstalar**. Verificar que composições, perfis e partituras continuam disponíveis.
2. Em paisagem no Realme 7 5G e Lenovo Tab M9, verificar que a tela **Escolha um modo** mostra os três cartões e o botão Voltar sem rolagem inteira, e que o painel **Criação** mantém cabeçalho fixo. Confirmar rolagem **somente em listas e detalhes extensos**, inclusive com gravações existentes.
3. Verificar gravação, parada, revisão, salvamento e retorno ao painel, sem perda de MIDI. Validar fontes ampliadas quando possível, mantendo fallback rolável em espaço insuficiente.
4. Não foram executados testes físicos; os testes Android de compilação, JVM e contratos não substituem essa validação. Sem afirmar cobertura de todos os dispositivos, densidades e tamanhos de fonte.
5. Integração à `main` e Release/Play Store são trabalhos distintos, fora do escopo desta entrega.
