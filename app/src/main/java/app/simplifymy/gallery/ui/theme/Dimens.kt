package app.simplifymy.gallery.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.unit.Dp
import app.simplifymy.gallery.R
import androidx.compose.ui.unit.dp

object Spacing {
    val gridGutter = 3.dp
    val base = 8.dp
    val folderGutter = 12.dp
    val screen = 20.dp
    val hitTarget = 44.dp
}

/** Applies to media thumbnails only; controls keep [ControlRadius]. */
enum class CornerStyle(val radius: Dp) {
    Sharp(2.dp),
    Default(4.dp),
    Soft(6.dp),
    Round(14.dp),
    ;

    val label: String get() = radius.value.toInt().toString()
}

val ControlRadius = 4.dp

enum class ThumbnailSpacing(@StringRes val labelRes: Int, val gap: Dp) {
    None(R.string.spacing_none, 0.dp),
    Small(R.string.spacing_small, 4.dp),
    Large(R.string.spacing_large, 12.dp),
}

enum class DetailsPlacement(@StringRes val labelRes: Int) {
    Below(R.string.placement_below),
    Overlay(R.string.placement_overlay),
}
