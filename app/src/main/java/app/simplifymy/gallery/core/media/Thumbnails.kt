package app.simplifymy.gallery.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
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
import coil3.size.Scale
import coil3.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
        val sharp = bitmap?.takeIf { needsSharperCrop(it, size) }?.let { centerCrop(size) }
        (sharp ?: bitmap)?.let { ImageFetchResult(it.asImage(), isSampled = true, dataSource = DataSource.DISK) }
            ?: original()
    }

    private fun needsSharperCrop(thumbnail: Bitmap, target: Size): Boolean =
        options.scale == Scale.FILL &&
            max(target.width.toFloat() / thumbnail.width, target.height.toFloat() / thumbnail.height) > MAX_THUMBNAIL_UPSCALE &&
            context.contentResolver.getType(data.uri)?.startsWith("image/") == true

    private fun centerCrop(target: Size): Bitmap? = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, data.uri)) { decoder, info, _ ->
            val scale = min(1f, max(target.width.toFloat() / info.size.width, target.height.toFloat() / info.size.height))
            val width = (info.size.width * scale).roundToInt()
            val height = (info.size.height * scale).roundToInt()
            val cropWidth = min(target.width, width)
            val cropHeight = min(target.height, height)
            val left = (width - cropWidth) / 2
            val top = (height - cropHeight) / 2
            decoder.setTargetSize(width, height)
            decoder.crop = Rect(left, top, left + cropWidth, top + cropHeight)
        }
    } catch (_: IOException) {
        null
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
private const val MAX_THUMBNAIL_UPSCALE = 1.5f
