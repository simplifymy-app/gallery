package app.simplifymy.gallery.feature.editor

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.simplifymy.gallery.ui.component.Chip
import java.util.Locale
import kotlin.math.abs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import app.simplifymy.gallery.ui.component.MenuEntry
import app.simplifymy.gallery.ui.component.OverflowMenu
import app.simplifymy.gallery.ui.component.SegmentedPicker
import kotlin.math.roundToInt
import kotlin.math.sign
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing
import kotlin.math.min

@Composable
fun CropBox(
    crop: Rect,
    ratio: Float?,
    interactive: Boolean,
    lines: Int,
    onStart: () -> Unit,
    onCrop: (Rect) -> Unit,
    onEnd: () -> Unit,
) {
    val current by rememberUpdatedState(crop)
    var overlay by remember { mutableStateOf(IntSize.Zero) }
    val marginPx = with(LocalDensity.current) { CROP_MARGIN.toPx() }
    val image = Size(overlay.width - marginPx * 2, overlay.height - marginPx * 2)

    Box(Modifier.outset(CROP_MARGIN).onSizeChanged { overlay = it }) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(ratio, interactive, image) {
                if (!interactive || image.minDimension <= 0f) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = current.onCanvas(image)
                    val grip = gripAt(down.position - Offset(marginPx, marginPx), start, CROP_GRAB.toPx())
                    if (grip == CropGrip.None) return@awaitEachGesture
                    down.consume()
                    onStart()
                    drag(down.id) { change ->
                        change.consume()
                        val moved = start.dragged(grip, change.position - down.position, ratio, image, CROP_MIN.toPx())
                        onCrop(moved.toFractions(image))
                    }
                    onEnd()
                }
            }
        ) {
            if (image.minDimension <= 0f) return@Canvas
            translate(marginPx, marginPx) { drawCropFrame(crop.onCanvas(image), image, lines, interactive) }
        }
        if (interactive && image.minDimension > 0f) {
            val box = crop.onCanvas(image)
            listOf(box.topLeft, box.topRight, box.bottomLeft, box.bottomRight).forEach { corner ->
                Spacer(
                    Modifier
                        .offset { corner.round() }
                        .size(CROP_MARGIN * 2)
                        .systemGestureExclusion()
                )
            }
        }
    }
}

private fun DrawScope.drawCropFrame(box: Rect, image: Size, lines: Int, interactive: Boolean) {
    drawRect(CROP_SHADE, size = Size(image.width, box.top))
    drawRect(CROP_SHADE, topLeft = Offset(0f, box.bottom), size = Size(image.width, image.height - box.bottom))
    drawRect(CROP_SHADE, topLeft = Offset(0f, box.top), size = Size(box.left, box.height))
    drawRect(CROP_SHADE, topLeft = Offset(box.right, box.top), size = Size(image.width - box.right, box.height))

    drawRect(CROP_EDGE, topLeft = box.topLeft, size = box.size, style = DrawStroke(width = 1.dp.toPx()))
    (1 until lines).forEach { step ->
        val fraction = step.toFloat() / lines
        val x = box.left + box.width * fraction
        val y = box.top + box.height * fraction
        drawLine(CROP_GUIDE, Offset(x, box.top), Offset(x, box.bottom))
        drawLine(CROP_GUIDE, Offset(box.left, y), Offset(box.right, y))
    }
    if (!interactive) return
    val arm = 20.dp.toPx().coerceAtMost(box.minDimension / 3f)
    val thick = 3.dp.toPx()
    listOf(
        box.topLeft to (Offset(arm, 0f) to Offset(0f, arm)),
        box.topRight to (Offset(-arm, 0f) to Offset(0f, arm)),
        box.bottomLeft to (Offset(arm, 0f) to Offset(0f, -arm)),
        box.bottomRight to (Offset(-arm, 0f) to Offset(0f, -arm)),
    ).forEach { (origin, arms) ->
        drawLine(CROP_CORNER, origin, origin + arms.first, strokeWidth = thick)
        drawLine(CROP_CORNER, origin, origin + arms.second, strokeWidth = thick)
    }
}

private fun Modifier.outset(margin: Dp) = layout { measurable, constraints ->
    val extra = margin.roundToPx()
    val placeable = measurable.measure(
        Constraints.fixed(constraints.maxWidth + extra * 2, constraints.maxHeight + extra * 2)
    )
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-extra, -extra) }
}

val CROP_MARGIN = 24.dp
private val CROP_GRAB = 32.dp
private val CROP_MIN = 48.dp
private val CROP_SHADE = Color(0x8C0B0B0C)
private val CROP_GUIDE = Color(0x47EDEDEF)
private val CROP_EDGE = Color(0xD9EDEDEF)
private val CROP_CORNER = Color(0xFFEDEDEF)

@Composable
fun PerspectiveBox(
    quad: List<Offset>,
    onStart: () -> Unit,
    onCorner: (Int, Offset) -> Unit,
    onEnd: () -> Unit,
) {
    val current by rememberUpdatedState(quad)
    val shade = Color(0x8C0B0B0C)
    val guide = Color(0x47EDEDEF)
    val edge = Color(0xD9EDEDEF)
    val handle = Color(0xFFEDEDEF)

    Canvas(
        Modifier.fillMaxSize().pointerInput(Unit) {
            var corner = -1
            detectDragGestures(
                onDragStart = { point ->
                    val bounds = Size(size.width.toFloat(), size.height.toFloat())
                    val nearest = current.indices.minBy { (current[it].onCanvas(bounds) - point).getDistance() }
                    corner = if ((current[nearest].onCanvas(bounds) - point).getDistance() <= GRAB_RADIUS.toPx()) nearest else -1
                    if (corner >= 0) onStart()
                },
                onDrag = { change, delta ->
                    if (corner < 0) return@detectDragGestures
                    change.consume()
                    val at = current[corner]
                    onCorner(corner, Offset(at.x + delta.x / size.width, at.y + delta.y / size.height))
                },
                onDragEnd = { if (corner >= 0) onEnd(); corner = -1 },
                onDragCancel = { if (corner >= 0) onEnd(); corner = -1 },
            )
        }
    ) {
        val points = quad.map { it.onCanvas(size) }
        val outline = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        val outside = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addPath(outline)
        }
        drawPath(outside, shade)
        listOf(1f / 3f, 2f / 3f).forEach { t ->
            drawLine(guide, lerp(points[0], points[1], t), lerp(points[3], points[2], t))
            drawLine(guide, lerp(points[0], points[3], t), lerp(points[1], points[2], t))
        }
        drawPath(outline, edge, style = DrawStroke(width = 1.dp.toPx()))
        points.forEach { drawCircle(handle, radius = 7.dp.toPx(), center = it) }
    }
}

private val GRAB_RADIUS = 28.dp

private fun Offset.onCanvas(size: Size) = Offset(x * size.width, y * size.height)

@Composable
fun CropPanel(
    state: EditorUiState,
    viewModel: EditorViewModel,
    onStraightening: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SegmentedPicker(
            options = CropMode.entries,
            selected = state.cropMode,
            onSelect = viewModel::pickCropMode,
            label = { stringResource(it.labelRes) },
        )
        Box(Modifier.fillMaxWidth().height(CROP_PANEL_HEIGHT), contentAlignment = Alignment.Center) {
            when (state.cropMode) {
                CropMode.Crop -> Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                    ) {
                        TurnButton(GalleryIcons.RotateLeft, stringResource(R.string.action_rotate_left)) {
                            viewModel.rotate(false)
                        }
                        TurnButton(GalleryIcons.RotateRight, stringResource(R.string.action_rotate_right)) {
                            viewModel.rotate(true)
                        }
                        TurnButton(GalleryIcons.Mirror, stringResource(R.string.action_mirror), onClick = viewModel::mirror)
                        TurnButton(
                            GalleryIcons.FlipVertical,
                            stringResource(R.string.action_flip_vertical),
                            onClick = viewModel::flipVertically,
                        )
                    }
                    RatioRow(state.edits.ratio, viewModel::pickRatio)
                }
                CropMode.Straighten -> Ruler(
                    degrees = state.edits.straighten,
                    onStart = {
                        onStraightening(true)
                        viewModel.beginGesture()
                    },
                    onChange = viewModel::setStraighten,
                    onEnd = {
                        onStraightening(false)
                        viewModel.endGesture()
                    },
                )
                CropMode.Perspective -> Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(R.string.perspective_hint),
                        style = GalleryTheme.type.body,
                        color = GalleryTheme.colors.textMuted,
                        textAlign = TextAlign.Center,
                    )
                    Chip(
                        label = stringResource(R.string.action_reset),
                        onClick = viewModel::resetPerspective,
                        modifier = Modifier.alpha(if (state.edits.quad != FULL_QUAD) 1f else DISABLED),
                    )
                }
            }
        }
    }
}

private val CROP_PANEL_HEIGHT = 100.dp

@Composable
private fun Ruler(
    degrees: Float,
    onStart: () -> Unit,
    onChange: (Float) -> Unit,
    onEnd: () -> Unit,
) {
    val colors = GalleryTheme.colors
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(degrees)
    val label = stringResource(R.string.editor_straighten)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(angleLabel(degrees), style = GalleryTheme.type.meta, color = colors.text)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(Spacing.hitTarget)
                .semantics { contentDescription = label }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onChange(0f) })
                }
                .pointerInput(Unit) {
                    var running = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            running = current
                            onStart()
                        },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            val before = straightened(running)
                            running = (running - dx / TICK_SPACING.toPx()).coerceIn(-STRAIGHTEN_LIMIT, STRAIGHTEN_LIMIT)
                            val after = straightened(running)
                            if (after != before && (after == 0f || before.sign * after.sign < 0f)) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onChange(after)
                        },
                        onDragEnd = onEnd,
                        onDragCancel = onEnd,
                    )
                }
        ) {
            val spacing = TICK_SPACING.toPx()
            val middle = size.width / 2f
            val base = size.height / 2f + 8.dp.toPx()
            (-STRAIGHTEN_LIMIT.toInt()..STRAIGHTEN_LIMIT.toInt()).forEach { degree ->
                val x = middle + (degree - degrees) * spacing
                if (x < 0f || x > size.width) return@forEach
                val tall = when {
                    degree % 15 == 0 -> 16.dp.toPx()
                    degree % 5 == 0 -> 11.dp.toPx()
                    else -> 6.dp.toPx()
                }
                drawLine(
                    color = if (degree % 15 == 0) colors.textMuted else colors.textFaint,
                    start = Offset(x, base - tall),
                    end = Offset(x, base),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            drawLine(
                color = colors.accent,
                start = Offset(middle, base - 22.dp.toPx()),
                end = Offset(middle, base + 2.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private val TICK_SPACING = 8.dp

private fun angleLabel(degrees: Float): String {
    val magnitude = String.format(Locale.ROOT, "%.1f", abs(degrees))
    return if (degrees < 0f) "\u2212$magnitude\u00B0" else "$magnitude\u00B0"
}

@Composable
private fun RowScope.TurnButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = GalleryTheme.colors
    Box(
        modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .clip(RoundedCornerShape(ControlRadius))
            .background(colors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = colors.text, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RatioRow(selected: CropRatio, onPick: (CropRatio) -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        CropRatio.entries.forEach { ratio ->
            val on = ratio == selected
            Text(
                ratio.labelRes?.let { stringResource(it) } ?: ratio.label,
                style = GalleryTheme.type.label,
                color = if (on) colors.onAccent else colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(min = 52.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(if (on) colors.accent else colors.surface)
                    .clickable { onPick(ratio) }
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            )
        }
    }
}

