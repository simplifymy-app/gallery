package app.simplifymy.gallery.feature.editor

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGradeTest {

    private val swatch = IntArray(64) { index ->
        val r = index * 4
        val g = 255 - index * 3
        val b = (index * 37) % 256
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    @Test
    fun noEditsLeavePixelsUntouched() {
        val pixels = swatch.copyOf()
        val grade = PhotoGrade(PhotoEdits())
        assertTrue(grade.isIdentity)
        grade.apply(pixels)
        assertArrayEquals(swatch, pixels)
    }

    @Test
    fun theSameRecipeGivesTheSamePixels() {
        val edits = PhotoEdits(preset = Preset.Ocean)
            .with(Adjustment.Exposure, 20)
            .with(Adjustment.Shadows, 40)
            .with(Adjustment.Vibrance, 35)
            .with(Adjustment.Temperature, -25)
        val first = swatch.copyOf().also { PhotoGrade(edits).apply(it) }
        val second = swatch.copyOf().also { PhotoGrade(edits).apply(it) }
        assertArrayEquals(first, second)
    }

    @Test
    fun positiveExposureBrightensMidGrey() {
        val pixels = intArrayOf(grey(128))
        PhotoGrade(PhotoEdits().with(Adjustment.Exposure, 50)).apply(pixels)
        assertTrue((pixels[0] and 0xFF) > 128)
    }

    @Test
    fun toneCurvesStayMonotone() {
        listOf(Adjustment.Exposure, Adjustment.Highlights, Adjustment.Shadows, Adjustment.Whites, Adjustment.Blacks, Adjustment.Fade).forEach { adjustment ->
            listOf(adjustment.range.first, adjustment.range.last).forEach { value ->
                val curve = toneCurve(PhotoEdits().with(adjustment, value)) ?: return@forEach
                (1 until curve.size).forEach { level -> assertTrue("$adjustment $value", curve[level] >= curve[level - 1]) }
            }
        }
    }

    @Test
    fun matrixOnlyEditsSkipTheToneCurve() {
        assertNull(toneCurve(PhotoEdits().with(Adjustment.Saturation, 40)))
    }

    @Test
    fun vibranceLeavesGreyAlone() {
        val pixels = intArrayOf(grey(90))
        PhotoGrade(PhotoEdits().with(Adjustment.Vibrance, 100)).apply(pixels)
        assertEquals(grey(90), pixels[0])
    }

    @Test
    fun alphaIsPreserved() {
        val pixels = intArrayOf((0x40 shl 24) or (200 shl 16) or (10 shl 8) or 60)
        PhotoGrade(PhotoEdits().with(Adjustment.Contrast, 60)).apply(pixels)
        assertEquals(0x40, pixels[0] ushr 24)
    }

    @Test
    fun aMonotoneCurvePassesThroughItsPointsAndNeverOvershoots() {
        val points = listOf(Offset(0f, 0f), Offset(0.3f, 0.6f), Offset(0.6f, 0.62f), Offset(1f, 1f))
        val curve = MonotoneCurve(points)
        points.forEach { assertEquals(it.y, curve.at(it.x), 1e-5f) }
        var previous = 0f
        (0..100).forEach { step ->
            val y = curve.at(step / 100f)
            assertTrue(y >= previous - 1e-6f)
            assertTrue(y in 0f..1f)
            previous = y
        }
    }

    @Test
    fun presetAtZeroIntensityMatchesNoPreset() {
        val plain = swatch.copyOf().also { PhotoGrade(PhotoEdits()).apply(it) }
        val zero = swatch.copyOf().also { PhotoGrade(PhotoEdits(preset = Preset.Sunset, presetAmount = 0)).apply(it) }
        assertArrayEquals(plain, zero)
    }

    @Test
    fun monoPresetRemovesColour() {
        val pixels = intArrayOf((0xFF shl 24) or (220 shl 16) or (40 shl 8) or 90)
        PhotoGrade(PhotoEdits(preset = Preset.Mono)).apply(pixels)
        val r = pixels[0] shr 16 and 0xFF
        val g = pixels[0] shr 8 and 0xFF
        val b = pixels[0] and 0xFF
        assertTrue(maxOf(r, g, b) - minOf(r, g, b) <= 2)
    }

    @Test
    fun hslOnlyTouchesItsBand() {
        val blue = (0xFF shl 24) or (40 shl 16) or (80 shl 8) or 220
        val pixels = intArrayOf(blue)
        PhotoGrade(PhotoEdits().withShift(HueBand.Red, HslShift(saturation = -100))).apply(pixels)
        assertEquals(blue, pixels[0])
    }

    @Test
    fun cameraFadeLiftsBlacks() {
        val pixels = intArrayOf(grey(0))
        PhotoGrade(PhotoEdits(camera = Camera.Instant, cameraSettings = Camera.Instant.defaults().copy(strength = 0))).apply(pixels)
        assertTrue((pixels[0] and 0xFF) > 0)
    }

    private fun grey(level: Int) = (0xFF shl 24) or (level shl 16) or (level shl 8) or level
}
