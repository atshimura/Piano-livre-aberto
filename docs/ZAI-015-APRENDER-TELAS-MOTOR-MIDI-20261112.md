# ZAI-015 — Laboratório > Aprender: escolher/tocar e leitura progressiva

**Data:** 12/11/2026. **Base:** `zai/lab` após ZAI-014/APK 0.9.75 (code84). **Produto aprovado:** quatro imagens em paisagem: entrada Aprender em dois cartões, configuração Escolher e tocar em três painéis, trilha Ler partitura e exercício de leitura de notas na pauta. **Entrega-alvo:** APK principal **0.9.76-open-beta / code85** via assinatura permanente em GitHub Actions Secrets, sem alterar `main`, repositório privado, tags ou Releases.

## Decisões funcionais

**Entrada:** Laboratório Musical oferece Descobrir / Aprender / Criação. Aprender deixa de ser Em breve; pode voltar ao seletor e ao ponto de entrada sem perder projetos. Dois caminhos claramente distintos:

1. **Escolher e tocar:** seleção por tipo (Nota, Acorde, Sequência), conteúdo real pesquisável por nome musical e mão de referência. Nota exige altura MIDI exata; acorde exige **conjunto completo de alturas exatas sustentadas simultaneamente**, sem janela de tempo arbitrária; sequência exige ataques de notas na ordem, podendo tocar a próprio ritmo. Acordes admitem a entrada progressiva das notas enquanto teclas anteriores permanecerem pressionadas; notas extras impedem validar até correção. A escolha de mão é apenas referência gráfica, **não afirma detectar a mão física**. Descrição da oitava/claves deve ser precisa. A partitura é exibida pelo mesmo OSMD local, sem mudar o MIDI da composição.
2. **Ler partitura:** trilha com módulos Notas na pauta, Movimentos e intervalos, Ritmo, Acordes e Leitura livre. Começar com **lições de notas na pauta**, oito exercícios reais e repetíveis com dificuldade gradual, usando clave de Sol e teclas MIDI reais (ou teclado virtual). "Movimentos e intervalos" e "Ritmo" podem oferecer lições simples de ataques, mas não podem declarar avaliação rítmica/timing sem uma implementação; módulos avançados só ficam disponíveis após aquisição de pré-requisitos comprovados. Progressão nunca avança por cronômetro nem por toque em Próximo sem acerto. Lições concluídas podem ser revisadas livremente. Dicas incluem nome da nota, tecla guia e demonstração; **dicas retiradas gradualmente**, mas podem ser pedidas de volta a qualquer momento.
3. **Progresso local por perfil:** contagens de exercícios concluídos, notas dominadas e nível/índice de lição armazenadas em chaves `user_<ID>_learning_v1` do SharedPreferences existente. O backup atual exporta todas as preferências tipadas, inclusive essas chaves; sem nova tabela/schema e sem substituir outras preferências. A entrada com outro perfil carrega somente seu progresso. Evitar pontos inflacionados pelo mesmo acerto repetido; permitir revisão sem apagar progresso.
4. **Entrada e saída:** USB-MIDI físico (novo Note On) e teclado virtual multitouch (somente transições de toque) com indicação clara da origem; não interpretar Note Off/pedal como acerto, ignorar eventos de demonstração e notas sustentadas de sessão anterior; zerar sessão ativa ao sair, trocar de perfil ou desconectar, mantendo progresso gravado. Não inventar latência ou avaliação de dedilhado/tempo real.
5. **Ouvir exemplo:** oferecer reprodução **local sem internet** de tom simples (não pretende simular o timbre do piano); não confundir com reconhecimento de execução. Pautas e notas via OSMD compartilhado (MusicXML original local), interface adaptável a resoluções de telefone/tablet, botões com tamanho mínimo 44–48dp e cabeçalho fixo. Não criar serviços externos, telemetria, autorizações ou dependências pagas.
6. **Direito autoral:** os exercícios são gerados a partir de notas, intervalos e acordes públicos e não incluem partituras protegidas.

## Plano técnico e critérios de aceite

P15-0: ler AGENTS, documentação e estado de `zai/lab`; registrar contrato antes do código.

P15-1: motor Java puro `MusicLearningEngine`: catálogo por tipo, lições determinísticas com IDs, estado de tentativa, verificação de nota/acorde/sequência, emissão de eventos apenas em ataques, ajuda progressiva e atualização idempotente de progresso. Criar testes JVM de acerto/erro/repetição/inversão/pedal/extra/saída.

P15-2: MusicXML pontual `MusicLearningScore`, com clave coerente, alturas/oitavas/acidentes, vozes e duração simplificada **somente para exercícios**. Renderizar no OSMD existente, sem modificar o gerador Criação nem partituras de usuários; testes de parser/JVM e navegador OSMD verificam geometria de pauta.

P15-3: interface Android `MusicLearningScreen`: quatro telas aprovadas, estados acessíveis, navegação/volta do sistema, busca de catálogo, configuração do treino, lições/progresso, pauta, ajuda/teclado, feedback imediato e opção de ouvir exemplo. Tornar Aprender clicável em `MusicLabLandingScreen`, integrar navegação `PianoScreens`, recepção MIDI e ciclo de vida `MainActivity` sem afetar Descobrir/Criação.

P15-4: testes de contratos de UI, correção por JVM, compilação Android debug isolado, smoke de emulador por navegação + notas virtuais, regressões OSMD/MIDI; corrigir falhas antes de PR/merge.

P15-5: PR somente para `zai/lab` com checks. CI principal release assina code85 com o mesmo certificado público permanente, pacote `br.org.pianolivre.opensource` sem INTERNET/debug; publicar **exatamente os mesmos bytes** em `zai/downloads` com SHA-256 remoto e relatório, sem tag/Release.

## Limites honestos desta primeira versão

Leitura musical completa exigirá lições didáticas mais profundas de claves, pausas, pontos, ritmo mensurado, polifonia e articulação. Não declarar que os módulos ilustrados são pedagógica ou tecnicamente completos quando ainda não forem. A versão inicial oferece uma trilha funcional de **notas na pauta** e pode reservar outros módulos como disponíveis apenas após implementação real. A dificuldade/adaptação inicial é simples e não equivale a avaliação por professor. Sem testes físicos no Yamaha P-45, Realme 7 5G e Lenovo Tab M9 — emulador não comprova conexão/latência reais.
