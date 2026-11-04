package app.simplifymy.gallery.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.simplifymy.gallery.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.component.CheckBox
import app.simplifymy.gallery.core.model.BarAction
import app.simplifymy.gallery.core.model.BarLayout
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.icon
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing

private val RowHeight = 46.dp

private fun BarLayout.slots(): List<BarAction?> = bar + null + menu

private fun List<BarAction?>.toLayout(hidden: Set<BarAction>): BarLayout {
    val divider = indexOf(null)
    return BarLayout(subList(0, divider).filterNotNull(), subList(divider + 1, size).filterNotNull(), hidden)
}

private fun List<BarAction?>.swapped(from: Int, to: Int): List<BarAction?> =
    toMutableList().also { it[from] = this[to]; it[to] = this[from] }

private fun List<BarAction?>.toggled(action: BarAction): List<BarAction?> {
    val inBar = indexOf(action) < indexOf(null)
    val rest = minus(action)
    val divider = rest.indexOf(null)
    return rest.toMutableList().also { it.add(if (inBar) divider + 1 else divider, action) }
}

@Composable
fun ActionBarsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(GalleryIcons.Back, onClick = onBack)
            Text(stringResource(R.string.section_actions), style = GalleryTheme.type.title, color = colors.text)
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.actions_hint),
                style = GalleryTheme.type.meta,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = Spacing.screen),
            )
            Text(
                stringResource(R.string.actions_overflow_hint),
                style = GalleryTheme.type.meta,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = Spacing.screen),
            )
            ActionBarEditor(stringResource(R.string.setting_selection_bar), settings.selectionBar, viewModel::setSelectionBar)
            ActionBarEditor(stringResource(R.string.setting_viewer_bar), settings.viewerBar, viewModel::setViewerBar)
        }
    }
}

@Composable
private fun ActionBarEditor(title: String, layout: BarLayout, onChange: (BarLayout) -> Unit) {
    val colors = GalleryTheme.colors
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }
    var slots by remember(layout) { mutableStateOf(layout.slots()) }
    var dragged by remember { mutableStateOf<BarAction?>(null) }
    var dragging by remember { mutableStateOf(false) }
    var offset by remember { mutableFloatStateOf(0f) }

    fun start(slot: BarAction?) {
        dragged = slot
        dragging = true
        offset = 0f
    }

    fun drag(dy: Float) {
        offset += dy
        val index = slots.indexOf(dragged)
        if (offset > rowPx / 2 && index < slots.lastIndex) {
            slots = slots.swapped(index, index + 1)
            offset -= rowPx
        } else if (offset < -rowPx / 2 && index > 0) {
            slots = slots.swapped(index, index - 1)
            offset += rowPx
        }
    }

    fun finish() {
        if (!dragging) return
        dragging = false
        offset = 0f
        onChange(slots.toLayout(layout.hidden))
    }

    Column(
        modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = GalleryTheme.type.body, color = colors.text)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surface)
                .border(1.dp, colors.hairline, RoundedCornerShape(6.dp)),
        ) {
            slots.forEach { slot ->
                key(slot?.name ?: "divider") {
                    val lifted = dragging && slot == dragged
                    val gestures = Modifier
                        .pointerInput(slot, layout) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { start(slot) },
                                onDrag = { change, amount -> change.consume(); drag(amount.y) },
                                onDragEnd = ::finish,
                                onDragCancel = ::finish,
                            )
                        }
                    Row(
                        modifier = Modifier
                            .zIndex(if (lifted) 1f else 0f)
                            .graphicsLayer { translationY = if (lifted) offset else 0f }
                            .fillMaxWidth()
                            .height(RowHeight)
                            .background(if (lifted) colors.surfaceAlt else colors.surface)
                            .then(gestures)
                            .then(
                                if (slot == null) Modifier
                                else Modifier.clickable {
                                    slots = slots.toggled(slot)
                                    onChange(slots.toLayout(layout.hidden))
                                }
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (slot == null) {
                            Box(Modifier.weight(1f).height(1.dp).background(colors.hairline))
                            Icon(GalleryIcons.More, contentDescription = null, tint = colors.textFaint, modifier = Modifier.size(14.dp))
                            Text(stringResource(R.string.actions_in_menu), style = GalleryTheme.type.meta, color = colors.textFaint)
                            Box(Modifier.weight(1f).height(1.dp).background(colors.hairline))
                        } else {
                            val on = slot !in layout.hidden
                            CheckBox(
                                checked = on,
                                modifier = Modifier.clickable { onChange(slots.toLayout(layout.hidden).shown(slot, !on)) },
                            )
                            Icon(slot.icon, contentDescription = null, tint = if (on) colors.text else colors.textFaint, modifier = Modifier.size(18.dp))
                            Text(
                                stringResource(slot.labelRes),
                                style = GalleryTheme.type.body,
                                color = if (on) colors.text else colors.textFaint,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Icon(
                            GalleryIcons.Grip,
                            contentDescription = null,
                            tint = colors.textFaint,
                            modifier = Modifier
                                .size(20.dp)
                                .pointerInput(slot, layout) {
                                    detectDragGestures(
                                        onDragStart = { start(slot) },
                                        onDrag = { change, amount -> change.consume(); drag(amount.y) },
                                        onDragEnd = ::finish,
                                        onDragCancel = ::finish,
                                    )
                                },
                        )
                    }
                }
            }
        }
    }
}
