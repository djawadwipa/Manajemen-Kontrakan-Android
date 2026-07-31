package id.djawadwipa.manajemenkontrakan.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Cream,
    secondary = GoldDark,
    tertiary = Teal,
    background = Cream,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE7EAF0),
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Navy,
    secondary = TealLight,
    tertiary = Gold,
    background = Navy,
    surface = SurfaceDark,
    surfaceVariant = NavyLight,
)

@Composable
fun ManajemenKontrakanTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(
                window,
                view,
            ).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
