package app.simplifymy.gallery.feature.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import app.simplifymy.gallery.R
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing
import kotlin.math.roundToInt
import kotlin.math.sign

const val DISABLED = 0.38f
const val SELECTED = 0.16f

fun TextStyle.scaled(factor: Float) = copy(fontSize = fontSize * factor)

fun signed(value: Int) = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}

@Composable
fun ValueSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
    labelWidth: Dp = 84.dp,
    track: (DrawScope.() -> Unit)? = null,
) {
    val colors = GalleryTheme.colors
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val start by rememberUpdatedState(onStart)
    val end by rememberUpdatedState(onEnd)
    val span = (range.last - range.first).toFloat()
    val fraction = (value - range.first) / span
    val origin = ((0 - range.first) / span).coerceIn(0f, 1f)
    fun valueAt(x: Float, width: Int): Int = (range.first + (x / width).coerceIn(0f, 1f) * span).roundToInt()
    fun moveTo(next: Int) {
        if (next == current) return
        if (range.first < 0 && (next == 0 || current.sign * next.sign < 0)) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        change(next)
    }

    Row(
        modifier = modifier.fillMaxWidth().height(Spacing.hitTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (labelWidth > 0.dp) {
            Text(
                label,
                style = GalleryTheme.type.label,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(labelWidth),
            )
        }
        Canvas(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .semantics { contentDescription = label }
                .pointerInput(range) {
                    detectTapGestures(
                        onDoubleTap = { change(0.coerceIn(range)) },
                        onTap = { change(valueAt(it.x, size.width)) },
                    )
                }
                .pointerInput(range) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            start()
                            moveTo(valueAt(it.x, size.width))
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            moveTo(valueAt(change.position.x, size.width))
                        },
                        onDragEnd = { end() },
                        onDragCancel = { end() },
                    )
                }
        ) {
            val middle = size.height / 2f
            val thick = 3.dp.toPx()
            if (track != null) {
                track()
            } else {
                drawLine(colors.surfaceAlt, Offset(0f, middle), Offset(size.width, middle), thick)
                drawLine(colors.accent, Offset(size.width * origin, middle), Offset(size.width * fraction, middle), thick)
            }
            drawCircle(colors.background, radius = 9.dp.toPx(), center = Offset(size.width * fraction, middle))
            drawCircle(colors.accent, radius = 6.dp.toPx(), center = Offset(size.width * fraction, middle))
        }
        Text(
            if (range.first < 0) signed(value) else "$value",
            style = GalleryTheme.type.meta,
            color = colors.text,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp),
        )
    }
}

@Composable
fun ToolItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = GalleryTheme.colors
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(ControlRadius))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(ControlRadius))
                .background(if (selected) colors.accent.copy(alpha = SELECTED) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = when {
                    !enabled -> colors.textFaint
                    selected -> colors.accent
                    else -> colors.text
                },
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            label,
            style = GalleryTheme.type.label.scaled(0.8f).copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) colors.text else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PhotoTile(
    bitmap: Bitmap?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    overlay: (DrawScope.() -> Unit)? = null,
) {
    val colors = GalleryTheme.colors
    val shape = RoundedCornerShape(GalleryTheme.corners.radius)
    Column(
        modifier = Modifier.width(64.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(60.dp)
                .then(if (selected) Modifier.ring(colors.accent, colors.background) else Modifier)
                .padding(if (selected) 4.dp else 0.dp)
                .clip(shape)
                .background(colors.surface)
        ) {
            if (bitmap != null) {
                val image = remember(bitmap) { bitmap.asImageBitmap() }
                Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            if (overlay != null) Canvas(Modifier.fillMaxSize()) { overlay() }
        }
        Text(
            label,
            style = GalleryTheme.type.label.scaled(0.8f),
            color = if (selected) colors.accent else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ResetTile(onClick: () -> Unit) {
    val colors = GalleryTheme.colors
    Column(
        modifier = Modifier.width(64.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(60.dp).clip(RoundedCornerShape(GalleryTheme.corners.radius)).background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(GalleryIcons.Reset, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
        }
        Text(stringResource(R.string.action_reset), style = GalleryTheme.type.label.scaled(0.8f), color = colors.textMuted)
    }
}

fun Modifier.ring(color: Color, gap: Color) = drawWithContent {
    drawContent()
    val radius = 4.dp.toPx() + 4.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        style = DrawStroke(width = 2.dp.toPx()),
    )
}

@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        options.forEach { option ->
            val on = option == selected
            Text(
                label(option),
                style = GalleryTheme.type.label,
                color = if (on) colors.onAccent else colors.textMuted,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(if (on) colors.accent else colors.surface)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
fun TileRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 12.dp)),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

@Composable
fun ToolTopBar(
    title: String,
    start: String,
    onStart: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = GalleryTheme.colors
    Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp)) {
        Text(
            start,
            style = GalleryTheme.type.label,
            color = colors.textMuted,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(ControlRadius))
                .clickable(onClick = onStart)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
        Text(
            title,
            style = GalleryTheme.type.title,
            color = colors.text,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            stringResource(R.string.action_done),
            style = GalleryTheme.type.label.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accent,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(ControlRadius))
                .clickable(onClick = onDone)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun ToolFooter(title: String, onCancel: () -> Unit, onDone: () -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(GalleryIcons.Close, onClick = onCancel, label = stringResource(R.string.action_cancel), tint = colors.textMuted)
        Text(
            title,
            style = GalleryTheme.type.title,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconAction(GalleryIcons.Check, onClick = onDone, label = stringResource(R.string.action_done), tint = colors.accent)
    }
}

@Composable
fun SwatchDot(color: Int, selected: Boolean, onClick: () -> Unit, size: Dp = 28.dp) {
    val colors = GalleryTheme.colors
    Box(
        Modifier
            .size(size + 8.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size + 8.dp)) {
            val radius = (size / 2).toPx()
            drawCircle(Color(color), radius = radius)
            drawCircle(colors.hairline, radius = radius, style = DrawStroke(width = 1.dp.toPx()))
            if (selected) drawCircle(colors.accent, radius = radius + 3.dp.toPx(), style = DrawStroke(width = 2.dp.toPx()))
        }
    }
}
