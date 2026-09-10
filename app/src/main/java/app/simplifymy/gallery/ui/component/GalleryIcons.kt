package app.simplifymy.gallery.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun icon(pathData: String, filled: Boolean = false): ImageVector =
    ImageVector.Builder(
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = if (filled) null else SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

object GalleryIcons {
    val Search = icon("M9 3a6 6 0 1 0 0 12a6 6 0 1 0 0-12M13.5 13.5L17 17")
    val Back = icon("M12 4L6 10L12 16")
    val SortDescending = icon("M10 3.75v12.5M5.625 11.875L10 16.25l4.375-4.375")
    val Pin = icon("M7.5 2.5h5l-.75 5.25 3.25 2.875H5l3.25-2.875zM10 10.625V17.5")
    val Check = icon("M4.375 10.625l3.75 3.75L15.625 5.625")
    val Chevron = icon("M6 8l4 4 4-4")
    val Pause = icon("M7 4.5v11M13 4.5v11")
    val Hide = icon("M3 10s2.8-4.5 7-4.5 7 4.5 7 4.5-2.8 4.5-7 4.5S3 10 3 10zM4 4l12 12")
    val Fit = icon("M3 7V4h3M17 7V4h-3M3 13v3h3M17 13v3h-3")
    val VolumeOn = icon("M4 7.5h3l4-3v11l-4-3H4zM13.5 7a4 4 0 0 1 0 6")
    val VolumeOff = icon("M4 7.5h3l4-3v11l-4-3H4zM13 8l4 4M17 8l-4 4")
    val Close = icon("M5 5l10 10M15 5L5 15")
    val Info = icon("M10 2.8a7.2 7.2 0 1 0 0 14.4a7.2 7.2 0 1 0 0-14.4M10 9v5M10 6.3v.6")
    val Share = icon("M10 13V3M6.5 6.5L10 3l3.5 3.5M4 12v4h12v-4")
    val Edit = icon("M13.4 3.6l3 3L7 16H4v-3z")
    val SetAs = icon("M10 3.5v9M6.8 9.3L10 12.5l3.2-3.2M3.2 3.5h13.6v13H3.2z")
    val Crop = icon("M5.5 1.5v13h13M1.5 5.5h13v13")
    val Adjust = icon(
        "M3 6h9M15.5 6H17M3 14h3M9.5 14H17" +
            "M13.7 6m-1.8 0a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0" +
            "M7.7 14m-1.8 0a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0"
    )
    val Filter = icon(
        "M7.6 7.6m-4.6 0a4.6 4.6 0 1 0 9.2 0a4.6 4.6 0 1 0 -9.2 0" +
            "M12.4 12.4m-4.6 0a4.6 4.6 0 1 0 9.2 0a4.6 4.6 0 1 0 -9.2 0"
    )
    val Draw = icon("M13.4 3.6l3 3L7 16H4v-3zM11.6 5.4l3 3")
    val Reset = icon("M4 10a6 6 0 1 0 1.9-4.4M3.4 3.6v3.4h3.4")
    val RotateLeft = icon("M4.2 10a5.8 5.8 0 1 0 1.8-4.2M3.6 4v3.4h3.4")
    val RotateRight = icon("M15.8 10a5.8 5.8 0 1 1-1.8-4.2M16.4 4v3.4h-3.4")
    val Mirror = icon("M10 2.5v15M7.2 6.8L4 10l3.2 3.2M12.8 6.8L16 10l-3.2 3.2")
    val Play = icon("M7 4.5l8 5.5-8 5.5z", filled = true)
    val Trash = icon("M3 6h14M8 6V4h4v2M5.5 6l.8 10.5h7.4L14.5 6")
    val Heart = icon("M10 16.8S2.6 12.4 2.6 7.5A3.9 3.9 0 0 1 10 5.4a3.9 3.9 0 0 1 7.4 2.1c0 4.9-7.4 9.3-7.4 9.3z")
    val More = icon(
        "M10 2.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8" +
            "M10 8.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8" +
            "M10 14.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8",
        filled = true,
    )
}
