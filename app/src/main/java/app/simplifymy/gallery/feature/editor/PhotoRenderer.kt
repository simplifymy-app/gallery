package app.simplifymy.gallery.feature.editor

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.graphics.Rect as AndroidRect
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun Rect.toPixels(width: Int, height: Int): AndroidRect {
    val left = (this.left * width).roundToInt().coerceIn(0, width - 1)
    val top = (this.top * height).roundToInt().coerceIn(0, height - 1)
    val right = (this.right * width).roundToInt().coerceIn(left + 1, width)
    val bottom = (this.bottom * height).roundToInt().coerceIn(top + 1, height)
    return AndroidRect(left, top, right, bottom)
}

fun decodePhoto(resolver: ContentResolver, uri: Uri, maxPixels: Long): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val pixels = info.size.width.toLong() * info.size.height
        if (pixels > maxPixels) {
            val shrink = sqrt(maxPixels.toDouble() / pixels)
            decoder.setTargetSize(
                (info.size.width * shrink).toInt().coerceAtLeast(1),
                (info.size.height * shrink).toInt().coerceAtLeast(1),
            )
        }
    }

suspend fun renderPreview(source: Bitmap, grading: PhotoEdits): Bitmap =
    source.copy(Bitmap.Config.ARGB_8888, true).also { develop(it, grading) }

suspend fun render(source: Bitmap, edits: PhotoEdits, date: Long, fonts: EditorFonts): Bitmap {
    val out = framed(source, edits)
    develop(out, edits)
    val canvas = Canvas(out)
    val width = out.width.toFloat()
    val height = out.height.toFloat()
    drawFinish(canvas, width, height, edits, date, fonts)
    drawStrokes(canvas, edits.strokes, edits.crop, width, height)
    drawTexts(canvas, width, height, edits.texts, fonts)
    return out
}

suspend fun develop(bitmap: Bitmap, edits: PhotoEdits) {
    grade(bitmap, PhotoGrade(edits))
    refine(bitmap, edits)
}

fun framed(source: Bitmap, edits: PhotoEdits): Bitmap {
    val (width, height) = orientedSize(source.width, source.height, edits.turns)
    val window = edits.crop.toPixels(width, height)
    val out = Bitmap.createBitmap(window.width(), window.height(), Bitmap.Config.ARGB_8888)
    val matrix = frameMatrix(source.width, source.height, edits).apply {
        postTranslate(-window.left.toFloat(), -window.top.toFloat())
    }
    Canvas(out).drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
    return out
}

fun frameMatrix(width: Int, height: Int, edits: PhotoEdits): Matrix {
    val (frameWidth, frameHeight) = orientedSize(width, height, edits.turns)
    val w = frameWidth.toFloat()
    val h = frameHeight.toFloat()
    return Matrix().apply {
        if (edits.flipped) postScale(-1f, 1f)
        postRotate(90f * edits.turns)
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        mapRect(bounds)
        postTranslate(-bounds.left, -bounds.top)
        if (edits.quad != FULL_QUAD) {
            val from = edits.quad.flatMap { listOf(it.x * w, it.y * h) }.toFloatArray()
            val to = FULL_QUAD.flatMap { listOf(it.x * w, it.y * h) }.toFloatArray()
            postConcat(Matrix().apply { setPolyToPoly(from, 0, to, 0, 4) })
        }
        if (edits.straighten != 0f) {
            val scale = coverScale(frameWidth, frameHeight, edits.straighten)
            postRotate(edits.straighten, w / 2f, h / 2f)
            postScale(scale, scale, w / 2f, h / 2f)
        }
    }
}

suspend fun grade(bitmap: Bitmap, grade: PhotoGrade) {
    if (grade.isIdentity) return
    val width = bitmap.width
    val height = bitmap.height
    val lanes = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    (0 until height step BAND_ROWS).chunked(lanes).forEach { tops ->
        coroutineScope {
            tops.map { top ->
                async(Dispatchers.Default) {
                    ensureActive()
                    val rows = minOf(BAND_ROWS, height - top)
                    val pixels = IntArray(width * rows)
                    bitmap.getPixels(pixels, 0, width, 0, top, width, rows)
                    grade.apply(pixels)
                    Triple(pixels, top, rows)
                }
            }.awaitAll()
        }.forEach { (pixels, top, rows) -> bitmap.setPixels(pixels, 0, width, 0, top, width, rows) }
    }
}

private const val BAND_ROWS = 64

private class Blur(source: Bitmap, factor: Int) {
    val width: Int
    val height: Int
    val pixels: IntArray
    private val scaleX: Float
    private val scaleY: Float

    init {
        val targetWidth = (source.width / factor).coerceAtLeast(1)
        val targetHeight = (source.height / factor).coerceAtLeast(1)
        var small = source
        while (small.width / 2 >= targetWidth && small.height / 2 >= targetHeight) {
            val next = Bitmap.createScaledBitmap(small, small.width / 2, small.height / 2, true)
            if (small !== source) small.recycle()
            small = next
        }
        if (small.width != targetWidth || small.height != targetHeight) {
            val next = Bitmap.createScaledBitmap(small, targetWidth, targetHeight, true)
            if (small !== source) small.recycle()
            small = next
        }
        width = small.width
        height = small.height
        pixels = IntArray(width * height).also { small.getPixels(it, 0, width, 0, 0, width, height) }
        if (small !== source) small.recycle()
        scaleX = width.toFloat() / source.width
        scaleY = height.toFloat() / source.height
    }

    fun sample(x: Int, y: Int, out: FloatArray) {
        val fx = ((x + 0.5f) * scaleX - 0.5f).coerceIn(0f, (width - 1).toFloat())
        val fy = ((y + 0.5f) * scaleY - 0.5f).coerceIn(0f, (height - 1).toFloat())
        val x0 = fx.toInt()
        val y0 = fy.toInt()
        val x1 = minOf(x0 + 1, width - 1)
        val y1 = minOf(y0 + 1, height - 1)
        val tx = fx - x0
        val ty = fy - y0
        val a = pixels[y0 * width + x0]
        val b = pixels[y0 * width + x1]
        val c = pixels[y1 * width + x0]
        val d = pixels[y1 * width + x1]
        for (shift in 0..2) {
            val bits = 16 - shift * 8
            val top = (a shr bits and 0xFF) * (1f - tx) + (b shr bits and 0xFF) * tx
            val bottom = (c shr bits and 0xFF) * (1f - tx) + (d shr bits and 0xFF) * tx
            out[shift] = top * (1f - ty) + bottom * ty
        }
    }
}

suspend fun refine(bitmap: Bitmap, edits: PhotoEdits) {
    val clarity = edits.total(Adjustment.Clarity) / 100f
    val sharpen = edits.total(Adjustment.Sharpen) / 100f
    val glow = edits.total(Adjustment.Glow) / 100f
    val glare = if (edits.camera != null) edits.cameraSettings.glare / 100f else 0f
    if (clarity == 0f && sharpen == 0f && glow == 0f && glare == 0f) return
    val longEdge = maxOf(bitmap.width, bitmap.height)
    val wide = if (clarity != 0f || glow > 0f || glare > 0f) Blur(bitmap, (longEdge / 96).coerceAtLeast(4)) else null
    val fine = if (sharpen > 0f) Blur(bitmap, (longEdge / 1000).coerceAtLeast(2)) else null
    val width = bitmap.width
    val height = bitmap.height
    val lanes = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    (0 until height step BAND_ROWS).chunked(lanes).forEach { tops ->
        coroutineScope {
            tops.map { top ->
                async(Dispatchers.Default) {
                    ensureActive()
                    val rows = minOf(BAND_ROWS, height - top)
                    val pixels = IntArray(width * rows)
                    bitmap.getPixels(pixels, 0, width, 0, top, width, rows)
                    val rgb = FloatArray(3)
                    val soft = FloatArray(3)
                    val near = FloatArray(3)
                    for (row in 0 until rows) {
                        for (x in 0 until width) {
                            val index = row * width + x
                            val pixel = pixels[index]
                            rgb[0] = (pixel shr 16 and 0xFF).toFloat()
                            rgb[1] = (pixel shr 8 and 0xFF).toFloat()
                            rgb[2] = (pixel and 0xFF).toFloat()
                            wide?.sample(x, top + row, soft)
                            fine?.sample(x, top + row, near)
                            val level = (0.299f * rgb[0] + 0.587f * rgb[1] + 0.114f * rgb[2]) / 255f
                            val softLevel = (0.299f * soft[0] + 0.587f * soft[1] + 0.114f * soft[2]) / 255f
                            val middle = 1f - (2f * level - 1f).let { it * it }
                            val bloom = glow * ((softLevel - 0.55f) / 0.45f).coerceIn(0f, 1f)
                            val halo = glare * ((softLevel - 0.6f) / 0.4f).coerceIn(0f, 1f)
                            for (c in 0..2) {
                                var value = rgb[c]
                                if (wide != null && clarity != 0f) value += clarity * 0.8f * (value - soft[c]) * middle
                                if (fine != null) value += sharpen * 1.5f * (value - near[c])
                                if (bloom > 0f) value = 255f - (255f - value) * (255f - soft[c] * bloom) / 255f
                                if (halo > 0f) value += halo * HALATION[c]
                                rgb[c] = value
                            }
                            pixels[index] = (pixel and (0xFF shl 24)) or
                                (rgb[0].roundToInt().coerceIn(0, 255) shl 16) or
                                (rgb[1].roundToInt().coerceIn(0, 255) shl 8) or
                                rgb[2].roundToInt().coerceIn(0, 255)
                        }
                    }
                    Triple(pixels, top, rows)
                }
            }.awaitAll()
        }.forEach { (pixels, top, rows) -> bitmap.setPixels(pixels, 0, width, 0, top, width, rows) }
    }
}

private val HALATION = floatArrayOf(120f, 52f, 24f)
