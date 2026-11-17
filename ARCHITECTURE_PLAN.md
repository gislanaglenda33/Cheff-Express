# Planejamento de Arquitetura — Chef Express

## 1. Estilo Arquitetural: Clean Architecture + MVVM
O projeto adota **Clean Architecture** organizada em três camadas principais dentro do módulo `:app`, garantindo separação clara de responsabilidades e inversão de dependência:

```
com.example
├── domain/                  # Camada de Domínio (Kotlin puro, sem dependências Android)
│   ├── model/               # Entidades de negócio (Recipe, Category, IngredientItem, SyncResult)
│   ├── repository/          # Interfaces (RecipeRepository, NetworkMonitor)
│   └── usecase/             # Regras de negócio encapsuladas (GetRecipesUseCase, etc.)
├── data/                    # Camada de Dados (implementações concretas)
│   ├── local/               # Room Database, Entities, DAOs, Converters, Seed inicial
│   ├── remote/              # Retrofit API, DTOs Moshi, OkHttp
│   ├── mapper/              # Conversores DTO <-> Entity <-> Domain Model
│   ├── network/             # AndroidNetworkMonitor (ConnectivityManager + CallbackFlow)
│   └── repository/          # OfflineFirstRecipeRepositoryImpl
├── presentation/            # Camada de Apresentação (Jetpack Compose + MVVM)
│   ├── navigation/          # Rotas @Serializable e ChefExpressNavGraph
│   ├── home/                # HomeScreen, HomeViewModel, HomeUiState
│   ├── favorites/           # FavoritesScreen, FavoritesViewModel, FavoritesUiState
│   ├── detail/              # RecipeDetailScreen, RecipeDetailViewModel, TimerState
│   └── components/          # Componentes reutilizáveis (RecipeCard, OfflineBanner, StateViews)
├── di/                      # Módulos Koin (DataModule, DomainModule, PresentationModule)
└── ui/theme/                # Design System Material 3 ("Bistrô Moderno"), tipografia e cores
```

### Regra de Dependência
- `domain` não importa classes de `data`, `presentation`, Retrofit ou Room.
- `data` depende de `domain` e implementa suas interfaces.
- `presentation` depende apenas de `domain` (ViewModels consomem UseCases e `NetworkMonitor`).
- `di` conecta as interfaces de `domain` às implementações de `data` e injeta nos ViewModels.

## 2. API REST Escolhida: TheMealDB
- **Base URL:** `https://www.themealdb.com/api/json/v1/1/`
- **Justificativa:** API pública gratuita, estável, sem necessidade de chave de API embutida, com cobertura abrangente de receitas reais, fotos de alta qualidade, categorias, países de origem, listas de até 20 ingredientes com medidas e instruções completas.
- **Endpoints Utilizados:**
  - `GET categories.php` — Lista todas as categorias culinárias.
  - `GET search.php?s={query}` — Busca receitas por nome (com `s=` retorna lote inicial variado).
  - `GET filter.php?c={category}` — Filtra resumos de receitas por categoria.
  - `GET lookup.php?i={id}` — Busca os detalhes completos de uma receita.
  - `GET random.php` — Obtém uma receita aleatória ("Sugestão do Chef").

## 3. Estratégia Offline-First e Sincronização
O aplicativo utiliza o **Room como Single Source of Truth (SSOT)**:
1. **Leitura Reativa:** Toda exibição na UI observa fluxos contínuos (`Flow`) diretamente do Room (`RecipeDao` e `CategoryDao`).
2. **Sincronização Não-Destrutiva:** Ao receber dados da API via Retrofit, o repositório realiza *upsert* preservando metadados locais do usuário (`isFavorite`, `checkedIngredients`, `savedAt`, e mantendo detalhes já baixados caso o endpoint de filtro retorne apenas resumos).
3. **Resiliência na Primeira Execução:** Caso o usuário abra o aplicativo pela primeira vez totalmente offline e o banco esteja vazio, um conjunto enxuto de receitas clássicas (`InitialRecipeSeed`) é populado no Room para evitar tela bloqueada. Assim que houver conexão, a sincronização enriquece e atualiza o catálogo automaticamente.
4. **Monitoramento de Conectividade:** `AndroidNetworkMonitor` expõe `StateFlow<Boolean>` via `ConnectivityManager.NetworkCallback`, permitindo feedback imediato na UI e acionamento de sincronização segura.

## 4. Tecnologias e Bibliotecas
- **Linguagem:** Kotlin 2.2
- **UI:** Jetpack Compose + Material Design 3 + Navigation Compose (rotas tipadas com `kotlinx.serialization`)
- **Injeção de Dependências:** Koin (`koin-android`, `koin-androidx-compose`)
- **Persistência Local:** Room + KSP
- **Rede & Serialização:** Retrofit + OkHttp + Moshi (`moshi-kotlin-codegen`)
- **Imagens:** Coil (`coil-compose`) com cache em disco e memória para visualização offline de fotos já carregadas
- **Assincronismo:** Kotlin Coroutines + Flow / StateFlow
- **Testes:** JUnit4, Kotlinx Coroutines Test, Robolectric + Compose UI Test
