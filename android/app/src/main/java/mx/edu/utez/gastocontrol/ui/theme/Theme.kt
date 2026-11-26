
package mx.edu.utez.gastocontrol.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GastoControlColors = lightColorScheme(
    primary = GCBlue,
    onPrimary = GCSurface,
    primaryContainer = GCBlueLight,
    onPrimaryContainer = GCBlueDark,

    secondary = GCTextSecondary,
    onSecondary = GCSurface,

    background = GCBackground,
    onBackground = GCTextPrimary,

    surface = GCSurface,
    onSurface = GCTextPrimary,

    surfaceVariant = GCSurfaceVariant,
    onSurfaceVariant = GCTextSecondary,

    outline = GCBorder,

    error = GCExpense,
    onError = GCSurface,
    errorContainer = GCExpenseLight
)

@Composable
fun GastoControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GastoControlColors,
        typography = Typography,
        content = content
    )
}
