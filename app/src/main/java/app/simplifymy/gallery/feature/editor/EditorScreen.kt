package app.simplifymy.gallery.feature.editor

import androidx.compose.foundation.Canvas
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
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TopBar(
            title = stringResource(state.tab.titleRes),
            canSave = state.source != null && !state.saving,
            onClose = onBack,
            onReset = viewModel::reset,
            onSave = viewModel::save,
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            state.source?.let { source ->
                Preview(
                    source = remember(source) { source.asImageBitmap() },
                    edits = state.edits,
                    cropping = state.tab == EditorTab.Crop,
                    ink = state.ink,
                    nib = state.nib,
                    drawing = state.tab == EditorTab.Draw,
                    onStroke = viewModel::addStroke,
                    onCrop = viewModel::setCrop,
                )
            }
        }

        when (state.tab) {
            EditorTab.Crop -> CropPanel(
                selected = state.edits.ratio,
                onPick = viewModel::pickRatio,
                onRotate = viewModel::rotate,
                onMirror = viewModel::mirror,
            )
            EditorTab.Adjust -> AdjustPanel(state.edits, viewModel)
            EditorTab.Filter -> FilterPanel(state, viewModel::pickFilter)
            EditorTab.Draw -> DrawPanel(state, viewModel::pickInk, viewModel::pickNib)
        }

        TabBar(state.tab, viewModel::pickTab)
    }
}

@Composable
private fun TopBar(
    title: String,
    canSave: Boolean,
    onClose: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconAction(GalleryIcons.Close, onClick = onClose, label = stringResource(R.string.action_close))
        Text(
            title,
            style = GalleryTheme.type.label.copy(fontWeight = FontWeight.SemiBold),
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconAction(
            GalleryIcons.Reset,
            onClick = onReset,
            label = stringResource(R.string.action_reset),
            tint = colors.textMuted,
        )
        Text(
            stringResource(R.string.action_save),
            style = GalleryTheme.type.label,
            color = colors.onAccent,
            modifier = Modifier
                .clip(RoundedCornerShape(ControlRadius))
                .background(if (canSave) colors.accent else colors.textFaint)
                .clickable(enabled = canSave, onClick = onSave)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Preview(
    source: ImageBitmap,
    edits: PhotoEdits,
    cropping: Boolean,
    drawing: Boolean,
    ink: Int,
    nib: Float,
    onStroke: (Stroke) -> Unit,
    onCrop: (Rect) -> Unit,
) {
    val window = if (cropping) FULL_FRAME else edits.crop
    val aspect = source.width * window.width / (source.height * window.height)
    val filter = remember(edits.filter, edits.brightness, edits.contrast, edits.saturation) {
        ColorFilter.colorMatrix(ColorMatrix(edits.colorMatrix().array))
    }
    var live by remember { mutableStateOf(emptyList<Offset>()) }
    var canvas by remember { mutableStateOf(Size.Zero) }

    Box(
        Modifier
            .aspectRatio(aspect)
            .then(if (aspect >= 1f) Modifier.fillMaxWidth() else Modifier.fillMaxHeight())
            .clip(RoundedCornerShape(2.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!drawing) Modifier else Modifier.pointerInput(ink, nib, window) {
                        detectDragGestures(
                            onDragStart = { live = listOf(it) },
                            onDrag = { change, _ -> live = live + change.position },
                            onDragEnd = {
                                val size = canvas
                                if (size.minDimension > 0f && live.size > 1) {
                                    onStroke(strokeOf(live, ink, nib, size, window))
                                }
                                live = emptyList()
                            },
                            onDragCancel = { live = emptyList() },
                        )
                    }
                )
        ) {
            canvas = size
            val src = window.toPixels(source.width, source.height)
            drawImage(
                image = source,
                srcOffset = IntOffset(src.left, src.top),
                srcSize = IntSize(src.width(), src.height()),
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                colorFilter = filter,
                filterQuality = FilterQuality.Medium,
            )
            val shown = edits.strokes + liveStroke(live, ink, nib, size, window)
            shown.forEach { stroke ->
                if (stroke.points.size < 2) return@forEach
                val path = Path().apply {
                    stroke.points.forEachIndexed { index, point ->
                        val x = (point.x - window.left) / window.width * size.width
                        val y = (point.y - window.top) / window.height * size.height
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(stroke.color),
                    style = DrawStroke(
                        width = stroke.width * size.width / window.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
        if (cropping) CropBox(edits.crop, edits.ratio.ratio, onCrop)
    }
}

private fun strokeOf(points: List<Offset>, ink: Int, nib: Float, canvas: Size, window: Rect) = Stroke(
    color = ink,
    width = nib / canvas.width * window.width,
    points = points.map {
        Offset(
            window.left + it.x / canvas.width * window.width,
            window.top + it.y / canvas.height * window.height,
        )
    },
)

private fun liveStroke(points: List<Offset>, ink: Int, nib: Float, canvas: Size, window: Rect) =
    if (points.size < 2 || canvas.minDimension <= 0f) emptyList()
    else listOf(strokeOf(points, ink, nib, canvas, window))

@Composable
private fun CropBox(crop: Rect, ratio: Float?, onCrop: (Rect) -> Unit) {
    val current by rememberUpdatedState(crop)
    var canvas by remember { mutableStateOf(Size.Zero) }
    var grip by remember { mutableStateOf(CropGrip.None) }
    val shade = Color(0x8C0B0B0C)
    val guide = Color(0x47EDEDEF)
    val edge = Color(0xD9EDEDEF)
    val corner = Color(0xFFEDEDEF)

    Canvas(
        Modifier.fillMaxSize().pointerInput(ratio) {
            detectDragGestures(
                onDragStart = { grip = gripAt(it, current.onCanvas(canvas)) },
                onDrag = { change, delta ->
                    if (grip == CropGrip.None) return@detectDragGestures
                    change.consume()
                    val moved = current.onCanvas(canvas).dragged(grip, delta, ratio, canvas)
                    onCrop(moved.toFractions(canvas))
                },
                onDragEnd = { grip = CropGrip.None },
                onDragCancel = { grip = CropGrip.None },
            )
        }
    ) {
        canvas = size
        val box = crop.onCanvas(size)
        drawRect(shade, size = Size(size.width, box.top))
        drawRect(shade, topLeft = Offset(0f, box.bottom), size = Size(size.width, size.height - box.bottom))
        drawRect(shade, topLeft = Offset(0f, box.top), size = Size(box.left, box.height))
        drawRect(shade, topLeft = Offset(box.right, box.top), size = Size(size.width - box.right, box.height))

        drawRect(edge, topLeft = box.topLeft, size = box.size, style = DrawStroke(width = 1.dp.toPx()))
        listOf(1f / 3f, 2f / 3f).forEach { fraction ->
            val x = box.left + box.width * fraction
            val y = box.top + box.height * fraction
            drawLine(guide, Offset(x, box.top), Offset(x, box.bottom))
            drawLine(guide, Offset(box.left, y), Offset(box.right, y))
        }
        val arm = 20.dp.toPx().coerceAtMost(box.minDimension / 3f)
        val thick = 3.dp.toPx()
        listOf(
            box.topLeft to (Offset(arm, 0f) to Offset(0f, arm)),
            box.topRight to (Offset(-arm, 0f) to Offset(0f, arm)),
            box.bottomLeft to (Offset(arm, 0f) to Offset(0f, -arm)),
            box.bottomRight to (Offset(-arm, 0f) to Offset(0f, -arm)),
        ).forEach { (origin, arms) ->
            drawLine(corner, origin, origin + arms.first, strokeWidth = thick)
            drawLine(corner, origin, origin + arms.second, strokeWidth = thick)
        }
    }
}

@Composable
private fun CropPanel(
    selected: CropRatio,
    onPick: (CropRatio) -> Unit,
    onRotate: (Boolean) -> Unit,
    onMirror: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            TurnButton(GalleryIcons.RotateLeft, stringResource(R.string.action_rotate_left)) { onRotate(false) }
            TurnButton(GalleryIcons.RotateRight, stringResource(R.string.action_rotate_right)) { onRotate(true) }
            TurnButton(GalleryIcons.Mirror, stringResource(R.string.action_mirror), onClick = onMirror)
        }
        RatioRow(selected, onPick)
    }
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
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
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
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(if (on) colors.accent else colors.surface)
                    .clickable { onPick(ratio) }
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AdjustPanel(edits: PhotoEdits, viewModel: EditorViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SliderRow(stringResource(R.string.editor_brightness), edits.brightness, viewModel::stepBrightness)
        SliderRow(stringResource(R.string.editor_contrast), edits.contrast, viewModel::stepContrast)
        SliderRow(stringResource(R.string.editor_saturation), edits.saturation, viewModel::stepSaturation)
    }
}

@Composable
private fun SliderRow(label: String, value: Int, onStep: (Int) -> Unit) {
    val colors = GalleryTheme.colors
    val fraction = (value - SLIDER_RANGE.first).toFloat() / (SLIDER_RANGE.last - SLIDER_RANGE.first)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = GalleryTheme.type.label, color = colors.textMuted, modifier = Modifier.width(72.dp))
        Step("−", enabled = value > SLIDER_RANGE.first) { onStep(-SLIDER_STEP) }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier.fillMaxWidth().height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.surfaceAlt)
            )
            Box(
                Modifier.fillMaxWidth(fraction).height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accent)
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(colors.accent))
            }
        }
        Step("+", enabled = value < SLIDER_RANGE.last) { onStep(SLIDER_STEP) }
        Text(
            if (value > 0) "+$value" else "$value",
            style = GalleryTheme.type.meta,
            color = colors.text,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
    }
}

@Composable
private fun Step(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = GalleryTheme.colors
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(ControlRadius))
            .background(colors.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = GalleryTheme.type.label, color = if (enabled) colors.text else colors.textFaint)
    }
}

@Composable
private fun FilterPanel(state: EditorUiState, onPick: (PhotoFilter) -> Unit) {
    val colors = GalleryTheme.colors
    val source = state.source
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PhotoFilter.entries.forEach { filter ->
            val on = filter == state.filterOrOriginal
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onPick(filter) },
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(ControlRadius))
                        .background(colors.surface)
                ) {
                    if (source != null) {
                        val thumb = remember(source) { source.asImageBitmap() }
                        val preview = remember(filter) {
                            ColorFilter.colorMatrix(ColorMatrix(PhotoEdits(filter = filter).colorMatrix().array))
                        }
                        Canvas(Modifier.fillMaxSize()) {
                            val square = min(thumb.width, thumb.height)
                            drawImage(
                                image = thumb,
                                srcOffset = IntOffset((thumb.width - square) / 2, (thumb.height - square) / 2),
                                srcSize = IntSize(square, square),
                                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = preview,
                                filterQuality = FilterQuality.Low,
                            )
                        }
                    }
                    if (on) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(ControlRadius))
                                .outline(colors.accent)
                        )
                    }
                }
                Text(
                    stringResource(filter.labelRes),
                    style = GalleryTheme.type.label.scaled(0.81f),
                    color = if (on) colors.accent else colors.textFaint,
                )
            }
        }
    }
}

private fun Modifier.outline(color: Color) = drawWithContent {
    drawContent()
    drawRect(color = color, style = DrawStroke(width = 2.dp.toPx()))
}

@Composable
private fun DrawPanel(state: EditorUiState, onInk: (Int) -> Unit, onNib: (Float) -> Unit) {
    val colors = GalleryTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.editor_color),
                style = GalleryTheme.type.label,
                color = colors.textMuted,
                modifier = Modifier.width(52.dp),
            )
            INKS.forEach { color ->
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .ring(if (color == state.ink) colors.accent else colors.hairline)
                        .clickable { onInk(color) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.editor_brush),
                style = GalleryTheme.type.label,
                color = colors.textMuted,
                modifier = Modifier.width(52.dp),
            )
            NIBS.forEach { nib ->
                val on = nib == state.nib
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(ControlRadius))
                        .background(if (on) colors.surfaceAlt else Color.Transparent)
                        .clickable { onNib(nib) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(nib.dp)
                            .clip(CircleShape)
                            .background(if (on) colors.accent else colors.textMuted)
                    )
                }
            }
        }
    }
}

private fun Modifier.ring(color: Color) = drawWithContent {
    drawContent()
    drawCircle(color = color, style = DrawStroke(width = 2.dp.toPx()))
}

@Composable
private fun TabBar(selected: EditorTab, onPick: (EditorTab) -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawTopHairline(colors.hairline),
    ) {
        EditorTab.entries.forEach { tab ->
            val on = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .then(if (on) Modifier.drawTopHairline(colors.accent, 2.dp) else Modifier)
                    .clickable { onPick(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    tab.icon,
                    contentDescription = stringResource(tab.labelRes),
                    tint = if (on) colors.accent else colors.textFaint,
                    modifier = Modifier.size(19.dp),
                )
                Box(Modifier.height(5.dp))
                Text(
                    stringResource(tab.labelRes),
                    style = GalleryTheme.type.label.scaled(0.77f),
                    color = if (on) colors.accent else colors.textFaint,
                )
            }
        }
    }
}

private fun Modifier.drawTopHairline(color: Color, thickness: Dp = 1.dp) = drawWithContent {
    drawContent()
    drawRect(color = color, size = Size(size.width, thickness.toPx()))
}

private fun TextStyle.scaled(factor: Float) = copy(fontSize = fontSize * factor)

private val EditorUiState.filterOrOriginal: PhotoFilter get() = edits.filter

private val EditorTab.icon
    get() = when (this) {
        EditorTab.Crop -> GalleryIcons.Crop
        EditorTab.Adjust -> GalleryIcons.Adjust
        EditorTab.Filter -> GalleryIcons.Filter
        EditorTab.Draw -> GalleryIcons.Draw
    }
