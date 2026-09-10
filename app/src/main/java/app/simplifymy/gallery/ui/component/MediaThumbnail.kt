package app.simplifymy.gallery.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import app.simplifymy.gallery.ui.theme.GalleryTheme

@Composable
fun MediaThumbnail(
    model: Any?,
    modifier: Modifier = Modifier,
    crop: Boolean = true,
    aspectRatio: Float = 1f,
) {
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = if (crop) ContentScale.Crop else ContentScale.Fit,
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(GalleryTheme.corners.radius))
            .background(GalleryTheme.colors.surface),
    )
}
