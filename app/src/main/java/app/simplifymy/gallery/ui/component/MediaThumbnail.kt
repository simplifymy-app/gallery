package app.simplifymy.gallery.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
import androidx.compose.runtime.remember
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import app.simplifymy.gallery.core.media.Thumbnail
import app.simplifymy.gallery.ui.theme.GalleryTheme

@Composable
fun MediaThumbnail(
    model: Uri?,
    modifier: Modifier = Modifier,
    crop: Boolean = true,
    aspectRatio: Float = 1f,
    description: String? = null,
) {
    val context = LocalPlatformContext.current
    val request = remember(model) {
        model?.let { ImageRequest.Builder(context).data(Thumbnail(it)).crossfade(false).build() }
    }
    AsyncImage(
        model = request,
        contentDescription = description,
        contentScale = if (crop) ContentScale.Crop else ContentScale.Fit,
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(GalleryTheme.corners.radius))
            .background(GalleryTheme.colors.surface),
    )
}
