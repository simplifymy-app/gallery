package app.simplifymy.gallery.feature.editor

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import app.simplifymy.gallery.R
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

data class Look(
    val adjust: Map<Adjustment, Int> = emptyMap(),
    val mono: Boolean = false,
    val hue: Float = 0f,
    val split: SplitTone = SplitTone(),
    val curve: List<Offset> = IDENTITY_CURVE,
) {
    operator fun plus(other: Look) = Look(
        adjust = (adjust.keys + other.adjust.keys).associateWith { (adjust[it] ?: 0) + (other.adjust[it] ?: 0) },
        mono = mono || other.mono,
        hue = hue + other.hue,
        split = if (other.split.isNeutral) split else other.split,
        curve = if (other.curve == IDENTITY_CURVE) curve else other.curve,
    )
}

enum class PresetPack(@StringRes val labelRes: Int, val code: String) {
    Cameras(R.string.editor_cameras, "CA"),
    Natural(R.string.pack_natural, "NA"),
    Warm(R.string.pack_warm, "WA"),
    Cool(R.string.pack_cool, "CO"),
    Film(R.string.pack_film, "FI"),
    Mono(R.string.pack_mono, "MO"),
}

private fun lifted(lift: Float, roll: Float) = listOf(Offset(0f, lift), Offset(0.5f, 0.5f), Offset(1f, 1f - roll))

private fun punch(amount: Float) = listOf(
    Offset(0f, 0f),
    Offset(0.25f, 0.25f - amount),
    Offset(0.75f, 0.75f + amount),
    Offset(1f, 1f),
)

@Serializable
enum class Preset(@StringRes val labelRes: Int, val pack: PresetPack, val look: Look) {
    Clear(R.string.preset_clear, PresetPack.Natural, Look(mapOf(Adjustment.Contrast to 10, Adjustment.Vibrance to 15, Adjustment.Clarity to 10))),
    Soft(R.string.preset_soft, PresetPack.Natural, Look(mapOf(Adjustment.Contrast to -15, Adjustment.Highlights to -20, Adjustment.Shadows to 15, Adjustment.Fade to 10))),
    Vivid(R.string.preset_vivid, PresetPack.Natural, Look(mapOf(Adjustment.Saturation to 22, Adjustment.Vibrance to 25), curve = punch(0.04f))),
    Matte(R.string.preset_matte, PresetPack.Natural, Look(mapOf(Adjustment.Contrast to -8, Adjustment.Saturation to -8), curve = lifted(0.1f, 0.05f))),
    Golden(R.string.preset_golden, PresetPack.Warm, Look(mapOf(Adjustment.Temperature to 35, Adjustment.Vibrance to 10, Adjustment.Highlights to -10))),
    Amber(R.string.preset_amber, PresetPack.Warm, Look(mapOf(Adjustment.Temperature to 22, Adjustment.Tint to 8), split = SplitTone(highlightHue = 35, highlightAmount = 30))),
    Sunset(R.string.preset_sunset, PresetPack.Warm, Look(mapOf(Adjustment.Temperature to 38, Adjustment.Tint to 14, Adjustment.Saturation to 10), split = SplitTone(shadowHue = 280, shadowAmount = 22))),
    Peach(R.string.preset_peach, PresetPack.Warm, Look(mapOf(Adjustment.Temperature to 18, Adjustment.Tint to 16, Adjustment.Exposure to 8, Adjustment.Fade to 14))),
    Ocean(R.string.preset_ocean, PresetPack.Cool, Look(mapOf(Adjustment.Temperature to -30, Adjustment.Saturation to 10, Adjustment.Contrast to 10))),
    Frost(R.string.preset_frost, PresetPack.Cool, Look(mapOf(Adjustment.Temperature to -25, Adjustment.Saturation to -25, Adjustment.Exposure to 10, Adjustment.Fade to 10))),
    Dusk(R.string.preset_dusk, PresetPack.Cool, Look(mapOf(Adjustment.Temperature to -15, Adjustment.Tint to 12, Adjustment.Exposure to -10), split = SplitTone(shadowHue = 240, shadowAmount = 30))),
    Mint(R.string.preset_mint, PresetPack.Cool, Look(mapOf(Adjustment.Temperature to -10, Adjustment.Tint to -20, Adjustment.Fade to 12), hue = -8f)),
    Faded(R.string.preset_faded, PresetPack.Film, Look(mapOf(Adjustment.Contrast to -15, Adjustment.Saturation to -10), curve = lifted(0.16f, 0.08f))),
    Classic(R.string.preset_classic, PresetPack.Film, Look(mapOf(Adjustment.Contrast to 18, Adjustment.Saturation to -10), split = SplitTone(highlightHue = 40, highlightAmount = 25, shadowHue = 200, shadowAmount = 20))),
    Retro(R.string.preset_retro, PresetPack.Film, Look(mapOf(Adjustment.Temperature to 15, Adjustment.Saturation to -20, Adjustment.Fade to 25), split = SplitTone(highlightHue = 45, highlightAmount = 30))),
    Chrome(R.string.preset_chrome, PresetPack.Film, Look(mapOf(Adjustment.Contrast to 22, Adjustment.Saturation to 15, Adjustment.Clarity to 15, Adjustment.Highlights to -15))),
    Mono(R.string.preset_mono, PresetPack.Mono, Look(mapOf(Adjustment.Contrast to 12), mono = true)),
    MonoSoft(R.string.preset_mono_soft, PresetPack.Mono, Look(mapOf(Adjustment.Contrast to -12, Adjustment.Fade to 15), mono = true)),
    MonoDeep(R.string.preset_mono_deep, PresetPack.Mono, Look(mapOf(Adjustment.Contrast to 35, Adjustment.Blacks to -20, Adjustment.Clarity to 15), mono = true)),
    Silver(R.string.preset_silver, PresetPack.Mono, Look(mapOf(Adjustment.Highlights to -10, Adjustment.Shadows to 10), mono = true, split = SplitTone(highlightHue = 210, highlightAmount = 15))),
    ;

    val code: String get() = "${pack.code} %02d".format(entries.filter { it.pack == pack }.indexOf(this) + 1)
}

enum class FilmStock(@StringRes val labelRes: Int, val look: Look) {
    Soft(R.string.stock_soft, Look(mapOf(Adjustment.Temperature to 10, Adjustment.Contrast to -6))),
    Cool(R.string.stock_cool, Look(mapOf(Adjustment.Temperature to -16, Adjustment.Tint to -4))),
    Mono(R.string.stock_mono, Look(mapOf(Adjustment.Contrast to 10), mono = true)),
}

@Serializable
enum class Camera(
    @StringRes val labelRes: Int,
    val base: Look,
    val glare: Int,
    val grain: Int,
    val fade: Int,
    val date: DateStyle?,
) {
    Digital(R.string.camera_digital, Look(mapOf(Adjustment.Contrast to 12, Adjustment.Sharpen to 20, Adjustment.Saturation to 10)), 0, 6, 0, DateStyle.Led),
    Pocket(R.string.camera_pocket, Look(mapOf(Adjustment.Highlights to -12, Adjustment.Vibrance to 8), split = SplitTone(highlightHue = 42, highlightAmount = 18)), 30, 24, 12, DateStyle.Classic),
    Zoom(R.string.camera_zoom, Look(mapOf(Adjustment.Contrast to 8, Adjustment.Clarity to 10), curve = punch(0.03f)), 18, 16, 6, null),
    Instant(R.string.camera_instant, Look(mapOf(Adjustment.Exposure to 6, Adjustment.Saturation to -12), curve = lifted(0.12f, 0.06f)), 24, 18, 20, null),
    Camcorder(R.string.camera_camcorder, Look(mapOf(Adjustment.Saturation to 18, Adjustment.Sharpen to 30, Adjustment.Contrast to 6)), 12, 30, 8, DateStyle.Led),
    ;

    fun defaults() = CameraSettings(stock = 0, strength = 80, glare = glare, grain = grain, fade = fade)

    fun look(settings: CameraSettings): Look = base + FilmStock.entries[settings.stock.coerceIn(FilmStock.entries.indices)].look
}

fun Look.scaled(amount: Float): Look = Look(
    adjust = adjust.mapValues { (it.value * amount).roundToInt() },
    mono = mono,
    hue = hue * amount,
    split = split.copy(
        highlightAmount = (split.highlightAmount * amount).roundToInt(),
        shadowAmount = (split.shadowAmount * amount).roundToInt(),
    ),
    curve = curve.map { Offset(it.x, it.x + (it.y - it.x) * amount) },
)

val PhotoEdits.monoAmount: Float
    get() = maxOf(
        if (preset?.look?.mono == true) presetAmount / 100f else 0f,
        if (camera?.look(cameraSettings)?.mono == true) cameraSettings.strength / 100f else 0f,
    )

val PhotoEdits.looks: List<Look>
    get() = listOfNotNull(
        preset?.look?.scaled(presetAmount / 100f),
        camera?.let { it.look(cameraSettings).scaled(cameraSettings.strength / 100f) },
    )
