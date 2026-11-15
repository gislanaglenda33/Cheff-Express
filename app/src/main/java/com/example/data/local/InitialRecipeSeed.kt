package com.example.data.local

import com.example.data.mapper.RecipeMappers

/**
 * Conjunto enxuto de contingência usado exclusivamente quando o aplicativo é aberto
 * pela primeira vez sem conexão com a internet e com o banco local vazio.
 */
object InitialRecipeSeed {

    val categories: List<CategoryEntity> = listOf(
        CategoryEntity(
            id = "seed_pasta",
            name = "Pasta",
            thumbnailUrl = "https://www.themealdb.com/images/category/pasta.png",
            description = "Massas artesanais e clássicos italianos."
        ),
        CategoryEntity(
            id = "seed_chicken",
            name = "Chicken",
            thumbnailUrl = "https://www.themealdb.com/images/category/chicken.png",
            description = "Pratos variados e suculentos com frango."
        ),
        CategoryEntity(
            id = "seed_seafood",
            name = "Seafood",
            thumbnailUrl = "https://www.themealdb.com/images/category/seafood.png",
            description = "Peixes e frutos do mar frescos."
        ),
        CategoryEntity(
            id = "seed_vegetarian",
            name = "Vegetarian",
            thumbnailUrl = "https://www.themealdb.com/images/category/vegetarian.png",
            description = "Receitas leves à base de vegetais, grãos e ervas."
        ),
        CategoryEntity(
            id = "seed_dessert",
            name = "Dessert",
            thumbnailUrl = "https://www.themealdb.com/images/category/dessert.png",
            description = "Sobremesas tradicionais e confeitaria."
        )
    )

    fun recipes(nowMillis: Long = System.currentTimeMillis()): List<RecipeEntity> = listOf(
        RecipeEntity(
            id = "52771",
            name = "Spicy Arrabiata Penne",
            category = "Vegetarian",
            area = "Italian",
            instructions = "Bring a large pot of water to a boil. Add kosher salt to the boiling water, then add the pasta.\nCook according to package instructions until al dente.\nIn a large skillet over medium heat, warm the olive oil and saute the garlic and chili flakes until fragrant.\nAdd crushed tomatoes and simmer for 10 minutes until thickened.\nToss the drained penne into the sauce with fresh basil and Parmigiano-Reggiano.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/ustsqw1468250014.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Pasta,Curry,Spicy",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Penne Rigate", "400g"),
                    IngredientLocalModel("Olive Oil", "3 tbsp"),
                    IngredientLocalModel("Garlic", "4 cloves"),
                    IngredientLocalModel("Chopped Tomatoes", "400g"),
                    IngredientLocalModel("Red Chilli Flakes", "1/2 tsp"),
                    IngredientLocalModel("Fresh Basil", "1 handful"),
                    IngredientLocalModel("Parmigiano-Reggiano", "50g")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        ),
        RecipeEntity(
            id = "52772",
            name = "Teriyaki Chicken Casserole",
            category = "Chicken",
            area = "Japanese",
            instructions = "Preheat oven to 175C. Mix soy sauce, water, brown sugar, ginger and garlic in a saucepan and bring to a boil.\nStir cornstarch dissolved in cold water into the sauce until thickened.\nPlace diced chicken in a baking dish, pour teriyaki glaze over and bake for 25 minutes.\nSteam broccoli and carrots, toss with cooked rice and serve hot with toasted sesame seeds.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/wvpsxx1468256321.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Meat,Casserole",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Chicken Breasts", "600g"),
                    IngredientLocalModel("Soy Sauce", "1/2 cup"),
                    IngredientLocalModel("Brown Sugar", "1/4 cup"),
                    IngredientLocalModel("Ground Ginger", "1 tsp"),
                    IngredientLocalModel("Garlic", "2 cloves"),
                    IngredientLocalModel("Cornstarch", "1 tbsp"),
                    IngredientLocalModel("Broccoli", "200g")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        ),
        RecipeEntity(
            id = "52959",
            name = "Baked Salmon with Fennel & Tomatoes",
            category = "Seafood",
            area = "British",
            instructions = "Heat oven to 180C. Arrange sliced fennel, cherry tomatoes, garlic and olive oil in a roasting tin.\nRoast for 15 minutes until tender.\nNestle salmon fillets onto the vegetables, season with lemon zest, sea salt and black pepper.\nBake for 12 minutes until salmon flakes easily and finish with fresh dill.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/1548772327.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Seafood,Healthy",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Salmon Fillets", "2 fillets"),
                    IngredientLocalModel("Fennel Bulb", "1 large"),
                    IngredientLocalModel("Cherry Tomatoes", "250g"),
                    IngredientLocalModel("Olive Oil", "2 tbsp"),
                    IngredientLocalModel("Lemon", "1"),
                    IngredientLocalModel("Fresh Dill", "1 bunch")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        ),
        RecipeEntity(
            id = "52844",
            name = "Lasagne",
            category = "Pasta",
            area = "Italian",
            instructions = "Warm olive oil in a large pan and soften onion, celery and garlic for 8 minutes.\nAdd minced beef and brown well, then stir in tomato puree, crushed tomatoes and oregano.\nSimmer gently for 35 minutes.\nLayer meat sauce, lasagne sheets and creamy bechamel in a baking dish, top with mozzarella and bake at 190C for 40 minutes until bubbling.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/wtsvxx1511296896.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Pasta,Classic",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Lasagne Sheets", "12 sheets"),
                    IngredientLocalModel("Minced Beef", "500g"),
                    IngredientLocalModel("Passata", "500ml"),
                    IngredientLocalModel("Onion", "1 large"),
                    IngredientLocalModel("Mozzarella", "150g"),
                    IngredientLocalModel("Olive Oil", "2 tbsp")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        ),
        RecipeEntity(
            id = "52893",
            name = "Apple & Blackberry Crumble",
            category = "Dessert",
            area = "British",
            instructions = "Preheat oven to 190C. Toss sliced apples and blackberries with demerara sugar and cinnamon in an ovenproof dish.\nRub cold butter into plain flour and oats until the mixture resembles coarse breadcrumbs.\nScatter the crumble topping evenly over the fruit and bake for 35 minutes until golden and bubbling.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/xvsurr1511719182.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Sweet,Dessert,Fruit",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Cooking Apples", "4"),
                    IngredientLocalModel("Blackberries", "150g"),
                    IngredientLocalModel("Plain Flour", "175g"),
                    IngredientLocalModel("Cold Butter", "100g"),
                    IngredientLocalModel("Demerara Sugar", "85g"),
                    IngredientLocalModel("Rolled Oats", "50g")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        )
    )
}
