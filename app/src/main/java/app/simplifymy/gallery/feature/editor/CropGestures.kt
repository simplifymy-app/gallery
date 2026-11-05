package app.simplifymy.gallery.feature.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs

enum class CropGrip { None, Move, TopLeft, TopRight, BottomLeft, BottomRight }

fun Rect.onCanvas(canvas: Size) =
    Rect(left * canvas.width, top * canvas.height, right * canvas.width, bottom * canvas.height)

fun Rect.toFractions(canvas: Size) =
    Rect(left / canvas.width, top / canvas.height, right / canvas.width, bottom / canvas.height)

fun gripAt(point: Offset, box: Rect, reach: Float): CropGrip {
    val corners = listOf(
        CropGrip.TopLeft to box.topLeft,
        CropGrip.TopRight to box.topRight,
        CropGrip.BottomLeft to box.bottomLeft,
        CropGrip.BottomRight to box.bottomRight,
    )
    val nearest = corners.minBy { (_, corner) -> (corner - point).getDistance() }
    if ((nearest.second - point).getDistance() <= reach) return nearest.first
    return if (box.contains(point)) CropGrip.Move else CropGrip.None
}

fun Rect.dragged(grip: CropGrip, delta: Offset, ratio: Float?, bounds: Size, minimum: Float): Rect {
    if (grip == CropGrip.None) return this
    if (grip == CropGrip.Move) {
        val dx = delta.x.coerceIn(-left, bounds.width - right)
        val dy = delta.y.coerceIn(-top, bounds.height - bottom)
        return translate(dx, dy)
    }

    var l = left
    var t = top
    var r = right
    var b = bottom
    when (grip) {
        CropGrip.TopLeft -> { l += delta.x; t += delta.y }
        CropGrip.TopRight -> { r += delta.x; t += delta.y }
        CropGrip.BottomLeft -> { l += delta.x; b += delta.y }
        CropGrip.BottomRight -> { r += delta.x; b += delta.y }
        else -> Unit
    }
    l = l.coerceIn(0f, right - minimum)
    t = t.coerceIn(0f, bottom - minimum)
    r = r.coerceIn(left + minimum, bounds.width)
    b = b.coerceIn(top + minimum, bounds.height)

    val box = Rect(l, t, r, b)
    return if (ratio == null) box else box.lockedTo(ratio, grip, bounds, minimum, from = this)
}

private fun Rect.lockedTo(ratio: Float, grip: CropGrip, bounds: Size, minimum: Float, from: Rect): Rect {
    val anchorX = if (grip == CropGrip.TopLeft || grip == CropGrip.BottomLeft) from.right else from.left
    val anchorY = if (grip == CropGrip.TopLeft || grip == CropGrip.TopRight) from.bottom else from.top
    val towardLeft = anchorX == from.right
    val towardTop = anchorY == from.bottom

    val widthChange = abs(width - from.width)
    val heightChange = abs(height - from.height) * ratio
    var w = if (heightChange > widthChange) height * ratio else width
    var h = w / ratio
    val roomX = if (towardLeft) anchorX else bounds.width - anchorX
    val roomY = if (towardTop) anchorY else bounds.height - anchorY
    if (w > roomX) { w = roomX; h = w / ratio }
    if (h > roomY) { h = roomY; w = h * ratio }
    if (w < minimum) { w = minimum; h = w / ratio }
    if (h < minimum) { h = minimum; w = h * ratio }

    val l = if (towardLeft) anchorX - w else anchorX
    val t = if (towardTop) anchorY - h else anchorY
    return Rect(l, t, l + w, t + h)
}
