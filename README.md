# Chef Express

Aplicativo Android de receitas culinárias desenvolvido em **Kotlin** e **Jetpack Compose**, aplicando **Spec-Driven Development (SDD)**, **Clean Architecture**, **Offline-First**, persistência local com **Room**, consumo de API REST com **Retrofit**, injeção de dependências com **Koin** e testes automatizados com **JUnit4** e **Robolectric**.

---

## 1. Objetivo e Problema Resolvido

Na cozinha, a perda de conectividade ou oscilações de Wi-Fi frequentemente interrompem o acompanhamento de uma receita no meio do preparo. O **Chef Express** resolve esse problema garantindo que receitas consultadas, favoritadas e seus respectivos checklists de ingredientes permaneçam disponíveis localmente no dispositivo, oferecendo uma experiência contínua mesmo sem acesso à internet.

---

## 2. Funcionalidades Implementadas

- **Exploração e Busca de Receitas:** Listagem de pratos internacionais com busca por nome ou ingrediente (com *debounce* para evitar chamadas excessivas de rede) e filtragem por categoria culinária.
- **Sugestão do Chef:** Sorteio de uma receita aleatória (`random.php` quando online ou seleção aleatória do cache local quando offline).
- **Detalhes da Receita:** Exibição de imagem em alta resolução, categoria, origem culinária, tags, link opcional de vídeo no YouTube e modo de preparo estruturado em etapas numeradas.
- **Checklist Interativo Persistente:** Marcação de ingredientes já separados, salva localmente no banco Room por receita.
- **Favoritos (Livro de Receitas Offline):** Gerenciamento de receitas favoritas em aba dedicada, com filtro próprio e estado vazio ilustrado.
- **Timer de Cozinha Integrado:** Contagem regressiva executada via Coroutines diretamente na tela de detalhes (atalhos de 5, 10, 15 min e +1 min).
- **Monitoramento de Conectividade:** Banner dinâmico indicando Modo Offline e botão de sincronização manual quando a rede é restabelecida.
- **Layout Adaptativo & Material 3:** Tema editorial *"Bistrô Moderno"* (claro e escuro), `NavigationBar` em telas compactas e `NavigationRail` em telas médias/expandidas.

---

## 3. Tecnologias e Bibliotecas Utilizadas

- **Linguagem:** Kotlin 2.2.10
- **UI:** Jetpack Compose + Material Design 3 + WindowSizeClass + Navigation Compose (rotas `@Serializable`)
- **Injeção de Dependências:** Koin (`koin-android`, `koin-androidx-compose`)
- **Assincronismo & Reatividade:** Kotlin Coroutines, `Flow` e `StateFlow` (`collectAsStateWithLifecycle`)
- **Consumo de API REST:** Retrofit 2.12 + OkHttp + Moshi (`moshi-kotlin-codegen`)
- **Persistência Local:** Room 2.7 + KSP
- **Carregamento de Imagens:** Coil (`coil-compose`) com cache em memória e disco
- **Testes:** JUnit4, `kotlinx-coroutines-test`, Robolectric 4.16.1, `androidx.compose.ui:ui-test-junit4`

---

## 4. API REST Utilizada

- **Serviço:** [TheMealDB API](https://www.themealdb.com/api.php)
- **URL Base:** `https://www.themealdb.com/api/json/v1/1/`
- **Justificativa da Escolha:** API pública gratuita, sem exigência de credenciais proprietárias para leitura de catálogo, com dados culinários estruturados (categorias, origem, fotos em alta resolução, até 20 ingredientes/medidas por prato e instruções completas).
- **Endpoints Consumidos:**
  - `GET categories.php` — Lista de categorias culinárias.
  - `GET search.php?s={query}` — Busca de receitas por nome e carregamento inicial do catálogo.
  - `GET filter.php?c={category}` — Filtragem de receitas por categoria.
  - `GET lookup.php?i={id}` — Consulta detalhada de uma receita específica.
  - `GET random.php` — Sugestão aleatória de receita.

---

## 5. Arquitetura e Separação de Camadas (Clean Architecture)

O projeto segue rigorosamente a regra de dependência (`Presentation -> Domain <- Data`):

```text
app/src/main/java/com/example/
├── domain/                   # Regras de negócio puras (sem dependências Android)
│   ├── model/                # Recipe, Category, IngredientItem, AppError, SyncOutcome
│   ├── repository/           # Contratos RecipeRepository e NetworkMonitor
│   └── usecase/              # Casos de uso encapsulados
├── data/                     # Implementações de acesso a dados
│   ├── local/                # ChefExpressDatabase, Entities, DAOs e InitialRecipeSeed
│   ├── remote/               # TheMealDbApi e DTOs Moshi
│   ├── mapper/               # Conversões DTO <-> Entity <-> Domain
│   ├── network/              # AndroidNetworkMonitor (ConnectivityManager)
│   └── repository/           # OfflineFirstRecipeRepositoryImpl
├── presentation/             # Interface Jetpack Compose + MVVM
│   ├── home/                 # HomeScreen, HomeViewModel, HomeUiState
│   ├── favorites/            # FavoritesScreen, FavoritesViewModel, FavoritesUiState
│   ├── detail/               # RecipeDetailScreen, RecipeDetailViewModel, KitchenTimerState
│   ├── components/           # Cards, banner offline e estados Loading/Empty/Error
│   └── navigation/           # ChefExpressNavGraph (rotas tipadas e shell adaptativo)
├── di/                       # Módulos Koin (dataModule, domainModule, presentationModule)
└── ui/theme/                 # Cores, Shapes e Tipografia (Playfair Display + Plus Jakarta Sans)
```

---

## 6. Estratégia Offline-First e Sincronização

1. **Single Source of Truth (SSOT):** Todas as telas observam `Flow` emitidos diretamente pelo Room (`RecipeDao` e `CategoryDao`), nunca consumindo DTOs da API diretamente na UI.
2. **Sincronização Não-Destrutiva:** Ao receber dados da API, `RecipeMappers` mescla os registros preservando o estado local do usuário (`isFavorite`, itens marcados no checklist e instruções previamente baixadas).
3. **Contingência de Primeira Execução:** Se o aplicativo for aberto pela primeira vez completamente sem internet e com o banco vazio, `InitialRecipeSeed` popula um conjunto inicial de receitas clássicas para garantir funcionamento imediato.
4. **Hidratação Sob Demanda:** Receitas listadas via filtro de categoria (`filter.php`) são hidratadas automaticamente com detalhes completos via `lookup.php` ao abrir a tela de detalhes quando online.

---

## 7. Documentação SDD no Repositório

- [`SPECIFICATION.md`](SPECIFICATION.md) — Requisitos funcionais, não funcionais e proposta do aplicativo.
- [`ARCHITECTURE_PLAN.md`](ARCHITECTURE_PLAN.md) — Planejamento arquitetural, API REST e estratégia Offline-First.
- [`TASKS.md`](TASKS.md) — Decomposição incremental das etapas de desenvolvimento.
- [`TEST_RESULTS.md`](TEST_RESULTS.md) — Relatório detalhado dos testes automatizados executados.

---

## 8. Como Configurar e Executar o Projeto

1. Clone o repositório e abra a pasta raiz no **Android Studio** (Meerkat ou superior) ou utilize o Gradle CLI compatível com JDK 17+.
2. Não é necessária nenhuma chave de API ou configuração em `local.properties` para a API TheMealDB.
3. Para compilar o APK de depuração via linha de comando:
   ```bash
   gradle :app:assembleDebug
   ```
   O APK gerado estará disponível em `app/build/outputs/apk/debug/app-debug.apk`.

---

## 9. Como Executar os Testes Unitários e Robolectric

Execute a suíte completa de testes locais (unitários + Room in-memory + Compose UI via Robolectric):

```bash
gradle :app:testDebugUnitTest
```

### Evidências dos Testes
A execução completa da suíte (`gradle :app:testDebugUnitTest :app:assembleDebug`) obteve **18/18 testes aprovados (0 falhas)** e gerou o APK de depuração em `app/build/outputs/apk/debug/app-debug.apk`. Consulte o relatório detalhado em [`TEST_RESULTS.md`](TEST_RESULTS.md), cobrindo:
- Casos de uso da camada `domain` (`RecipeUseCasesTest` — 4/4)
- Mappers e preservação de estado offline (`RecipeMappersTest` — 2/2)
- Repositório Offline-First (`OfflineFirstRecipeRepositoryTest` — 3/3)
- Estados reativos de `HomeViewModel` e `RecipeDetailViewModel` (`HomeViewModelTest` — 2/2, `RecipeDetailViewModelTest` — 2/2)
- Consultas SQL do Room (`ChefExpressDatabaseTest` — 1/1)
- Fluxos de UI e recursos (`ChefExpressUiTest` — 2/2, `ExampleRobolectricTest` — 1/1, `ExampleUnitTest` — 1/1)

---

## 10. Limitações Conhecidas

- O conteúdo textual das receitas retornado pela API pública **TheMealDB** é originalmente em inglês, enquanto toda a interface, navegação e mensagens do aplicativo estão em português (PT-BR).
- O timer de cozinha integrado funciona enquanto o `ViewModel` está ativo na tela de detalhes, não utilizando alarme em segundo plano (`ForegroundService`) para manter permissões mínimas.
- Capturas de tela manuais em dispositivo físico/emulador local ficam a critério do avaliador durante a inspeção do APK gerado (`app-debug.apk`), uma vez que a validação automatizada em ambiente headless ocorre via Robolectric.
