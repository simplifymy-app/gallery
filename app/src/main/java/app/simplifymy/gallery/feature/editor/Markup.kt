package app.simplifymy.gallery.feature.editor

import android.graphics.Canvas
import android.graphics.DiscretePathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.R
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MarkupPanel(state: EditorUiState, viewModel: EditorViewModel) {
    val colors = GalleryTheme.colors
    val erasing = state.markup == MarkupTool.Eraser
    Column(
        modifier = Modifier.fillMaxWidth().height(PANEL_HEIGHT).padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TileRow {
            MarkupTool.entries.forEach { tool ->
                ToolItem(
                    icon = tool.icon,
                    label = stringResource(tool.labelRes),
                    selected = state.markup == tool,
                    onClick = { viewModel.pickMarkup(tool) },
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 20.dp).alpha(if (erasing) DISABLED else 1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            INKS.forEach { ink ->
                SwatchDot(ink, selected = !erasing && ink == state.ink, onClick = { viewModel.pickInk(ink) }, size = 22.dp)
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NIBS.forEach { nib ->
                val on = nib == state.nib
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(ControlRadius))
                        .background(if (on) colors.accent.copy(alpha = SELECTED) else Color.Transparent)
                        .clickable { viewModel.pickNib(nib) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(nib.dp).clip(CircleShape).background(if (on) colors.accent else colors.textMuted))
                }
            }
            Spacer(Modifier.weight(1f))
            IconAction(
                GalleryIcons.Undo,
                onClick = viewModel::undoStroke,
                label = stringResource(R.string.draw_undo_stroke),
                tint = colors.textMuted,
                enabled = state.edits.strokes.isNotEmpty(),
            )
            IconAction(
                GalleryIcons.Trash,
                onClick = viewModel::clearStrokes,
                label = stringResource(R.string.markup_clear),
                tint = colors.textMuted,
                enabled = state.edits.strokes.isNotEmpty(),
            )
        }
    }
}

val MarkupTool.icon: ImageVector
    get() = when (this) {
        MarkupTool.Pen -> GalleryIcons.Pen
        MarkupTool.Pencil -> GalleryIcons.Pencil
        MarkupTool.Marker -> GalleryIcons.Marker
        MarkupTool.Highlighter -> GalleryIcons.Highlighter
        MarkupTool.Line -> GalleryIcons.Line
        MarkupTool.Arrow -> GalleryIcons.Arrow
        MarkupTool.Rectangle -> GalleryIcons.Rectangle
        MarkupTool.Ellipse -> GalleryIcons.Ellipse
        MarkupTool.Eraser -> GalleryIcons.Eraser
    }

fun drawStrokes(canvas: Canvas, strokes: List<Stroke>, window: Rect, width: Float, height: Float) {
    if (strokes.isEmpty() || window.width <= 0f || window.height <= 0f) return
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        val points = stroke.points.map {
            Offset((it.x - window.left) / window.width * width, (it.y - window.top) / window.height * height)
        }
        val tool = stroke.tool
        paint.color = stroke.color
        paint.alpha = (android.graphics.Color.alpha(stroke.color) * tool.opacity).roundToInt()
        paint.strokeWidth = stroke.width * width / window.width
        paint.strokeCap = if (tool == MarkupTool.Highlighter) Paint.Cap.BUTT else Paint.Cap.ROUND
        paint.pathEffect = if (tool == MarkupTool.Pencil) {
            DiscretePathEffect((paint.strokeWidth * 1.5f).coerceAtLeast(2f), paint.strokeWidth * 0.3f)
        } else null
        canvas.drawPath(tool.path(points, paint.strokeWidth), paint)
    }
}

private fun MarkupTool.path(points: List<Offset>, strokeWidth: Float): Path {
    val start = points.first()
    val end = points.last()
    return Path().apply {
        when (this@path) {
            MarkupTool.Line -> {
                moveTo(start.x, start.y)
                lineTo(end.x, end.y)
            }
            MarkupTool.Arrow -> {
                moveTo(start.x, start.y)
                lineTo(end.x, end.y)
                val angle = atan2(end.y - start.y, end.x - start.x)
                val head = (strokeWidth * 5f).coerceAtMost(hypot(end.x - start.x, end.y - start.y) * 0.5f)
                listOf(angle + ARROW_SPREAD, angle - ARROW_SPREAD).forEach { side ->
                    moveTo(end.x, end.y)
                    lineTo(end.x - head * cos(side), end.y - head * sin(side))
                }
            }
            MarkupTool.Rectangle -> addRect(boundsOf(start, end), Path.Direction.CW)
            MarkupTool.Ellipse -> addOval(boundsOf(start, end), Path.Direction.CW)
            MarkupTool.Pen, MarkupTool.Pencil, MarkupTool.Marker, MarkupTool.Highlighter, MarkupTool.Eraser -> {
                moveTo(start.x, start.y)
                points.zipWithNext().drop(1).forEach { (from, to) ->
                    quadTo(from.x, from.y, (from.x + to.x) / 2f, (from.y + to.y) / 2f)
                }
                lineTo(end.x, end.y)
            }
        }
    }
}

private fun boundsOf(a: Offset, b: Offset) = RectF(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y))

private const val ARROW_SPREAD = 0.5f

fun Stroke.touches(point: Offset, radius: Float, aspect: Float): Boolean {
    val scale = { it: Offset -> Offset(it.x, it.y * aspect) }
    val target = scale(point)
    val reach = radius + width / 2f
    return outline().map(scale).zipWithNext().any { (a, b) -> distanceToSegment(target, a, b) <= reach }
}

private fun Stroke.outline(): List<Offset> {
    val start = points.first()
    val end = points.last()
    return when (tool) {
        MarkupTool.Line, MarkupTool.Arrow -> listOf(start, end)
        MarkupTool.Rectangle -> listOf(start, Offset(end.x, start.y), end, Offset(start.x, end.y), start)
        MarkupTool.Ellipse -> {
            val center = (start + end) / 2f
            val rx = (end.x - start.x) / 2f
            val ry = (end.y - start.y) / 2f
            (0..ELLIPSE_SEGMENTS).map {
                val t = it * 2f * Math.PI.toFloat() / ELLIPSE_SEGMENTS
                Offset(center.x + rx * cos(t), center.y + ry * sin(t))
            }
        }
        else -> if (points.size == 1) points + points else points
    }
}

private const val ELLIPSE_SEGMENTS = 32

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val length = ab.x * ab.x + ab.y * ab.y
    val t = if (length == 0f) 0f else (((p - a).x * ab.x + (p - a).y * ab.y) / length).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}
