package app.simplifymy.gallery.feature.editor

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

fun PhotoEdits.total(adjustment: Adjustment): Int {
    val fromLooks = looks.sumOf { it.adjust[adjustment] ?: 0 }
    val fromCamera = if (adjustment == Adjustment.Fade && camera != null) cameraSettings.fade else 0
    return (this[adjustment] + fromLooks + fromCamera).coerceIn(adjustment.range)
}

class PhotoGrade(edits: PhotoEdits) {

    private val tone: Array<IntArray>? = toneCurves(edits)
    private val matrix: FloatArray? = colorMatrix(edits).takeUnless { it.contentEquals(IDENTITY) }
    private val vibrance: Float = edits.total(Adjustment.Vibrance) / 100f
    private val bands: List<Pair<HueBand, HslShift>> = edits.hsl.entries.map { it.key to it.value }
    private val split: SplitTone = edits.split.takeUnless { it.isNeutral }
        ?: edits.looks.map { it.split }.lastOrNull { !it.isNeutral }
        ?: SplitTone()
    private val highlightTint = hueColor(split.highlightHue.toFloat())
    private val shadowTint = hueColor(split.shadowHue.toFloat())

    val isIdentity: Boolean
        get() = tone == null && matrix == null && vibrance == 0f && bands.isEmpty() && split.isNeutral

    fun apply(pixels: IntArray) {
        if (isIdentity) return
        val rgb = FloatArray(3)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            rgb[0] = (pixel shr 16 and 0xFF).toFloat()
            rgb[1] = (pixel shr 8 and 0xFF).toFloat()
            rgb[2] = (pixel and 0xFF).toFloat()
            grade(rgb)
            pixels[i] = (pixel and ALPHA) or (channel(rgb[0]) shl 16) or (channel(rgb[1]) shl 8) or channel(rgb[2])
        }
    }

    private fun grade(rgb: FloatArray) {
        val tone = tone
        if (tone != null) {
            rgb[0] = tone[0][rgb[0].toInt()].toFloat()
            rgb[1] = tone[1][rgb[1].toInt()].toFloat()
            rgb[2] = tone[2][rgb[2].toInt()].toFloat()
        }
        val m = matrix
        if (m != null) {
            val r = rgb[0]
            val g = rgb[1]
            val b = rgb[2]
            rgb[0] = m[0] * r + m[1] * g + m[2] * b + m[4]
            rgb[1] = m[5] * r + m[6] * g + m[7] * b + m[9]
            rgb[2] = m[10] * r + m[11] * g + m[12] * b + m[14]
        }
        if (bands.isNotEmpty()) shiftBands(rgb)
        if (vibrance != 0f) {
            val high = maxOf(rgb[0], rgb[1], rgb[2]).coerceIn(0f, 255f)
            val low = minOf(rgb[0], rgb[1], rgb[2]).coerceIn(0f, 255f)
            val amount = 1f + vibrance * (1f - (high - low) / 255f)
            val luma = luma(rgb)
            for (c in 0..2) rgb[c] = luma + (rgb[c] - luma) * amount
        }
        if (!split.isNeutral) {
            val level = (luma(rgb) / 255f).coerceIn(0f, 1f)
            val pivot = 0.5f - split.balance / 200f
            val high = ((level - pivot) / (1f - pivot)).coerceIn(0f, 1f) * split.highlightAmount / 100f
            val low = ((pivot - level) / pivot).coerceIn(0f, 1f) * split.shadowAmount / 100f
            for (c in 0..2) rgb[c] += ((highlightTint[c] - 0.5f) * high + (shadowTint[c] - 0.5f) * low) * SPLIT_STRENGTH
        }
    }

    private fun shiftBands(rgb: FloatArray) {
        val r = (rgb[0] / 255f).coerceIn(0f, 1f)
        val g = (rgb[1] / 255f).coerceIn(0f, 1f)
        val b = (rgb[2] / 255f).coerceIn(0f, 1f)
        val high = maxOf(r, g, b)
        val low = minOf(r, g, b)
        val lightness = (high + low) / 2f
        val chroma = high - low
        if (chroma < 1e-4f) return
        val saturation = chroma / (1f - abs(2f * lightness - 1f)).coerceAtLeast(1e-4f)
        var hue = when (high) {
            r -> 60f * (((g - b) / chroma).mod(6f))
            g -> 60f * ((b - r) / chroma + 2f)
            else -> 60f * ((r - g) / chroma + 4f)
        }
        var satScale = 1f
        var lightShift = 0f
        var hueShift = 0f
        bands.forEach { (band, shift) ->
            val distance = abs(((hue - band.hue + 540f) % 360f) - 180f)
            val weight = (1f - distance / BAND_WIDTH).coerceAtLeast(0f)
            if (weight == 0f) return@forEach
            hueShift += weight * shift.hue / 100f * HUE_REACH
            satScale += weight * shift.saturation / 100f
            lightShift += weight * shift.luminance / 100f * LIGHT_REACH * saturation
        }
        hue = (hue + hueShift + 360f) % 360f
        val s = (saturation * satScale).coerceIn(0f, 1f)
        val l = (lightness + lightShift).coerceIn(0f, 1f)
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((hue / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (nr, ng, nb) = when ((hue / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        rgb[0] = (nr + m) * 255f
        rgb[1] = (ng + m) * 255f
        rgb[2] = (nb + m) * 255f
    }

    private companion object {
        const val ALPHA = 0xFF shl 24
        const val BAND_WIDTH = 45f
        const val HUE_REACH = 30f
        const val LIGHT_REACH = 0.25f
        const val SPLIT_STRENGTH = 90f
        fun channel(value: Float): Int = value.roundToInt().coerceIn(0, 255)
        fun luma(rgb: FloatArray) = 0.299f * rgb[0] + 0.587f * rgb[1] + 0.114f * rgb[2]
    }
}

fun hueColor(hue: Float): FloatArray {
    val h = ((hue % 360f) + 360f) % 360f / 60f
    val x = 1f - abs(h % 2f - 1f)
    return when (h.toInt()) {
        0 -> floatArrayOf(1f, x, 0f)
        1 -> floatArrayOf(x, 1f, 0f)
        2 -> floatArrayOf(0f, 1f, x)
        3 -> floatArrayOf(0f, x, 1f)
        4 -> floatArrayOf(x, 0f, 1f)
        else -> floatArrayOf(1f, 0f, x)
    }
}

private val IDENTITY = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

fun toneCurve(edits: PhotoEdits): IntArray? = toneCurves(edits)?.get(0)

fun toneCurves(edits: PhotoEdits): Array<IntArray>? {
    val exposure = 2f.pow(edits.total(Adjustment.Exposure) / 100f * 1.5f)
    val highlights = edits.total(Adjustment.Highlights) / 100f
    val shadows = edits.total(Adjustment.Shadows) / 100f
    val whites = edits.total(Adjustment.Whites) / 100f
    val blacks = edits.total(Adjustment.Blacks) / 100f
    val fade = edits.total(Adjustment.Fade) / 100f
    val lookCurves = edits.looks.map { it.curve }.filter { it != IDENTITY_CURVE }.map(::MonotoneCurve)
    val master = edits.curves[CurveChannel.Rgb]?.let(::MonotoneCurve)
    val channels = listOf(CurveChannel.Red, CurveChannel.Green, CurveChannel.Blue).map { edits.curves[it]?.let(::MonotoneCurve) }
    val flat = exposure == 1f && highlights == 0f && shadows == 0f && whites == 0f && blacks == 0f && fade == 0f
    if (flat && lookCurves.isEmpty() && master == null && channels.all { it == null }) return null
    val shared = FloatArray(256) { level ->
        var x = (level / 255f * exposure).coerceIn(0f, 1f)
        val rest = 1f - x
        x += shadows * BAND * x * rest * rest
        x += highlights * BAND * x * x * rest
        x += whites * END * x * x * x
        x += blacks * END * rest * rest * rest
        x = x.coerceIn(0f, 1f)
        x = FADE_LIFT * fade + x * (1f - FADE_SPAN * fade)
        lookCurves.forEach { x = it.at(x) }
        master?.let { x = it.at(x) }
        x
    }
    return Array(3) { c ->
        val own = channels[c]
        IntArray(256) { level ->
            val x = own?.at(shared[level]) ?: shared[level]
            (x.coerceIn(0f, 1f) * 255f).roundToInt()
        }
    }
}

private const val BAND = 6.75f * 0.14f
private const val END = 0.2f
private const val FADE_LIFT = 0.14f
private const val FADE_SPAN = 0.2f

class MonotoneCurve(points: List<Offset>) {
    private val xs = points.map { it.x }.toFloatArray()
    private val ys = points.map { it.y }.toFloatArray()
    private val slopes = FloatArray(xs.size)

    init {
        val n = xs.size
        val deltas = FloatArray((n - 1).coerceAtLeast(0)) { (ys[it + 1] - ys[it]) / (xs[it + 1] - xs[it]).coerceAtLeast(1e-6f) }
        if (n == 2) {
            slopes[0] = deltas[0]
            slopes[1] = deltas[0]
        } else if (n > 2) {
            slopes[0] = deltas[0]
            slopes[n - 1] = deltas[n - 2]
            for (i in 1 until n - 1) {
                slopes[i] = if (deltas[i - 1] * deltas[i] <= 0f) 0f else (deltas[i - 1] + deltas[i]) / 2f
            }
            for (i in 0 until n - 1) {
                if (deltas[i] == 0f) {
                    slopes[i] = 0f
                    slopes[i + 1] = 0f
                    continue
                }
                val a = slopes[i] / deltas[i]
                val b = slopes[i + 1] / deltas[i]
                val length = a * a + b * b
                if (length > 9f) {
                    val t = 3f / kotlin.math.sqrt(length)
                    slopes[i] = t * a * deltas[i]
                    slopes[i + 1] = t * b * deltas[i]
                }
            }
        }
    }

    fun at(x: Float): Float {
        if (x <= xs.first()) return ys.first()
        if (x >= xs.last()) return ys.last()
        var i = 0
        while (x > xs[i + 1]) i++
        val h = xs[i + 1] - xs[i]
        val t = (x - xs[i]) / h
        val t2 = t * t
        val t3 = t2 * t
        return (2 * t3 - 3 * t2 + 1) * ys[i] + (t3 - 2 * t2 + t) * h * slopes[i] +
            (-2 * t3 + 3 * t2) * ys[i + 1] + (t3 - t2) * h * slopes[i + 1]
    }
}

fun colorMatrix(edits: PhotoEdits): FloatArray {
    val temperature = edits.total(Adjustment.Temperature) / 100f
    val tint = edits.total(Adjustment.Tint) / 100f
    val hue = edits.looks.sumOf { it.hue.toDouble() }.toFloat()
    return hueRotate(hue) then
        saturate(1f - edits.monoAmount) then
        brightness(1f + edits.total(Adjustment.Brightness) / 100f) then
        contrast(1f + edits.total(Adjustment.Contrast) / 100f) then
        saturate(1f + edits.total(Adjustment.Saturation) / 100f) then
        scale(1f + 0.2f * temperature + 0.1f * tint, 1f - 0.2f * tint, 1f - 0.2f * temperature + 0.1f * tint)
}

infix fun FloatArray.then(next: FloatArray): FloatArray {
    val out = FloatArray(20)
    for (row in 0 until 4) {
        for (column in 0 until 5) {
            var sum = if (column == 4) next[row * 5 + 4] else 0f
            for (k in 0 until 4) sum += next[row * 5 + k] * this[k * 5 + column]
            out[row * 5 + column] = sum
        }
    }
    return out
}

private fun scale(r: Float, g: Float, b: Float) = floatArrayOf(
    r, 0f, 0f, 0f, 0f,
    0f, g, 0f, 0f, 0f,
    0f, 0f, b, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

private fun brightness(amount: Float) = scale(amount, amount, amount)

private fun saturate(amount: Float): FloatArray {
    val inverse = 1f - amount
    val r = 0.213f * inverse
    val g = 0.715f * inverse
    val b = 0.072f * inverse
    return floatArrayOf(
        r + amount, g, b, 0f, 0f,
        r, g + amount, b, 0f, 0f,
        r, g, b + amount, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun contrast(amount: Float): FloatArray {
    val shift = (1f - amount) / 2f * 255f
    return floatArrayOf(
        amount, 0f, 0f, 0f, shift,
        0f, amount, 0f, 0f, shift,
        0f, 0f, amount, 0f, shift,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun hueRotate(degrees: Float): FloatArray {
    if (degrees == 0f) return IDENTITY
    val radians = degrees * Math.PI.toFloat() / 180f
    val c = cos(radians)
    val s = sin(radians)
    return floatArrayOf(
        0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
        0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
        0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}
