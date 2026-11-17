package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val ChefLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = OnTerracotta,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = HerbSageSecondary,
    onSecondary = OnHerbSage,
    secondaryContainer = HerbSageContainer,
    onSecondaryContainer = OnHerbSageContainer,
    tertiary = SaffronTertiary,
    onTertiary = OnSaffron,
    tertiaryContainer = SaffronContainer,
    onTertiaryContainer = OnSaffronContainer,
    background = WarmCreamBackground,
    onBackground = EspressoOnBackground,
    surface = WarmParchmentSurface,
    onSurface = EspressoOnBackground,
    surfaceVariant = WarmSurfaceContainer,
    onSurfaceVariant = WarmOutline,
    surfaceContainerLow = WarmSurfaceContainerLow,
    surfaceContainer = WarmSurfaceContainer,
    surfaceContainerHigh = WarmSurfaceContainerHigh,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant
)

private val ChefDarkColorScheme = darkColorScheme(
    primary = DarkTerracotta,
    onPrimary = DarkOnTerracotta,
    primaryContainer = DarkTerracottaContainer,
    onPrimaryContainer = DarkOnTerracottaContainer,
    secondary = DarkHerbSage,
    onSecondary = DarkOnHerbSage,
    secondaryContainer = DarkHerbSageContainer,
    onSecondaryContainer = DarkOnHerbSageContainer,
    tertiary = DarkSaffron,
    onTertiary = DarkOnSaffron,
    tertiaryContainer = DarkSaffronContainer,
    onTertiaryContainer = DarkOnSaffronContainer,
    background = DarkEspressoBackground,
    onBackground = DarkCreamOnBackground,
    surface = DarkEspressoBackground,
    onSurface = DarkCreamOnBackground,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkOutline,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

val ChefExpressShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ChefExpressTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ChefDarkColorScheme
        else -> ChefLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChefExpressTypography,
        shapes = ChefExpressShapes,
        content = content
    )
}
