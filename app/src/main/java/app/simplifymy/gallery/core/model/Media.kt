package app.simplifymy.gallery.core.model

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

enum class MediaType { Image, Video, Gif }

data class Medium(
    val id: Long,
    override val name: String,
    val bucketId: Long,
    override val dateTaken: Long,
    override val dateModified: Long,
    override val size: Long,
    val type: MediaType,
    val durationMs: Long,
    val isFavorite: Boolean,
) : Sortable {
    override val typeRank: Int get() = type.ordinal
    val uri: Uri get() = mediaUri(id, type)
}

data class Folder(
    val bucketId: Long,
    override val name: String,
    val itemCount: Int,
    override val size: Long,
    override val dateTaken: Long,
    override val dateModified: Long,
    val coverId: Long,
    val coverType: Int,
    val isPinned: Boolean = false,
) : Sortable {
    override val typeRank: Int get() = coverType
    val coverUri: Uri get() = mediaUri(coverId, MediaType.entries[coverType])
}

data class FolderEntry(
    val bucketId: Long,
    val name: String,
    val path: String,
    val itemCount: Int,
)

fun mediaUri(id: Long, type: MediaType): Uri = ContentUris.withAppendedId(
    if (type == MediaType.Video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    id,
)
