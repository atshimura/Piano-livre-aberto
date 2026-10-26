# 11 — Manual da beta pública

Para obter um APK público, abra a página GitHub Actions → **Open Piano Livre - tests and APK** → execução da `main` com status success → artefato `PianoLivre-open-release-apk` (desde 0.9.35; versões anteriores usavam `PianoLivre-open-debug-apk`). O link expira após poucos dias.

O aplicativo aberto tem identificador `br.org.pianolivre.opensource`: pode coexistir com o antigo pacote privado, mas **não compartilha dados nem o substitui automaticamente**. Não desinstale a edição antiga sem conferir backup e restauração.

Teste na biblioteca a abertura de partituras, as claves Sol/Fá e a rolagem. Em seguida confira teclado-guia visual, modos Espera/Contínuo/Demonstração, reconhecimento MIDI, pedal, backup de teste e navegação. As notas extras no Espera são indicadas, mas não bloqueiam o avanço. A calibração de acordes depende do piano real.

O teclado virtual guia mostra graficamente notas de ambas as pautas; isso não equivale a sintetizador sonoro no celular. Testes no Yamaha P-45, Realme 7 5G e Lenovo Tab M9 continuam necessários.

## Recursos da linha atual

Na biblioteca, busque por título/arquivo (acentos e maiúsculas não interferem), marque favoritos e crie coleções. Um toque longo no cartão permite organizar a partitura. Nomes, avatares e exclusão confirmada de perfis ficam em Opções → Gerenciar perfil; cada perfil tem biblioteca organizada e histórico próprios. Partituras importadas são compartilhadas no aparelho e não são apagadas ao excluir perfil/coleção.

Histórico registra o tempo de partitura aberta em primeiro plano, incluindo leitura e pausas. Escolha uma partitura no histórico para definir/remover uma meta de minutos. Não é avaliação de notas, ritmo ou esforço. O período aberto pode se perder em encerramento abrupto. Antes de limpar histórico e metas, exporte backup se quiser guardá-los.

Importar partitura aceita XML/MusicXML/MXL e MIDI SMF 0/1 PPQN. MIDI exige escolher direita/esquerda/ignorar por trilha; não se deduz mão pela altura. A escrita é aproximada em 4/4, sem reconstruir pedal, controladores ou programas; o .mid original fica no backup junto ao XML derivado. Nomes repetidos não sobrescrevem arquivos anteriores. Microtons, transposição e compassos MusicXML incompatíveis são informados; exporte versão compatível antes de importar.

A consolidação 0.9.31 prepara distribuição durável em Releases, com assinatura permanente e SHA256SUMS após checks. Confira a existência da pré-release e o commit efetivo. O ensaio automático de atualização usa dados sintéticos; instalação/atualização e MIDI nos aparelhos reais seguem pendentes. Veja documento 40.
