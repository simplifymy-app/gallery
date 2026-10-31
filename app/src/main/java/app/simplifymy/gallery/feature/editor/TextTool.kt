package app.simplifymy.gallery.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.R
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val EDGE = Color(0xD9EDEDEF)

@Composable
fun TextOverlay(texts: List<TextLayer>, selected: Long?, viewModel: EditorViewModel) {
    val colors = GalleryTheme.colors
    val layers by rememberUpdatedState(texts)
    val chosen by rememberUpdatedState(selected)
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val active = texts.firstOrNull { it.id == selected }
        val box = active?.let { textLayout(it, width, viewModel.fonts) }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { point ->
                        val hit = layers.lastOrNull { layer ->
                            val layout = textLayout(layer, size.width.toFloat(), viewModel.fonts)
                            hits(layer, layout.width.toFloat(), layout.height.toFloat(), point, size.width.toFloat(), size.height.toFloat())
                        }
                        viewModel.selectText(hit?.id)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val id = chosen ?: return@detectTransformGestures
                        val layer = layers.firstOrNull { it.id == id } ?: return@detectTransformGestures
                        if (layer.locked) return@detectTransformGestures
                        viewModel.updateText(id) {
                            it.copy(
                                center = Offset(
                                    (it.center.x + pan.x / size.width).coerceIn(0f, 1f),
                                    (it.center.y + pan.y / size.height).coerceIn(0f, 1f),
                                ),
                                size = (it.size * zoom).coerceIn(0.02f, 0.4f),
                                rotation = it.rotation + rotation,
                            )
                        }
                    }
                }
        ) {
            if (active == null || box == null) return@Canvas
            val center = Offset(active.center.x * size.width, active.center.y * size.height)
            val pad = 6.dp.toPx()
            val half = Size(box.width / 2f + pad, box.height / 2f + pad)
            rotate(active.rotation, center) {
                drawRect(
                    color = EDGE,
                    topLeft = center - Offset(half.width, half.height),
                    size = Size(half.width * 2f, half.height * 2f),
                    style = DrawStroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                )
                listOf(
                    Offset(-half.width, -half.height),
                    Offset(half.width, -half.height),
                    Offset(half.width, half.height),
                    Offset(-half.width, half.height),
                ).forEach { drawCircle(EDGE, radius = 4.dp.toPx(), center = center + it) }
            }
        }

        if (active != null && box != null) {
            val toolbarWidth = with(density) { 120.dp.toPx() }
            val toolbarHeight = with(density) { 40.dp.toPx() }
            val x = (active.center.x * width - toolbarWidth / 2f).coerceIn(0f, (width - toolbarWidth).coerceAtLeast(0f))
            val y = (active.center.y * height - box.height / 2f - toolbarHeight - with(density) { 14.dp.toPx() })
                .coerceIn(0f, (height - toolbarHeight).coerceAtLeast(0f))
            Row(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(colors.surfaceAlt),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                LayerAction(GalleryIcons.Trash, stringResource(R.string.action_delete)) { viewModel.deleteText(active.id) }
                LayerAction(GalleryIcons.Duplicate, stringResource(R.string.action_duplicate)) { viewModel.duplicateText(active.id) }
                LayerAction(
                    if (active.locked) GalleryIcons.Lock else GalleryIcons.Unlock,
                    stringResource(if (active.locked) R.string.action_unlock else R.string.action_lock),
                ) { viewModel.updateText(active.id) { it.copy(locked = !it.locked) } }
            }
        }
    }
}

@Composable
private fun LayerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = GalleryTheme.colors.text, modifier = Modifier.size(18.dp))
    }
}

private fun hits(layer: TextLayer, boxWidth: Float, boxHeight: Float, point: Offset, width: Float, height: Float): Boolean {
    val dx = point.x - layer.center.x * width
    val dy = point.y - layer.center.y * height
    val radians = -layer.rotation * Math.PI.toFloat() / 180f
    val x = dx * cos(radians) - dy * sin(radians)
    val y = dx * sin(radians) + dy * cos(radians)
    val slop = 16f
    return abs(x) <= boxWidth / 2f + slop && abs(y) <= boxHeight / 2f + slop
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun TextPanel(state: EditorUiState, viewModel: EditorViewModel) {
    val colors = GalleryTheme.colors
    val layer = state.edits.texts.firstOrNull { it.id == state.selectedText }
    val focus = remember { FocusRequester() }

    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = PANEL_HEIGHT).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
            if (layer != null) {
                BasicTextField(
                    value = layer.text,
                    onValueChange = { text -> viewModel.updateText(layer.id) { it.copy(text = text) } },
                    textStyle = GalleryTheme.type.body.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ControlRadius))
                        .background(colors.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .focusRequester(focus),
                    decorationBox = { field ->
                        Box {
                            if (layer.text.isEmpty()) {
                                Text(stringResource(R.string.text_placeholder), style = GalleryTheme.type.body, color = colors.textFaint)
                            }
                            field()
                        }
                    },
                )
                LaunchedEffect(layer.id) { if (layer.text.isEmpty()) focus.requestFocus() }
            } else {
                Text(
                    stringResource(R.string.text_hint),
                    style = GalleryTheme.type.body,
                    color = colors.textMuted,
                    modifier = Modifier.weight(1f),
                )
            }
            IconAction(GalleryIcons.Add, onClick = viewModel::addText, label = stringResource(R.string.text_add), tint = colors.accent)
        }
        if (layer != null) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                TextFont.entries.forEach { font ->
                    val on = font == layer.font
                    val family = remember(font) {
                        FontFamily(
                            Font(
                                font.fontRes,
                                FontWeight(font.weight),
                                variationSettings = FontVariation.Settings(FontVariation.weight(font.weight)),
                            )
                        )
                    }
                    Text(
                        font.label,
                        style = GalleryTheme.type.label.copy(fontFamily = family),
                        color = if (on) colors.onAccent else colors.text,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(ControlRadius))
                            .background(if (on) colors.accent else colors.surface)
                            .clickable { viewModel.updateText(layer.id) { it.copy(font = font) } }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                INKS.forEach { ink ->
                    SwatchDot(ink, selected = layer.color == ink, onClick = { viewModel.updateText(layer.id) { it.copy(color = ink) } }, size = 22.dp)
                }
                Spacer(Modifier.weight(1f))
                IconAction(
                    GalleryIcons.Plate,
                    onClick = { viewModel.updateText(layer.id) { it.copy(plate = !it.plate) } },
                    label = stringResource(R.string.text_plate),
                    tint = if (layer.plate) colors.accent else colors.textMuted,
                )
                IconAction(
                    when (layer.align) {
                        TextAlign.Start -> GalleryIcons.AlignLeft
                        TextAlign.Center -> GalleryIcons.AlignCenter
                        TextAlign.End -> GalleryIcons.AlignRight
                    },
                    onClick = {
                        viewModel.updateText(layer.id) {
                            it.copy(align = TextAlign.entries[(it.align.ordinal + 1) % TextAlign.entries.size])
                        }
                    },
                    label = stringResource(R.string.text_align),
                    tint = colors.textMuted,
                )
            }
        }
    }
}
