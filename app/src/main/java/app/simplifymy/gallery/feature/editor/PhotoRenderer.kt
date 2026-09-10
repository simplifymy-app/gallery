package app.simplifymy.gallery.feature.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect as AndroidRect
import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

fun Rect.toPixels(width: Int, height: Int): AndroidRect {
    val left = (this.left * width).roundToInt().coerceIn(0, width - 1)
    val top = (this.top * height).roundToInt().coerceIn(0, height - 1)
    val right = (this.right * width).roundToInt().coerceIn(left + 1, width)
    val bottom = (this.bottom * height).roundToInt().coerceIn(top + 1, height)
    return AndroidRect(left, top, right, bottom)
}

fun render(source: Bitmap, edits: PhotoEdits): Bitmap {
    val crop = edits.crop.toPixels(source.width, source.height)
    val out = Bitmap.createBitmap(crop.width(), crop.height(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(edits.colorMatrix())
    }
    canvas.drawBitmap(source, crop, AndroidRect(0, 0, crop.width(), crop.height()), paint)
    drawStrokes(canvas, edits.strokes, edits.crop, crop.width().toFloat(), crop.height().toFloat())
    return out
}

fun drawStrokes(canvas: Canvas, strokes: List<Stroke>, window: Rect, width: Float, height: Float) {
    if (strokes.isEmpty() || window.width <= 0f || window.height <= 0f) return
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        paint.color = stroke.color
        paint.strokeWidth = stroke.width * width / window.width
        val path = Path()
        stroke.points.forEachIndexed { index, point ->
            val x = (point.x - window.left) / window.width * width
            val y = (point.y - window.top) / window.height * height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }
}
