# Decomposição de Tarefas (SDD) — Chef Express

Este documento acompanha a decomposição incremental das etapas de desenvolvimento do **Chef Express**, seus critérios objetivos de conclusão, validações pendentes e estado real de progresso.

---

## Etapa 1: Especificação e Planejamento (SDD)
**Critério de conclusão:** Documentos `SPECIFICATION.md`, `ARCHITECTURE_PLAN.md`, `PLANNING.md` e `TASKS.md` completos, coerentes com o código-fonte real, contendo requisitos funcionais/não funcionais, regras de negócio, critérios de aceitação, estratégias arquiteturais e plano incremental.

- [x] Definir proposta, requisitos funcionais (RF01–RF08) e não funcionais (RNF01–RNF06) em `SPECIFICATION.md`.
- [x] Documentar regras de negócio explícitas (RN01–RN07) e critérios de aceitação verificáveis (CA01–CA09) em `SPECIFICATION.md`.
- [x] Planejar arquitetura Clean Architecture, escolha da API TheMealDB e estratégia Offline-First em `ARCHITECTURE_PLAN.md` (preservado).
- [x] Consolidar planejamento técnico detalhado (DI, Room, Retrofit, sincronização, testes e sequência real) em `PLANNING.md`.
- [x] Definir decomposição incremental, critérios de conclusão e tabela geral de progresso em `TASKS.md`.

---

## Etapa 2: Configuração Base, Identidade Visual e Dependências
**Critério de conclusão:** Módulo `:app` configurado no Gradle com `applicationId` único (`com.aistudio.chefexpress.krmqvx`), recursos visuais customizados instalados e tema Material 3 + Koin inicializando sem erros em `ChefExpressApp`.

- [x] Atualizar nome do app (`Chef Express`), `applicationId` único e `strings.xml`.
- [x] Instalar tipografia editorial local (`playfair_display.ttf` e `plus_jakarta_sans.ttf`).
- [x] Configurar ícone adaptativo personalizado (`ic_chef_foreground.jpg` e `mipmap-*`) e ilustrações estáticas (`img_hero_culinary.jpg`, `img_empty_kitchen.jpg`).
- [x] Configurar dependências no Gradle (`Koin 4.0.2`, `Navigation Compose 2.8.9`, `Kotlinx Serialization 1.8.1`, `Coil 2.7.0`, `Room 2.7.0`, `Retrofit 2.12.0`, `Moshi 1.15.2`, `Robolectric 4.16.1`).
- [x] Criar tema Material 3 (*"Bistrô Moderno"* em `Color.kt`, `Type.kt`, `Theme.kt`) e classe `ChefExpressApp` com guarda `GlobalContext.getOrNull() == null`.

---

## Etapa 3: Camada Domain (Regras de Negócio Puras)
**Critério de conclusão:** Pacote `domain` compilando em Kotlin puro sem importações de Android/Room/Retrofit/Data, com modelos, contratos e casos de uso cobertos por testes unitários JVM.

- [x] Modelar entidades de domínio (`Recipe`, `Category`, `IngredientItem`, `AppError`, `SyncOutcome`) e parser `instructionSteps` em `Recipe.kt`.
- [x] Definir contratos de repositório (`RecipeRepository`) e monitoramento de rede (`NetworkMonitor`).
- [x] Implementar os 8 Casos de Uso (`ObserveRecipesUseCase`, `ObserveCategoriesUseCase`, `ObserveFavoriteRecipesUseCase`, `ObserveRecipeDetailUseCase`, `SyncCatalogUseCase`, `ToggleFavoriteUseCase`, `ToggleIngredientCheckUseCase`, `GetRandomChefRecipeUseCase`).

---

## Etapa 4: Camada Data (Room, Retrofit e Offline-First)
**Critério de conclusão:** Persistência Room funcionando como SSOT, cliente Retrofit/Moshi integrado à TheMealDB, monitoramento de conectividade ativo e repositório preservando favoritos/checklist durante sincronizações.

- [x] Configurar banco Room (`ChefExpressDatabase`, `RecipeEntity`, `CategoryEntity`, `IngredientLocalModel`, `RecipeDao`, `CategoryDao`).
- [x] Configurar cliente Retrofit (`TheMealDbApi`, `TheMealDbDto` com `@JsonClass(generateAdapter = true)`, timeouts e `HttpLoggingInterceptor`).
- [x] Implementar `RecipeMappers` entre DTO, Entity e Domain Model com merge não-destrutivo.
- [x] Implementar `AndroidNetworkMonitor` e `OfflineFirstRecipeRepositoryImpl` com preservação de favoritos/checklist, fallback de receita aleatória e seed de contingência offline.

---

## Etapa 5: Localização PT-BR e Migração do Banco Local
**Critério de conclusão:** Interface, seed offline e metadados culinários traduzidos para Português (PT-BR), tradução bidirecional de categorias (`PT-BR <-> API`) validada em testes unitários e versão do banco Room atualizada para suportar o novo catálogo inicial.

- [x] Implementar `CulinaryPtBrLocalizer.kt` com tradução de categorias, origens culinárias, tags, ingredientes frequentes, unidades de medida e mapeamento reverso `toApiCategoryKey`.
- [x] Atualizar `InitialRecipeSeed.kt` com 5 categorias e 5 receitas clássicas integralmente em Português (PT-BR).
- [x] Atualizar `ChefExpressDatabase` para `version = 2` com `fallbackToDestructiveMigration(dropAllTables = true)` para renovação limpa do catálogo inicial localizado.
- [x] Atualizar mapeamento em `RecipeMappers.kt` e `OfflineFirstRecipeRepositoryImpl.syncCategoryRecipes` para utilizar `CulinaryPtBrLocalizer`.
- [ ] Adicionar tradução automática de textos extensos de `strInstructions` vindos da API (fora do escopo offline atual; mantido no idioma original do servidor).

---

## Etapa 6: Camada Presentation (Jetpack Compose & Navigation)
**Critério de conclusão:** Telas `HomeScreen`, `FavoritesScreen` e `RecipeDetailScreen` conectadas por `ChefExpressNavGraph` com rotas `@Serializable`, layout responsivo (`NavigationBar`/`NavigationRail`) e tratamento explícito dos estados Loading, Success, Empty e Error.

- [x] Implementar grafo de navegação tipado (`ChefExpressNavGraph`) com `DiscoverRoute`, `FavoritesRoute`, `RecipeDetailRoute` e shell adaptativo.
- [x] Implementar `HomeViewModel` e `HomeScreen` com estados Loading, Success, Empty e Error, busca híbrida (debounce 400 ms e mínimo 2 chars remoto), categorias, banner offline e Sugestão do Chef.
- [x] Implementar `FavoritesViewModel` e `FavoritesScreen` com lista de receitas salvas offline, filtro local e empty state ilustrado.
- [x] Implementar `RecipeDetailViewModel` e `RecipeDetailScreen` com hidratação sob demanda, checklist persistente de ingredientes, etapas numeradas e timer de cozinha.
- [ ] Refatorar `RecipeDetailScreen.kt` (atualmente com 705 linhas) extraindo componentes (como `KitchenTimerCard`) para manter todos os arquivos abaixo de 500 linhas.

---

## Etapa 7: Estratégia de Validação de Testes, Documentação e Entrega
**Critério de conclusão:** Build de depuração gerado sem erros, suíte unitária JVM aprovada após as últimas alterações, suíte Robolectric reexecutada e aprovada no ambiente pós-PT-BR, documentação 100% consistente, evidências visuais reais coletadas em `evidencias/` e código publicado no GitHub.

### 7.1 Testes e Build
- [x] Escrever testes unitários JVM (`RecipeUseCasesTest`, `RecipeMappersTest`, `OfflineFirstRecipeRepositoryTest`, `HomeViewModelTest`, `RecipeDetailViewModelTest`, `ExampleUnitTest`).
- [x] Escrever e atualizar testes Robolectric (`ChefExpressDatabaseTest`, `ChefExpressUiTest`, `ExampleRobolectricTest`) para refletir as categorias e receitas em PT-BR.
- [x] Executar compilação pós-PT-BR e gerar APK de depuração (`app/build/outputs/apk/debug/app-debug.apk`).
- [x] Executar suíte unitária JVM pós-PT-BR (`16/16` testes aprovados).
- [x] Executar validação inicial dos testes Robolectric antes da localização PT-BR (15/11/2026 — `4/4` aprovados).
- [ ] Reexecutar os 4 testes Robolectric após a atualização PT-BR (compilação `compileDebugUnitTestKotlin` aprovada em 24/11/2026, porém execução bloqueada neste runner por erro HTTP 429 do Maven Central ao baixar `android-all-instrumented:16-robolectric-13921718-i7.jar`).
- [ ] Implementar testes unitários para `FavoritesViewModel`, `AndroidNetworkMonitor` e contrato HTTP com `MockWebServer`.

### 7.2 Documentação e Entrega Acadêmica
- [x] Atualizar `SPECIFICATION.md`, `TASKS.md`, `README.md`, `TEST_RESULTS.md` e criar `PLANNING.md` coerentes com o código e com a auditoria.
- [x] Criar pasta `evidencias/` na raiz do projeto para recebimento de capturas reais.
- [ ] Executar o APK (`app-debug.apk`) em emulador/dispositivo Android local, validar fluxos online/offline reais com a API TheMealDB e salvar capturas de tela em `evidencias/`.
- [ ] Realizar commit das atualizações documentais finais e publicar o repositório atualizado no GitHub (não há `remote` / credenciais GitHub configuradas neste contêiner).

---

## Tabela Geral de Progresso

| Etapa | Descrição | Estado atual | Pendências |
| :---: | --- | --- | --- |
| **1** | Especificação e Planejamento (SDD) | **Concluída** | Nenhuma |
| **2** | Configuração Base, Identidade Visual e Dependências | **Concluída** | Nenhuma |
| **3** | Camada Domain (Modelos, Contratos e UseCases) | **Concluída e Validada (JVM)** | Nenhuma |
| **4** | Camada Data (Room, Retrofit, Offline-First e Rede) | **Implementada (Validação parcial)** | Teste dedicado de `AndroidNetworkMonitor` e teste HTTP com `MockWebServer` |
| **5** | Localização PT-BR e Migração Room v2 | **Implementada e Validada em Mappers/Repo** | Tradução automática de instruções extensas da API (opcional/não planejada) |
| **6** | Camada Presentation (Compose M3 + Navigation) | **Implementada (Validação parcial)** | Refatoração opcional de `RecipeDetailScreen.kt` (< 500 linhas) e teste unitário de `FavoritesViewModel` |
| **7.1** | Testes Automatizados e Build | **Parcialmente Validada** (Build + 16/16 JVM OK) | Reexecução runtime dos 4 testes Robolectric pós-PT-BR (bloqueio HTTP 429 Maven Central) e testes extras |
| **7.2** | Documentação, Evidências e Entrega Git/GitHub | **Documentação Concluída / Entrega Pendente** | Capturas reais em `evidencias/` via emulador/dispositivo, commit final e push no GitHub |
