package app.simplifymy.gallery.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.simplifymy.gallery.R

enum class ThemeMode(@StringRes val labelRes: Int) {
    System(R.string.mode_auto),
    Light(R.string.mode_light),
    Dark(R.string.mode_dark),
    Black(R.string.mode_black),
}

enum class Contrast(@StringRes val labelRes: Int, val shift: Float) {
    Soft(R.string.contrast_soft, -0.12f),
    Normal(R.string.contrast_normal, 0f),
    High(R.string.contrast_high, 0.16f),
}

enum class Accent(val color: Color) {
    Indigo(Color(0xFF6E8BFF)),
    Amber(Color(0xFFE0A458)),
    Jade(Color(0xFF5FBF8F)),
    Rose(Color(0xFFD96A8B)),
    Violet(Color(0xFFA98BFF)),
}

val ViewerBackground = Color.Black

@Immutable
data class GalleryColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val hairline: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,
    val onAccent: Color,
)

private val Dark = GalleryColors(
    background = Color(0xFF0B0B0C),
    surface = Color(0xFF141416),
    surfaceAlt = Color(0xFF1C1C1F),
    hairline = Color(0xFF2A2A2E),
    text = Color(0xFFEDEDEF),
    textMuted = Color(0xFF8A8A92),
    textFaint = Color(0xFF5A5A62),
    accent = Accent.Indigo.color,
    onAccent = Color(0xFF0B0B0C),
)

private val Black = Dark.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0C0C0D),
    surfaceAlt = Color(0xFF161618),
    hairline = Color(0xFF232326),
)

private val Light = GalleryColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F6),
    surfaceAlt = Color(0xFFEDEDEF),
    hairline = Color(0xFFE3E3E6),
    text = Color(0xFF131315),
    textMuted = Color(0xFF6E6E76),
    textFaint = Color(0xFF9A9AA2),
    accent = Accent.Indigo.color,
    onAccent = Color(0xFF0B0B0C),
)

fun galleryColors(
    dark: Boolean,
    black: Boolean,
    accent: Color,
    contrast: Contrast,
): GalleryColors {
    val base = if (dark) (if (black) Black else Dark) else Light
    return base.copy(
        text = base.text.contrasted(base.background, contrast),
        textMuted = base.textMuted.contrasted(base.background, contrast),
        textFaint = base.textFaint.contrasted(base.background, contrast),
        accent = accent,
        onAccent = if (accent.luminance() > 0.5f) Color(0xFF0B0B0C) else Color(0xFFFFFFFF),
    )
}

private fun Color.contrasted(background: Color, contrast: Contrast): Color {
    if (contrast == Contrast.Normal) return this
    val target = if (background.luminance() < 0.5f) 1f else 0f
    val amount = contrast.shift
    return Color(
        red = (red + (target - red) * amount).coerceIn(0f, 1f),
        green = (green + (target - green) * amount).coerceIn(0f, 1f),
        blue = (blue + (target - blue) * amount).coerceIn(0f, 1f),
        alpha = alpha,
    )
}
