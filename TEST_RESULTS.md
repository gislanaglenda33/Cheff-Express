# Relatório de Testes — Chef Express

**Comando executado:**
```bash
gradle :app:testDebugUnitTest :app:assembleDebug --console=plain
```

**Data da execução:** 15/11/2026  
**Resultado Geral:** **BUILD SUCCESSFUL** — **18 testes executados, 18 aprovados, 0 falhas, 0 ignorados**  
**Artefato APK gerado:** `app/build/outputs/apk/debug/app-debug.apk` (22 MB)

---

## 1. Resumo por Suíte de Testes

| Suíte de Teste | Camada / Tipo | Executados | Aprovados | Falhas |
| --- | --- | :---: | :---: | :---: |
| `RecipeUseCasesTest` | Domain (Unitário JVM) | 4 | 4 | 0 |
| `RecipeMappersTest` | Data Mapper (Unitário JVM) | 2 | 2 | 0 |
| `OfflineFirstRecipeRepositoryTest` | Data Repository (Unitário + Coroutines) | 3 | 3 | 0 |
| `ChefExpressDatabaseTest` | Data Room (Robolectric In-Memory SQLite) | 1 | 1 | 0 |
| `HomeViewModelTest` | Presentation ViewModel (Unitário + StateFlow) | 2 | 2 | 0 |
| `RecipeDetailViewModelTest` | Presentation ViewModel + Timer | 2 | 2 | 0 |
| `ChefExpressUiTest` | Presentation UI (Robolectric + Compose Test) | 2 | 2 | 0 |
| `ExampleRobolectricTest` | Recursos Android (Robolectric) | 1 | 1 | 0 |
| `ExampleUnitTest` | Baseline JVM | 1 | 1 | 0 |
| **Total** | | **18** | **18** | **0** |

---

## 2. Casos Validados

### Camada Domain (`RecipeUseCasesTest`)
- Normalização de busca e categorias em branco em `ObserveRecipesUseCase`.
- Validação de índice negativo em `ToggleIngredientCheckUseCase`.
- Delegação de `ToggleFavoriteUseCase` e `GetRandomChefRecipeUseCase`.
- Limpeza e segmentação de etapas em `Recipe.instructionSteps`.

### Camada Data (`RecipeMappersTest`, `OfflineFirstRecipeRepositoryTest`, `ChefExpressDatabaseTest`)
- Preservação de estado local (`isFavorite` e ingredientes marcados) ao sincronizar receita completa da API.
- Proteção contra sobrescrita de detalhes completos quando `filter.php` retorna apenas resumos.
- Contingência Offline-First: seed inicial no Room quando o app inicia sem internet e retorno `SyncOutcome.OfflineFallback`.
- Fallback local em `getRandomRecipeId()` quando offline.
- Consultas SQL reais no `ChefExpressDatabase` (busca por texto/ingrediente, filtro por categoria e favoritos).

### Camada Presentation (`HomeViewModelTest`, `RecipeDetailViewModelTest`, `ChefExpressUiTest`, `ExampleRobolectricTest`)
- Transições de `HomeUiState` (`Loading` -> `Success` -> `Empty`) com filtragem reativa e emissão de `HomeEvent.NavigateToRecipe`.
- Alternância reativa de favorito e checklist em `RecipeDetailViewModel`.
- Contagem regressiva, presets (5/10/15m), incremento (+1m) e reset do timer de cozinha.
- Renderização de banner offline (`offline_status_banner`) e clique em cards de receita no `HomeScreen`.
- Rolagem, disparo de timer e marcação de ingredientes no `RecipeDetailScreen`.
- Verificação do recurso `R.string.app_name == "Chef Express"`.

---

## 3. Problemas Identificados e Corrigidos Durante a Validação

1. **Reinicialização do Koin em múltiplos testes Robolectric:**  
   - *Sintoma:* `KoinApplicationAlreadyStartedException` ao executar múltiplas classes de teste Robolectric no mesmo processo JVM.  
   - *Correção:* Adicionada guarda `if (GlobalContext.getOrNull() == null)` antes de `startKoin` em `ChefExpressApp.onCreate()`.
2. **Sincronização de animação e rolagem em `ChefExpressUiTest`:**  
   - *Sintoma:* Elementos fora do viewport inicial em `LazyColumn`/`LazyVerticalGrid` e `AnimatedVisibility` do banner offline exigiam avanço do relógio e scroll explícito.  
   - *Correção:* Aplicado `composeTestRule.mainClock.advanceTimeBy(500)` e `performScrollToNode(hasTestTag(...))`, resultando em 100% de aprovação.
