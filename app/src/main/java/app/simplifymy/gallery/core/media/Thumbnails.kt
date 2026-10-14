package app.simplifymy.gallery.core.media

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.IOException
import kotlin.coroutines.coroutineContext

data class Thumbnail(val uri: Uri)

class ThumbnailFetcher(
    private val context: Context,
    private val data: Thumbnail,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val signal = CancellationSignal()
        coroutineContext[Job]?.invokeOnCompletion { signal.cancel() }
        val size = Size(
            options.size.width.pxOrElse { FALLBACK_PX },
            options.size.height.pxOrElse { FALLBACK_PX },
        )
        val bitmap = try {
            context.contentResolver.loadThumbnail(data.uri, size, signal)
        } catch (_: IOException) {
            null
        }
        bitmap?.let { ImageFetchResult(it.asImage(), isSampled = true, dataSource = DataSource.DISK) }
            ?: original()
    }

    private fun original(): FetchResult {
        val resolver = context.contentResolver
        val descriptor = checkNotNull(resolver.openAssetFileDescriptor(data.uri, "r")) {
            "Unable to open ${data.uri}."
        }
        return SourceFetchResult(
            source = ImageSource(descriptor.createInputStream().source().buffer(), options.fileSystem),
            mimeType = resolver.getType(data.uri),
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Thumbnail> {
        override fun create(data: Thumbnail, options: Options, imageLoader: ImageLoader): Fetcher =
            ThumbnailFetcher(context, data, options)
    }
}

class ThumbnailKeyer : Keyer<Thumbnail> {
    override fun key(data: Thumbnail, options: Options): String = data.uri.toString()
}

private const val FALLBACK_PX = 512
