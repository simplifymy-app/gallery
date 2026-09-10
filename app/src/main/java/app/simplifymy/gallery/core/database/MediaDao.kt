package app.simplifymy.gallery.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query(
        """
        SELECT m.bucket_id, m.bucket_name, COUNT(*) AS item_count, SUM(m.size) AS size,
               MAX(m.date_taken) AS date_taken, MAX(m.date_modified) AS date_modified,
               (SELECT c.id FROM media c WHERE c.bucket_id = m.bucket_id AND c.type IN (:types)
                ORDER BY c.date_taken DESC LIMIT 1) AS cover_id,
               (SELECT c.type FROM media c WHERE c.bucket_id = m.bucket_id AND c.type IN (:types)
                ORDER BY c.date_taken DESC LIMIT 1) AS cover_type
        FROM media m
        WHERE m.type IN (:types)
          AND m.bucket_id NOT IN (SELECT bucket_id FROM ignored_folders)
        GROUP BY m.bucket_id, m.bucket_name
        """
    )
    fun folders(types: List<Int>): Flow<List<FolderRow>>

    @Query("SELECT * FROM media WHERE bucket_id = :bucketId AND type IN (:types)")
    fun mediaIn(bucketId: Long, types: List<Int>): Flow<List<MediumEntity>>

    @Query("SELECT * FROM media WHERE name LIKE '%' || :query || '%' ORDER BY date_taken DESC LIMIT 300")
    fun search(query: String): Flow<List<MediumEntity>>

    @Query("SELECT * FROM media WHERE bucket_id IN (:bucketIds)")
    suspend fun mediaForFolders(bucketIds: List<Long>): List<MediumEntity>

    @Query("SELECT relative_path || name FROM media WHERE id = :id")
    suspend fun pathOf(id: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediumEntity>)

    @Query("DELETE FROM media")
    suspend fun clear()

    /** MediaStore is the source of truth; the table is a pure cache, so swap it wholesale. */
    @Transaction
    suspend fun replaceAll(items: List<MediumEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface IgnoredFolderDao {
    @Query("SELECT * FROM ignored_folders ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<IgnoredFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(folder: IgnoredFolderEntity)

    @Query("DELETE FROM ignored_folders WHERE bucket_id = :bucketId")
    suspend fun remove(bucketId: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT path FROM favorites")
    fun paths(): Flow<List<String>>

    @Query("INSERT OR REPLACE INTO favorites(path) VALUES(:path)")
    suspend fun add(path: String)

    @Query("DELETE FROM favorites WHERE path = :path")
    suspend fun remove(path: String)
}
