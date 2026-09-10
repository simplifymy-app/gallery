package app.simplifymy.gallery.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MediumEntity::class, FavoriteEntity::class, IgnoredFolderEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun ignoredFolderDao(): IgnoredFolderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS ignored_folders " +
                        "(bucket_id INTEGER NOT NULL, name TEXT NOT NULL, PRIMARY KEY(bucket_id))"
                )
            }
        }
    }
}
