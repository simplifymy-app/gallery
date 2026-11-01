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
    val Panorama = icon("M10 3a7 7 0 1 0 0 14a7 7 0 1 0 0-14M3 10a7 2.6 0 0 0 14 0M10 3a2.6 7 0 0 0 0 14")
    val Info = icon("M10 2.8a7.2 7.2 0 1 0 0 14.4a7.2 7.2 0 1 0 0-14.4M10 9v5M10 6.3v.6")
    val Share = icon("M10 13V3M6.5 6.5L10 3l3.5 3.5M4 12v4h12v-4")
    val Edit = icon("M13.4 3.6l3 3L7 16H4v-3z")
    val SetAs = icon("M10 3.5v9M6.8 9.3L10 12.5l3.2-3.2M4.8 3.5h10.4a1.6 1.6 0 0 1 1.6 1.6v9.8a1.6 1.6 0 0 1 -1.6 1.6h-10.4a1.6 1.6 0 0 1 -1.6 -1.6v-9.8a1.6 1.6 0 0 1 1.6 -1.6z")
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
    val Markup = icon("M12.6 3.4l3 3-8 8H4.6v-3zM3 17.2c1.5-1.2 3 .8 4.5 0s3-1.2 4.5 0")
    val Pen = icon("M7 2.8h6l1.8 6.7L10 17.5 5.2 9.5zM10 17.5v-6.3M10 10.2m-1 0a1 1 0 1 0 2 0a1 1 0 1 0 -2 0")
    val Pencil = icon("M14 2.8l3.2 3.2-9.7 9.7-4.4 1.2 1.2-4.4zM12 4.8l3.2 3.2M4.3 12.5l3.2 3.2")
    val Marker = icon("M12 3l5 5-6.5 6.5-5-5zM5.5 9.5L3.5 16.5l7-2")
    val Highlighter = icon("M12.5 2.5l4 4-6 6-4-4zM6.5 8.5L5 13l2 2 4.5-1.5M3 17.5h14")
    val Line = icon("M4 16L16 4")
    val Arrow = icon("M4 16L16 4M9.5 4H16v6.5")
    val Rectangle = icon("M3.5 5h13v10h-13z")
    val Ellipse = icon("M3 10a7 5.5 0 1 0 14 0a7 5.5 0 1 0 -14 0")
    val Eraser = icon("M11.5 3.5l5 5-7.5 7.5H6l-2.5-2.5zM7.5 7.5l5 5M9 16h7.5")
    val Undo = icon("M7 4.5L3.5 8L7 11.5M3.5 8h8.25a4.25 4.25 0 0 1 0 8.5H9")
    val Redo = icon("M13 4.5L16.5 8L13 11.5M16.5 8H8.25a4.25 4.25 0 0 0 0 8.5H11")
    val Reset = icon("M4 10a6 6 0 1 0 1.9-4.4M3.4 3.6v3.4h3.4")
    val RotateLeft = icon("M4.2 10a5.8 5.8 0 1 0 1.8-4.2M3.6 4v3.4h3.4")
    val RotateRight = icon("M15.8 10a5.8 5.8 0 1 1-1.8-4.2M16.4 4v3.4h-3.4")
    val Mirror = icon("M10 2.5v15M7.2 6.8L4 10l3.2 3.2M12.8 6.8L16 10l-3.2 3.2")
    val FlipVertical = icon("M2.5 10h15M6.8 7.2L10 4l3.2 3.2M6.8 12.8L10 16l3.2-3.2")
    val Straighten = icon("M3 15h14M3 15L15.5 7.5M9.5 15a6.5 6.5 0 0 0-.93-3.34")
    val Perspective = icon("M6.5 4h7l3.5 12h-14zM10 4v12")
    val Exposure = icon("M4.5 3h11a1.5 1.5 0 0 1 1.5 1.5v11a1.5 1.5 0 0 1 -1.5 1.5h-11a1.5 1.5 0 0 1 -1.5 -1.5v-11a1.5 1.5 0 0 1 1.5 -1.5zM3.6 16.4L16.4 3.6M5.5 7.5h3.5M7.25 5.75v3.5M11 12.5h3.5")
    val Brightness = icon("M10 7a3 3 0 1 0 0 6a3 3 0 1 0 0 -6M10 2.5v2M10 15.5v2M2.5 10h2M15.5 10h2M4.7 4.7l1.4 1.4M13.9 13.9l1.4 1.4M4.7 15.3l1.4-1.4M13.9 6.1l1.4-1.4")
    val Contrast = icon("M10 2.8a7.2 7.2 0 1 0 0 14.4a7.2 7.2 0 1 0 0 -14.4M10 2.8v14.4M12.6 6v8")
    val Highlights = icon("M10 2.8a7.2 7.2 0 1 0 0 14.4a7.2 7.2 0 1 0 0 -14.4M5.28 6.2h9.43M4.3 8.8h11.4")
    val Shadows = icon("M10 2.8a7.2 7.2 0 1 0 0 14.4a7.2 7.2 0 1 0 0 -14.4M4.3 11.2h11.4M5.28 13.8h9.43")
    val Whites = icon("M10 2.8a7.2 7.2 0 1 0 0 14.4a7.2 7.2 0 1 0 0 -14.4M10 7a3 3 0 1 0 0 6a3 3 0 1 0 0 -6")
    val Blacks = icon("M10 2.8a7.2 7.2 0 1 0 0 14.4a7.2 7.2 0 1 0 0 -14.4M10 7a3 3 0 1 0 0 6a3 3 0 1 0 0 -6M7.9 12.1l4.2-4.2")
    val Saturation = icon("M10 3s-5 5.6-5 9a5 5 0 0 0 10 0c0-3.4-5-9-5-9zM7.5 12.5a2.5 2.5 0 0 0 2.5 2.5")
    val Vibrance = icon("M8.5 4.5s-4.5 5-4.5 8a4.5 4.5 0 0 0 9 0c0-3-4.5-8-4.5-8zM15.5 3v4M13.5 5h4")
    val Temperature = icon("M8.5 4a1.5 1.5 0 0 1 3 0v7.4a3 3 0 1 1-3 0zM10 8v5M14 5h2.5M14 8h2.5")
    val Tint = icon("M10 3s-5 5.6-5 9a5 5 0 0 0 10 0c0-3.4-5-9-5-9zM5.4 12h9.2")
    val Fade = icon("M4.5 4h11a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1 -1.5 1.5h-11a1.5 1.5 0 0 1 -1.5 -1.5v-9a1.5 1.5 0 0 1 1.5 -1.5zM6.5 16v-7M10 16v-4.5M13.5 16v-2")
    val Clarity = icon("M10 3.5l7 12.5H3zM10 8.5v4")
    val Sharpen = icon("M10 3.5l6 11H4zM10 8l3 5.5H7z")
    val Glow = icon("M10 6.8a3.2 3.2 0 1 0 0 6.4a3.2 3.2 0 1 0 0 -6.4M16.26 12.66A6.8 6.8 0 0 1 12.66 16.26M7.34 16.26A6.8 6.8 0 0 1 3.74 12.66M3.74 7.34A6.8 6.8 0 0 1 7.34 3.74M12.66 3.74A6.8 6.8 0 0 1 16.26 7.34")
    val Grain = icon("M5.5 5.5h.01M10 5.5h.01M14.5 5.5h.01M7.75 10h.01M12.25 10h.01M5.5 14.5h.01M10 14.5h.01M14.5 14.5h.01")
    val Vignette = icon("M4.3 4h11.4a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1 -1.5 1.5h-11.4a1.5 1.5 0 0 1 -1.5 -1.5v-9a1.5 1.5 0 0 1 1.5 -1.5zM10 6.8c2.8 0 4.4 1.4 4.4 3.2s-1.6 3.2-4.4 3.2-4.4-1.4-4.4-3.2 1.6-3.2 4.4-3.2z")
    val Curves = icon("M3.5 3.5v13h13M5.5 14.5C9 14.5 9 6 16 5")
    val Hsl = icon("M10 3.5a3.5 3.5 0 1 0 0 7.0a3.5 3.5 0 1 0 0 -7.0M7 9.0a3.5 3.5 0 1 0 0 7.0a3.5 3.5 0 1 0 0 -7.0M13 9.0a3.5 3.5 0 1 0 0 7.0a3.5 3.5 0 1 0 0 -7.0")
    val Split = icon("M10 2.8a7.2 7.2 0 1 0 0 14.4a7.2 7.2 0 1 0 0 -14.4M2.8 10h14.4")
    val Effects = icon("M10 2.5c.6 4.2 3.3 6.9 7.5 7.5-4.2.6-6.9 3.3-7.5 7.5-.6-4.2-3.3-6.9-7.5-7.5 4.2-.6 6.9-3.3 7.5-7.5z")
    val Text = icon("M4 4.5h12M10 4.5v11.5")
    val Font = icon("M2.5 15.5L6.5 4.5l4 11M4 11.5h5M14.5 10.5a2.5 2.5 0 1 0 0 5.0a2.5 2.5 0 1 0 0 -5.0M17 10.5v5")
    val Palette = icon("M10 3.5a6.5 6.5 0 1 0 0 13c1 0 1.5-.7 1.5-1.5s-.5-1.2-.5-2 .7-1.5 1.5-1.5H15a2 2 0 0 0 2-2c0-3.3-3.1-6-7-6zM6.8 9h.01M9 6.5h.01M12.5 7h.01")
    val Plate = icon("M4.5 3h11a1.5 1.5 0 0 1 1.5 1.5v11a1.5 1.5 0 0 1 -1.5 1.5h-11a1.5 1.5 0 0 1 -1.5 -1.5v-11a1.5 1.5 0 0 1 1.5 -1.5zM7.3 13.5L10 6.5l2.7 7M8.3 11h3.4")
    val Lock = icon("M6 9h8a1.5 1.5 0 0 1 1.5 1.5v5a1.5 1.5 0 0 1 -1.5 1.5h-8a1.5 1.5 0 0 1 -1.5 -1.5v-5a1.5 1.5 0 0 1 1.5 -1.5zM7 9V6.5a3 3 0 0 1 6 0V9")
    val Unlock = icon("M6 9h8a1.5 1.5 0 0 1 1.5 1.5v5a1.5 1.5 0 0 1 -1.5 1.5h-8a1.5 1.5 0 0 1 -1.5 -1.5v-5a1.5 1.5 0 0 1 1.5 -1.5zM7 9V6.5a3 3 0 0 1 5.8-1")
    val Duplicate = icon("M4.5 3h7a1.5 1.5 0 0 1 1.5 1.5v7a1.5 1.5 0 0 1 -1.5 1.5h-7a1.5 1.5 0 0 1 -1.5 -1.5v-7a1.5 1.5 0 0 1 1.5 -1.5zM7 17h10V7")
    val AlignLeft = icon("M3.5 5h13M3.5 8.5h8M3.5 12h13M3.5 15.5h8")
    val AlignCenter = icon("M3.5 5h13M6 8.5h8M3.5 12h13M6 15.5h8")
    val AlignRight = icon("M3.5 5h13M8.5 8.5h8M3.5 12h13M8.5 15.5h8")
    val Add = icon("M10 4v12M4 10h12")
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
