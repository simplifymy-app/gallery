package app.simplifymy.gallery.core.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPaths @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun absolute(media: List<Medium>): List<String> = withContext(Dispatchers.IO) {
        val (videos, images) = media.partition { it.type == MediaType.Video }
        val imagePaths = pathsIn(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, images)
        val videoPaths = pathsIn(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videos)
        media.mapNotNull { (if (it.type == MediaType.Video) videoPaths else imagePaths)[it.id] }
    }

    suspend fun folders(bucketIds: Set<Long>?): List<String> = withContext(Dispatchers.IO) {
        val found = (foldersIn(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, bucketIds) +
            foldersIn(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, bucketIds)).toSortedSet()
        found.filter { folder -> found.none { it != folder && folder.startsWith("$it/") } }
    }

    @Suppress("DEPRECATION")
    private fun foldersIn(collection: Uri, bucketIds: Set<Long>?): Set<String> {
        val selection = bucketIds?.let { "${MediaStore.MediaColumns.BUCKET_ID} IN (${it.joinToString(",")})" }
        val folders = mutableSetOf<String>()
        context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns.DATA), selection, null, null)?.use { c ->
            while (c.moveToNext()) {
                c.getString(0)?.substringBeforeLast('/', "")?.takeIf { it.isNotEmpty() }?.let(folders::add)
            }
        }
        return folders
    }

    @Suppress("DEPRECATION")
    private fun pathsIn(collection: Uri, media: List<Medium>): Map<Long, String> {
        if (media.isEmpty()) return emptyMap()
        val ids = media.joinToString(",") { it.id.toString() }
        val paths = mutableMapOf<Long, String>()
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA),
            "${MediaStore.MediaColumns._ID} IN ($ids)",
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                c.getString(1)?.let { paths[c.getLong(0)] = it }
            }
        }
        return paths
    }
}
