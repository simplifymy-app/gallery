package app.simplifymy.gallery.feature.editor

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.R
import app.simplifymy.gallery.ui.component.SegmentedPicker
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CurvesPanel(state: EditorUiState, viewModel: EditorViewModel) {
    val edits = state.edits
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedPicker(
                options = CurvesMode.entries,
                selected = state.curvesMode,
                onSelect = viewModel::pickCurvesMode,
                label = { stringResource(it.labelRes) },
            )
            Spacer(Modifier.weight(1f))
            if (state.curvesMode == CurvesMode.Curves) {
                CurveChannel.entries.forEach { channel ->
                    SwatchDot(channel.swatch, selected = state.channel == channel, onClick = { viewModel.pickChannel(channel) }, size = 20.dp)
                }
            }
        }
        when (state.curvesMode) {
            CurvesMode.Curves -> CurveGraph(
                points = edits.curve(state.channel),
                line = if (state.channel == CurveChannel.Rgb) GalleryTheme.colors.text else Color(state.channel.swatch),
                fonts = viewModel.fonts,
                onChange = { viewModel.setCurve(state.channel, it) },
            )
            CurvesMode.Hsl -> HslPanel(state, viewModel)
            CurvesMode.Split -> SplitPanel(edits.split, viewModel)
        }
    }
}

@Composable
private fun CurveGraph(points: List<Offset>, line: Color, fonts: EditorFonts, onChange: (List<Offset>) -> Unit) {
    val colors = GalleryTheme.colors
    val current by rememberUpdatedState(points)
    val change by rememberUpdatedState(onChange)
    var active by remember { mutableIntStateOf(-1) }
    var leaving by remember { mutableStateOf(false) }
    val label = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = fonts.mono } }

    fun toCurve(position: Offset, size: Size) = Offset(position.x / size.width, 1f - position.y / size.height)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(ControlRadius))
            .background(colors.surface)
            .pointerInput(Unit) {
                val bounds = Size(size.width.toFloat(), size.height.toFloat())
                detectTapGestures(
                    onTap = { position ->
                        addedCurvePoint(current, toCurve(position, bounds))?.let { (next, _) -> change(next) }
                    },
                    onDoubleTap = { position ->
                        val nearest = nearestPoint(current, position, bounds, 28.dp.toPx())
                        if (nearest in 1 until current.lastIndex) change(current.filterIndexed { i, _ -> i != nearest })
                    },
                )
            }
            .pointerInput(Unit) {
                val bounds = Size(size.width.toFloat(), size.height.toFloat())
                detectDragGestures(
                    onDragStart = { position ->
                        val nearest = nearestPoint(current, position, bounds, 28.dp.toPx())
                        active = if (nearest >= 0) nearest else {
                            addedCurvePoint(current, toCurve(position, bounds))?.let { (next, index) ->
                                change(next)
                                index
                            } ?: -1
                        }
                    },
                    onDrag = { change, _ ->
                        if (active < 0) return@detectDragGestures
                        change.consume()
                        val point = toCurve(change.position, bounds)
                        leaving = active in 1 until current.lastIndex && (point.y < -0.15f || point.y > 1.15f)
                        change(movedCurvePoint(current, active, point))
                    },
                    onDragEnd = {
                        if (leaving && active in 1 until current.lastIndex) {
                            change(current.filterIndexed { i, _ -> i != active })
                        }
                        active = -1
                        leaving = false
                    },
                    onDragCancel = {
                        active = -1
                        leaving = false
                    },
                )
            }
    ) {
        for (step in 1..3) {
            val x = size.width * step / 4f
            val y = size.height * step / 4f
            drawLine(colors.hairline, Offset(x, 0f), Offset(x, size.height))
            drawLine(colors.hairline, Offset(0f, y), Offset(size.width, y))
        }
        drawLine(
            colors.textFaint,
            Offset(0f, size.height),
            Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
        )
        val curve = MonotoneCurve(points)
        val path = Path().apply {
            for (step in 0..64) {
                val x = step / 64f
                val px = x * size.width
                val py = (1f - curve.at(x).coerceIn(0f, 1f)) * size.height
                if (step == 0) moveTo(px, py) else lineTo(px, py)
            }
        }
        drawPath(path, line, style = DrawStroke(width = 1.5.dp.toPx()))
        points.forEachIndexed { index, point ->
            val center = Offset(point.x * size.width, (1f - point.y) * size.height)
            if (index == active) {
                drawCircle(colors.accent, radius = 7.dp.toPx(), center = center)
            } else {
                drawCircle(colors.surface, radius = 6.dp.toPx(), center = center)
                drawCircle(line, radius = 6.dp.toPx(), center = center, style = DrawStroke(width = 1.5.dp.toPx()))
            }
        }
        points.getOrNull(active)?.let { point -> drawReadout(point, label, colors.text, colors.background) }
    }
}

private fun nearestPoint(points: List<Offset>, position: Offset, size: Size, reach: Float): Int {
    val index = points.indices.minByOrNull {
        (Offset(points[it].x * size.width, (1f - points[it].y) * size.height) - position).getDistance()
    } ?: return -1
    val distance = (Offset(points[index].x * size.width, (1f - points[index].y) * size.height) - position).getDistance()
    return if (distance <= reach) index else -1
}

private fun DrawScope.drawReadout(point: Offset, paint: Paint, fill: Color, ink: Color) {
    val text = String.format(Locale.ROOT, "%d → %d", (point.x * 100).roundToInt(), (point.y * 100).roundToInt())
    paint.textSize = 11.dp.toPx()
    paint.color = ink.toArgb()
    val width = paint.measureText(text) + 16.dp.toPx()
    val height = 22.dp.toPx()
    val x = (point.x * size.width - width / 2f).coerceIn(0f, size.width - width)
    val y = ((1f - point.y) * size.height - height - 12.dp.toPx()).coerceIn(0f, size.height - height)
    drawRoundRect(fill, Offset(x, y), Size(width, height), CornerRadius(height / 2f, height / 2f))
    drawIntoCanvas { it.nativeCanvas.drawText(text, x + 8.dp.toPx(), y + height / 2f + paint.textSize / 3f, paint) }
}

@Composable
private fun HslPanel(state: EditorUiState, viewModel: EditorViewModel) {
    val band = state.band
    val shift = state.edits.shift(band)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${stringResource(R.string.editor_hsl)} · ${stringResource(band.labelRes)}".uppercase(Locale.getDefault()),
            style = GalleryTheme.type.section,
            color = GalleryTheme.colors.textMuted,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HueBand.entries.forEach { entry ->
                SwatchDot(entry.swatch, selected = entry == band, onClick = { viewModel.pickBand(entry) }, size = 24.dp)
            }
        }
        ValueSlider(
            label = stringResource(R.string.editor_hue),
            value = shift.hue,
            range = -100..100,
            onChange = { viewModel.setShift(band, shift.copy(hue = it)) },
            onStart = viewModel::beginGesture,
            onEnd = viewModel::endGesture,
            track = { hueTrack(band.hue - 30f, band.hue + 30f) },
        )
        ValueSlider(
            label = stringResource(R.string.editor_saturation),
            value = shift.saturation,
            range = -100..100,
            onChange = { viewModel.setShift(band, shift.copy(saturation = it)) },
            onStart = viewModel::beginGesture,
            onEnd = viewModel::endGesture,
        )
        ValueSlider(
            label = stringResource(R.string.editor_luminance),
            value = shift.luminance,
            range = -100..100,
            onChange = { viewModel.setShift(band, shift.copy(luminance = it)) },
            onStart = viewModel::beginGesture,
            onEnd = viewModel::endGesture,
        )
    }
}

@Composable
private fun SplitPanel(split: SplitTone, viewModel: EditorViewModel) {
    Column(Modifier.height(240.dp).verticalScroll(rememberScrollState())) {
        ValueSlider(
            label = stringResource(R.string.split_highlights_hue),
            value = split.highlightHue,
            range = 0..360,
            onChange = { viewModel.setSplit(split.copy(highlightHue = it)) },
            onStart = viewModel::beginGesture,
            onEnd = viewModel::endGesture,
            labelWidth = 104.dp,
            track = { hueTrack(0f, 360f) },
        )
        ValueSlider(
            label = stringResource(R.string.split_highlights),
            value = split.highlightAmount,
            range = 0..100,
            onChange = { viewModel.setSplit(split.copy(highlightAmount = it)) },
            onStart = viewModel::beginGesture,
            onEnd = viewModel::endGesture,
            labelWidth = 104.dp,
        )
        ValueSlider(
            label = stringResource(R.string.split_shadows_hue),
            value = split.shadowHue,
            range = 0..360,
            onChange = { viewModel.setSplit(split.copy(shadowHue = it)) },
            onStart = viewModel::beginGesture,
            onEnd = viewModel::endGesture,
            labelWidth = 104.dp,
            track = { hueTrack(0f, 360f) },
        )
        ValueSlider(
            label = stringResource(R.string.split_shadows),
            value = split.shadowAmount,
            range = 0..100,
            onChange = { viewModel.setSplit(split.copy(shadowAmount = it)) },
            onStart = viewModel::beginGesture,
            onEnd = viewModel::endGesture,
            labelWidth = 104.dp,
        )
        ValueSlider(
            label = stringResource(R.string.split_balance),
            value = split.balance,
            range = -100..100,
            onChange = { viewModel.setSplit(split.copy(balance = it)) },
            onStart = viewModel::beginGesture,
            onEnd = viewModel::endGesture,
            labelWidth = 104.dp,
        )
    }
}

private fun DrawScope.hueTrack(from: Float, to: Float) {
    val stops = (0..12).map { step ->
        val rgb = hueColor(from + (to - from) * step / 12f)
        Color(rgb[0], rgb[1], rgb[2])
    }
    val middle = size.height / 2f
    drawLine(Brush.horizontalGradient(stops), Offset(0f, middle), Offset(size.width, middle), strokeWidth = 3.dp.toPx())
}
