package app.simplifymy.gallery.feature.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

enum class CropGrip { None, Move, TopLeft, TopRight, BottomLeft, BottomRight }

/** Smallest box the user can pinch down to, and how close a finger must land to catch a corner. */
private const val MIN_PX = 72f
private const val GRAB_PX = 56f

fun Rect.onCanvas(canvas: Size) =
    Rect(left * canvas.width, top * canvas.height, right * canvas.width, bottom * canvas.height)

fun Rect.toFractions(canvas: Size) =
    Rect(left / canvas.width, top / canvas.height, right / canvas.width, bottom / canvas.height)

fun gripAt(point: Offset, box: Rect): CropGrip {
    val corners = listOf(
        CropGrip.TopLeft to Offset(box.left, box.top),
        CropGrip.TopRight to Offset(box.right, box.top),
        CropGrip.BottomLeft to Offset(box.left, box.bottom),
        CropGrip.BottomRight to Offset(box.right, box.bottom),
    )
    val nearest = corners.minByOrNull { (_, corner) -> (corner - point).getDistance() }
    if (nearest != null && (nearest.second - point).getDistance() <= GRAB_PX) return nearest.first
    return if (box.contains(point)) CropGrip.Move else CropGrip.None
}

fun Rect.dragged(grip: CropGrip, delta: Offset, ratio: Float?, bounds: Size): Rect {
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
    l = l.coerceIn(0f, right - MIN_PX)
    t = t.coerceIn(0f, bottom - MIN_PX)
    r = r.coerceIn(left + MIN_PX, bounds.width)
    b = b.coerceIn(top + MIN_PX, bounds.height)

    val box = Rect(l, t, r, b)
    return if (ratio == null) box else box.lockedTo(ratio, grip, bounds)
}

private fun Rect.lockedTo(ratio: Float, grip: CropGrip, bounds: Size): Rect {
    val anchorX = if (grip == CropGrip.TopLeft || grip == CropGrip.BottomLeft) right else left
    val anchorY = if (grip == CropGrip.TopLeft || grip == CropGrip.TopRight) bottom else top
    val towardLeft = anchorX == right
    val towardTop = anchorY == bottom

    var w = width
    var h = w / ratio
    val roomX = if (towardLeft) anchorX else bounds.width - anchorX
    val roomY = if (towardTop) anchorY else bounds.height - anchorY
    if (w > roomX) { w = roomX; h = w / ratio }
    if (h > roomY) { h = roomY; w = h * ratio }
    if (w < MIN_PX) { w = MIN_PX; h = w / ratio }
    if (h < MIN_PX) { h = MIN_PX; w = h * ratio }

    val l = if (towardLeft) anchorX - w else anchorX
    val t = if (towardTop) anchorY - h else anchorY
    return Rect(l, t, l + w, t + h)
}
