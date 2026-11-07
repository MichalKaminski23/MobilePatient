package pl.polsl.mobilepatient.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkNavyPrimary,
    onPrimary = DarkNavyOnPrimary,
    primaryContainer = DarkNavyPrimaryContainer,
    onPrimaryContainer = DarkNavyOnPrimaryContainer,
    secondary = DarkNavySecondary,
    onSecondary = DarkNavyOnSecondary,
    secondaryContainer = DarkNavySecondaryContainer,
    onSecondaryContainer = DarkNavyOnSecondaryContainer,
    tertiary = DarkNavyTertiary,
    onTertiary = DarkNavyOnTertiary,
    tertiaryContainer = DarkNavyTertiaryContainer,
    onTertiaryContainer = DarkNavyOnTertiaryContainer,
    background = DarkNavyBackground,
    onBackground = DarkNavyOnBackground,
    surface = DarkNavySurface,
    onSurface = DarkNavyOnSurface,
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = DarkNavyOnSurfaceVariant,
    outline = DarkNavyOutline
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = NavyOnPrimary,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = NavyOnPrimaryContainer,
    secondary = NavySecondary,
    onSecondary = NavyOnSecondary,
    secondaryContainer = NavySecondaryContainer,
    onSecondaryContainer = NavyOnSecondaryContainer,
    tertiary = NavyTertiary,
    onTertiary = NavyOnTertiary,
    tertiaryContainer = NavyTertiaryContainer,
    onTertiaryContainer = NavyOnTertiaryContainer,
    background = NavyBackground,
    onBackground = NavyOnBackground,
    surface = NavySurface,
    onSurface = NavyOnSurface,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = NavyOnSurfaceVariant,
    outline = NavyOutline
)

@Composable
fun MobilePatientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}