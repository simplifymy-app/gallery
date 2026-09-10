package app.simplifymy.gallery.core.data

import app.simplifymy.gallery.core.database.FavoriteDao
import app.simplifymy.gallery.core.database.FolderRow
import app.simplifymy.gallery.core.database.IgnoredFolderDao
import app.simplifymy.gallery.core.database.IgnoredFolderEntity
import app.simplifymy.gallery.core.database.MediaDao
import app.simplifymy.gallery.core.database.MediumEntity
import app.simplifymy.gallery.core.media.MediaStoreScanner
import app.simplifymy.gallery.core.model.Folder
import app.simplifymy.gallery.core.model.MediaFilter
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.core.model.mediaUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val media: MediaDao,
    private val favorites: FavoriteDao,
    private val ignored: IgnoredFolderDao,
    private val scanner: MediaStoreScanner,
) {

    val changes: Flow<Unit> = scanner.changes

    fun folders(filter: MediaFilter): Flow<List<Folder>> =
        media.folders(filter.types).map { rows -> rows.map(FolderRow::toFolder) }

    fun mediaIn(bucketId: Long, filter: MediaFilter): Flow<List<Medium>> =
        media.mediaIn(bucketId, filter.types).withFavorites()

    fun search(query: String): Flow<List<Medium>> = media.search(query).withFavorites()

    private fun Flow<List<MediumEntity>>.withFavorites(): Flow<List<Medium>> =
        combine(this, favorites.paths()) { items, favPaths ->
            val favs = favPaths.toHashSet()
            items.map { it.toMedium(it.path in favs) }
        }

    val ignoredFolders: Flow<List<IgnoredFolderEntity>> = ignored.all()

    suspend fun ignoreFolder(bucketId: Long, name: String) =
        ignored.add(IgnoredFolderEntity(bucketId, name))

    suspend fun ignoreFolders(folders: List<Folder>) =
        folders.forEach { ignored.add(IgnoredFolderEntity(it.bucketId, it.name)) }

    suspend fun medium(id: Long): Medium? = media.medium(id)?.toMedium(false)

    suspend fun mediaForFolders(bucketIds: List<Long>): List<Medium> =
        media.mediaForFolders(bucketIds).map { it.toMedium(false) }

    suspend fun unignoreFolder(bucketId: Long) = ignored.remove(bucketId)

    suspend fun sync() = media.replaceAll(scanner.scan())

    suspend fun setFavorite(medium: Medium, favorite: Boolean) {
        val path = media.pathOf(medium.id) ?: return
        if (favorite) favorites.add(path) else favorites.remove(path)
    }
}

private val MediumEntity.path: String get() = relativePath + name

private fun MediumEntity.toMedium(isFavorite: Boolean) = Medium(
    id = id,
    name = name,
    bucketId = bucketId,
    dateTaken = dateTaken,
    dateModified = dateModified,
    size = size,
    type = MediaType.entries[type],
    durationMs = durationMs,
    isFavorite = isFavorite,
)

private fun FolderRow.toFolder() = Folder(
    bucketId = bucketId,
    name = bucketName,
    itemCount = itemCount,
    size = size,
    dateTaken = dateTaken,
    dateModified = dateModified,
    coverId = coverId,
    coverType = coverType,
)

