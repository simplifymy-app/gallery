package app.simplifymy.gallery.core.media

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import app.simplifymy.gallery.core.database.MediumEntity
import app.simplifymy.gallery.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val IMAGES: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
private val VIDEOS: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

private val BASE_COLUMNS = arrayOf(
    MediaStore.MediaColumns._ID,
    MediaStore.MediaColumns.DISPLAY_NAME,
    MediaStore.MediaColumns.BUCKET_ID,
    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
    MediaStore.MediaColumns.RELATIVE_PATH,
    MediaStore.MediaColumns.DATE_TAKEN,
    MediaStore.MediaColumns.DATE_MODIFIED,
    MediaStore.MediaColumns.SIZE,
    MediaStore.MediaColumns.MIME_TYPE,
)

private val VIDEO_COLUMNS = BASE_COLUMNS + MediaStore.Video.VideoColumns.DURATION

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun scan(): List<MediumEntity> = withContext(Dispatchers.IO) {
        read(IMAGES, BASE_COLUMNS, video = false) + read(VIDEOS, VIDEO_COLUMNS, video = true)
    }

    private fun read(collection: Uri, columns: Array<String>, video: Boolean): List<MediumEntity> {
        val out = ArrayList<MediumEntity>(512)
        context.contentResolver.query(collection, columns, null, null, null)?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val bucketId = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val path = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val taken = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val modified = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val size = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val duration = if (video) c.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

            while (c.moveToNext()) {
                val modifiedMs = c.getLong(modified) * 1000
                out += MediumEntity(
                    id = c.getLong(id),
                    name = c.getString(name).orEmpty(),
                    bucketId = c.getLong(bucketId),
                    bucketName = c.getString(bucketName) ?: "Unknown",
                    relativePath = c.getString(path).orEmpty(),
                    dateTaken = c.getLong(taken).takeIf { it > 0 } ?: modifiedMs,
                    dateModified = modifiedMs,
                    size = c.getLong(size),
                    type = typeOf(video, c.getString(mime)).ordinal,
                    durationMs = if (duration >= 0) c.getLong(duration) else 0L,
                )
            }
        }
        return out
    }

    fun mediaIdOf(uri: Uri): Long? {
        val media = if (DocumentsContract.isDocumentUri(context, uri)) {
            runCatching { MediaStore.getMediaUri(context, uri) }.getOrNull()
        } else {
            uri
        }
        if (media?.authority != MediaStore.AUTHORITY) return null
        return runCatching { ContentUris.parseId(media) }.getOrNull()?.takeIf { it >= 0 }
    }

    val changes: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        context.contentResolver.registerContentObserver(IMAGES, true, observer)
        context.contentResolver.registerContentObserver(VIDEOS, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
}

private fun typeOf(video: Boolean, mime: String?): MediaType = when {
    video -> MediaType.Video
    mime == "image/gif" -> MediaType.Gif
    else -> MediaType.Image
}
