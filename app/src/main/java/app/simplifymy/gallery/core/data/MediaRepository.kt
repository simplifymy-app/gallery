package app.simplifymy.gallery.core.data

import android.net.Uri

import app.simplifymy.gallery.core.database.FavoriteDao
import app.simplifymy.gallery.core.database.FolderRow
import app.simplifymy.gallery.core.database.IgnoredFolderDao
import app.simplifymy.gallery.core.database.IgnoredFolderEntity
import app.simplifymy.gallery.core.database.MediaDao
import app.simplifymy.gallery.core.database.MediumEntity
import app.simplifymy.gallery.core.database.PinnedFolderDao
import app.simplifymy.gallery.core.database.PinnedFolderEntity
import app.simplifymy.gallery.core.media.MediaStoreScanner
import app.simplifymy.gallery.core.model.Folder
import app.simplifymy.gallery.core.model.FolderEntry
import app.simplifymy.gallery.core.model.MediaFilter
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.core.model.RECENT_BUCKET_ID
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
    private val pinned: PinnedFolderDao,
    private val scanner: MediaStoreScanner,
    private val writer: MediaWriter,
) {

    val changes: Flow<Unit> = scanner.changes

    fun folders(filter: MediaFilter): Flow<List<Folder>> =
        combine(media.folders(filter.types), pinned.ids()) { rows, pins ->
            val pinnedIds = pins.toHashSet()
            rows.map { it.toFolder(it.bucketId in pinnedIds) }
        }

    val folderEntries: Flow<List<FolderEntry>> =
        combine(media.allFolders(), ignored.all()) { rows, hidden ->
            val present = rows.map { FolderEntry(it.bucketId, it.bucketName, it.relativePath, it.itemCount) }
            val known = present.map(FolderEntry::bucketId).toHashSet()
            (present + hidden.filter { it.bucketId !in known }.map { FolderEntry(it.bucketId, it.name, "", 0) })
                .sortedBy { it.name.lowercase() }
        }

    suspend fun setIgnoredFolders(folders: List<FolderEntry>) =
        ignored.replaceAll(folders.map { IgnoredFolderEntity(it.bucketId, it.name) })

    suspend fun setPinned(bucketIds: List<Long>, pin: Boolean) =
        if (pin) pinned.addAll(bucketIds.map(::PinnedFolderEntity)) else pinned.removeAll(bucketIds)

    fun mediaIn(bucketId: Long, filter: MediaFilter): Flow<List<Medium>> =
        (if (bucketId == RECENT_BUCKET_ID) media.recent(filter.types) else media.mediaIn(bucketId, filter.types)).withFavorites()

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

    suspend fun locate(uri: Uri): Medium? {
        val id = scanner.mediaIdOf(uri) ?: return null
        return medium(id) ?: run {
            sync()
            medium(id)
        }
    }

    suspend fun mediaForFolders(bucketIds: List<Long>): List<Medium> =
        media.mediaForFolders(bucketIds).map { it.toMedium(false) }

    suspend fun sync() = media.replaceAll(scanner.scan())

    suspend fun copy(items: List<Medium>, folder: String): Int = items.count { writer.copy(it, folder) }

    suspend fun move(items: List<Medium>, folder: String): Int = items.count { medium ->
        relocate(medium, { folder + it.substringAfterLast('/') }) { writer.move(medium, folder) }
    }

    suspend fun rename(medium: Medium, name: String): Boolean =
        relocate(medium, { it.substringBeforeLast('/', "") + "/" + name }) { writer.rename(medium, name) }

    private suspend fun relocate(medium: Medium, target: (String) -> String, write: suspend () -> Boolean): Boolean {
        val path = media.pathOf(medium.id)
        if (!write()) return false
        if (medium.isFavorite && path != null) {
            favorites.remove(path)
            favorites.add(target(path))
        }
        return true
    }

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

private fun FolderRow.toFolder(isPinned: Boolean) = Folder(
    bucketId = bucketId,
    name = bucketName,
    itemCount = itemCount,
    size = size,
    dateTaken = dateTaken,
    dateModified = dateModified,
    coverId = coverId,
    coverType = coverType,
    isPinned = isPinned,
)
