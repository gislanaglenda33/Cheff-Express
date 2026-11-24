# Relatório de Testes — Chef Express

**Data da última verificação:** 24/11/2026  
**Artefato APK gerado:** `app/build/outputs/apk/debug/app-debug.apk` (`BUILD SUCCESSFUL` após atualização PT-BR)

---

## 1. Resumo de Estado dos Testes

| Categoria | Quantidade | Estado Atual | Observação |
| --- | :---: | --- | --- |
| **Testes Unitários JVM** | 16 | **Aprovados (16/16)** | Executados com sucesso após a atualização PT-BR |
| **Testes Robolectric (Room + Compose UI)** | 4 | **Não executados pós-PT-BR (Bloqueio de infraestrutura)** | Aprovados em 15/11 (pré-PT-BR); compilam pós-PT-BR, mas falharam na inicialização do runner em 24/11 por **HTTP 429** do Maven Central |
| **Testes Instrumentados (`androidTest`) / Dispositivo** | — | **Não executados** | Ambiente sem emulador/ADB disponível |
| **Lacunas de Cobertura Mapeadas** | — | **Pendentes de implementação** | Ausência de testes dedicados para `FavoritesViewModel`, `AndroidNetworkMonitor` e `MockWebServer` |

---

## 2. Testes Unitários JVM — Aprovados (Pós-atualização PT-BR)

**Comando executado em 24/11/2026:**
```bash
gradle :app:testDebugUnitTest --tests "com.example.ExampleUnitTest" --tests "com.example.domain.*" --tests "com.example.data.mapper.*" --tests "com.example.data.repository.*" --tests "com.example.presentation.*ViewModelTest" --console=plain
```

**Resultado:** **BUILD SUCCESSFUL** — **16 testes executados, 16 aprovados, 0 falhas**

| Suíte de Teste | Camada / Escopo | Executados | Aprovados | Falhas | Status |
| --- | --- | :---: | :---: | :---: | --- |
| `RecipeUseCasesTest` | Domain (Casos de uso e `instructionSteps`) | 4 | 4 | 0 | Aprovado |
| `RecipeMappersTest` | Data Mapper + `CulinaryPtBrLocalizer` | 3 | 3 | 0 | Aprovado |
| `OfflineFirstRecipeRepositoryTest` | Data Repository + Mapeamento PT-BR -> API | 4 | 4 | 0 | Aprovado |
| `HomeViewModelTest` | Presentation ViewModel (`StateFlow` / `HomeEvent`) | 2 | 2 | 0 | Aprovado |
| `RecipeDetailViewModelTest` | Presentation ViewModel + Timer de Cozinha | 2 | 2 | 0 | Aprovado |
| `ExampleUnitTest` | Baseline JVM | 1 | 1 | 0 | Aprovado |
| **Total JVM** | | **16** | **16** | **0** | **Aprovado** |

---

## 3. Testes Robolectric (Room + Compose UI) — Não Executados na Reexecução Pós-PT-BR

**Status de compilação pós-PT-BR (24/11/2026):** As classes abaixo foram atualizadas para o catálogo em Português (PT-BR) e **compilam sem erros** (`kspDebugUnitTestKotlin` e `compileDebugUnitTestKotlin` concluídos com sucesso).

**Bloqueio de execução em 24/11/2026:** Ao executar `gradle :app:testDebugUnitTest`, o runner do Robolectric falhou antes da execução dos testes ao tentar baixar o artefato `org.robolectric:android-all-instrumented:16-robolectric-13921718-i7.jar` no repositório `repo1.maven.org`, recebendo erro **HTTP 429 (Too Many Requests)**. Portanto, os 4 testes abaixo permanecem **pendentes de reexecução runtime pós-PT-BR**.

| Suíte de Teste | Casos | Execução Inicial (15/11, pré-PT-BR) | Situação Atual (24/11, pós-PT-BR) |
| --- | :---: | --- | --- |
| `ChefExpressDatabaseTest` | 1 | 1/1 aprovado | Compila; **não executado** (bloqueio HTTP 429 ao baixar SDK Robolectric) |
| `ChefExpressUiTest` | 2 | 2/2 aprovados | Compila; **não executado** (bloqueio HTTP 429 ao baixar SDK Robolectric) |
| `ExampleRobolectricTest` | 1 | 1/1 aprovado | Compila; **não executado** (bloqueio HTTP 429 ao baixar SDK Robolectric) |

---

## 4. Comportamentos Validados na Suíte JVM Pós-PT-BR

- **`CulinaryPtBrLocalizer`:** tradução bidirecional de categorias (`Massas` <-> `Pasta`, `Frutos do Mar` <-> `Seafood`), origens culinárias (`Japonesa`, `Italiana`, etc.), tags e unidades de medida (`col. sopa`, `xícara`).
- **`RecipeMappers`:** preservação de `isFavorite` e do estado `isChecked` dos ingredientes em atualizações vindas da API, além da proteção para que resumos de `filter.php` não apaguem detalhes previamente baixados.
- **`OfflineFirstRecipeRepositoryImpl`:** população do `InitialRecipeSeed` no arranque offline (`OfflineFallback`), conversão da categoria selecionada em português (`"Massas"`) para o parâmetro da API TheMealDB (`"Pasta"`), e fallback local de `getRandomRecipeId()`.
- **Camada `domain` e `presentation`:** normalização de parâmetros nos casos de uso, remoção de prefixos em `Recipe.instructionSteps`, filtros/eventos no `HomeViewModel` e contagem regressiva/reset do timer no `RecipeDetailViewModel`.

---

## 5. Testes Pendentes de Implementação ou Ambiente

- Reexecução completa de `gradle :app:testDebugUnitTest` em ambiente com acesso desbloqueado ao Maven Central (ou cache local de `android-all-instrumented`) para confirmar os 4 testes Robolectric em tempo de execução após a localização PT-BR.
- Criação de testes unitários para `FavoritesViewModel` e `AndroidNetworkMonitor`.
- Criação de teste de contrato de serialização/endpoints com `MockWebServer` para `TheMealDbApi`.
- Validação manual ponta a ponta do APK (`app/build/outputs/apk/debug/app-debug.apk`) em dispositivo físico ou emulador Android, com registro de capturas reais na pasta `evidencias/`.
