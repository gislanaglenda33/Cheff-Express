# Planejamento Técnico e Arquitetural (SDD) — Chef Express

> Este documento consolida o planejamento técnico completo do **Chef Express**, utilizando [`ARCHITECTURE_PLAN.md`](ARCHITECTURE_PLAN.md) como base arquitetural original e refletindo fielmente a estrutura real implementada no projeto, incluindo localização PT-BR, estratégia de testes e critérios de validação.

---

## 1. Arquitetura Planejada

O projeto adota **Clean Architecture** combinada ao padrão **MVVM (Model-View-ViewModel)** com **fluxo de dados unidirecional (UDF)** dentro do módulo `:app`.

### 1.1 Regra de Dependência
- **`domain` (núcleo puro Kotlin):** não possui dependências de Android Framework, Compose, Room, Retrofit ou Koin. Define modelos de domínio, interfaces de contrato (`RecipeRepository`, `NetworkMonitor`) e casos de uso (`UseCases`).
- **`data` (infraestrutura e repositórios):** depende exclusivamente de `domain` e implementa seus contratos integrando Room, Retrofit/Moshi, `ConnectivityManager` e `CulinaryPtBrLocalizer`.
- **`presentation` (interface e estado):** depende exclusivamente de `domain`. Os `ViewModel`s orquestram casos de uso e `NetworkMonitor`, expondo `StateFlow` imutáveis (`@Immutable`) e eventos pontuais (`SharedFlow`) para os Composables.
- **`di` e `ui/theme`:** conectam as abstrações de `domain` às implementações concretas de `data` e fornecem o sistema visual Material Design 3 à camada de apresentação.

---

## 2. Estrutura Real de Diretórios do Projeto

A organização abaixo corresponde exatamente aos arquivos existentes no repositório:

```text
/
├── app/
│   ├── build.gradle.kts                      # Configuração do módulo :app (applicationId, Room/Moshi KSP, Koin, Compose)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml           # Permissões INTERNET/ACCESS_NETWORK_STATE e ChefExpressApp
│       │   ├── java/com/example/
│       │   │   ├── ChefExpressApp.kt         # Application com inicialização protegida do Koin
│       │   │   ├── MainActivity.kt           # Entry point Edge-to-Edge e cálculo de WindowSizeClass
│       │   │   ├── di/
│       │   │   │   └── AppModules.kt         # Módulos Koin: dataModule, domainModule, presentationModule
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   │   └── Recipe.kt         # IngredientItem, Recipe, Category, AppError, SyncOutcome
│       │   │   │   ├── repository/
│       │   │   │   │   └── RecipeRepository.kt # Contratos RecipeRepository e NetworkMonitor
│       │   │   │   └── usecase/
│       │   │   │       └── RecipeUseCases.kt # 8 casos de uso de negócio
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── ChefExpressDatabase.kt # Banco Room (versão 2)
│       │   │   │   │   ├── RecipeEntities.kt      # RecipeEntity, CategoryEntity, IngredientLocalModel
│       │   │   │   │   ├── RecipeDao.kt           # RecipeDao e CategoryDao
│       │   │   │   │   └── InitialRecipeSeed.kt   # Seed de contingência offline em PT-BR
│       │   │   │   ├── remote/
│       │   │   │   │   ├── TheMealDbApi.kt        # Interface Retrofit e BASE_URL
│       │   │   │   │   └── TheMealDbDto.kt        # DTOs Moshi + extração de 20 ingredientes
│       │   │   │   ├── mapper/
│       │   │   │   │   ├── CulinaryPtBrLocalizer.kt # Dicionário e tradução bidirecional PT-BR <-> EN
│       │   │   │   │   └── RecipeMappers.kt       # Conversões DTO <-> Entity <-> Domain e merge seguro
│       │   │   │   ├── network/
│       │   │   │   │   └── AndroidNetworkMonitor.kt # StateFlow de conectividade via NetworkCallback
│       │   │   │   └── repository/
│       │   │   │       └── OfflineFirstRecipeRepositoryImpl.kt # Implementação SSOT Offline-First
│       │   │   ├── presentation/
│       │   │   │   ├── components/
│       │   │   │   │   └── ChefComponents.kt      # OfflineStatusBanner, RecipeEditorialCard, estados Loading/Empty/Error
│       │   │   │   ├── home/
│       │   │   │   │   ├── HomeViewModel.kt       # HomeUiState, HomeEvent e lógica de descoberta/busca
│       │   │   │   │   └── HomeScreen.kt          # Tela Explorar com hero banner, busca e grid adaptativo
│       │   │   │   ├── favorites/
│       │   │   │   │   ├── FavoritesViewModel.kt  # FavoritesUiState e filtro local de favoritos
│       │   │   │   │   └── FavoritesScreen.kt     # Tela Livro de Receitas
│       │   │   │   ├── detail/
│       │   │   │   │   ├── RecipeDetailViewModel.kt # RecipeDetailUiState, KitchenTimerState e checklist
│       │   │   │   │   └── RecipeDetailScreen.kt  # Tela de detalhes, checklist de ingredientes, etapas e timer
│       │   │   │   └── navigation/
│       │   │   │       └── ChefExpressNavGraph.kt # Rotas @Serializable e shell adaptativo (NavigationBar/NavigationRail)
│       │   │   └── ui/theme/
│       │   │       ├── Color.kt                   # Paleta "Bistrô Moderno" (claro/escuro)
│       │   │       ├── Type.kt                    # Tipografia Playfair Display + Plus Jakarta Sans
│       │   │       └── Theme.kt                   # ChefExpressTheme e ChefExpressShapes
│       │   └── res/
│       │       ├── drawable/                      # Ícone foreground e ilustrações estáticas geradas
│       │       ├── font/                          # Fontes locais (.ttf)
│       │       ├── mipmap-*/                      # Ícones adaptativos do launcher
│       │       └── values/                        # strings.xml e themes.xml
│       └── test/java/com/example/
│           ├── ExampleUnitTest.kt                 # Teste baseline JVM
│           ├── ExampleRobolectricTest.kt          # Validação de recursos Android (app_name) via Robolectric
│           ├── domain/usecase/
│           │   └── RecipeUseCasesTest.kt          # Testes unitários dos casos de uso e instructionSteps
│           ├── data/
│           │   ├── local/
│           │   │   └── ChefExpressDatabaseTest.kt # Testes Room em memória (Robolectric)
│           │   ├── mapper/
│           │   │   └── RecipeMappersTest.kt       # Testes de merge não-destrutivo e CulinaryPtBrLocalizer
│           │   └── repository/
│           │       └── OfflineFirstRecipeRepositoryTest.kt # Testes de fluxo Offline-First e categorias
│           └── presentation/
│               ├── HomeViewModelTest.kt           # Testes de estado e eventos do HomeViewModel
│               ├── RecipeDetailViewModelTest.kt   # Testes de checklist, favorito e timer
│               └── ChefExpressUiTest.kt           # Testes de UI Compose com Robolectric
├── evidencias/                                    # Diretório destinado às capturas reais do aplicativo
├── gradle/libs.versions.toml                      # Catálogo central de versões e dependências
├── SPECIFICATION.md                               # Especificação funcional, não funcional, RN e CA
├── ARCHITECTURE_PLAN.md                           # Documento arquitetural inicial preservado
├── PLANNING.md                                    # Planejamento técnico detalhado (este documento)
├── TASKS.md                                       # Decomposição de tarefas e tabela de progresso
├── TEST_RESULTS.md                                # Registro de evidências e execuções de testes
└── README.md                                      # Documentação geral de uso, arquitetura e execução
```

---

## 3. Tecnologias e Bibliotecas

Configuração definida em `gradle/libs.versions.toml` e `app/build.gradle.kts` (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`, `JavaVersion.VERSION_11`):

| Categoria | Tecnologia / Biblioteca | Versão | Uso no Projeto |
| --- | --- | --- | --- |
| **Linguagem & Build** | Kotlin / AGP / KSP | `2.2.10` / `9.1.1` / `2.3.5` | Código-fonte Kotlin, build Gradle DSL e geração de código (Room/Moshi) |
| **UI & Navegação** | Jetpack Compose BOM / Material 3 / WindowSizeClass | `2024.09.00` | UI declarativa M3, layout adaptativo e Edge-to-Edge |
| **Navegação Tipada** | Navigation Compose + Kotlinx Serialization | `2.8.9` / `1.8.1` | Rotas `@Serializable` (`DiscoverRoute`, `FavoritesRoute`, `RecipeDetailRoute`) |
| **Injeção de Dependências** | Koin (`koin-android`, `koin-androidx-compose`) | `4.0.2` | Módulos desacoplados e injeção em ViewModels |
| **Assincronismo** | Kotlin Coroutines (`core`, `android`) | `1.10.2` | Execução assíncrona em `Dispatchers.IO`, `Flow` e `StateFlow` |
| **Persistência Local** | Room (`room-runtime`, `room-ktx`, `room-compiler`) | `2.7.0` | Banco SQLite reativo como SSOT |
| **Rede & Serialização** | Retrofit / OkHttp / Moshi (`moshi-kotlin`, `codegen`) | `2.12.0` / `4.10.0` / `1.15.2` | Consumo REST da TheMealDB e adapters JSON gerados via KSP |
| **Imagens** | Coil (`coil-compose`) | `2.7.0` | Carregamento assíncrono com cache em memória e disco |
| **Testes** | JUnit4 / Coroutines Test / Robolectric / Compose UI Test | `4.13.2` / `1.10.2` / `4.16.1` | Testes unitários JVM, testes de DAO e testes de componentes Compose |

---

## 4. Organização das Camadas

1. **Camada `domain`:**
   - **Modelos (`Recipe.kt`):** `Recipe` (com propriedade computada `instructionSteps` que higieniza marcadores de passo), `IngredientItem`, `Category`, `AppError` (`NetworkUnavailable`, `Timeout`, `ServerError`, `NotFound`, `Unknown`) e `SyncOutcome` (`Success`, `OfflineFallback`, `Failure`).
   - **Contratos (`RecipeRepository.kt`):** define operações reativas de observação (`Flow`) e operações suspensas de sincronização e mutação local, além do contrato `NetworkMonitor`.
   - **Casos de Uso (`RecipeUseCases.kt`):** encapsula ações pontuais (`ObserveRecipesUseCase`, `ObserveCategoriesUseCase`, `ObserveFavoriteRecipesUseCase`, `ObserveRecipeDetailUseCase`, `SyncCatalogUseCase`, `ToggleFavoriteUseCase`, `ToggleIngredientCheckUseCase`, `GetRandomChefRecipeUseCase`), normalizando entradas (como `trim()` e índice `>= 0`).

2. **Camada `data`:**
   - **`local/`:** schema Room (`ChefExpressDatabase`, `RecipeEntity`, `CategoryEntity`), consultas SQL reativas (`RecipeDao`, `CategoryDao`) e carga de contingência (`InitialRecipeSeed`).
   - **`remote/`:** interface `TheMealDbApi` e DTOs `CategoriesResponseDto`, `CategoryDto`, `MealsResponseDto`, `MealDto`.
   - **`mapper/`:** `CulinaryPtBrLocalizer` (tradução de categorias, áreas, tags, medidas e ingredientes frequentes para PT-BR + mapeamento reverso de categoria para consulta na API) e `RecipeMappers` (conversão segura e preservação de estado local).
   - **`network/`:** `AndroidNetworkMonitor` observando `ConnectivityManager.NetworkCallback`.
   - **`repository/`:** `OfflineFirstRecipeRepositoryImpl` coordenando Room, Retrofit, seed e mapeamento de exceções.

3. **Camada `presentation`:**
   - **Estados Imutáveis:** `HomeUiState`, `FavoritesUiState`, `RecipeDetailUiState` (com `Loading`, `Success`, `Empty` quando aplicável, e `Error`), além de `KitchenTimerState` e `HomeEvent`.
   - **Telas e Shell:** `ChefExpressNavGraph` alterna entre `NavigationBar` (largura `Compact`) e `NavigationRail` (largura `Medium`/`Expanded`), hospeda `SnackbarHost` e conecta as telas `HomeScreen`, `FavoritesScreen` e `RecipeDetailScreen`.
   - **Componentes Compartilhados:** `ChefComponents.kt` centraliza `OfflineStatusBanner`, `RecipeEditorialCard`, `LoadingChefState`, `EmptyChefState` e `ErrorChefState`.

---

## 5. Estratégia de Injeção de Dependências

A DI é estruturada com **Koin 4.0.2** no arquivo `di/AppModules.kt`, agrupando três módulos em `appModules`:
- **`dataModule`:** registra como `single` o `Moshi` (com `KotlinJsonAdapterFactory`), `OkHttpClient` (com `HttpLoggingInterceptor.Level.BASIC` e timeouts), `TheMealDbApi` via `Retrofit`, `ChefExpressDatabase` via `Room.databaseBuilder`, DAOs (`recipeDao()`, `categoryDao()`), `NetworkMonitor` (`AndroidNetworkMonitor`) e `RecipeRepository` (`OfflineFirstRecipeRepositoryImpl`).
- **`domainModule`:** registra como `factory` todos os 8 casos de uso, mantendo-os leves e sem estado próprio.
- **`presentationModule`:** registra os `viewModel`s (`HomeViewModel`, `FavoritesViewModel` e `RecipeDetailViewModel` com parâmetro dinâmico `{ (recipeId: String) -> ... }` passado via `parametersOf(route.recipeId)` na navegação).
- **Inicialização segura para testes:** `ChefExpressApp.onCreate()` verifica `if (GlobalContext.getOrNull() == null)` antes de executar `startKoin`, prevenindo `KoinApplicationAlreadyStartedException` ao rodar múltiplas classes de teste com Robolectric.

---

## 6. Estratégia de Persistência com Room

- **Banco:** `ChefExpressDatabase` (`DATABASE_NAME = "chef_express.db"`), atualmente na **versão 2** (`exportSchema = false`), configurada com `.fallbackToDestructiveMigration(dropAllTables = true)` para atualização limpa do catálogo quando o seed inicial foi migrado integralmente para PT-BR.
- **Modelagem:**
  - `RecipeEntity` (`@Entity(tableName = "recipes")`): armazena dados completos da receita, estado `isFavorite`, flag `isDetailLoaded`, `lastSyncedAt`, `tagsCsv` e `ingredientsJson` (lista de `IngredientLocalModel` serializada com `kotlinx.serialization.json.Json`).
  - `CategoryEntity` (`@Entity(tableName = "categories")`): armazena categorias localizadas em PT-BR.
- **Consultas Reativas e Updates Parciais (`RecipeDao` / `CategoryDao`):**
  - `observeRecipes(query, category)`: retorna `Flow<List<RecipeEntity>>` filtrando por categoria exata e busca `LIKE` em `name`, `category`, `area` ou `ingredientsJson`, ordenando por `isFavorite DESC, name ASC`.
  - `observeFavoriteRecipes()`: retorna `Flow<List<RecipeEntity>>` filtrando `isFavorite = 1`.
  - `updateFavorite` e `updateIngredients`: atualizações cirúrgicas por ID que evitam reescrever a linha inteira ao favoritar ou marcar ingredientes.
  - `getRandomLocalRecipeId()`: consulta aleatória local (`ORDER BY RANDOM() LIMIT 1`) para contingência offline da Sugestão do Chef.

---

## 7. Estratégia de Integração com TheMealDB

- **Base URL:** `https://www.themealdb.com/api/json/v1/1/` (API REST pública e gratuita, sem exigência de chave proprietária em `.env`).
- **Cliente HTTP:** `OkHttpClient` com `connectTimeout(15, TimeUnit.SECONDS)`, `readTimeout(20, TimeUnit.SECONDS)` e `HttpLoggingInterceptor.Level.BASIC`; conversor `MoshiConverterFactory` com adapters gerados em compilação via `@JsonClass(generateAdapter = true)` e KSP.
- **Endpoints e Contratos:**
  - `GET categories.php` -> `CategoriesResponseDto`: popula categorias culinárias.
  - `GET search.php?s={query}` -> `MealsResponseDto`: carga inicial (`s=""`) e busca textual remota (`query.trim().length >= 2`).
  - `GET filter.php?c={category}` -> `MealsResponseDto`: retorna resumos por categoria (recebe chave em inglês convertida por `CulinaryPtBrLocalizer.toApiCategoryKey`).
  - `GET lookup.php?i={id}` -> `MealsResponseDto`: hidrata detalhes completos de receitas resumidas.
  - `GET random.php` -> `MealsResponseDto`: obtém sugestão aleatória do chef.
- **Normalização e Tratamento de Exceções:** `MealDto.extractIngredientPairs()` consolida os 20 pares opcionais (`strIngredient1..20` / `strMeasure1..20`) descartando entradas vazias. No repositório, `CancellationException` é sempre relançada para respeitar cancelamento estruturado de corrotinas; `UnknownHostException` e `IOException` mapeiam para `AppError.NetworkUnavailable`, `SocketTimeoutException` para `AppError.Timeout` e `HttpException` para `AppError.ServerError`.

---

## 8. Estratégia Offline-First

O aplicativo adota o **Room como Single Source of Truth (SSOT)**:
1. **Fluxo Reativo Contínuo:** a UI nunca consome DTOs de rede diretamente; toda renderização observa `Flow` do Room via `StateFlow` com `SharingStarted.WhileSubscribed(5_000)` e `collectAsStateWithLifecycle()`.
2. **Monitoramento de Rede em Tempo Real:** `AndroidNetworkMonitor` verifica `NET_CAPABILITY_INTERNET` no estado inicial e mantém `StateFlow<Boolean>` atualizado via `ConnectivityManager.registerDefaultNetworkCallback`.
3. **Contingência de Primeira Execução Offline:** se o banco estiver vazio e não houver rede (ou falhar a requisição inicial), `InitialRecipeSeed` insere 5 categorias e 5 receitas completas em PT-BR para garantir usabilidade imediata.
4. **Resiliência nas Ações Offline:** favoritos, checklist de ingredientes, timer de cozinha, filtragem local e *Sugestão do Chef* (via `getRandomLocalRecipeId()`) funcionam integralmente sem internet. Imagens já carregadas permanecem disponíveis pelo cache em disco/memória do Coil.

---

## 9. Estratégia de Sincronização

1. **Sincronização Inicial (`syncInitialCatalog`):**
   - Offline -> executa `seedIfEmpty()` e retorna `SyncOutcome.OfflineFallback`.
   - Online com `forceRefresh == false`, `recipeCount >= 10` e `categoryCount > 0` -> reaproveita cache local (`SyncOutcome.Success`).
   - Online (banco insuficiente ou `forceRefresh == true`) -> busca `categories.php` e `search.php?s=`, mescla com registros existentes no Room e realiza *upsert* em `Dispatchers.IO`.
2. **Sincronização por Busca (`searchAndCacheRecipes`):** acionada pelo `HomeViewModel` após `debounce(400)` quando `query.trim().length >= 2` e online; resultados são persistidos no Room, atualizando automaticamente a lista observada.
3. **Sincronização por Categoria (`syncCategoryRecipes`):** ao selecionar um chip de categoria em PT-BR, a UI filtra imediatamente os itens já em cache enquanto uma corrotina busca `filter.php?c={apiCategory}` e faz *upsert* não-destrutivo.
4. **Hidratação Sob Demanda (`ensureRecipeDetailLoaded`):** ao abrir `RecipeDetailScreen`, se `isDetailLoaded == false` ou `instructions` estiver vazio, consulta `lookup.php?i={id}` quando online; se offline e existir registro parcial local, retorna `OfflineFallback` preservando a experiência sem erro fatal.
5. **Merge Não-Destrutivo (`RecipeMappers.toEntity`):** preserva `isFavorite`, estado `isChecked` dos ingredientes e detalhes completos anteriores caso uma nova resposta contenha apenas campos resumidos.

---

## 10. Estratégia de Testes

A validação automatizada foi planejada em duas frentes complementares dentro de `app/src/test/`:
1. **Testes Unitários JVM Puros (execução rápida sem emulador Android):**
   - `RecipeUseCasesTest`: valida normalização de busca/categoria, guarda de índice no checklist, delegação de favoritos/aleatório e limpeza de prefixos em `Recipe.instructionSteps`.
   - `RecipeMappersTest`: valida preservação de favoritos/checklist no merge, proteção contra sobrescrita por resumo e traduções do `CulinaryPtBrLocalizer`.
   - `OfflineFirstRecipeRepositoryTest`: utiliza DAOs em memória (`InMemoryRecipeDao`, `InMemoryCategoryDao`), `FakeMealApi` e `FakeNetworkMonitor` com `StandardTestDispatcher` para testar seed offline, preservação pós-ressincronização, tradução de categoria PT-BR -> EN e fallback aleatório local.
   - `HomeViewModelTest` e `RecipeDetailViewModelTest`: validam transições de `StateFlow`, eventos `SharedFlow`, debounce/filtros e contagem/reset do timer de cozinha.
   - `ExampleUnitTest`: verificação baseline de execução JVM.
2. **Testes Android Locais com Robolectric (`@Config(sdk = [36])`):**
   - `ChefExpressDatabaseTest`: valida consultas SQL reais do `RecipeDao` e `CategoryDao` em banco Room `inMemoryDatabaseBuilder`.
   - `ChefExpressUiTest`: valida com `createComposeRule()` a exibição do banner offline, cards, navegação por clique, checklist de ingredientes e timer na UI Compose.
   - `ExampleRobolectricTest`: valida leitura de recursos Android (`R.string.app_name`).
3. **Lacunas mapeadas para evolução futura:** inclusão de testes dedicados para `FavoritesViewModel`, `AndroidNetworkMonitor`, testes de contrato HTTP com `MockWebServer` e execução em dispositivo/emulador real.

---

## 11. Sequência Planejada de Implementação

1. **Etapa 1 — Especificação e Planejamento SDD:** definição de escopo, requisitos, regras de negócio, arquitetura e decomposição em tarefas (`SPECIFICATION.md`, `ARCHITECTURE_PLAN.md`, `PLANNING.md`, `TASKS.md`).
2. **Etapa 2 — Base do Projeto, Identidade Visual e DI:** configuração Gradle (`libs.versions.toml`, `app/build.gradle.kts`), ícone adaptativo, fontes locais, tema *"Bistrô Moderno"*, ilustrações estáticas e módulos Koin (`AppModules.kt`, `ChefExpressApp.kt`).
3. **Etapa 3 — Camada Domain:** modelos puros, contratos `RecipeRepository`/`NetworkMonitor` e os 8 `UseCases`.
4. **Etapa 4 — Camada Data:** entidades/DAOs Room, cliente Retrofit/Moshi para TheMealDB, mappers, `AndroidNetworkMonitor`, `InitialRecipeSeed` e `OfflineFirstRecipeRepositoryImpl`.
5. **Etapa 5 — Camada Presentation:** shell adaptativo `ChefExpressNavGraph`, componentes reutilizáveis e telas/ViewModels (`Home`, `Favorites`, `RecipeDetail` com checklist e timer).
6. **Etapa 6 — Localização PT-BR e Migração do Banco:** implementação de `CulinaryPtBrLocalizer.kt`, tradução de `InitialRecipeSeed.kt`, atualização de `ChefExpressDatabase` para `version = 2` e adaptação dos testes.
7. **Etapa 7 — Testes, Validação e Consolidação Documental:** compilação do APK de depuração, execução das suítes JVM e Robolectric, registro honesto em `TEST_RESULTS.md`, organização de `evidencias/` e documentação final para entrega.

---

## 12. Critérios Gerais de Validação

Para considerar o projeto validado em nível acadêmico e técnico, verificam-se os seguintes critérios:
- **Compilação limpa:** `gradle :app:assembleDebug` gera o APK de depuração sem erros de Kotlin, KSP ou recursos.
- **Integridade arquitetural:** nenhuma classe de `domain/` importa tipos de Android, Room, Retrofit ou `data/`; toda UI observa exclusivamente `StateFlow` dos `ViewModel`s.
- **Verificação automatizada transparente:** separação explícita em `TEST_RESULTS.md` entre testes JVM efetivamente aprovados na versão atual, testes Robolectric já validados em execução anterior vs bloqueados por rede/infraestrutura na reexecução, e eventuais suítes ainda não implementadas.
- **Coerência documental:** alinhamento exato entre `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `README.md`, `TEST_RESULTS.md` e o código-fonte em `:app`.
- **Entrega final externa:** captura de telas reais em emulador/dispositivo para a pasta `evidencias/` e publicação do histórico completo no repositório remoto GitHub.
