package app.simplifymy.gallery.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing

@Composable
fun IconAction(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    tint: Color = GalleryTheme.colors.text,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(Spacing.hitTarget)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(ControlRadius))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Modifier.island(on: Boolean): Modifier =
    if (on) background(GalleryTheme.colors.surface.copy(alpha = 0.55f), RoundedCornerShape(ControlRadius)) else this

fun islandGap(on: Boolean): Dp = if (on) 6.dp else 0.dp

@Composable
fun TextStyle.shadowed(on: Boolean): TextStyle =
    if (on) copy(shadow = Shadow(GalleryTheme.colors.background.copy(alpha = 0.9f), blurRadius = 14f)) else this

@Composable
fun Chip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    trailingIcon: ImageVector? = null,
) {
    val colors = GalleryTheme.colors
    Row(
        modifier = modifier
            .sizeIn(minHeight = 32.dp)
            .clip(RoundedCornerShape(ControlRadius))
            .background(if (selected) colors.accent else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val content = if (selected) colors.onAccent else colors.textMuted
        Text(label, style = GalleryTheme.type.label, color = content)
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun <T> SegmentedPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (T) -> String = { it.toString() },
) {
    val colors = GalleryTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(colors.surface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 28.dp, minHeight = 28.dp)
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(if (on) colors.accent else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = GalleryTheme.type.label,
                    color = if (on) colors.onAccent else colors.textMuted,
                )
            }
        }
    }
}
