package app.simplifymy.gallery.feature.editor

import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import app.simplifymy.gallery.R
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class EditorTab(@StringRes val labelRes: Int) {
    Presets(R.string.editor_presets),
    Adjust(R.string.editor_adjust),
    Effects(R.string.editor_effects),
    Crop(R.string.editor_crop),
    Markup(R.string.editor_markup),
    Text(R.string.editor_text),
}

enum class EditorTool { Crop, Curves, Camera, Markup, Text }

enum class CropMode(@StringRes val labelRes: Int) {
    Crop(R.string.editor_crop),
    Straighten(R.string.editor_straighten),
    Perspective(R.string.editor_perspective),
}

enum class CurvesMode(@StringRes val labelRes: Int, @StringRes val titleRes: Int) {
    Curves(R.string.editor_curves, R.string.editor_title_curves),
    Hsl(R.string.editor_hsl, R.string.editor_title_hsl),
    Split(R.string.editor_split, R.string.editor_title_split),
}

@Serializable
enum class MarkupTool(@StringRes val labelRes: Int, val scale: Float, val opacity: Float, val shape: Boolean = false) {
    Pen(R.string.markup_pen, 1f, 1f),
    Pencil(R.string.markup_pencil, 0.6f, 0.85f),
    Marker(R.string.markup_marker, 2.2f, 0.9f),
    Highlighter(R.string.markup_highlighter, 4f, 0.4f),
    Line(R.string.markup_line, 1f, 1f, shape = true),
    Arrow(R.string.markup_arrow, 1f, 1f, shape = true),
    Rectangle(R.string.markup_rectangle, 1f, 1f, shape = true),
    Ellipse(R.string.markup_ellipse, 1f, 1f, shape = true),
    Eraser(R.string.markup_eraser, 3f, 0f),
}

@Serializable
enum class CropRatio(val ratio: Float?, @StringRes val labelRes: Int? = null, val label: String = "") {
    Free(null, labelRes = R.string.crop_free),
    Original(null, labelRes = R.string.crop_original),
    Square(1f, label = "1:1"),
    Portrait(4f / 5f, label = "4:5"),
    Photo(4f / 3f, label = "4:3"),
    Classic(3f / 2f, label = "3:2"),
    Wide(16f / 9f, label = "16:9"),
    Tall(9f / 16f, label = "9:16"),
    ;

    fun aspect(width: Int, height: Int): Float? = if (this == Original) width.toFloat() / height else ratio
}

@Serializable
enum class Adjustment(@StringRes val labelRes: Int, val bipolar: Boolean = true) {
    Exposure(R.string.editor_exposure),
    Brightness(R.string.editor_brightness),
    Contrast(R.string.editor_contrast),
    Highlights(R.string.editor_highlights),
    Shadows(R.string.editor_shadows),
    Whites(R.string.editor_whites),
    Blacks(R.string.editor_blacks),
    Saturation(R.string.editor_saturation),
    Vibrance(R.string.editor_vibrance),
    Temperature(R.string.editor_temperature),
    Tint(R.string.editor_tint),
    Fade(R.string.editor_fade, bipolar = false),
    Clarity(R.string.editor_clarity),
    Sharpen(R.string.editor_sharpen, bipolar = false),
    Glow(R.string.editor_glow, bipolar = false),
    Grain(R.string.editor_grain, bipolar = false),
    Vignette(R.string.editor_vignette),
    ;

    val range: IntRange get() = if (bipolar) -100..100 else 0..100
}

@Serializable
enum class CurveChannel(val swatch: Int) {
    Rgb(0xFFEDEDEF.toInt()),
    Red(0xFFE0574A.toInt()),
    Green(0xFF5FBF8F.toInt()),
    Blue(0xFF6E8BFF.toInt()),
}

@Serializable
enum class HueBand(@StringRes val labelRes: Int, val hue: Float, val swatch: Int) {
    Red(R.string.hue_red, 0f, 0xFFE0574A.toInt()),
    Orange(R.string.hue_orange, 30f, 0xFFE8955A.toInt()),
    Yellow(R.string.hue_yellow, 58f, 0xFFE6CB5C.toInt()),
    Green(R.string.hue_green, 120f, 0xFF5FBF8F.toInt()),
    Aqua(R.string.hue_aqua, 180f, 0xFF5CC4C4.toInt()),
    Blue(R.string.hue_blue, 222f, 0xFF6E8BFF.toInt()),
    Purple(R.string.hue_purple, 270f, 0xFFA98BFF.toInt()),
    Magenta(R.string.hue_magenta, 315f, 0xFFD96A8B.toInt()),
}

@Serializable
data class HslShift(val hue: Int = 0, val saturation: Int = 0, val luminance: Int = 0) {
    val isNeutral: Boolean get() = hue == 0 && saturation == 0 && luminance == 0
}

@Serializable
data class SplitTone(
    val highlightHue: Int = 40,
    val highlightAmount: Int = 0,
    val shadowHue: Int = 220,
    val shadowAmount: Int = 0,
    val balance: Int = 0,
) {
    val isNeutral: Boolean get() = highlightAmount == 0 && shadowAmount == 0
}

@Serializable
enum class Blend(@StringRes val labelRes: Int) {
    Screen(R.string.blend_screen),
    Overlay(R.string.blend_overlay),
    Multiply(R.string.blend_multiply),
}

@Serializable
enum class EffectKind(@StringRes val labelRes: Int, val blend: Blend, val opacity: Int) {
    Leak(R.string.effect_leak, Blend.Screen, 70),
    Dust(R.string.effect_dust, Blend.Screen, 60),
    Scratches(R.string.effect_scratches, Blend.Screen, 55),
    Burn(R.string.effect_burn, Blend.Screen, 65),
    Vhs(R.string.effect_vhs, Blend.Overlay, 45),
}

const val EFFECT_VARIANTS = 4

@Serializable
data class EffectLayer(val variant: Int, val opacity: Int, val blend: Blend)

@Serializable
enum class DateStyle(@StringRes val labelRes: Int, val color: Int) {
    Classic(R.string.date_classic, 0xFFFF8A3D.toInt()),
    Led(R.string.date_led, 0xFFFF5A36.toInt()),
    White(R.string.date_white, 0xFFF3F1EC.toInt()),
    Ink(R.string.date_ink, 0xFF1F2222.toInt()),
}

@Serializable
enum class TextFont(val label: String, @FontRes val fontRes: Int, val weight: Int) {
    SpaceGrotesk("Space Grotesk", R.font.space_grotesk, 600),
    Outfit("Outfit", R.font.outfit, 600),
    PlexSans("IBM Plex Sans", R.font.ibm_plex_sans, 500),
    PlexMono("IBM Plex Mono", R.font.ibm_plex_mono_medium, 500),
}

@Serializable
enum class TextAlign { Start, Center, End }

@Serializable
data class TextLayer(
    val id: Long,
    val text: String = "",
    val font: TextFont = TextFont.SpaceGrotesk,
    val color: Int = INKS.first(),
    val size: Float = 0.08f,
    val align: TextAlign = TextAlign.Center,
    val plate: Boolean = false,
    @Serializable(OffsetSerializer::class) val center: Offset = Offset(0.5f, 0.5f),
    val rotation: Float = 0f,
    val locked: Boolean = false,
)

@Serializable
data class Stroke(
    val color: Int,
    val width: Float,
    val points: List<@Serializable(OffsetSerializer::class) Offset>,
    val tool: MarkupTool = MarkupTool.Pen,
)

@Serializable
data class CameraSettings(
    val stock: Int = 0,
    val strength: Int = 80,
    val glare: Int = 0,
    val grain: Int = 0,
    val fade: Int = 0,
)

val FULL_FRAME = Rect(0f, 0f, 1f, 1f)
val FULL_QUAD = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(1f, 1f), Offset(0f, 1f))
val IDENTITY_CURVE = listOf(Offset(0f, 0f), Offset(1f, 1f))
const val STRAIGHTEN_LIMIT = 45f
const val QUAD_REACH = 0.42f

@Serializable
data class PhotoEdits(
    @Serializable(RectSerializer::class) val crop: Rect = FULL_FRAME,
    val ratio: CropRatio = CropRatio.Free,
    val turns: Int = 0,
    val flipped: Boolean = false,
    val straighten: Float = 0f,
    val quad: List<@Serializable(OffsetSerializer::class) Offset> = FULL_QUAD,
    val preset: Preset? = null,
    val presetAmount: Int = 100,
    val camera: Camera? = null,
    val cameraSettings: CameraSettings = CameraSettings(),
    val adjustments: Map<Adjustment, Int> = emptyMap(),
    val curves: Map<CurveChannel, List<@Serializable(OffsetSerializer::class) Offset>> = emptyMap(),
    val hsl: Map<HueBand, HslShift> = emptyMap(),
    val split: SplitTone = SplitTone(),
    val effects: Map<EffectKind, EffectLayer> = emptyMap(),
    val dateStamp: DateStyle? = null,
    val texts: List<TextLayer> = emptyList(),
    val strokes: List<Stroke> = emptyList(),
) {
    operator fun get(adjustment: Adjustment): Int = adjustments[adjustment] ?: 0

    fun with(adjustment: Adjustment, value: Int): PhotoEdits {
        val clamped = value.coerceIn(adjustment.range)
        return copy(adjustments = if (clamped == 0) adjustments - adjustment else adjustments + (adjustment to clamped))
    }

    fun curve(channel: CurveChannel): List<Offset> = curves[channel] ?: IDENTITY_CURVE

    fun withCurve(channel: CurveChannel, points: List<Offset>): PhotoEdits =
        copy(curves = if (points == IDENTITY_CURVE) curves - channel else curves + (channel to points))

    fun shift(band: HueBand): HslShift = hsl[band] ?: HslShift()

    fun withShift(band: HueBand, shift: HslShift): PhotoEdits =
        copy(hsl = if (shift.isNeutral) hsl - band else hsl + (band to shift))

    val grading: PhotoEdits
        get() = PhotoEdits(
            preset = preset,
            presetAmount = presetAmount,
            camera = camera,
            cameraSettings = cameraSettings.copy(grain = 0),
            adjustments = adjustments - Adjustment.Grain - Adjustment.Vignette,
            curves = curves,
            hsl = hsl,
            split = split,
        )

    val oriented: PhotoEdits get() = copy(straighten = 0f, quad = FULL_QUAD)

    fun withGeometry(source: PhotoEdits): PhotoEdits = copy(
        crop = source.crop,
        ratio = source.ratio,
        turns = source.turns,
        flipped = source.flipped,
        straighten = source.straighten,
        quad = source.quad,
        strokes = source.strokes,
    )

    val grain: Int get() = maxOf(this[Adjustment.Grain], if (camera != null) cameraSettings.grain else 0)
}

fun straightened(degrees: Float): Float =
    ((degrees * 10f).roundToInt() / 10f).coerceIn(-STRAIGHTEN_LIMIT, STRAIGHTEN_LIMIT)

fun coverScale(width: Int, height: Int, degrees: Float): Float {
    val radians = abs(degrees) * PI.toFloat() / 180f
    val long = maxOf(width, height).toFloat() / minOf(width, height)
    return cos(radians) + long * sin(radians)
}

fun movedCorner(quad: List<Offset>, corner: Int, to: Offset): List<Offset> {
    val home = FULL_QUAD[corner]
    val x = if (home.x == 0f) to.x.coerceIn(0f, QUAD_REACH) else to.x.coerceIn(1f - QUAD_REACH, 1f)
    val y = if (home.y == 0f) to.y.coerceIn(0f, QUAD_REACH) else to.y.coerceIn(1f - QUAD_REACH, 1f)
    return quad.toMutableList().also { it[corner] = Offset(x, y) }
}

fun orientedSize(width: Int, height: Int, turns: Int): Pair<Int, Int> =
    if (turns % 2 == 0) width to height else height to width

fun PhotoEdits.turned(clockwise: Boolean, widthOverHeight: Float): PhotoEdits {
    val box = crop.let {
        if (clockwise) Rect(1f - it.bottom, it.left, 1f - it.top, it.right)
        else Rect(it.top, 1f - it.right, it.bottom, 1f - it.left)
    }
    return copy(
        crop = box,
        turns = Math.floorMod(turns + if (clockwise) 1 else -1, 4),
        quad = if (clockwise) listOf(quad[3], quad[0], quad[1], quad[2]).map { Offset(1f - it.y, it.x) }
        else listOf(quad[1], quad[2], quad[3], quad[0]).map { Offset(it.y, 1f - it.x) },
        strokes = strokes.map { stroke ->
            stroke.copy(
                width = stroke.width * widthOverHeight,
                points = stroke.points.map {
                    if (clockwise) Offset(1f - it.y, it.x) else Offset(it.y, 1f - it.x)
                },
            )
        },
    )
}

fun PhotoEdits.mirrored(): PhotoEdits = copy(
    crop = Rect(1f - crop.right, crop.top, 1f - crop.left, crop.bottom),
    turns = Math.floorMod(-turns, 4),
    flipped = !flipped,
    straighten = -straighten,
    quad = listOf(quad[1], quad[0], quad[3], quad[2]).map { Offset(1f - it.x, it.y) },
    strokes = strokes.map { stroke ->
        stroke.copy(points = stroke.points.map { Offset(1f - it.x, it.y) })
    },
)

fun PhotoEdits.flippedVertically(widthOverHeight: Float): PhotoEdits =
    mirrored().turned(clockwise = true, widthOverHeight).turned(clockwise = true, 1f / widthOverHeight)

fun centredCrop(width: Int, height: Int, ratio: Float?): Rect {
    if (ratio == null) return FULL_FRAME
    val source = width.toFloat() / height
    return if (source > ratio) {
        val w = ratio / source
        Rect((1f - w) / 2f, 0f, (1f + w) / 2f, 1f)
    } else {
        val h = source / ratio
        Rect(0f, (1f - h) / 2f, 1f, (1f + h) / 2f)
    }
}

fun movedCurvePoint(points: List<Offset>, index: Int, to: Offset): List<Offset> {
    val last = points.lastIndex
    val x = when (index) {
        0 -> 0f
        last -> 1f
        else -> to.x.coerceIn(points[index - 1].x + CURVE_GAP, points[index + 1].x - CURVE_GAP)
    }
    return points.toMutableList().also { it[index] = Offset(x, to.y.coerceIn(0f, 1f)) }
}

fun addedCurvePoint(points: List<Offset>, at: Offset): Pair<List<Offset>, Int>? {
    if (points.size >= MAX_CURVE_POINTS) return null
    val index = points.indexOfFirst { it.x > at.x }
    if (index <= 0) return null
    if (at.x - points[index - 1].x < CURVE_GAP || points[index].x - at.x < CURVE_GAP) return null
    return points.toMutableList().also { it.add(index, Offset(at.x, at.y.coerceIn(0f, 1f))) } to index
}

const val CURVE_GAP = 0.04f
const val MAX_CURVE_POINTS = 8
const val SLIDER_STEP = 6

val INKS = listOf(0xFFEDEDEF, 0xFF0B0B0C, 0xFFE0574A, 0xFFE0A458, 0xFF5FBF8F, 0xFF6E8BFF).map(Long::toInt)
val NIBS = listOf(3f, 5f, 8f, 12f)

@OptIn(ExperimentalSerializationApi::class)
object RectSerializer : KSerializer<Rect> {
    private val floats = FloatArraySerializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("Rect", floats.descriptor)

    override fun serialize(encoder: Encoder, value: Rect) =
        encoder.encodeSerializableValue(floats, floatArrayOf(value.left, value.top, value.right, value.bottom))

    override fun deserialize(decoder: Decoder): Rect =
        decoder.decodeSerializableValue(floats).let { Rect(it[0], it[1], it[2], it[3]) }
}

@OptIn(ExperimentalSerializationApi::class)
object OffsetSerializer : KSerializer<Offset> {
    private val floats = FloatArraySerializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("Offset", floats.descriptor)

    override fun serialize(encoder: Encoder, value: Offset) =
        encoder.encodeSerializableValue(floats, floatArrayOf(value.x, value.y))

    override fun deserialize(decoder: Decoder): Offset =
        decoder.decodeSerializableValue(floats).let { Offset(it[0], it[1]) }
}
