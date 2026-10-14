package app.simplifymy.gallery.ui.component

import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme

@Composable
fun OverflowMenu(
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = GalleryTheme.colors.text,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        IconAction(GalleryIcons.More, onClick = { open = true }, label = stringResource(R.string.action_more), tint = tint)
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(6.dp),
            containerColor = GalleryTheme.colors.surface,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)),
        ) {
            content { open = false }
        }
    }
}

@Composable
fun MenuEntry(label: String, value: String? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(label, style = GalleryTheme.type.body, color = GalleryTheme.colors.text)
        },
        trailingIcon = value?.let {
            {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(ControlRadius))
                        .background(GalleryTheme.colors.surfaceAlt)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(it, style = GalleryTheme.type.meta, color = GalleryTheme.colors.textMuted)
                }
            }
        },
        onClick = onClick,
    )
}

@Composable
fun <T> DropdownSetting(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: @Composable (T) -> String = { it.toString() },
) {
    var open by remember { mutableStateOf(false) }
    val colors = GalleryTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = GalleryTheme.type.body, color = colors.text, modifier = Modifier.weight(1f))
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(colors.surface)
                    .clickable { open = true }
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(optionLabel(selected), style = GalleryTheme.type.label, color = colors.text)
                Icon(GalleryIcons.Chevron, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(14.dp))
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RoundedCornerShape(6.dp),
                containerColor = colors.surface,
            ) {
                options.forEach { option ->
                    MenuEntry(optionLabel(option), value = if (option == selected) "\u2713" else null) {
                        onSelect(option)
                        open = false
                    }
                }
            }
        }
    }
}

@Composable
fun MenuStepper(
    label: String,
    value: Int,
    range: IntRange,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    val colors = GalleryTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = GalleryTheme.type.body, color = colors.text, modifier = Modifier.weight(1f))
        Step("\u2212", value > range.first) { onChange(value - 1) }
        Text(
            value.toString(),
            style = GalleryTheme.type.label,
            color = colors.text,
            modifier = Modifier.widthIn(min = 18.dp),
            textAlign = TextAlign.Center,
        )
        Step("+", value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun Step(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = GalleryTheme.colors
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(ControlRadius))
            .background(colors.surfaceAlt)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = GalleryTheme.type.label, color = if (enabled) colors.text else colors.textFaint)
    }
}
