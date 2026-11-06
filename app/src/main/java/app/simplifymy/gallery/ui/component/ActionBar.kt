package app.simplifymy.gallery.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.core.model.BarAction
import app.simplifymy.gallery.core.model.BarLayout
import app.simplifymy.gallery.ui.theme.GalleryTheme

val BarAction.icon: ImageVector
    get() = when (this) {
        BarAction.Share -> GalleryIcons.Share
        BarAction.Favorite -> GalleryIcons.Heart
        BarAction.Edit -> GalleryIcons.Edit
        BarAction.SetAs -> GalleryIcons.SetAs
        BarAction.Copy -> GalleryIcons.Duplicate
        BarAction.Move -> GalleryIcons.Move
        BarAction.Rename -> GalleryIcons.Rename
        BarAction.CopyPath -> GalleryIcons.Link
        BarAction.Metadata -> GalleryIcons.Tag
        BarAction.Delete -> GalleryIcons.Trash
    }

@Composable
fun ActionBar(
    layout: BarLayout,
    onAction: (BarAction) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GalleryTheme.colors.text,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fitted = layout.fitting((maxWidth / (GalleryTheme.hitTarget + 12.dp)).toInt())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            fitted.bar.forEach { action ->
                IconAction(action.icon, onClick = { onAction(action) }, label = stringResource(action.labelRes), tint = tint)
            }
            if (fitted.menu.isNotEmpty()) {
                OverflowMenu(tint = tint) { dismiss ->
                    fitted.menu.forEach { action ->
                        MenuEntry(stringResource(action.labelRes), icon = action.icon) { dismiss(); onAction(action) }
                    }
                }
            }
        }
    }
}
