# Especificação do Projeto — Chef Express

## 1. Visão Geral e Proposta
O **Chef Express** é um aplicativo Android de receitas culinárias projetado segundo a metodologia **Spec-Driven Development (SDD)** e arquitetura **Offline-First**. Ele resolve o problema comum de cozinheiros amadores e entusiastas perderem o acesso às receitas no meio do preparo por instabilidade de rede ou falta de conexão na cozinha, combinando um catálogo gastronômico rico via REST API (**TheMealDB**) com persistência local reativa via **Room** e interface totalmente localizada em Português (PT-BR).

## 2. Objetivos
- Oferecer descoberta rápida de receitas internacionais por nome, ingrediente, categoria e sugestão aleatória do chef.
- Garantir disponibilidade contínua das receitas consultadas e favoritadas mesmo sem conexão com a internet.
- Fornecer ferramentas práticas durante o preparo, como checklist interativo de ingredientes (persistido localmente) e timer de cozinha integrado.
- Aplicar rigorosamente Clean Architecture, programação reativa com Coroutines/Flow, injeção de dependências com Koin e UI moderna em Jetpack Compose (Material Design 3).

## 3. Requisitos Funcionais (RF)
- **RF01 — Catálogo e Descoberta:** Exibir lista de receitas obtidas da API TheMealDB e cacheadas localmente no Room, com busca textual instantânea e filtragem por categorias culinárias.
- **RF02 — Sugestão do Chef:** Permitir sortear uma receita surpresa (`random.php` quando online, ou seleção aleatória do cache local quando offline).
- **RF03 — Detalhes da Receita:** Exibir foto em alta resolução, categoria, origem culinária (área), tags, link opcional de vídeo no YouTube, lista estruturada de ingredientes com medidas e instruções passo a passo numeradas.
- **RF04 — Checklist de Ingredientes Persistente:** Permitir marcar/desmarcar ingredientes já separados na bancada, salvando o progresso localmente por receita.
- **RF05 — Favoritos (Livro de Receitas Offline):** Permitir favoritar e desfavoritar receitas, mantendo-as sempre acessíveis na aba dedicada "Favoritos" mesmo offline, com filtro próprio.
- **RF06 — Timer de Cozinha Integrado:** Disponibilizar controle rápido de contagem regressiva (5, 10, 15 min, acréscimo de +1 min, iniciar/pausar e reiniciar) diretamente na tela de detalhes da receita.
- **RF07 — Modo Offline-First e Indicador de Rede:** Monitorar o estado da conectividade em tempo real, exibindo banner informativo quando offline e botão de sincronização manual quando online.
- **RF08 — Estados de Interface:** Tratar explicitamente os estados de carregamento (Loading), sucesso (Success), ausência de resultados (Empty State) e falha recuperável (Error com retry).

## 4. Requisitos Não Funcionais (RNF)
- **RNF01 — Arquitetura:** Clean Architecture com camadas `domain`, `data` e `presentation` desacopladas e regra de dependência estrita (`presentation -> domain <- data`).
- **RNF02 — Injeção de Dependências:** Koin para gerenciamento modular de dependências entre camadas.
- **RNF03 — Assincronismo e Reatividade:** Kotlin Coroutines com `Dispatchers.IO` para rede/banco e `Flow`/`StateFlow` observados com `collectAsStateWithLifecycle()`.
- **RNF04 — UI e Responsividade:** Jetpack Compose com Material 3, suporte a Edge-to-Edge, layout adaptativo (`NavigationBar` em telas compactas e `NavigationRail` em telas médias/expandidas) e acessibilidade (`contentDescription`, alvos mínimos de 48dp).
- **RNF05 — Qualidade e Testabilidade:** Cobertura por testes unitários JVM (casos de uso, mappers, repositório e ViewModels) e testes Robolectric (Room + Compose UI).
- **RNF06 — Segurança e Prevenção de Memory Leaks:** Sem referências estáticas a `Context`/`Activity` em ViewModels ou casos de uso, uso de `viewModelScope`, cancelamento adequado de jobs e `SharingStarted.WhileSubscribed(5000)`.

## 5. Regras de Negócio (RN)

### RN01 — Preservação de Favoritos e Ingredientes Marcados na Sincronização
- Toda operação de gravação vinda da API (`syncInitialCatalog`, `searchAndCacheRecipes`, `syncCategoryRecipes`, `ensureRecipeDetailLoaded` e `getRandomRecipeId`) consulta os registros existentes no Room antes do *upsert*.
- Em `RecipeMappers.toEntity`, o campo `isFavorite` existente no banco local é sempre preservado (`existing?.isFavorite ?: false`), impedindo que atualizações remotas desmarquem receitas salvas pelo usuário.
- O estado `isChecked` de cada ingrediente já salvo no campo `ingredientsJson` é preservado durante a resincronização por correspondência case-insensitive tanto pelo nome localizado em PT-BR quanto pelo nome original em inglês.
- Quando um endpoint retorna apenas resumo da receita (como `filter.php?c=`, em que `strInstructions` é nulo ou vazio), o mapeamento **nunca** sobrescreve `instructions`, `ingredientsJson`, `area`, `tagsCsv`, `youtubeUrl`, `sourceUrl` nem `isDetailLoaded` de uma receita que já possua detalhes completos em cache.

### RN02 — Comportamento da Pesquisa Híbrida e Mínimo de Dois Caracteres
- A pesquisa na tela **Explorar** atua de forma híbrida sobre o Room (SSOT) e a API TheMealDB:
  1. **Filtragem local imediata:** qualquer caractere digitado atualiza imediatamente a query SQL no `RecipeDao.observeRecipes`, filtrando por correspondência parcial (`LIKE`) em `name`, `category`, `area` ou `ingredientsJson` (combinada à categoria selecionada).
  2. **Pesquisa remota condicionada:** no `HomeViewModel`, o fluxo de busca aplica *debounce* de **400 ms** e `distinctUntilChanged()`. A chamada remota `syncCatalogUseCase.searchRemote(query)` (`GET search.php?s={query}`) só é disparada quando `query.trim().length >= 2` e o dispositivo está online; consultas com 0 ou 1 caractere (após `trim()`) filtram apenas o cache local, evitando tráfego e chamadas desnecessárias à API.
  3. **Cache incremental:** resultados retornados pela busca remota são convertidos e persistidos no Room via *upsert*, passando automaticamente a integrar os fluxos reativos da UI.

### RN03 — Primeira Inicialização Sem Conexão (Contingência Offline)
- Ao abrir o aplicativo pela primeira vez sem conectividade (`!networkMonitor.isOnline.value`) ou diante de falha de rede com o banco local ainda vazio durante `syncInitialCatalog(forceRefresh = false)`:
  1. O método `seedIfEmpty()` popula o banco Room com **5 categorias** (`Massas`, `Frango`, `Frutos do Mar`, `Vegetariano`, `Sobremesas`) e **5 receitas clássicas completas** definidas em `InitialRecipeSeed`, todas em Português (PT-BR) e já marcadas com `isDetailLoaded = true`.
  2. O repositório retorna `SyncOutcome.OfflineFallback`, permitindo que o `HomeViewModel` conclua o carregamento inicial (`hasCompletedInitialLoad = true`) e exiba o catálogo inicial sem bloquear o usuário em tela de erro.
  3. Se o erro ocorrer com `forceRefresh = true` e ainda não houver receitas locais, é retornado `SyncOutcome.Failure(AppError)` para exibição da tela de erro com opção de nova tentativa.

### RN04 — Comportamento do Catálogo Offline e Alternativas Disponíveis Sem Internet
- Quando não houver conexão com a internet:
  - **Indicador visual contínuo:** o `OfflineStatusBanner` é exibido automaticamente nas abas **Explorar** e **Favoritos**, informando *"Modo Offline ativo — Exibindo receitas em cache local (Room)"*, além da mensagem no banner principal de que as receitas salvas continuam prontas.
  - **Navegação e filtros locais:** todas as receitas e categorias previamente armazenadas no Room continuam navegáveis, filtráveis por categoria e pesquisáveis localmente na aba **Explorar**; receitas salvas como favoritas continuam integralmente acessíveis e filtráveis na aba **Favoritos**.
  - **Leitura de detalhes e preparo:** receitas com `isDetailLoaded == true` (incluindo as 5 do seed e qualquer receita previamente detalhada via `search.php`, `lookup.php` ou `random.php`) exibem o badge *"Disponível Offline"* / *"Offline"*, lista completa de ingredientes, modo de preparo em etapas numeradas, checklist persistente e timer de cozinha funcional.
  - **Alternativa na Sugestão do Chef:** ao tocar em **Sugestão do Chef** sem internet (ou quando `random.php` falhar), `getRandomRecipeId()` executa automaticamente `RecipeDao.getRandomLocalRecipeId()` (`ORDER BY RANDOM() LIMIT 1`), direcionando o usuário para uma receita aleatória do catálogo local.
  - **Receitas apenas resumidas ou ausentes offline:** caso o usuário tente abrir uma receita que só teve resumo baixado (`filter.php`, `isDetailLoaded == false`) sem rede, `ensureRecipeDetailLoaded` retorna `SyncOutcome.OfflineFallback` e exibe o registro parcial disponível sem crash; se o `recipeId` não existir no Room e estiver offline, retorna `Failure(AppError.NetworkUnavailable)` e apresenta a tela de erro *"Receita ainda não está em cache e você está offline"* com botão *Tentar novamente*.
  - **Busca ou filtro sem correspondência offline:** o estado vazio orienta o usuário a alterar o termo, limpar filtros (botão *Limpar filtros*) ou sincronizar quando a conexão for restabelecida.
  - **Sincronização manual segura:** caso o usuário acione *Sincronizar* ainda sem rede, o app mantém o catálogo local intacto e exibe um `Snackbar`: *"Sem conexão. Exibindo receitas salvas no dispositivo."*

### RN05 — Política de Atualização Online e Localização PT-BR
- **Cache inicial online:** em `syncInitialCatalog(forceRefresh = false)`, se houver rede e o Room já possuir `>= 10` receitas e `> 0` categorias, o cache local é reutilizado sem nova requisição inicial. O acionamento manual (*Sincronizar*) usa `forceRefresh = true` e atualiza categorias (`categories.php`) e lote inicial (`search.php?s=`).
- **Localização PT-BR e chave de API:** `CulinaryPtBrLocalizer` traduz categorias, origens culinárias (`area`), tags, ingredientes frequentes e unidades de medida da TheMealDB para Português (PT-BR) antes da persistência local. Ao filtrar por uma categoria em português na UI (ex.: `"Massas"`), `OfflineFirstRecipeRepositoryImpl.syncCategoryRecipes` converte o nome para a chave esperada pela API (`"Pasta"`) via `toApiCategoryKey()`, mantendo o registro salvo localmente com a categoria traduzida.
- **Ordenação do catálogo:** na tela Explorar, `RecipeDao.observeRecipes` ordena os resultados priorizando favoritas (`ORDER BY isFavorite DESC, name ASC`). Na tela Favoritos, ordena alfabeticamente (`ORDER BY name ASC`).

### RN06 — Formatação de Etapas e Regras do Timer de Cozinha
- **Etapas de preparo (`Recipe.instructionSteps`):** quebra o texto por linha, descarta linhas em branco e remove prefixos numéricos ou marcadores como `1.`, `2)`, `STEP 1:`, `PASSO 1:` e `ETAPA 1:` para numeração limpa na UI.
- **Timer de cozinha (`KitchenTimerState`):** inicia por padrão configurado em `05:00` (`300` segundos); permite presets rápidos de `5`, `10` e `15` minutos (com mínimo garantido de `60` segundos), incremento `+1m` (+60 s), alternância entre *Iniciar* e *Pausar* com contagem via corrotina em `viewModelScope` (tick de 1.000 ms), botão de *Reiniciar* e estado de conclusão (`remainingSeconds == 0` exibe *"Tempo esgotado!"*). O timer é cancelado em `onCleared()` e não executa serviço em segundo plano.

## 6. Critérios de Aceitação (CA)

| ID | Requisito / Regra | Critério de Aceitação Verificável |
| --- | --- | --- |
| **CA01** | RF01, RN02 | Ao digitar na barra de busca da aba Explorar, o catálogo local é filtrado em tempo real; termos com menos de 2 caracteres (`trim().length < 2`) não disparam chamada remota à API; termos com `>= 2` caracteres disparam `search.php?s=` após 400 ms de debounce quando online. |
| **CA02** | RF02, RN04 | Ao tocar em "Sugestão do Chef" online, uma receita de `random.php` é salva no Room e aberta na tela de detalhes; quando offline ou em falha de rede, uma receita existente no Room é sorteada via `getRandomLocalRecipeId()` e aberta normalmente. |
| **CA03** | RF03, RN01, RN06 | Ao abrir uma receita que possuía apenas resumo (`isDetailLoaded == false`) estando online, o app consulta `lookup.php?i=`, persiste ingredientes e instruções no Room e exibe os passos numerados limpos, categoria, área, tags e link de vídeo (quando houver). |
| **CA04** | RF04, RN01 | Ao marcar ou desmarcar ingredientes na tela de detalhes, o estado `isChecked` é persistido no Room e refletido no contador (`X/Y separados`) e barra de progresso; após nova sincronização da mesma receita pela API, os itens marcados e o status de favorito permanecem preservados. |
| **CA05** | RF05, RN04 | Ao favoritar/desfavoritar uma receita na home ou nos detalhes, a aba "Favoritos" reflete a alteração imediatamente mesmo sem internet e permite filtrar favoritos localmente por nome, categoria ou origem; sem favoritos, exibe estado vazio com botão "Explorar receitas". |
| **CA06** | RF06, RN06 | Na tela de detalhes, o timer inicia em `05:00`, aceita presets `5m`/`10m`/`15m` e `+1m`, decrementa 1 segundo por vez enquanto ativo, pausa/reinicia corretamente e exibe "Tempo esgotado!" ao atingir `00:00`. |
| **CA07** | RF07, RN03, RN04 | Na primeira abertura totalmente offline, o app popula as 5 categorias e 5 receitas do `InitialRecipeSeed`, exibe o banner "Modo Offline ativo" e permite explorar, favoritar e abrir qualquer receita do seed sem crashes. |
| **CA08** | RF08, RN04 | Estados de `Loading` (indicador de progresso), `Success` (grade/detalhes), `Empty` (ilustração + ação de limpar filtros/explorar) e `Error` (mensagem legível + botão "Tentar novamente") são exibidos de forma explícita conforme cada cenário. |
| **CA09** | RN05 | Categorias, origens, tags, ingredientes frequentes e unidades de medida da API são traduzidos para PT-BR na exibição e persistência; selecionar uma categoria em PT-BR (ex.: `"Massas"`) consulta a chave correspondente em inglês (`"Pasta"`) na API `filter.php`. |
