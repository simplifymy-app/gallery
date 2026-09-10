package app.simplifymy.gallery.core.media

import android.content.Context
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.mediaUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class MediaDetails(
    val dimensions: String? = null,
    val camera: String? = null,
    val exposure: String? = null,
    val path: String? = null,
)

private val COLUMNS = arrayOf(
    MediaStore.Files.FileColumns.WIDTH,
    MediaStore.Files.FileColumns.HEIGHT,
    MediaStore.Files.FileColumns.RELATIVE_PATH,
)

@Singleton
class MediaDetailsLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun load(id: Long, type: MediaType): MediaDetails = withContext(Dispatchers.IO) {
        val uri = mediaUri(id, type)
        var details = MediaDetails()

        context.contentResolver.query(uri, COLUMNS, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val width = c.getInt(0)
                val height = c.getInt(1)
                details = details.copy(
                    dimensions = if (width > 0 && height > 0) "$width × $height" else null,
                    path = c.getString(2)?.trimEnd('/')?.let { "/$it" },
                )
            }
        }

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                details = details.copy(
                    camera = listOfNotNull(
                        exif.getAttribute(ExifInterface.TAG_MAKE),
                        exif.getAttribute(ExifInterface.TAG_MODEL),
                    ).joinToString(" ").ifBlank { null },
                    exposure = exif.exposureSummary(),
                )
            }
        }
        details
    }
}

private fun ExifInterface.exposureSummary(): String? {
    val aperture = getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "ƒ/$it" }
    val shutter = getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        ?.toFloatOrNull()
        ?.let { if (it >= 1f) "${it}s" else "1/${(1 / it).toInt()}" }
    val iso = getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" }
    return listOfNotNull(aperture, shutter, iso).joinToString(" · ").ifBlank { null }
}
