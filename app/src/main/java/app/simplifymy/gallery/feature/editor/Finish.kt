package app.simplifymy.gallery.feature.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import app.simplifymy.gallery.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.random.Random

class EditorFonts(context: Context) {
    private val app = context.applicationContext
    private val cache = mutableMapOf<TextFont, Typeface>()

    val mono: Typeface by lazy { ResourcesCompat.getFont(app, R.font.ibm_plex_mono_medium) ?: Typeface.MONOSPACE }

    fun typeface(font: TextFont): Typeface = cache.getOrPut(font) {
        val base = ResourcesCompat.getFont(app, font.fontRes) ?: Typeface.DEFAULT
        Typeface.create(base, font.weight, false)
    }
}

private object GrainTile {
    val bitmap: Bitmap by lazy {
        val size = 256
        val random = Random(11)
        val pixels = IntArray(size * size) {
            val noise = (random.nextFloat() + random.nextFloat() + random.nextFloat()) / 3f
            val level = (noise * 255f).toInt().coerceIn(0, 255)
            Color.argb(255, level, level, level)
        }
        Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}

fun drawFinish(canvas: Canvas, width: Float, height: Float, edits: PhotoEdits, date: Long, fonts: EditorFonts) {
    drawVignette(canvas, width, height, edits.total(Adjustment.Vignette) / 100f)
    drawGrain(canvas, width, height, edits.grain / 100f)
    edits.effects.forEach { (kind, layer) -> drawEffect(canvas, width, height, kind, layer) }
    edits.dateStamp?.let { drawDate(canvas, width, height, it, date, fonts) }
}

private fun drawVignette(canvas: Canvas, width: Float, height: Float, amount: Float) {
    if (amount == 0f) return
    val edge = if (amount > 0f) Color.BLACK else Color.WHITE
    val alpha = (kotlin.math.abs(amount) * 0.75f * 255f).toInt()
    val radius = maxOf(width, height) * 0.75f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            width / 2f,
            height / 2f,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, (edge and 0x00FFFFFF) or (alpha shl 24)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, width, height, paint)
}

private fun drawGrain(canvas: Canvas, width: Float, height: Float, amount: Float) {
    if (amount <= 0f) return
    val scale = (maxOf(width, height) / 1400f).coerceAtLeast(0.5f)
    val paint = Paint().apply {
        shader = BitmapShader(GrainTile.bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
            setLocalMatrix(Matrix().apply { setScale(scale, scale) })
        }
        blendMode = BlendMode.OVERLAY
        alpha = (amount * 0.7f * 255f).toInt()
        isFilterBitmap = true
    }
    canvas.drawRect(0f, 0f, width, height, paint)
}

fun drawEffect(canvas: Canvas, width: Float, height: Float, kind: EffectKind, layer: EffectLayer) {
    val layerPaint = Paint().apply {
        alpha = (layer.opacity / 100f * 255f).toInt()
        blendMode = when (layer.blend) {
            Blend.Screen -> BlendMode.SCREEN
            Blend.Overlay -> BlendMode.OVERLAY
            Blend.Multiply -> BlendMode.MULTIPLY
        }
    }
    canvas.saveLayer(0f, 0f, width, height, layerPaint)
    val variant = layer.variant.coerceIn(0, EFFECT_VARIANTS - 1)
    when (kind) {
        EffectKind.Leak -> drawLeak(canvas, width, height, variant)
        EffectKind.Burn -> drawBurn(canvas, width, height, variant)
        EffectKind.Dust -> drawDust(canvas, width, height, variant)
        EffectKind.Scratches -> drawScratches(canvas, width, height, variant)
        EffectKind.Vhs -> drawVhs(canvas, width, height, variant)
    }
    canvas.restore()
}

private val LEAK_COLORS = intArrayOf(0xFFFF9A3C.toInt(), 0xFFFF4F7B.toInt(), 0xFFFFD27A.toInt(), 0xFFB46CFF.toInt())

private fun anchor(width: Float, height: Float, variant: Int): Pair<Float, Float> = when (variant) {
    0 -> 0f to height * 0.35f
    1 -> width to height * 0.6f
    2 -> width to 0f
    else -> 0f to height
}

private fun glow(x: Float, y: Float, radius: Float, colors: IntArray, stops: FloatArray) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    shader = RadialGradient(x, y, radius.coerceAtLeast(1f), colors, stops, Shader.TileMode.CLAMP)
}

private fun drawLeak(canvas: Canvas, width: Float, height: Float, variant: Int) {
    val (x, y) = anchor(width, height, variant)
    val color = LEAK_COLORS[variant]
    val long = maxOf(width, height)
    canvas.drawRect(
        0f, 0f, width, height,
        glow(x, y, long * 0.8f, intArrayOf(color, (color and 0x00FFFFFF) or 0x66000000, Color.TRANSPARENT), floatArrayOf(0f, 0.35f, 1f)),
    )
    canvas.drawRect(
        0f, 0f, width, height,
        glow(x, y, long * 0.25f, intArrayOf(0xCCFFF4E0.toInt(), Color.TRANSPARENT), floatArrayOf(0f, 1f)),
    )
}

private fun drawBurn(canvas: Canvas, width: Float, height: Float, variant: Int) {
    val corners = listOf(0f to 0f, width to 0f, width to height, 0f to height)
    val (x, y) = corners[variant]
    canvas.drawRect(
        0f, 0f, width, height,
        glow(
            x, y, maxOf(width, height) * 0.6f,
            intArrayOf(0xFFFFF1C2.toInt(), 0xFFFF7A1A.toInt(), 0xAAB3200A.toInt(), Color.TRANSPARENT),
            floatArrayOf(0f, 0.22f, 0.5f, 1f),
        ),
    )
}

private fun drawDust(canvas: Canvas, width: Float, height: Float, variant: Int) {
    val random = Random(100 + variant)
    val unit = maxOf(width, height) / 1000f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    repeat(160) {
        paint.alpha = (130 + random.nextInt(110))
        canvas.drawCircle(random.nextFloat() * width, random.nextFloat() * height, unit * (0.6f + random.nextFloat() * 1.8f), paint)
    }
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = unit * 0.8f
    paint.strokeCap = Paint.Cap.ROUND
    repeat(7) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * height
        val reach = unit * (14f + random.nextFloat() * 26f)
        paint.alpha = 150
        canvas.drawPath(Path().apply {
            moveTo(x, y)
            quadTo(x + reach * (random.nextFloat() - 0.5f), y + reach * 0.6f, x + reach * (random.nextFloat() - 0.5f), y + reach)
        }, paint)
    }
}

private fun drawScratches(canvas: Canvas, width: Float, height: Float, variant: Int) {
    val random = Random(200 + variant)
    val unit = maxOf(width, height) / 1000f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    repeat(5 + variant * 2) {
        var x = random.nextFloat() * width
        paint.strokeWidth = unit * (0.6f + random.nextFloat() * 0.9f)
        paint.alpha = 90 + random.nextInt(110)
        val start = random.nextFloat() * height * 0.3f
        val end = height - random.nextFloat() * height * 0.3f
        canvas.drawPath(Path().apply {
            moveTo(x, start)
            val steps = 6
            for (step in 1..steps) {
                x += (random.nextFloat() - 0.5f) * unit * 8f
                lineTo(x, start + (end - start) * step / steps)
            }
        }, paint)
    }
}

private fun drawVhs(canvas: Canvas, width: Float, height: Float, variant: Int) {
    val random = Random(300 + variant)
    val unit = maxOf(width, height) / 1000f
    val paint = Paint().apply { color = Color.WHITE }
    val gap = unit * (5f + variant)
    var y = 0f
    while (y < height) {
        paint.alpha = 40
        canvas.drawRect(0f, y, width, y + unit * 1.6f, paint)
        y += gap
    }
    repeat(3) {
        val top = random.nextFloat() * height
        val band = unit * (6f + random.nextFloat() * 18f)
        paint.alpha = 70
        canvas.drawRect(0f, top, width, top + band, paint)
        paint.color = 0xFFFF3D9A.toInt()
        paint.alpha = 60
        canvas.drawRect(0f, top + band, width, top + band + unit * 2f, paint)
        paint.color = 0xFF3DF2FF.toInt()
        canvas.drawRect(0f, top - unit * 2f, width, top, paint)
        paint.color = Color.WHITE
    }
}

fun dateText(style: DateStyle, date: Long): String {
    val pattern = when (style) {
        DateStyle.Classic -> "''yy MM dd"
        DateStyle.Led -> "MMM dd yyyy"
        DateStyle.White, DateStyle.Ink -> "yyyy.MM.dd"
    }
    return SimpleDateFormat(pattern, Locale.ENGLISH).format(Date(date)).uppercase(Locale.ENGLISH)
}

private fun drawDate(canvas: Canvas, width: Float, height: Float, style: DateStyle, date: Long, fonts: EditorFonts) {
    val size = minOf(width, height) * 0.055f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fonts.mono
        textSize = size
        color = style.color
        textAlign = Paint.Align.RIGHT
        letterSpacing = 0.08f
        if (style == DateStyle.Led || style == DateStyle.Classic) setShadowLayer(size * 0.45f, 0f, 0f, style.color)
    }
    val inset = minOf(width, height) * 0.06f
    canvas.drawText(dateText(style, date), width - inset, height - inset, paint)
}

fun textLayout(layer: TextLayer, width: Float, fonts: EditorFonts): StaticLayout {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fonts.typeface(layer.font)
        textSize = (layer.size * width).coerceAtLeast(1f)
        color = layer.color
        fontVariationSettings = "'wght' ${layer.font.weight}"
    }
    val alignment = when (layer.align) {
        TextAlign.Start -> Layout.Alignment.ALIGN_NORMAL
        TextAlign.Center -> Layout.Alignment.ALIGN_CENTER
        TextAlign.End -> Layout.Alignment.ALIGN_OPPOSITE
    }
    val text = layer.text.ifEmpty { " " }
    val limit = (width * 0.9f).toInt().coerceAtLeast(1)
    val loose = StaticLayout.Builder.obtain(text, 0, text.length, paint, limit).build()
    val fitted = (0 until loose.lineCount).maxOf { loose.getLineWidth(it) }
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, ceil(fitted).toInt().coerceIn(1, limit))
        .setAlignment(alignment)
        .build()
}

fun drawTexts(canvas: Canvas, width: Float, height: Float, texts: List<TextLayer>, fonts: EditorFonts) {
    texts.forEach { layer ->
        if (layer.text.isBlank()) return@forEach
        val layout = textLayout(layer, width, fonts)
        val boxWidth = layout.width.toFloat()
        val boxHeight = layout.height.toFloat()
        canvas.save()
        canvas.translate(layer.center.x * width, layer.center.y * height)
        canvas.rotate(layer.rotation)
        canvas.translate(-boxWidth / 2f, -boxHeight / 2f)
        if (layer.plate) {
            val pad = layer.size * width * 0.35f
            val light = Color.luminance(layer.color) > 0.5f
            val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (light) 0x8C000000.toInt() else 0xBFFFFFFF.toInt() }
            canvas.drawRoundRect(RectF(-pad, -pad * 0.6f, boxWidth + pad, boxHeight + pad * 0.6f), pad * 0.4f, pad * 0.4f, plate)
        }
        layout.draw(canvas)
        canvas.restore()
    }
}
