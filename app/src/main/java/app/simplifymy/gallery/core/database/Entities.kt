package app.simplifymy.gallery.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "media", indices = [Index("bucket_id"), Index("date_taken")])
data class MediumEntity(
    @PrimaryKey val id: Long,
    val name: String,
    @ColumnInfo(name = "bucket_id") val bucketId: Long,
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    @ColumnInfo(name = "relative_path") val relativePath: String,
    @ColumnInfo(name = "date_taken") val dateTaken: Long,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    val size: Long,
    val type: Int,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
)

@Entity(tableName = "ignored_folders")
data class IgnoredFolderEntity(
    @PrimaryKey @ColumnInfo(name = "bucket_id") val bucketId: Long,
    val name: String,
)

@Entity(tableName = "pinned_folders")
data class PinnedFolderEntity(@PrimaryKey @ColumnInfo(name = "bucket_id") val bucketId: Long)

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val path: String)

data class FolderRow(
    @ColumnInfo(name = "bucket_id") val bucketId: Long,
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    @ColumnInfo(name = "item_count") val itemCount: Int,
    val size: Long,
    @ColumnInfo(name = "date_taken") val dateTaken: Long,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    @ColumnInfo(name = "cover_id") val coverId: Long,
    @ColumnInfo(name = "cover_type") val coverType: Int,
)

data class FolderNameRow(
    @ColumnInfo(name = "bucket_id") val bucketId: Long,
    @ColumnInfo(name = "bucket_name") val bucketName: String,
    @ColumnInfo(name = "relative_path") val relativePath: String,
    @ColumnInfo(name = "item_count") val itemCount: Int,
)
