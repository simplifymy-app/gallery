package app.simplifymy.gallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark, ThemeMode.Black -> true
}

private val LocalColors = staticCompositionLocalOf<GalleryColors> { error("No GalleryTheme") }
private val LocalType = staticCompositionLocalOf { galleryType(GalleryFont.SpaceGrotesk, TextScale.Default) }
private val LocalCorners = staticCompositionLocalOf { CornerStyle.Default }

object GalleryTheme {
    val colors: GalleryColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: GalleryType
        @Composable @ReadOnlyComposable get() = LocalType.current
    val corners: CornerStyle
        @Composable @ReadOnlyComposable get() = LocalCorners.current
}

@Composable
fun GalleryTheme(
    mode: ThemeMode = ThemeMode.System,
    accent: Color = Accent.Indigo.color,
    contrast: Contrast = Contrast.Normal,
    corners: CornerStyle = CornerStyle.Default,
    font: GalleryFont = GalleryFont.SpaceGrotesk,
    textScale: TextScale = TextScale.Default,
    content: @Composable () -> Unit,
) {
    val dark = mode.isDark()
    val colors = remember(dark, mode, accent, contrast) {
        galleryColors(dark, mode == ThemeMode.Black, accent, contrast)
    }
    val type = remember(font, textScale) { galleryType(font, textScale) }

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalType provides type,
        LocalCorners provides corners,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(dark),
            typography = type.toMaterial(),
            content = content,
        )
    }
}

private fun GalleryColors.toMaterial(dark: Boolean) = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = accent,
    onPrimary = onAccent,
    background = background,
    onBackground = text,
    surface = surface,
    onSurface = text,
    surfaceVariant = surfaceAlt,
    onSurfaceVariant = textMuted,
    outline = hairline,
    outlineVariant = hairline,
)

private fun GalleryType.toMaterial() = Typography(
    titleLarge = title,
    titleMedium = title,
    bodyMedium = body,
    bodySmall = meta,
    labelMedium = label,
    labelSmall = section,
)
