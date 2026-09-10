package app.simplifymy.gallery

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp
import app.simplifymy.gallery.core.media.Thumbnail
import app.simplifymy.gallery.core.media.ThumbnailFetcher
import app.simplifymy.gallery.core.media.ThumbnailKeyer
import app.simplifymy.gallery.core.settings.AppIconManager
import app.simplifymy.gallery.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GalleryApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var appIcons: AppIconManager

    /** A restored backup brings the preference back but not the alias state, so queue a reconcile. */
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            appIcons.request(settings.settings.first().appIcon)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(ThumbnailFetcher.Factory(context), Thumbnail::class)
                add(ThumbnailKeyer(), Thumbnail::class)
                add(AnimatedImageDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
}
