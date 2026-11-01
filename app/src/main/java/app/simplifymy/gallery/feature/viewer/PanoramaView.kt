package app.simplifymy.gallery.feature.viewer

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import app.simplifymy.gallery.core.media.Panorama
import app.simplifymy.gallery.core.media.readPanorama
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_PANORAMA_SIDE = 8192
private const val DEFAULT_FOV = 75f
private const val MIN_FOV = 30f
private const val MAX_FOV = 110f
private const val MAX_PITCH = 89f

class PanoramaMode(val available: Boolean, val sphere: Panorama?, val toggle: () -> Unit)

@Composable
fun rememberPanoramaMode(uri: Uri, isVideo: Boolean, forAll: Boolean): PanoramaMode {
    val detected = rememberPanorama(uri, isVideo)
    val panorama = detected ?: Panorama().takeIf { forAll }
    var flat by rememberSaveable(uri, detected != null) { mutableStateOf(detected == null) }
    return PanoramaMode(panorama != null, panorama?.takeIf { !flat }) { flat = !flat }
}

@Composable
private fun rememberPanorama(uri: Uri, isVideo: Boolean): Panorama? {
    val resolver = LocalContext.current.contentResolver
    return produceState<Panorama?>(null, uri) {
        value = withContext(Dispatchers.IO) { readPanorama(resolver, uri, isVideo) }
    }.value
}

@Composable
fun PanoramaPhoto(uri: Uri, panorama: Panorama, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val resolver = LocalContext.current.contentResolver
    val renderer = remember(uri) { PanoramaRenderer(panorama, video = false) }
    LaunchedEffect(renderer) {
        decodePanorama(resolver, uri)?.let(renderer::showBitmap)
    }
    PanoramaSurface(renderer, onTap, modifier)
}

@OptIn(UnstableApi::class)
@Composable
fun PanoramaVideo(player: ExoPlayer, panorama: Panorama, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val renderer = remember(player) { PanoramaRenderer(panorama, video = true) }
    DisposableEffect(renderer) {
        renderer.onVideoSurface = { player.setVideoSurface(it) }
        renderer.setStereoMode(player.videoFormat?.stereoMode ?: C.STEREO_MODE_MONO)
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                renderer.setStereoMode(player.videoFormat?.stereoMode ?: C.STEREO_MODE_MONO)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            renderer.videoSurface?.let(player::clearVideoSurface)
            renderer.release()
        }
    }
    PanoramaSurface(renderer, onTap, modifier)
}

@Composable
private fun PanoramaSurface(renderer: PanoramaRenderer, onTap: () -> Unit, modifier: Modifier) {
    val currentOnTap by rememberUpdatedState(onTap)
    val context = LocalContext.current
    LifecycleResumeEffect(renderer) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensors?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = DeviceLookListener(renderer::sense)
        if (sensor != null) sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose {
            sensors?.unregisterListener(listener)
            renderer.settleMotion()
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(renderer) {
                detectTransformGestures { _, pan, zoom, _ -> renderer.look(pan.x, pan.y, zoom, size.height) }
            }
            .pointerInput(renderer) { detectTapGestures { currentOnTap() } },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    renderer.view = this
                }
            },
            onRelease = { renderer.view = null },
        )
    }
}

private class DeviceLookListener(private val onLook: (yaw: Float, pitch: Float) -> Unit) : SensorEventListener {
    private val matrix = FloatArray(9)

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        val forwardX = -matrix[2]
        val forwardY = -matrix[5]
        val forwardZ = -matrix[8]
        onLook(
            Math.toDegrees(atan2(forwardX, forwardY).toDouble()).toFloat(),
            Math.toDegrees(asin(forwardZ.coerceIn(-1f, 1f)).toDouble()).toFloat(),
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

private suspend fun decodePanorama(resolver: ContentResolver, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val scale = MAX_PANORAMA_SIDE.toFloat() / max(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
            }
        }.getOrNull()
    }

private class PanoramaRenderer(private val panorama: Panorama, private val video: Boolean) : GLSurfaceView.Renderer {

    @Volatile var view: GLSurfaceView? = null
        set(value) {
            field = value
            value?.requestRender()
        }
    var onVideoSurface: (Surface) -> Unit = {}
    var videoSurface: Surface? = null
        private set

    private val main = Handler(Looper.getMainLooper())
    private var released = false
    private val lock = Any()
    private var yaw = (panorama.left + panorama.width / 2f - 0.5f) * 360f
    private var pitch = (0.5f - (panorama.top + panorama.height / 2f)) * 180f
    private var fov = DEFAULT_FOV
    private var motionOrigin: FloatArray? = null
    private var motionYaw = 0f
    private var motionPitch = 0f
    private var region = floatArrayOf(0f, 0f, 1f, 1f)

    @Volatile private var bitmap: Bitmap? = null
    @Volatile private var bitmapUploaded = false
    @Volatile private var frameAvailable = false
    private var surfaceTexture: SurfaceTexture? = null
    private var program = 0
    private var texture = 0
    private var aspect = 1f
    private val rotation = FloatArray(9)
    private val surfaceMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private val texMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private val flipY = floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 1f)
    private val quad = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        .put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)).apply { position(0) }

    fun showBitmap(decoded: Bitmap) {
        bitmap = decoded
        bitmapUploaded = false
        view?.requestRender()
    }

    fun setStereoMode(mode: Int) {
        synchronized(lock) {
            region = when (mode) {
                C.STEREO_MODE_TOP_BOTTOM -> floatArrayOf(0f, 0f, 1f, 0.5f)
                C.STEREO_MODE_LEFT_RIGHT -> floatArrayOf(0f, 0f, 0.5f, 1f)
                else -> floatArrayOf(0f, 0f, 1f, 1f)
            }
        }
        view?.requestRender()
    }

    fun look(dx: Float, dy: Float, zoom: Float, heightPx: Int) {
        synchronized(lock) {
            val degreesPerPixel = fov / heightPx.coerceAtLeast(1)
            yaw -= dx * degreesPerPixel
            pitch = (pitch + dy * degreesPerPixel).coerceIn(-MAX_PITCH - motionPitch, MAX_PITCH - motionPitch)
            if (zoom > 0f) fov = (fov / zoom).coerceIn(MIN_FOV, MAX_FOV)
        }
        view?.requestRender()
    }

    fun sense(deviceYaw: Float, devicePitch: Float) {
        synchronized(lock) {
            val origin = motionOrigin ?: floatArrayOf(deviceYaw, devicePitch).also { motionOrigin = it }
            motionYaw = deviceYaw - origin[0]
            motionPitch = devicePitch - origin[1]
        }
        view?.requestRender()
    }

    fun settleMotion() {
        synchronized(lock) {
            yaw += motionYaw
            pitch = (pitch + motionPitch).coerceIn(-MAX_PITCH, MAX_PITCH)
            motionYaw = 0f
            motionPitch = 0f
            motionOrigin = null
        }
    }

    fun release() {
        released = true
        videoSurface?.release()
        videoSurface = null
        surfaceTexture?.release()
        surfaceTexture = null
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = buildProgram(VERTEX_SHADER, fragmentShader(video))
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        texture = ids[0]
        val target = if (video) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D
        GLES20.glBindTexture(target, texture)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        bitmapUploaded = false
        if (video) attachVideoSurface()
    }

    private fun attachVideoSurface() {
        val previousTexture = surfaceTexture
        val previousSurface = videoSurface
        val created = SurfaceTexture(texture).apply {
            setOnFrameAvailableListener {
                frameAvailable = true
                view?.requestRender()
            }
        }
        surfaceTexture = created
        val surface = Surface(created)
        main.post {
            if (released) {
                surface.release()
                created.release()
                return@post
            }
            videoSurface = surface
            onVideoSurface(surface)
            previousSurface?.release()
            previousTexture?.release()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height.coerceAtLeast(1)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (video) {
            val texture = surfaceTexture ?: return
            if (frameAvailable) {
                frameAvailable = false
                runCatching { texture.updateTexImage() }
                texture.getTransformMatrix(surfaceMatrix)
                Matrix.multiplyMM(texMatrix, 0, surfaceMatrix, 0, flipY, 0)
            }
        } else if (!bitmapUploaded) {
            val source = bitmap ?: return
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, fitToTextureLimit(source), 0)
            bitmapUploaded = true
        }

        val (yaw, pitch, fov, region) = synchronized(lock) { Camera(yaw + motionYaw, (pitch + motionPitch).coerceIn(-MAX_PITCH, MAX_PITCH), fov, region.copyOf()) }
        fillRotation(yaw, pitch)
        val tanY = tan(Math.toRadians(fov / 2.0)).toFloat()

        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(if (video) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0)
        GLES20.glUniformMatrix3fv(GLES20.glGetUniformLocation(program, "uRotation"), 1, false, rotation, 0)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uTanHalf"), tanY * aspect, tanY)
        GLES20.glUniform4f(
            GLES20.glGetUniformLocation(program, "uCrop"),
            panorama.left, panorama.top, panorama.width, panorama.height,
        )
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(program, "uRegion"), 1, region, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uTexMatrix"), 1, false, texMatrix, 0)
        val position = GLES20.glGetAttribLocation(program, "aPos")
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(position)
    }

    private fun fillRotation(yawDegrees: Float, pitchDegrees: Float) {
        val yaw = Math.toRadians(yawDegrees.toDouble())
        val pitch = Math.toRadians(pitchDegrees.toDouble())
        val cy = cos(yaw).toFloat()
        val sy = -sin(yaw).toFloat()
        val cp = cos(pitch).toFloat()
        val sp = sin(pitch).toFloat()
        rotation[0] = cy; rotation[1] = 0f; rotation[2] = -sy
        rotation[3] = sy * sp; rotation[4] = cp; rotation[5] = cy * sp
        rotation[6] = sy * cp; rotation[7] = -sp; rotation[8] = cy * cp
    }

    private fun fitToTextureLimit(source: Bitmap): Bitmap {
        val limit = IntArray(1).also { GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, it, 0) }[0]
        val longest = max(source.width, source.height)
        if (limit <= 0 || longest <= limit) return source
        val scale = limit.toFloat() / longest
        return Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
            .also { bitmap = it }
    }

    private data class Camera(val yaw: Float, val pitch: Float, val fov: Float, val region: FloatArray)
}

private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
    fun compile(type: Int, source: String) = GLES20.glCreateShader(type).also {
        GLES20.glShaderSource(it, source)
        GLES20.glCompileShader(it)
    }
    return GLES20.glCreateProgram().also {
        GLES20.glAttachShader(it, compile(GLES20.GL_VERTEX_SHADER, vertexSource))
        GLES20.glAttachShader(it, compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource))
        GLES20.glLinkProgram(it)
    }
}

private const val VERTEX_SHADER = """
attribute vec2 aPos;
varying vec2 vNdc;
void main() {
    vNdc = aPos;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

private fun fragmentShader(video: Boolean) = (
    if (video) "#extension GL_OES_EGL_image_external : require\n#define SAMPLER samplerExternalOES\n"
    else "#define SAMPLER sampler2D\n"
    ) + """
precision highp float;
uniform SAMPLER uTexture;
uniform mat3 uRotation;
uniform vec2 uTanHalf;
uniform vec4 uCrop;
uniform vec4 uRegion;
uniform mat4 uTexMatrix;
varying vec2 vNdc;
const float PI = 3.14159265;
void main() {
    vec3 dir = normalize(uRotation * vec3(vNdc * uTanHalf, -1.0));
    vec2 uv = vec2(atan(dir.x, -dir.z) / (2.0 * PI) + 0.5, 0.5 - asin(clamp(dir.y, -1.0, 1.0)) / PI);
    uv = (uv - uCrop.xy) / uCrop.zw;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    uv = uRegion.xy + uv * uRegion.zw;
    gl_FragColor = texture2D(uTexture, (uTexMatrix * vec4(uv, 0.0, 1.0)).xy);
}
"""
