package app.simplifymy.gallery.core.media

import android.content.ContentResolver
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

data class Panorama(
    val left: Float = 0f,
    val top: Float = 0f,
    val width: Float = 1f,
    val height: Float = 1f,
)

private const val MAX_MOOV_BYTES = 32L shl 20
private val SPHERICAL_MARKERS = listOf("sv3d", "GSpherical").map { it.toByteArray(Charsets.US_ASCII) }

fun readPanorama(resolver: ContentResolver, uri: Uri, isVideo: Boolean): Panorama? = runCatching {
    if (isVideo) {
        resolver.openFileDescriptor(uri, "r")?.use { fd ->
            FileInputStream(fd.fileDescriptor).channel.use { if (isSphericalMp4(it)) Panorama() else null }
        }
    } else {
        resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeBytes(ExifInterface.TAG_XMP)
                ?.toString(Charsets.UTF_8)
                ?.let(::panoramaFromXmp)
        }
    }
}.getOrNull()

fun panoramaFromXmp(xmp: String): Panorama? {
    if (!gpano(xmp, "ProjectionType").equals("equirectangular", ignoreCase = true)) return null
    val fullWidth = gpanoSize(xmp, "FullPanoWidthPixels")
    val fullHeight = gpanoSize(xmp, "FullPanoHeightPixels")
    val croppedWidth = gpanoSize(xmp, "CroppedAreaImageWidthPixels")
    val croppedHeight = gpanoSize(xmp, "CroppedAreaImageHeightPixels")
    if (fullWidth == null || fullHeight == null || croppedWidth == null || croppedHeight == null) return Panorama()
    return Panorama(
        left = (gpanoNumber(xmp, "CroppedAreaLeftPixels") ?: 0f) / fullWidth,
        top = (gpanoNumber(xmp, "CroppedAreaTopPixels") ?: 0f) / fullHeight,
        width = croppedWidth / fullWidth,
        height = croppedHeight / fullHeight,
    )
}

private fun gpano(xmp: String, name: String): String? =
    Regex("""GPano:$name\s*(?:=\s*["']|>)\s*([^"'<]+)""").find(xmp)?.groupValues?.get(1)?.trim()

private fun gpanoNumber(xmp: String, name: String): Float? =
    gpano(xmp, name)?.toFloatOrNull()?.takeIf { it >= 0f && it.isFinite() }

private fun gpanoSize(xmp: String, name: String): Float? = gpanoNumber(xmp, name)?.takeIf { it > 0f }

fun isSphericalMp4(channel: FileChannel): Boolean {
    val header = ByteBuffer.allocate(16)
    var position = 0L
    val end = channel.size()
    while (position + 8 <= end) {
        header.clear().limit(8)
        channel.read(header, position)
        header.flip()
        var size = header.int.toLong() and 0xFFFFFFFFL
        val type = ByteArray(4).also { header.get(it) }.toString(Charsets.US_ASCII)
        var headerSize = 8L
        if (size == 1L) {
            header.clear().limit(8)
            channel.read(header, position + 8)
            header.flip()
            size = header.long
            headerSize = 16L
        } else if (size == 0L) {
            size = end - position
        }
        if (size < headerSize) return false
        if (type == "moov") {
            val body = ByteBuffer.allocate(minOf(size - headerSize, MAX_MOOV_BYTES).toInt())
            channel.read(body, position + headerSize)
            val bytes = body.array()
            return SPHERICAL_MARKERS.any { bytes.contains(it) }
        }
        position += size
    }
    return false
}

private fun ByteArray.contains(needle: ByteArray): Boolean {
    outer@ for (start in 0..size - needle.size) {
        for (i in needle.indices) if (this[start + i] != needle[i]) continue@outer
        return true
    }
    return false
}
