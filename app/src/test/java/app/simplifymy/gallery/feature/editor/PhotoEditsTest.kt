package app.simplifymy.gallery.feature.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoEditsTest {

    private val edits = PhotoEdits(
        crop = Rect(0.1f, 0.2f, 0.7f, 0.9f),
        ratio = CropRatio.Photo,
        turns = 1,
        flipped = true,
        preset = Preset.Golden,
        camera = Camera.Pocket,
        curves = mapOf(CurveChannel.Rgb to listOf(Offset(0f, 0.1f), Offset(0.5f, 0.6f), Offset(1f, 1f))),
        hsl = mapOf(HueBand.Orange to HslShift(hue = -8, saturation = -12, luminance = 22)),
        effects = mapOf(EffectKind.Leak to EffectLayer(2, 70, Blend.Screen)),
        dateStamp = DateStyle.Led,
        texts = listOf(TextLayer(id = 7, text = "slow sundays", rotation = 12f)),
        adjustments = mapOf(Adjustment.Exposure to 30, Adjustment.Tint to -12),
        strokes = listOf(Stroke(color = 0xFFE0574A.toInt(), width = 0.01f, points = listOf(Offset(0.2f, 0.3f), Offset(0.4f, 0.5f)))),
    )

    @Test
    fun recipeSurvivesAJsonRoundTrip() {
        val json = Json.encodeToString(PhotoEdits.serializer(), edits)
        assertEquals(edits, Json.decodeFromString(PhotoEdits.serializer(), json))
    }

    @Test
    fun fourQuarterTurnsReturnToTheStart() {
        var turned = edits
        repeat(4) { turned = turned.turned(clockwise = true, widthOverHeight = 1f) }
        assertEquals(edits.turns, turned.turns)
        assertRectEquals(edits.crop, turned.crop)
    }

    @Test
    fun turningBackUndoesATurn() {
        val back = edits.turned(clockwise = true, widthOverHeight = 1f).turned(clockwise = false, widthOverHeight = 1f)
        assertEquals(edits.turns, back.turns)
        assertRectEquals(edits.crop, back.crop)
    }

    @Test
    fun mirroringTwiceIsANoOp() {
        val twice = edits.mirrored().mirrored()
        assertEquals(edits.turns, twice.turns)
        assertEquals(edits.flipped, twice.flipped)
        assertRectEquals(edits.crop, twice.crop)
    }

    @Test
    fun aZeroAdjustmentIsDropped() {
        val cleared = edits.with(Adjustment.Exposure, 0).with(Adjustment.Tint, 0)
        assertFalse(cleared.adjustments.containsKey(Adjustment.Exposure))
        assertEquals(mapOf<Adjustment, Int>(), cleared.adjustments)
    }

    @Test
    fun adjustmentsAreClampedToTheSliderRange() {
        assertEquals(Adjustment.Contrast.range.last, PhotoEdits().with(Adjustment.Contrast, 400)[Adjustment.Contrast])
        assertEquals(Adjustment.Contrast.range.first, PhotoEdits().with(Adjustment.Contrast, -400)[Adjustment.Contrast])
    }

    @Test
    fun fourQuarterTurnsBringThePerspectiveQuadBack() {
        val skewed = PhotoEdits(quad = listOf(Offset(0.1f, 0.05f), Offset(0.9f, 0f), Offset(1f, 0.95f), Offset(0.02f, 1f)))
        var turned = skewed
        repeat(4) { turned = turned.turned(clockwise = true, widthOverHeight = 1f) }
        skewed.quad.zip(turned.quad).forEach { (a, b) ->
            assertEquals(a.x, b.x, 1e-5f)
            assertEquals(a.y, b.y, 1e-5f)
        }
    }

    @Test
    fun mirroringNegatesTheStraightenAngle() {
        assertEquals(-3.5f, PhotoEdits(straighten = 3.5f).mirrored().straighten)
    }

    @Test
    fun straightenIsRoundedAndClamped() {
        assertEquals(3.1f, straightened(3.14f))
        assertEquals(STRAIGHTEN_LIMIT, straightened(90f))
    }

    @Test
    fun coverScaleGrowsWithTheAngleAndIsOneWhenLevel() {
        assertEquals(1f, coverScale(400, 300, 0f), 1e-6f)
        assertTrue(coverScale(400, 300, 10f) > coverScale(400, 300, 5f))
        assertEquals(coverScale(400, 300, 7f), coverScale(300, 400, 7f), 1e-6f)
    }

    @Test
    fun perspectiveCornersStayInTheirQuadrant() {
        val moved = movedCorner(FULL_QUAD, 0, Offset(0.9f, -0.2f))
        assertEquals(QUAD_REACH, moved[0].x)
        assertEquals(0f, moved[0].y)
    }

    @Test
    fun curveEndpointsKeepTheirXAndInteriorPointsStayOrdered() {
        val points = listOf(Offset(0f, 0f), Offset(0.5f, 0.5f), Offset(1f, 1f))
        assertEquals(0f, movedCurvePoint(points, 0, Offset(0.3f, 0.2f))[0].x)
        val moved = movedCurvePoint(points, 1, Offset(1.4f, 0.7f))[1]
        assertTrue(moved.x < 1f)
        assertEquals(0.7f, moved.y)
    }

    @Test
    fun aPointIsAddedInOrder() {
        val (points, index) = addedCurvePoint(IDENTITY_CURVE, Offset(0.3f, 0.4f))!!
        assertEquals(1, index)
        assertEquals(3, points.size)
    }

    @Test
    fun gradingIgnoresGeometryAndFinish() {
        val graded = edits.grading
        assertEquals(FULL_FRAME, graded.crop)
        assertTrue(graded.texts.isEmpty())
        assertTrue(graded.effects.isEmpty())
        assertEquals(edits.preset, graded.preset)
    }

    private fun assertRectEquals(expected: Rect, actual: Rect) {
        assertEquals(expected.left, actual.left, 1e-5f)
        assertEquals(expected.top, actual.top, 1e-5f)
        assertEquals(expected.right, actual.right, 1e-5f)
        assertEquals(expected.bottom, actual.bottom, 1e-5f)
    }
}
