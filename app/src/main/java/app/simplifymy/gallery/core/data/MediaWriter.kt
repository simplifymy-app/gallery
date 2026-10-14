package app.simplifymy.gallery.core.data

import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun trashRequest(media: List<Medium>, trashed: Boolean): IntentSender =
        MediaStore.createTrashRequest(context.contentResolver, media.map(Medium::uri), trashed).intentSender

    fun deleteRequest(media: List<Medium>): IntentSender =
        MediaStore.createDeleteRequest(context.contentResolver, media.map(Medium::uri)).intentSender

    suspend fun saveCopy(source: Medium, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val folder = resolver.query(source.uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?: "${android.os.Environment.DIRECTORY_PICTURES}/"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, editedName(source.name))
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext null
        runCatching {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                ?: error("Unable to open $uri.")
        }.onFailure {
            resolver.delete(uri, null, null)
            return@withContext null
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        uri
    }
}

private fun editedName(name: String): String {
    val stem = name.substringBeforeLast('.', name)
    return "${stem}_edited_${System.currentTimeMillis()}.jpg"
}
