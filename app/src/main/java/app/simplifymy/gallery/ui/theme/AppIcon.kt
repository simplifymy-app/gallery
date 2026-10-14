package app.simplifymy.gallery.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import app.simplifymy.gallery.R

enum class AppIcon(@StringRes val labelRes: Int, val background: Color) {
    Carbon(R.string.icon_carbon, Color(0xFF161616)),
    Blue(R.string.icon_blue, Color(0xFF0F62FE)),
    Purple(R.string.icon_purple, Color(0xFF8A3FFC)),
    Magenta(R.string.icon_magenta, Color(0xFFD02670)),
    Teal(R.string.icon_teal, Color(0xFF009D9A)),
    Green(R.string.icon_green, Color(0xFF198038)),
    ;

    val enabledByDefault: Boolean get() = this == Carbon
}
