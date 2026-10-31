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
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver get() = context.contentResolver

    fun trashRequest(media: List<Medium>, trashed: Boolean): IntentSender =
        MediaStore.createTrashRequest(resolver, media.map(Medium::uri), trashed).intentSender

    fun deleteRequest(media: List<Medium>): IntentSender =
        MediaStore.createDeleteRequest(resolver, media.map(Medium::uri)).intentSender

    fun writeRequest(media: List<Medium>): IntentSender =
        MediaStore.createWriteRequest(resolver, media.map(Medium::uri)).intentSender

    suspend fun saveCopy(source: Medium, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val folder = resolver.query(source.uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?: "${android.os.Environment.DIRECTORY_PICTURES}/"
        insert(MediaType.Image, editedName(source.name), "image/jpeg", folder) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
        }
    }

    suspend fun copy(source: Medium, folder: String): Boolean = withContext(Dispatchers.IO) {
        val mime = resolver.getType(source.uri) ?: return@withContext false
        insert(source.type, source.name, mime, folder) { output ->
            resolver.openInputStream(source.uri)?.use { it.copyTo(output) } ?: error("Unable to read ${source.uri}.")
        } != null
    }

    suspend fun move(medium: Medium, folder: String): Boolean =
        update(medium, MediaStore.MediaColumns.RELATIVE_PATH, folder)

    suspend fun rename(medium: Medium, name: String): Boolean =
        update(medium, MediaStore.MediaColumns.DISPLAY_NAME, name)

    private suspend fun update(medium: Medium, column: String, value: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            resolver.update(medium.uri, ContentValues().apply { put(column, value) }, null, null) > 0
        }.getOrDefault(false)
    }

    private fun insert(type: MediaType, name: String, mime: String, folder: String, write: (OutputStream) -> Unit): Uri? {
        val collection = if (type == MediaType.Video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = runCatching { resolver.insert(collection, values) }.getOrNull() ?: return null
        runCatching {
            resolver.openOutputStream(uri)?.use(write) ?: error("Unable to open $uri.")
        }.onFailure {
            resolver.delete(uri, null, null)
            return null
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return uri
    }
}

private fun editedName(name: String): String {
    val stem = name.substringBeforeLast('.', name)
    return "${stem}_edited_${System.currentTimeMillis()}.jpg"
}
