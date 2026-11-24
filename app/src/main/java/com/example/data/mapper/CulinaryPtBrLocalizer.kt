package com.example.data.mapper

/**
 * Traduz e normaliza categorias, origens culinárias, tags, unidades de medida
 * e ingredientes frequentes retornados pela API pública TheMealDB para Português (PT-BR).
 */
object CulinaryPtBrLocalizer {

    private val categoryToPt = mapOf(
        "beef" to "Carnes",
        "chicken" to "Frango",
        "dessert" to "Sobremesas",
        "lamb" to "Cordeiro",
        "miscellaneous" to "Variados",
        "pasta" to "Massas",
        "pork" to "Suínos",
        "seafood" to "Frutos do Mar",
        "side" to "Acompanhamentos",
        "starter" to "Entradas",
        "vegan" to "Vegano",
        "vegetarian" to "Vegetariano",
        "breakfast" to "Café da Manhã",
        "goat" to "Caprino"
    )

    private val ptToApiCategory = mapOf(
        "carnes" to "Beef",
        "frango" to "Chicken",
        "sobremesas" to "Dessert",
        "cordeiro" to "Lamb",
        "variados" to "Miscellaneous",
        "massas" to "Pasta",
        "suínos" to "Pork",
        "suinos" to "Pork",
        "frutos do mar" to "Seafood",
        "acompanhamentos" to "Side",
        "entradas" to "Starter",
        "vegano" to "Vegan",
        "vegetariano" to "Vegetarian",
        "café da manhã" to "Breakfast",
        "cafe da manha" to "Breakfast",
        "caprino" to "Goat"
    )

    private val areaToPt = mapOf(
        "american" to "Americana",
        "british" to "Britânica",
        "canadian" to "Canadense",
        "chinese" to "Chinesa",
        "croatian" to "Croata",
        "dutch" to "Holandesa",
        "egyptian" to "Egípcia",
        "filipino" to "Filipina",
        "french" to "Francesa",
        "greek" to "Grega",
        "indian" to "Indiana",
        "irish" to "Irlandesa",
        "italian" to "Italiana",
        "jamaican" to "Jamaicana",
        "japanese" to "Japonesa",
        "kenyan" to "Queniana",
        "malaysian" to "Malaia",
        "mexican" to "Mexicana",
        "moroccan" to "Marroquina",
        "norwegian" to "Norueguesa",
        "polish" to "Polonesa",
        "portuguese" to "Portuguesa",
        "russian" to "Russa",
        "spanish" to "Espanhola",
        "thai" to "Tailandesa",
        "tunisian" to "Tunisiana",
        "turkish" to "Turca",
        "ukrainian" to "Ucraniana",
        "vietnamese" to "Vietnamita",
        "brazilian" to "Brasileira",
        "unknown" to "Internacional"
    )

    private val tagToPt = mapOf(
        "pasta" to "Massa",
        "spicy" to "Picante",
        "meat" to "Carne",
        "casserole" to "Assado",
        "seafood" to "Frutos do Mar",
        "healthy" to "Saudável",
        "classic" to "Clássico",
        "sweet" to "Doce",
        "dessert" to "Sobremesa",
        "fruit" to "Frutas",
        "cake" to "Bolo",
        "baking" to "Forno",
        "soup" to "Sopa",
        "breakfast" to "Café da Manhã",
        "vegetarian" to "Vegetariano",
        "vegan" to "Vegano",
        "sidedish" to "Acompanhamento",
        "snack" to "Lanche",
        "tart" to "Torta",
        "pie" to "Torta",
        "stew" to "Ensopado",
        "fish" to "Peixe",
        "chicken" to "Frango",
        "bun" to "Pão",
        "savory" to "Salgado",
        "alcohol" to "Com Álcool"
    )

    private val commonIngredientsToPt = mapOf(
        "olive oil" to "Azeite de Oliva",
        "extra virgin olive oil" to "Azeite Extravirgem",
        "vegetable oil" to "Óleo Vegetal",
        "garlic" to "Alho",
        "garlic cloves" to "Dentes de Alho",
        "onion" to "Cebola",
        "onions" to "Cebolas",
        "red onion" to "Cebola Roxa",
        "salt" to "Sal",
        "sea salt" to "Sal Marinho",
        "kosher salt" to "Sal Grosso",
        "black pepper" to "Pimenta-do-reino",
        "pepper" to "Pimenta",
        "butter" to "Manteiga",
        "unsalted butter" to "Manteiga sem Sal",
        "cold butter" to "Manteiga Gelada",
        "sugar" to "Açúcar",
        "caster sugar" to "Açúcar Refinado",
        "brown sugar" to "Açúcar Mascavo",
        "demerara sugar" to "Açúcar Demerara",
        "flour" to "Farinha de Trigo",
        "plain flour" to "Farinha de Trigo",
        "all-purpose flour" to "Farinha de Trigo",
        "cornstarch" to "Amido de Milho",
        "milk" to "Leite",
        "water" to "Água",
        "egg" to "Ovo",
        "eggs" to "Ovos",
        "egg yolks" to "Gemas",
        "chicken" to "Frango",
        "chicken breast" to "Peito de Frango",
        "chicken breasts" to "Peitos de Frango",
        "beef" to "Carne Bovina",
        "minced beef" to "Carne Moída",
        "tomato" to "Tomate",
        "tomatoes" to "Tomates",
        "chopped tomatoes" to "Tomates Picados",
        "cherry tomatoes" to "Tomates-Cereja",
        "tomato puree" to "Extrato de Tomate",
        "passata" to "Passata de Tomate",
        "lemon" to "Limão",
        "lemon juice" to "Suco de Limão",
        "lime" to "Limão Taiti",
        "soy sauce" to "Molho Shoyu",
        "mozzarella" to "Muçarela",
        "parmesan" to "Queijo Parmesão",
        "parmesan cheese" to "Queijo Parmesão",
        "parmigiano-reggiano" to "Parmesão",
        "cheddar cheese" to "Queijo Cheddar",
        "cheese" to "Queijo",
        "cream" to "Creme de Leite",
        "heavy cream" to "Creme de Leite Fresco",
        "double cream" to "Creme de Leite Fresco",
        "fresh basil" to "Manjericão Fresco",
        "basil" to "Manjericão",
        "parsley" to "Salsinha",
        "fresh parsley" to "Salsinha Fresca",
        "coriander" to "Coentro",
        "cilantro" to "Coentro",
        "fresh dill" to "Endro Fresco",
        "rosemary" to "Alecrim",
        "thyme" to "Tomilho",
        "oregano" to "Orégano",
        "ground ginger" to "Gengibre em Pó",
        "ginger" to "Gengibre",
        "cinnamon" to "Canela",
        "vanilla extract" to "Essência de Baunilha",
        "honey" to "Mel",
        "broccoli" to "Brócolis",
        "rice" to "Arroz",
        "carrot" to "Cenoura",
        "carrots" to "Cenouras",
        "potato" to "Batata",
        "potatoes" to "Batatas",
        "penne rigate" to "Macarrão Penne",
        "spaghetti" to "Espaguete",
        "lasagne sheets" to "Massa de Lasanha",
        "salmon" to "Salmão",
        "salmon fillets" to "Filés de Salmão",
        "red chilli flakes" to "Pimenta Calabresa",
        "rolled oats" to "Aveia em Flocos",
        "blackberries" to "Amoras",
        "cooking apples" to "Maçãs"
    )

    private val measureReplacements = listOf(
        Regex("\\btablespoons\\b", RegexOption.IGNORE_CASE) to "col. sopa",
        Regex("\\btablespoon\\b", RegexOption.IGNORE_CASE) to "col. sopa",
        Regex("\\btbsp\\b", RegexOption.IGNORE_CASE) to "col. sopa",
        Regex("\\btbs\\b", RegexOption.IGNORE_CASE) to "col. sopa",
        Regex("\\bteaspoons\\b", RegexOption.IGNORE_CASE) to "col. chá",
        Regex("\\bteaspoon\\b", RegexOption.IGNORE_CASE) to "col. chá",
        Regex("\\btsp\\b", RegexOption.IGNORE_CASE) to "col. chá",
        Regex("\\bcups\\b", RegexOption.IGNORE_CASE) to "xícaras",
        Regex("\\bcup\\b", RegexOption.IGNORE_CASE) to "xícara",
        Regex("\\bcloves\\b", RegexOption.IGNORE_CASE) to "dentes",
        Regex("\\bclove\\b", RegexOption.IGNORE_CASE) to "dente",
        Regex("\\bhandfuls\\b", RegexOption.IGNORE_CASE) to "punhados",
        Regex("\\bhandful\\b", RegexOption.IGNORE_CASE) to "punhado",
        Regex("\\bbunches\\b", RegexOption.IGNORE_CASE) to "maços",
        Regex("\\bbunch\\b", RegexOption.IGNORE_CASE) to "maço",
        Regex("\\bfillets\\b", RegexOption.IGNORE_CASE) to "filés",
        Regex("\\bfillet\\b", RegexOption.IGNORE_CASE) to "filé",
        Regex("\\bsheets\\b", RegexOption.IGNORE_CASE) to "folhas",
        Regex("\\bslices\\b", RegexOption.IGNORE_CASE) to "fatias",
        Regex("\\bslice\\b", RegexOption.IGNORE_CASE) to "fatia",
        Regex("\\bto taste\\b", RegexOption.IGNORE_CASE) to "a gosto",
        Regex("\\bpinch\\b", RegexOption.IGNORE_CASE) to "pitada",
        Regex("\\blarge\\b", RegexOption.IGNORE_CASE) to "grande",
        Regex("\\bmedium\\b", RegexOption.IGNORE_CASE) to "médio",
        Regex("\\bsmall\\b", RegexOption.IGNORE_CASE) to "pequeno",
        Regex("\\bcans\\b", RegexOption.IGNORE_CASE) to "latas",
        Regex("\\bcan\\b", RegexOption.IGNORE_CASE) to "lata",
        Regex("\\bfinely chopped\\b", RegexOption.IGNORE_CASE) to "picado fino",
        Regex("\\bchopped\\b", RegexOption.IGNORE_CASE) to "picado",
        Regex("\\bgrated\\b", RegexOption.IGNORE_CASE) to "ralado",
        Regex("\\bmelted\\b", RegexOption.IGNORE_CASE) to "derretida"
    )

    fun translateCategory(rawCategory: String): String {
        val trimmed = rawCategory.trim()
        if (trimmed.isBlank()) return "Geral"
        return categoryToPt[trimmed.lowercase()] ?: trimmed
    }

    fun toApiCategoryKey(category: String): String {
        val trimmed = category.trim()
        return ptToApiCategory[trimmed.lowercase()] ?: trimmed
    }

    fun translateArea(rawArea: String): String {
        val trimmed = rawArea.trim()
        if (trimmed.isBlank()) return "Internacional"
        return areaToPt[trimmed.lowercase()] ?: trimmed
    }

    fun translateTag(rawTag: String): String {
        val trimmed = rawTag.trim()
        return tagToPt[trimmed.lowercase()] ?: trimmed
    }

    fun translateIngredient(rawName: String): String {
        val trimmed = rawName.trim()
        return commonIngredientsToPt[trimmed.lowercase()] ?: trimmed
    }

    fun translateMeasure(rawMeasure: String): String {
        var result = rawMeasure.trim()
        if (result.isBlank()) return ""
        for ((pattern, replacement) in measureReplacements) {
            result = pattern.replace(result, replacement)
        }
        return result
    }
}
