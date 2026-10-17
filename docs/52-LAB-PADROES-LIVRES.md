# 52 — Laboratório: reconhecimento livre monofônico de escalas e arpejos (0.9.43)

## Escopo
O modo Descobrir mantém as sequências delimitadas por Iniciar/Concluir da 0.9.39, mas também acompanha em paralelo uma **janela deslizante de ataques de notas isoladas**, sem exigir iniciar gravação. Para não inventar ordem de dedos simultâneos, um ataque que se sobrepõe a outro cancela a hipótese corrente. Capturas delimitadas continuam separadas.

O analisador `MusicPatternTracker` limita armazenamento às 13 últimas alturas MIDI e examina sufixos completos de 1 oitava com base em `MusicSequenceAnalyzer`. Aceita padrões ascendentes/descendentes identificáveis e evita impor intervalo em milissegundos não calibrado. Identificações são **compatibilidades de padrão**, não conclusão de tonalidade funcional, fraseado ou ritmo. Teclado virtual Tocar e entrada USB-MIDI usam a mesma estratégia. Sem conexão de rede, persistência adicional ou sintetizador.

## Testes e limites
`MusicPatternTrackerTest` valida escalas, arpejos, direção, limites de memória, entradas incorretas e rejeição de simultaneidade. Conferir regressões Android/emulador do SHA do PR. Instrumentos reais continuam sem teste #11. Reconhecimento livre continua conservador: legato com dedos sobrepostos, sequências incompletas, mudanças de direção, modos inferidos sem contexto, improvisação e ritmos não são classificados automaticamente. Nunca inventar tonalidade.

## Dependência concluída
O agrupamento de vigas (#41) foi integrado à `main` em `f62586b61e1a77690812b4e438d85dc84c954062` com CI e emulador aprovados. Revalidar este PR no próprio head após retarget para `main`.
