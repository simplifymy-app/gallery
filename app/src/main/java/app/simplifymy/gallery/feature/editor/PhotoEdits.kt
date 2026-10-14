package app.simplifymy.gallery.feature.editor

import android.graphics.ColorMatrix
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import app.simplifymy.gallery.R

enum class EditorTab(@StringRes val labelRes: Int, @StringRes val titleRes: Int) {
    Crop(R.string.editor_crop, R.string.editor_title_crop),
    Adjust(R.string.editor_adjust, R.string.editor_title_adjust),
    Filter(R.string.editor_filter, R.string.editor_title_filter),
    Draw(R.string.editor_draw, R.string.editor_title_draw),
}

enum class CropRatio(val ratio: Float?, @StringRes val labelRes: Int? = null, val label: String = "") {
    Free(null, labelRes = R.string.crop_free),
    Square(1f, label = "1:1"),
    Photo(4f / 3f, label = "4:3"),
    Wide(16f / 9f, label = "16:9"),
    Tall(9f / 16f, label = "9:16"),
}

enum class PhotoFilter(@StringRes val labelRes: Int) {
    Original(R.string.filter_original),
    Mono(R.string.filter_mono),
    Sepia(R.string.filter_sepia),
    Cold(R.string.filter_cold),
    Warm(R.string.filter_warm),
    Fade(R.string.filter_fade),
}

data class Stroke(val color: Int, val width: Float, val points: List<Offset>)

val FULL_FRAME = Rect(0f, 0f, 1f, 1f)

data class PhotoEdits(
    val crop: Rect = FULL_FRAME,
    val ratio: CropRatio = CropRatio.Free,
    val filter: PhotoFilter = PhotoFilter.Original,
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val strokes: List<Stroke> = emptyList(),
) {
    fun colorMatrix(): ColorMatrix = ColorMatrix().apply {
        when (filter) {
            PhotoFilter.Original -> Unit
            PhotoFilter.Mono -> { postConcat(saturate(0f)); postConcat(contrast(1.12f)) }
            PhotoFilter.Sepia -> { postConcat(sepia(0.72f)); postConcat(saturate(1.1f)) }
            PhotoFilter.Cold -> {
                postConcat(saturate(0.85f)); postConcat(hueRotate(-18f)); postConcat(brightness(1.04f))
            }
            PhotoFilter.Warm -> {
                postConcat(saturate(1.25f)); postConcat(hueRotate(12f)); postConcat(brightness(1.02f))
            }
            PhotoFilter.Fade -> {
                postConcat(contrast(0.82f)); postConcat(saturate(0.7f)); postConcat(brightness(1.1f))
            }
        }
        postConcat(brightness(1f + this@PhotoEdits.brightness / 100f))
        postConcat(contrast(1f + this@PhotoEdits.contrast / 100f))
        postConcat(saturate(1f + saturation / 100f))
    }
}

fun PhotoEdits.turned(clockwise: Boolean, widthOverHeight: Float): PhotoEdits {
    val box = crop.let {
        if (clockwise) Rect(1f - it.bottom, it.left, 1f - it.top, it.right)
        else Rect(it.top, 1f - it.right, it.bottom, 1f - it.left)
    }
    return copy(
        crop = box,
        strokes = strokes.map { stroke ->
            stroke.copy(
                width = stroke.width * widthOverHeight,
                points = stroke.points.map {
                    if (clockwise) Offset(1f - it.y, it.x) else Offset(it.y, 1f - it.x)
                },
            )
        },
    )
}

fun PhotoEdits.mirrored(): PhotoEdits = copy(
    crop = Rect(1f - crop.right, crop.top, 1f - crop.left, crop.bottom),
    strokes = strokes.map { stroke ->
        stroke.copy(points = stroke.points.map { Offset(1f - it.x, it.y) })
    },
)

fun centredCrop(width: Int, height: Int, ratio: Float?): Rect {
    if (ratio == null) return FULL_FRAME
    val source = width.toFloat() / height
    return if (source > ratio) {
        val w = ratio / source
        Rect((1f - w) / 2f, 0f, (1f + w) / 2f, 1f)
    } else {
        val h = source / ratio
        Rect(0f, (1f - h) / 2f, 1f, (1f + h) / 2f)
    }
}

private fun saturate(amount: Float) = ColorMatrix().apply { setSaturation(amount) }

private fun brightness(amount: Float) = ColorMatrix(
    floatArrayOf(
        amount, 0f, 0f, 0f, 0f,
        0f, amount, 0f, 0f, 0f,
        0f, 0f, amount, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
)

private fun contrast(amount: Float): ColorMatrix {
    val shift = (1f - amount) / 2f * 255f
    return ColorMatrix(
        floatArrayOf(
            amount, 0f, 0f, 0f, shift,
            0f, amount, 0f, 0f, shift,
            0f, 0f, amount, 0f, shift,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}

private fun sepia(amount: Float) = ColorMatrix(
    floatArrayOf(
        lerp(1f, 0.393f, amount), lerp(0f, 0.769f, amount), lerp(0f, 0.189f, amount), 0f, 0f,
        lerp(0f, 0.349f, amount), lerp(1f, 0.686f, amount), lerp(0f, 0.168f, amount), 0f, 0f,
        lerp(0f, 0.272f, amount), lerp(0f, 0.534f, amount), lerp(1f, 0.131f, amount), 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
)

private fun hueRotate(degrees: Float): ColorMatrix {
    val radians = degrees * Math.PI.toFloat() / 180f
    val c = kotlin.math.cos(radians)
    val s = kotlin.math.sin(radians)
    return ColorMatrix(
        floatArrayOf(
            0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
            0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
            0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}

private fun lerp(from: Float, to: Float, amount: Float) = from + (to - from) * amount

val SLIDER_RANGE = -100..100
const val SLIDER_STEP = 6

val INKS = listOf(0xFFEDEDEF, 0xFF0B0B0C, 0xFFE0574A, 0xFFE0A458, 0xFF5FBF8F, 0xFF6E8BFF).map(Long::toInt)
val NIBS = listOf(3f, 5f, 8f, 12f)
