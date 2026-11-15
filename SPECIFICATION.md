# Especificação do Projeto — Chef Express

## 1. Visão Geral e Proposta
O **Chef Express** é um aplicativo Android de receitas culinárias projetado segundo a metodologia **Spec-Driven Development (SDD)** e arquitetura **Offline-First**. Ele resolve o problema comum de cozinheiros amadores e entusiastas perderem o acesso às receitas no meio do preparo por instabilidade de rede ou falta de conexão na cozinha, combinando um catálogo gastronômico rico via REST API (**TheMealDB**) com persistência local reativa via **Room**.

## 2. Objetivos
- Oferecer descoberta rápida de receitas internacionais por nome, categoria e sugestão aleatória do chef.
- Garantir disponibilidade contínua das receitas consultadas e favoritadas mesmo sem conexão com a internet.
- Fornecer ferramentas práticas durante o preparo, como checklist interativo de ingredientes (persistido localmente) e timer de cozinha integrado.
- Aplicar rigorosamente Clean Architecture, programação reativa com Coroutines/Flow, injeção de dependências com Koin e UI moderna em Jetpack Compose (Material Design 3).

## 3. Requisitos Funcionais (RF)
- **RF01 — Catálogo e Descoberta:** Exibir lista de receitas obtidas da API TheMealDB e cacheadas localmente no Room, com busca textual instantânea e filtragem por categorias culinárias.
- **RF02 — Sugestão do Chef:** Permitir sortear uma receita surpresa (`random.php` quando online, ou seleção aleatória do cache local quando offline).
- **RF03 — Detalhes da Receita:** Exibir foto em alta resolução, categoria, origem culinária (área), tags, lista estruturada de ingredientes com medidas e instruções passo a passo numeradas.
- **RF04 — Checklist de Ingredientes Persistente:** Permitir marcar/desmarcar ingredientes já separados na bancada, salvando o progresso localmente por receita.
- **RF05 — Favoritos (Livro de Receitas Offline):** Permitir favoritar e desfavoritar receitas, mantendo-as sempre acessíveis na aba dedicada "Favoritos" mesmo offline.
- **RF06 — Timer de Cozinha Integrado:** Disponibilizar controle rápido de contagem regressiva (5, 10, 15 min ou ajuste manual) diretamente na tela de detalhes da receita.
- **RF07 — Modo Offline-First e Indicador de Rede:** Monitorar o estado da conectividade em tempo real, exibindo banner informativo quando offline e botão de sincronização manual quando online.
- **RF08 — Estados de Interface:** Tratar explicitamente os estados de carregamento (Loading), sucesso (Success), ausência de resultados (Empty State) e falha recuperável (Error com retry).

## 4. Requisitos Não Funcionais (RNF)
- **RNF01 — Arquitetura:** Clean Architecture com camadas `domain`, `data` e `presentation` desacopladas e regra de dependência estrita (`presentation -> domain <- data`).
- **RNF02 — Injeção de Dependências:** Koin para gerenciamento modular de dependências entre camadas.
- **RNF03 — Assincronismo e Reatividade:** Kotlin Coroutines com `Dispatchers.IO` para rede/banco e `Flow`/`StateFlow` observados com `collectAsStateWithLifecycle()`.
- **RNF04 — UI e Responsividade:** Jetpack Compose com Material 3, suporte a Edge-to-Edge, layout adaptativo (`NavigationBar` em telas compactas e `NavigationRail` em telas médias/expandidas) e acessibilidade (`contentDescription`, alvos mínimos de 48dp).
- **RNF05 — Qualidade e Testabilidade:** Cobertura por testes unitários (casos de uso, mappers, repositórios e ViewModels) e testes Robolectric (Room + Compose CUJ).
- **RNF06 — Segurança e Prevenção de Memory Leaks:** Sem referências estáticas a `Context`/`Activity` em ViewModels ou casos de uso, uso de `viewModelScope` e `SharingStarted.WhileSubscribed(5000)`.
