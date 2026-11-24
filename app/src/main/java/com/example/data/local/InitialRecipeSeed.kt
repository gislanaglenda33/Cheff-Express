package com.example.data.local

import com.example.data.mapper.RecipeMappers

/**
 * Conjunto enxuto de contingência em Português (PT-BR) usado quando o aplicativo
 * é aberto pela primeira vez sem conexão com a internet e com o banco local vazio.
 */
object InitialRecipeSeed {

    val categories: List<CategoryEntity> = listOf(
        CategoryEntity(
            id = "seed_pasta",
            name = "Massas",
            thumbnailUrl = "https://www.themealdb.com/images/category/pasta.png",
            description = "Massas artesanais e clássicos italianos."
        ),
        CategoryEntity(
            id = "seed_chicken",
            name = "Frango",
            thumbnailUrl = "https://www.themealdb.com/images/category/chicken.png",
            description = "Pratos variados e suculentos com frango."
        ),
        CategoryEntity(
            id = "seed_seafood",
            name = "Frutos do Mar",
            thumbnailUrl = "https://www.themealdb.com/images/category/seafood.png",
            description = "Peixes e frutos do mar frescos."
        ),
        CategoryEntity(
            id = "seed_vegetarian",
            name = "Vegetariano",
            thumbnailUrl = "https://www.themealdb.com/images/category/vegetarian.png",
            description = "Receitas leves à base de vegetais, grãos e ervas."
        ),
        CategoryEntity(
            id = "seed_dessert",
            name = "Sobremesas",
            thumbnailUrl = "https://www.themealdb.com/images/category/dessert.png",
            description = "Sobremesas tradicionais e confeitaria."
        )
    )

    fun recipes(nowMillis: Long = System.currentTimeMillis()): List<RecipeEntity> = listOf(
        RecipeEntity(
            id = "52771",
            name = "Penne Arrabiata Picante",
            category = "Vegetariano",
            area = "Italiana",
            instructions = "Leve uma panela grande com água ao fogo alto até ferver, adicione sal grosso e cozinhe o penne.\nCozinhe seguindo o tempo da embalagem até ficar al dente.\nEm uma frigideira larga em fogo médio, aqueça o azeite e refogue o alho e a pimenta calabresa até perfumar.\nAdicione os tomates picados e deixe apurar por 10 minutos até o molho encorpar.\nMisture o penne escorrido ao molho com folhas de manjericão fresco e queijo parmesão ralado.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/ustsqw1468250014.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Massa,Picante",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Macarrão Penne", "400g"),
                    IngredientLocalModel("Azeite de Oliva", "3 col. sopa"),
                    IngredientLocalModel("Alho", "4 dentes"),
                    IngredientLocalModel("Tomates Picados", "400g"),
                    IngredientLocalModel("Pimenta Calabresa", "1/2 col. chá"),
                    IngredientLocalModel("Manjericão Fresco", "1 punhado"),
                    IngredientLocalModel("Queijo Parmesão", "50g")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        ),
        RecipeEntity(
            id = "52772",
            name = "Frango Teriyaki Assado",
            category = "Frango",
            area = "Japonesa",
            instructions = "Preaqueça o forno a 175°C. Misture molho shoyu, água, açúcar mascavo, gengibre e alho em uma panela e leve à fervura.\nDissolva o amido de milho em água fria e incorpore ao molho até engrossar.\nDisponha o frango em cubos em um refratário, cubra com o molho teriyaki e asse por 25 minutos.\nCozinhe o brócolis no vapor e sirva bem quente com arroz branco e gergelim tostado.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/wvpsxx1468256321.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Carne,Assado",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Peito de Frango", "600g"),
                    IngredientLocalModel("Molho Shoyu", "1/2 xícara"),
                    IngredientLocalModel("Açúcar Mascavo", "1/4 xícara"),
                    IngredientLocalModel("Gengibre em Pó", "1 col. chá"),
                    IngredientLocalModel("Alho", "2 dentes"),
                    IngredientLocalModel("Amido de Milho", "1 col. sopa"),
                    IngredientLocalModel("Brócolis", "200g")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        ),
        RecipeEntity(
            id = "52959",
            name = "Salmão Assado com Funcho e Tomates",
            category = "Frutos do Mar",
            area = "Britânica",
            instructions = "Aqueça o forno a 180°C. Disponha o funcho fatiado, os tomates-cereja, o alho e o azeite em uma assadeira.\nAsse por 15 minutos até os legumes ficarem macios.\nAcomode os filés de salmão sobre os legumes e tempere com raspas de limão, sal marinho e pimenta-do-reino.\nAsse por mais 12 minutos até o salmão lascar com facilidade e finalize com endro fresco.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/1548772327.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Frutos do Mar,Saudável",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Filés de Salmão", "2 filés"),
                    IngredientLocalModel("Bulbo de Funcho", "1 grande"),
                    IngredientLocalModel("Tomates-Cereja", "250g"),
                    IngredientLocalModel("Azeite de Oliva", "2 col. sopa"),
                    IngredientLocalModel("Limão", "1 unidade"),
                    IngredientLocalModel("Endro Fresco", "1 maço")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        ),
        RecipeEntity(
            id = "52844",
            name = "Lasanha Clássica à Bolonhesa",
            category = "Massas",
            area = "Italiana",
            instructions = "Aqueça o azeite em uma panela larga e refogue a cebola, o salsão e o alho por 8 minutos.\nAdicione a carne moída, doure bem e junte o extrato de tomate, a passata e o orégano.\nCozinhe em fogo brando por 35 minutos até o molho encorpar.\nIntercale camadas de molho à bolonhesa, massa de lasanha e molho branco, finalize com muçarela e asse a 190°C por 40 minutos.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/wtsvxx1511296896.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Massa,Clássico",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Massa de Lasanha", "12 folhas"),
                    IngredientLocalModel("Carne Moída", "500g"),
                    IngredientLocalModel("Passata de Tomate", "500ml"),
                    IngredientLocalModel("Cebola", "1 grande"),
                    IngredientLocalModel("Muçarela", "150g"),
                    IngredientLocalModel("Azeite de Oliva", "2 col. sopa")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        ),
        RecipeEntity(
            id = "52893",
            name = "Crumble de Maçã e Amora",
            category = "Sobremesas",
            area = "Britânica",
            instructions = "Preaqueça o forno a 190°C. Misture as maçãs fatiadas e as amoras com açúcar demerara e canela em um refratário.\nCom as pontas dos dedos, misture a manteiga gelada à farinha de trigo e à aveia até formar uma farofa úmida.\nEspalhe a farofa sobre as frutas e asse por 35 minutos até dourar e borbulhar.",
            thumbnailUrl = "https://www.themealdb.com/images/media/meals/xvsurr1511719182.jpg",
            youtubeUrl = null,
            sourceUrl = null,
            tagsCsv = "Doce,Sobremesa,Frutas",
            ingredientsJson = RecipeMappers.encodeIngredients(
                listOf(
                    IngredientLocalModel("Maçãs", "4 unidades"),
                    IngredientLocalModel("Amoras", "150g"),
                    IngredientLocalModel("Farinha de Trigo", "175g"),
                    IngredientLocalModel("Manteiga Gelada", "100g"),
                    IngredientLocalModel("Açúcar Demerara", "85g"),
                    IngredientLocalModel("Aveia em Flocos", "50g")
                )
            ),
            isFavorite = false,
            isDetailLoaded = true,
            lastSyncedAt = nowMillis
        )
    )
}
