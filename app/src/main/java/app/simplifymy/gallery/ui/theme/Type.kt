package app.simplifymy.gallery.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.simplifymy.gallery.R

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun family(resId: Int) =
    FontFamily(variable(resId, 400), variable(resId, 500), variable(resId, 600), variable(resId, 700))

enum class GalleryFont(val label: String, @StringRes val labelRes: Int? = null) {
    SpaceGrotesk("Space Grotesk"),
    Outfit("Outfit"),
    PlexSans("IBM Plex Sans"),
    System("System", R.string.follow_system),
    ;

    val family: FontFamily by lazy {
        when (this) {
            SpaceGrotesk -> family(R.font.space_grotesk)
            Outfit -> family(R.font.outfit)
            PlexSans -> family(R.font.ibm_plex_sans)
            System -> FontFamily.SansSerif
        }
    }
}

enum class TextScale(val label: String, val factor: Float) {
    Small("S", 0.9f),
    Default("M", 1f),
    Large("L", 1.12f),
    Huge("XL", 1.26f),
}

private val Mono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

@Immutable
data class GalleryType(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val meta: TextStyle,
    val section: TextStyle,
)

fun galleryType(font: GalleryFont, scale: TextScale): GalleryType {
    val ui = font.family
    val f = scale.factor
    return GalleryType(
        display = TextStyle(
            fontFamily = ui,
            fontSize = (27 * f).sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.03).em,
        ),
        title = TextStyle(
            fontFamily = ui,
            fontSize = (19 * f).sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).em,
        ),
        body = TextStyle(fontFamily = ui, fontSize = (13.5f * f).sp, fontWeight = FontWeight.Normal),
        label = TextStyle(fontFamily = ui, fontSize = (13 * f).sp, fontWeight = FontWeight.Medium),
        meta = TextStyle(fontFamily = Mono, fontSize = (11 * f).sp, fontWeight = FontWeight.Normal),
        section = TextStyle(
            fontFamily = Mono,
            fontSize = (10.5f * f).sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.09.em,
        ),
    )
}
