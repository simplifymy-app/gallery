package app.simplifymy.gallery.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import app.simplifymy.gallery.core.database.FavoriteDao
import app.simplifymy.gallery.core.database.GalleryDatabase
import app.simplifymy.gallery.core.database.IgnoredFolderDao
import app.simplifymy.gallery.core.database.MediaDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): GalleryDatabase =
        Room.databaseBuilder(context, GalleryDatabase::class.java, "gallery.db")
            .addMigrations(GalleryDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun mediaDao(db: GalleryDatabase): MediaDao = db.mediaDao()

    @Provides
    fun favoriteDao(db: GalleryDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun ignoredFolderDao(db: GalleryDatabase): IgnoredFolderDao = db.ignoredFolderDao()
}
