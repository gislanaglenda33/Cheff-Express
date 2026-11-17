# Decomposição de Tarefas (SDD) — Chef Express

## Etapa 1: Especificação e Planejamento
- [x] Definir proposta, requisitos funcionais e não funcionais (`SPECIFICATION.md`).
- [x] Planejar arquitetura Clean Architecture, escolha da API TheMealDB e estratégia Offline-First (`ARCHITECTURE_PLAN.md`).
- [x] Definir decomposição incremental e rastreável (`TASKS.md`).

## Etapa 2: Configuração Base, Identidade Visual e Dependências
- [x] Atualizar nome do app (`Chef Express`), `applicationId` único e `strings.xml`.
- [x] Instalar tipografia editorial local (`Playfair Display` e `Plus Jakarta Sans`) via `font-util`.
- [x] Gerar ícone adaptativo personalizado e ilustrações do app (hero banner e empty state).
- [x] Configurar dependências no Gradle (`Koin`, `Navigation Compose`, `Kotlinx Serialization`, `Coil`, `Room`, `Retrofit`, `Moshi`).
- [x] Criar tema Material 3 ("Bistrô Moderno") e classe `ChefExpressApp` com inicialização do Koin.

## Etapa 3: Camada Domain (Regras de Negócio Puras)
- [x] Modelar entidades de domínio (`Recipe`, `Category`, `IngredientItem`, `AppError`, `SyncOutcome`).
- [x] Definir contratos de repositório (`RecipeRepository`) e monitoramento de rede (`NetworkMonitor`).
- [x] Implementar Casos de Uso (`ObserveRecipesUseCase`, `ObserveCategoriesUseCase`, `ObserveFavoriteRecipesUseCase`, `ObserveRecipeDetailUseCase`, `SyncCatalogUseCase`, `ToggleFavoriteUseCase`, `ToggleIngredientCheckUseCase`, `GetRandomChefRecipeUseCase`).

## Etapa 4: Camada Data (Room, Retrofit e Offline-First)
- [x] Configurar banco Room (`ChefExpressDatabase`, `RecipeEntity`, `CategoryEntity`, `RecipeDao`, `CategoryDao`).
- [x] Configurar cliente Retrofit (`TheMealDbApi`, DTOs com Moshi, logging interceptor e tratamento de exceções HTTP/IO).
- [x] Implementar `RecipeMappers` entre DTO, Entity e Domain Model.
- [x] Implementar `AndroidNetworkMonitor` e `OfflineFirstRecipeRepositoryImpl` com preservação de favoritos/checklist e seed de contingência offline.

## Etapa 5: Camada Presentation (Jetpack Compose & Navigation)
- [x] Implementar grafo de navegação tipado (`ChefExpressNavGraph`) e layout adaptativo (`NavigationBar` / `NavigationRail`).
- [x] Implementar `HomeViewModel` e `HomeScreen` com estados Loading, Success, Empty e Error, busca, categorias, banner offline e Sugestão do Chef.
- [x] Implementar `FavoritesViewModel` e `FavoritesScreen` com lista de receitas salvas offline e empty state ilustrado.
- [x] Implementar `RecipeDetailViewModel` e `RecipeDetailScreen` com checklist persistente de ingredientes, modo de preparo e timer de cozinha.

## Etapa 6: Validação, Testes e Documentação Final
- [x] Escrever testes unitários para UseCases, Mappers, Repository e ViewModels.
- [x] Escrever testes Robolectric para Room DAO e fluxo principal de UI Compose.
- [x] Executar `gradle :app:testDebugUnitTest` e `gradle :app:assembleDebug` para validação final.
- [x] Registrar evidências reais em `TEST_RESULTS.md` e completar `README.md`.
