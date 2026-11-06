package app.simplifymy.gallery.feature.about

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.min

private val Steel = Color(0xFF9CA2C4)
private val SteelDark = Color(0xFF686C92)
private val SteelLight = Color(0xFFB2B8D6)
private val Pink = Color(0xFFFF4FD8)
private val Orange = Color(0xFFFF8C3C)
private val Cream = Color(0xFFFFF6C8)
private val Yellow = Color(0xFFFFD640)
private val Wing = Color(0xFFECB224)
private val Ink = Color(0xFF16121E)
private val Beak = Color(0xFFFF7C24)
private val Glass = Color(0x3896DCFF)
private val GlassRim = Color(0xFF9FD6F5)
private val Gun = Color(0xFF60648F)
private val GunBody = Color(0xFFD6DAF0)
private val Cyan = Color(0xFF5AC8E6)

private const val VIEW_LEFT = -96f
private const val VIEW_TOP = -170f
private const val VIEW_WIDTH = 172f
private const val VIEW_HEIGHT = 202f

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private val TankStripes = svg("M-34 -102 H34 M-34 -93 H34")
private val NozzleRight = svg("M52 -26 L64 -26 L67 -14 L49 -14 Z")
private val NozzleLeft = svg("M-52 -26 L-64 -26 L-67 -14 L-49 -14 Z")
private val Collar = svg("M-36 -71 Q0 -61 36 -71 L34 -57 Q0 -47 -34 -57 Z")
private val CollarLine = svg("M-31 -64 Q0 -54 31 -64")
private val Grip = svg("M-70 -46 H-58 L-61 -26 Q-62 -22 -66 -22 H-70 Q-73 -22 -73 -26 Z")
private val Dome = svg("M-73 -64 A8 8 0 0 1 -57 -64 Z")
private val GunGlint = svg("M-76 -57 A10 10 0 0 1 -66 -60")
private val Muzzle = Offset(-68f, -50f)

@Composable
fun Duck(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val k = min(size.width / VIEW_WIDTH, size.height / VIEW_HEIGHT)
        val left = (size.width - VIEW_WIDTH * k) / 2 - VIEW_LEFT * k
        val top = (size.height - VIEW_HEIGHT * k) / 2 - VIEW_TOP * k
        translate(left, top) {
            scale(k, k, Offset.Zero) { drawDuck() }
        }
    }
}

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) =
    drawOval(color, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))

private fun DrawScope.flame(cx: Float) {
    oval(cx, 2f, 7f, 14f, Pink.copy(alpha = 0.85f))
    oval(cx, -3f, 5f, 9f, Orange)
    oval(cx, -8f, 2.5f, 5f, Cream)
}

private fun DrawScope.tube(x: Float) {
    drawRoundRect(SteelLight, Offset(x, -114f), Size(16f, 90f), CornerRadius(8f))
    drawRoundRect(SteelDark, Offset(x, -114f), Size(16f, 90f), CornerRadius(8f), style = Stroke(3f))
    drawRoundRect(Pink, Offset(x + 2f, -119f), Size(12f, 8f), CornerRadius(3f))
}

private fun DrawScope.blaster() {
    drawPath(Grip, Gun)
    oval(-52f, -44f, 14f, 20f, Wing)
    val outline = Stroke(3f, join = StrokeJoin.Round)
    drawRoundRect(GunBody, Offset(-78f, -64f), Size(26f, 20f), CornerRadius(9f))
    drawRoundRect(Gun, Offset(-78f, -64f), Size(26f, 20f), CornerRadius(9f), style = outline)
    drawPath(Dome, Cyan)
    drawPath(Dome, Gun, style = outline)
    drawCircle(Pink, 13f, Muzzle)
    drawCircle(Gun, 13f, Muzzle, style = outline)
    drawCircle(GunBody, 8f, Muzzle)
    drawCircle(Gun, 8f, Muzzle, style = outline)
    drawCircle(Ink, 4.5f, Muzzle)
    drawPath(GunGlint, Color.White.copy(alpha = 0.8f), style = Stroke(2.5f, cap = StrokeCap.Round))
    drawCircle(Color.White, 1.8f, Offset(-66f, -69f))
}

private fun DrawScope.drawDuck() {
    drawRoundRect(Steel, Offset(-44f, -112f), Size(88f, 52f), CornerRadius(9f))
    drawRoundRect(SteelDark, Offset(-44f, -112f), Size(88f, 52f), CornerRadius(9f), style = Stroke(3f))
    drawPath(TankStripes, SteelDark, style = Stroke(3f, cap = StrokeCap.Round))
    flame(58f)
    drawPath(NozzleRight, SteelDark)
    tube(50f)
    flame(-58f)
    drawPath(NozzleLeft, SteelDark)
    tube(-66f)
    oval(52f, -44f, 14f, 20f, Wing)
    oval(0f, -34f, 62f, 42f, Yellow)
    oval(-14f, -22f, 26f, 12f, Cream.copy(alpha = 0.5f))
    oval(0f, -100f, 36f, 34f, Yellow)
    oval(-19f, -108f, 7f, 8f, Ink)
    oval(19f, -108f, 7f, 8f, Ink)
    drawCircle(Cream, 2.5f, Offset(-21f, -111f))
    drawCircle(Cream, 2.5f, Offset(17f, -111f))
    oval(0f, -94f, 20f, 7f, Beak)
    drawCircle(Glass, 50f, Offset(0f, -104f))
    drawCircle(GlassRim, 50f, Offset(0f, -104f), style = Stroke(6f))
    drawArc(
        Color.White.copy(alpha = 0.85f),
        startAngle = 194.4f,
        sweepAngle = 54f,
        useCenter = false,
        topLeft = Offset(-39f, -143f),
        size = Size(78f, 78f),
        style = Stroke(8f, cap = StrokeCap.Round),
    )
    drawCircle(Color.White.copy(alpha = 0.7f), 4f, Offset(30f, -136f))
    drawPath(Collar, SteelLight)
    drawPath(Collar, SteelDark, style = Stroke(4f, join = StrokeJoin.Round))
    drawPath(CollarLine, SteelDark, style = Stroke(3f))
    blaster()
}
