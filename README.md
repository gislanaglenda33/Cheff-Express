# Chef Express

Aplicativo Android de receitas culinárias desenvolvido em **Kotlin** e **Jetpack Compose**, aplicando **Spec-Driven Development (SDD)**, **Clean Architecture**, **Offline-First**, persistência local com **Room**, consumo de API REST com **Retrofit**, injeção de dependências com **Koin** e testes automatizados com **JUnit4** e **Robolectric**.

---

## 1. Objetivo e Problema Resolvido

Na cozinha, a perda de conectividade ou oscilações de Wi-Fi frequentemente interrompem o acompanhamento de uma receita no meio do preparo. O **Chef Express** resolve esse problema garantindo que receitas consultadas, favoritadas e seus respectivos checklists de ingredientes permaneçam disponíveis localmente no dispositivo, oferecendo uma experiência contínua mesmo sem acesso à internet.

---

## 2. Funcionalidades Implementadas

- **Exploração e Busca de Receitas:** Listagem de pratos internacionais com filtragem instantânea local e pesquisa remota com *debounce* de 400 ms (disparada a partir de 2 caracteres), além de filtros por categoria culinária.
- **Sugestão do Chef:** Sorteio de uma receita aleatória (`random.php` quando online ou seleção aleatória do cache local via Room quando offline).
- **Detalhes da Receita:** Exibição de imagem em alta resolução, categoria, origem culinária, tags, link opcional de vídeo no YouTube e modo de preparo estruturado em etapas numeradas.
- **Checklist Interativo Persistente:** Marcação de ingredientes já separados, salva localmente no banco Room por receita e preservada entre sincronizações.
- **Favoritos (Livro de Receitas Offline):** Gerenciamento de receitas favoritas em aba dedicada, com filtro próprio por nome/categoria/origem e estado vazio ilustrado.
- **Timer de Cozinha Integrado:** Contagem regressiva executada via Coroutines diretamente na tela de detalhes (atalhos de 5, 10, 15 min, incremento +1 min, iniciar/pausar e reiniciar).
- **Localização em Português (PT-BR):** Toda a interface, catálogo de contingência inicial (`InitialRecipeSeed`), categorias, origens culinárias, tags, unidades de medida e ingredientes frequentes traduzidos via `CulinaryPtBrLocalizer`, com conversão reversa de categoria para consultas à API.
- **Monitoramento de Conectividade:** Banner dinâmico (`OfflineStatusBanner`) indicando Modo Offline e botão de sincronização manual na tela Explorar.
- **Layout Adaptativo & Material 3:** Tema editorial *"Bistrô Moderno"* (claro e escuro) com fontes locais (`Playfair Display` e `Plus Jakarta Sans`), `NavigationBar` em telas compactas e `NavigationRail` em telas médias/expandidas.

---

## 3. Tecnologias e Bibliotecas Utilizadas

- **Linguagem:** Kotlin `2.2.10` (Java 11 bytecode target)
- **Android SDK:** `compileSdk 36` / `targetSdk 36` / `minSdk 24` / AGP `9.1.1` / KSP `2.3.5`
- **UI & Navegação:** Jetpack Compose (BOM `2024.09.00`), Material Design 3, `WindowSizeClass`, Navigation Compose `2.8.9` (rotas `@Serializable` com `kotlinx-serialization-json 1.8.1`)
- **Injeção de Dependências:** Koin `4.0.2` (`koin-android`, `koin-androidx-compose`)
- **Assincronismo & Reatividade:** Kotlin Coroutines `1.10.2`, `Flow` e `StateFlow` (`collectAsStateWithLifecycle`)
- **Consumo de API REST:** Retrofit `2.12.0`, OkHttp `4.10.0` (com `HttpLoggingInterceptor`) e Moshi `1.15.2` (`moshi-kotlin`, `moshi-kotlin-codegen` via KSP)
- **Persistência Local:** Room `2.7.0` (`room-runtime`, `room-ktx`, `room-compiler`)
- **Carregamento de Imagens:** Coil `2.7.0` (`coil-compose`) com cache em memória e disco
- **Testes:** JUnit `4.13.2`, `kotlinx-coroutines-test 1.10.2`, Robolectric `4.16.1`, `androidx.compose.ui:ui-test-junit4`

---

## 4. API REST Utilizada

- **Serviço:** [TheMealDB API](https://www.themealdb.com/api.php)
- **URL Base:** `https://www.themealdb.com/api/json/v1/1/`
- **Autenticação:** Não requer chave de API privada para os endpoints públicos utilizados (não há necessidade de configurar segredo no `.env` para o funcionamento do catálogo).
- **Justificativa da Escolha:** API pública gratuita e estável, com dados culinários estruturados (categorias, origem, fotos em alta resolução, até 20 pares de ingredientes/medidas por prato e instruções completas), ideal para demonstrar paginação/filtragem, hidratação sob demanda e sincronização Offline-First.
- **Endpoints Consumidos (`TheMealDbApi.kt`):**
  - `GET categories.php` — Lista todas as categorias culinárias.
  - `GET search.php?s={query}` — Busca receitas por nome (`s=""` para o lote inicial e `query.trim().length >= 2` na busca remota).
  - `GET filter.php?c={category}` — Filtra resumos de receitas por categoria (recebe chave em inglês convertida por `CulinaryPtBrLocalizer.toApiCategoryKey`).
  - `GET lookup.php?i={id}` — Consulta detalhada de uma receita específica para hidratação completa.
  - `GET random.php` — Obtém uma receita aleatória para a "Sugestão do Chef".

---

## 5. Arquitetura e Separação de Camadas (Clean Architecture)

O projeto segue rigorosamente a regra de dependência (`Presentation -> Domain <- Data`):

- **`domain/` (Kotlin puro):** contém as entidades (`Recipe`, `Category`, `IngredientItem`, `AppError`, `SyncOutcome`), os contratos (`RecipeRepository`, `NetworkMonitor`) e os 8 casos de uso (`RecipeUseCases.kt`), sem qualquer dependência de Android, Room ou Retrofit.
- **`data/` (implementações e acesso a dados):** implementa os contratos de `domain` utilizando Room (`ChefExpressDatabase`, `RecipeDao`, `CategoryDao`), Retrofit/Moshi (`TheMealDbApi`, `TheMealDbDto`), tradução/merge (`CulinaryPtBrLocalizer`, `RecipeMappers`), monitoramento de rede (`AndroidNetworkMonitor`) e `OfflineFirstRecipeRepositoryImpl`.
- **`presentation/` (Jetpack Compose + MVVM):** consome exclusivamente os casos de uso e `NetworkMonitor` por meio de `HomeViewModel`, `FavoritesViewModel` e `RecipeDetailViewModel`, expondo estados imutáveis (`Loading`, `Success`, `Empty`, `Error`) e gerenciando a navegação em `ChefExpressNavGraph`.
- **`di/` e `ui/theme/`:** organizam os módulos Koin (`dataModule`, `domainModule`, `presentationModule`) e o tema Material Design 3 (*"Bistrô Moderno"*).

---

## 6. Estratégia Offline-First e Sincronização

1. **Single Source of Truth (SSOT):** Todas as telas observam `Flow` emitidos diretamente pelo Room (`RecipeDao` e `CategoryDao`), nunca consumindo DTOs da API diretamente na UI.
2. **Sincronização Não-Destrutiva:** Ao receber dados da API, `RecipeMappers.toEntity` mescla os registros preservando o estado local do usuário (`isFavorite`, itens marcados no checklist e instruções/detalhes previamente baixados).
3. **Contingência de Primeira Execução:** Se o aplicativo for aberto pela primeira vez completamente sem internet e com o banco vazio, `InitialRecipeSeed` popula 5 categorias e 5 receitas completas em Português (PT-BR) no Room (versão 2) para garantir funcionamento imediato.
4. **Hidratação Sob Demanda:** Receitas listadas apenas em resumo via filtro de categoria (`filter.php`) são hidratadas automaticamente com detalhes completos via `lookup.php` ao abrir a tela de detalhes quando online.
5. **Fallback da Sugestão do Chef:** Sem conexão ou em caso de erro de rede, `getRandomRecipeId()` sorteia uma receita já armazenada no Room via `ORDER BY RANDOM() LIMIT 1`.

---

## 7. Organização dos Diretórios

```text
/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── ChefExpressApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── di/AppModules.kt
│       │   │   ├── domain/
│       │   │   │   ├── model/Recipe.kt
│       │   │   │   ├── repository/RecipeRepository.kt
│       │   │   │   └── usecase/RecipeUseCases.kt
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── ChefExpressDatabase.kt
│       │   │   │   │   ├── InitialRecipeSeed.kt
│       │   │   │   │   ├── RecipeDao.kt
│       │   │   │   │   └── RecipeEntities.kt
│       │   │   │   ├── remote/
│       │   │   │   │   ├── TheMealDbApi.kt
│       │   │   │   │   └── TheMealDbDto.kt
│       │   │   │   ├── mapper/
│       │   │   │   │   ├── CulinaryPtBrLocalizer.kt
│       │   │   │   │   └── RecipeMappers.kt
│       │   │   │   ├── network/AndroidNetworkMonitor.kt
│       │   │   │   └── repository/OfflineFirstRecipeRepositoryImpl.kt
│       │   │   ├── presentation/
│       │   │   │   ├── components/ChefComponents.kt
│       │   │   │   ├── home/{HomeScreen.kt, HomeViewModel.kt}
│       │   │   │   ├── favorites/{FavoritesScreen.kt, FavoritesViewModel.kt}
│       │   │   │   ├── detail/{RecipeDetailScreen.kt, RecipeDetailViewModel.kt}
│       │   │   │   └── navigation/ChefExpressNavGraph.kt
│       │   │   └── ui/theme/{Color.kt, Theme.kt, Type.kt}
│       │   └── res/ (drawable, font, mipmap-*, values, xml)
│       └── test/java/com/example/
│           ├── ExampleUnitTest.kt
│           ├── ExampleRobolectricTest.kt
│           ├── domain/usecase/RecipeUseCasesTest.kt
│           ├── data/local/ChefExpressDatabaseTest.kt
│           ├── data/mapper/RecipeMappersTest.kt
│           ├── data/repository/OfflineFirstRecipeRepositoryTest.kt
│           └── presentation/{ChefExpressUiTest.kt, HomeViewModelTest.kt, RecipeDetailViewModelTest.kt}
├── evidencias/               # Pasta reservada para capturas reais de execução
├── gradle/libs.versions.toml
├── SPECIFICATION.md          # Especificação, regras de negócio e critérios de aceitação
├── ARCHITECTURE_PLAN.md      # Planejamento arquitetural inicial preservado
├── PLANNING.md               # Planejamento técnico detalhado atualizado
├── TASKS.md                  # Decomposição de tarefas e tabela geral de progresso
└── TEST_RESULTS.md           # Relatório detalhado de compilação e execução de testes
```

---

## 8. Pré-requisitos

- **JDK 11 ou superior** (compatível com `JavaVersion.VERSION_11` configurado em `app/build.gradle.kts`).
- **Android Studio** (recomendado para execução em emulador/dispositivo) ou **Gradle 9.3.1** (`gradle` instalado no PATH — este repositório não inclui script `./gradlew` na raiz, utilizando `gradle/wrapper/gradle-wrapper.properties`).
- **Android SDK 36** (`compileSdk 36`, `targetSdk 36`, `minSdk 24`).
- Conexão com a internet para download de dependências na primeira compilação e para sincronização online com a API TheMealDB.

---

## 9. Como Configurar e Executar o Projeto

1. **Clonar ou importar o projeto:**
   - Abra a pasta raiz do projeto diretamente no **Android Studio** (*File > Open*), aguardando a sincronização do Gradle.
   - Não é necessária configuração de chaves em `.env` ou `local.properties` para o consumo da API TheMealDB.
2. **Compilar o APK de depuração via terminal:**
   ```bash
   gradle :app:assembleDebug
   ```
   O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.
3. **Executar em dispositivo ou emulador:**
   - No Android Studio, selecione um dispositivo físico (com depuração USB ativa) ou um emulador Android (API 24+) e clique em **Run (`app`)**.

---

## 10. Como Executar os Testes Unitários

- **Executar toda a suíte de testes unitários e Robolectric:**
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Executar apenas os testes unitários JVM puros (sem dependência de artefatos externos do Robolectric):**
  ```bash
  gradle :app:testDebugUnitTest \
    --tests "com.example.ExampleUnitTest" \
    --tests "com.example.domain.*" \
    --tests "com.example.data.mapper.*" \
    --tests "com.example.data.repository.*" \
    --tests "com.example.presentation.*ViewModelTest" \
    --console=plain
  ```

---

## 11. Resultados dos Testes e Documentação SDD

- **Suíte Unitária JVM (pós-atualização PT-BR):** **16/16 testes aprovados** (0 falhas), cobrindo `RecipeUseCasesTest` (4/4), `RecipeMappersTest` (3/3), `OfflineFirstRecipeRepositoryTest` (4/4), `HomeViewModelTest` (2/2), `RecipeDetailViewModelTest` (2/2) e `ExampleUnitTest` (1/1).
- **Suíte Robolectric (Room + Compose UI):** Os 3 arquivos de teste Robolectric (`ChefExpressDatabaseTest`, `ChefExpressUiTest`, `ExampleRobolectricTest` — 4 casos no total) passaram na execução inicial (15/11/2026) e compilam sem erros após a atualização PT-BR (`compileDebugUnitTestKotlin`), porém a **reexecução em tempo de execução pós-PT-BR encontra-se pendente/bloqueada neste runner** por erro externo `HTTP 429` do Maven Central ao baixar `org.robolectric:android-all-instrumented:16-robolectric-13921718-i7.jar`.
- **Relatório completo:** consulte [`TEST_RESULTS.md`](TEST_RESULTS.md).

Documentação complementar de SDD:
- [`SPECIFICATION.md`](SPECIFICATION.md)
- [`PLANNING.md`](PLANNING.md)
- [`ARCHITECTURE_PLAN.md`](ARCHITECTURE_PLAN.md)
- [`TASKS.md`](TASKS.md)

---

## 12. Limitações Conhecidas

- Textos extensos de instruções de preparo (`strInstructions`) retornados dinamicamente pela API pública **TheMealDB** são exibidos no idioma original retornado pelo servidor (inglês), enquanto UI, seed offline, categorias, origens, tags, unidades de medida e ingredientes frequentes estão em Português (PT-BR).
- O timer de cozinha integrado funciona enquanto o `RecipeDetailViewModel` está ativo na tela de detalhes, sem serviço em segundo plano ou notificações (`ForegroundService` / `POST_NOTIFICATIONS`).
- Fotos de receitas ainda não visualizadas quando online dependem do cache prévio do Coil para aparecerem sem internet (exibindo imagem editorial de placeholder quando indisponíveis offline).
- A reexecução runtime da suíte Robolectric pós-PT-BR depende de rede desbloqueada para o Maven Central (bloqueio HTTP 429 registrado no contêiner).
- A pasta `evidencias/` encontra-se vazia aguardando capturas de tela obtidas em execução manual num emulador ou dispositivo físico (o ambiente headless atual não possui emulador/ADB).

---

## 13. Funcionalidades Ainda Não Implementadas

- **Alarme/Notificação em Segundo Plano para o Timer:** disparo sonoro ou notificação persistente ao sair da tela de detalhes ou bloquear o aparelho.
- **Tradução Dinâmica de Instruções Extensas da API:** tradução automática de parágrafos completos de modo de preparo baixados da TheMealDB.
- **Cobertura de Testes Complementar:** testes unitários dedicados para `FavoritesViewModel` e `AndroidNetworkMonitor`, além de testes de contrato HTTP com `MockWebServer` para `TheMealDbApi`.
- **Sincronização em Nuvem / Contas de Usuário:** o aplicativo é estritamente local por dispositivo (Room), sem autenticação ou backup remoto entre aparelhos.
- **Publicação Remota e Capturas Finais:** envio do histórico para repositório remoto no GitHub e preenchimento de `evidencias/` com capturas de execução real online e offline.
