package app.simplifymy.gallery.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.ui.theme.GalleryTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Lightness stays inside this band so a custom accent can never lose contrast on either theme. */
private val SAFE_LIGHTNESS = 0.46f..0.78f

private val HUE_TRACK = Brush.horizontalGradient(
    (0..6).map { Color.hsl(it * 60f, 0.72f, 0.62f) }
)

@Composable
fun AccentPicker(argb: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = GalleryTheme.colors
    val (hue, saturation, lightness) = Color(argb).toHsl()

    fun emit(h: Float = hue, s: Float = saturation, l: Float = lightness) =
        onChange(Color.hsl(h, s, l.coerceIn(SAFE_LIGHTNESS)).toArgb())

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(argb)))
            Text(
                "#%06X".format(argb and 0xFFFFFF),
                style = GalleryTheme.type.meta,
                color = colors.textMuted,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(HUE_TRACK)
        )
        Track("Hue", hue / 360f) { emit(h = it * 360f) }
        Track("Saturation", saturation) { emit(s = it) }
        Track("Lightness", lightness.normalize()) { emit(l = SAFE_LIGHTNESS.lerp(it)) }
    }
}

@Composable
private fun Track(label: String, value: Float, onChange: (Float) -> Unit) {
    val colors = GalleryTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = GalleryTheme.type.meta, color = colors.textFaint, modifier = Modifier.size(width = 66.dp, height = 16.dp))
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.surfaceAlt,
            ),
        )
    }
}

private fun ClosedFloatingPointRange<Float>.lerp(t: Float) = start + (endInclusive - start) * t

private fun Float.normalize() =
    ((this - SAFE_LIGHTNESS.start) / (SAFE_LIGHTNESS.endInclusive - SAFE_LIGHTNESS.start)).coerceIn(0f, 1f)

private fun Color.toHsl(): Triple<Float, Float, Float> {
    val max = max(red, max(green, blue))
    val min = min(red, min(green, blue))
    val lightness = (max + min) / 2f
    if (abs(max - min) < 1e-4f) return Triple(0f, 0f, lightness)

    val delta = max - min
    val saturation = delta / (1f - abs(2f * lightness - 1f))
    val hue = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    return Triple((hue + 360f) % 360f, saturation.coerceIn(0f, 1f), lightness)
}
