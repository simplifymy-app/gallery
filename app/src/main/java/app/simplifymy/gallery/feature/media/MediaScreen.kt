package app.simplifymy.gallery.feature.media

import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalConfiguration
import app.simplifymy.gallery.core.model.GridItem
import app.simplifymy.gallery.core.model.HeaderKind
import java.time.format.DateTimeFormatter
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.core.model.ScrollDirection
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.component.MenuEntry
import app.simplifymy.gallery.ui.component.MenuStepper
import app.simplifymy.gallery.ui.component.OverflowMenu
import app.simplifymy.gallery.ui.component.MediaThumbnail
import app.simplifymy.gallery.ui.component.island
import app.simplifymy.gallery.ui.component.islandGap
import app.simplifymy.gallery.ui.component.shadowed
import app.simplifymy.gallery.ui.formatCount
import app.simplifymy.gallery.ui.formatDuration
import app.simplifymy.gallery.ui.formatSize
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing

@Composable
fun MediaScreen(
    onBack: () -> Unit,
    onOpenMedium: (Medium) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors
    val context = LocalContext.current

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { if (it.resultCode == Activity.RESULT_OK) viewModel.clearSelection() }

    val overlay = state.settings.overlayUi
    val gridState = rememberLazyGridState()
    var barHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    LaunchedEffect(state.settings.scrollDirection, state.items.isNotEmpty()) {
        if (state.settings.scrollDirection == ScrollDirection.Up && state.items.isNotEmpty()) {
            gridState.scrollToItem(state.items.lastIndex)
        }
    }

    val bar: @Composable () -> Unit = {
        if (state.selecting) {
            SelectionBar(
                count = state.selected.size,
                island = overlay,
                onClose = viewModel::clearSelection,
                onSelectAll = viewModel::selectAll,
            )
        } else {
            BrowseBar(
                title = state.title,
                subtitle = stringResource(
                    R.string.media_summary,
                    formatCount(state.totalCount),
                    formatSize(state.totalSize),
                ),
                island = overlay,
                onBack = onBack,
                menu = {
                    OverflowMenu(Modifier.island(overlay)) { dismiss ->
                        MenuEntry(stringResource(R.string.menu_group_by), value = stringResource(state.settings.grouping.labelRes)) {
                            viewModel.cycleGrouping()
                        }
                        MenuEntry(stringResource(R.string.menu_sort_by), value = stringResource(state.settings.sorting.key.labelRes)) {
                            viewModel.cycleSorting()
                        }
                        MenuStepper(
                            label = stringResource(R.string.menu_columns),
                            value = state.settings.mediaColumns,
                            range = COLUMN_RANGE,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            onChange = viewModel::setColumns,
                        )
                        MenuEntry(stringResource(R.string.menu_ignore_folder)) { dismiss(); viewModel.ignoreFolder(); onBack() }
                        MenuEntry(stringResource(R.string.menu_settings)) { dismiss(); onOpenSettings() }
                    }
                },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(
                if (overlay) WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                else WindowInsets.safeDrawing
            ),
    ) {
        if (!overlay) bar()

        Box(Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(state.settings.mediaColumns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = state.settings.thumbnailSpacing.gap,
                    end = state.settings.thumbnailSpacing.gap,
                    top = if (overlay) barHeight else 0.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(state.settings.thumbnailSpacing.gap),
                verticalArrangement = Arrangement.spacedBy(state.settings.thumbnailSpacing.gap),
            ) {
                state.items.forEach { item ->
                    when (item) {
                        is GridItem.Header -> item(span = { GridItemSpan(maxLineSpan) }, key = item.date) {
                            GroupHeader(item)
                        }
                        is GridItem.Item -> item(key = item.medium.id) {
                            MediaTile(
                                medium = item.medium,
                                selected = item.medium.id in state.selected,
                                selecting = state.selecting,
                                showDuration = state.settings.showVideoDuration,
                                onClick = {
                                    if (state.selecting) viewModel.toggle(item.medium.id)
                                    else onOpenMedium(item.medium)
                                },
                                onLongClick = { viewModel.toggle(item.medium.id) },
                            )
                        }
                    }
                }
            }

            if (overlay) {
                Box(
                    modifier = Modifier
                        .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
                ) { bar() }
            }
        }

        if (state.selecting) {
            ActionBar(
                onShare = {
                    val media = viewModel.selectedMedia()
                    if (media.isNotEmpty()) {
                        context.startActivity(Intent.createChooser(shareIntent(media), null))
                    }
                },
                onFavorite = viewModel::toggleFavorite,
                onDelete = {
                    trashLauncher.launch(
                        IntentSenderRequest.Builder(
                            viewModel.trashSelected()
                        ).build()
                    )
                },
            )
        }
    }
}

@Composable
private fun BrowseBar(
    title: String,
    subtitle: String,
    island: Boolean,
    onBack: () -> Unit,
    menu: @Composable () -> Unit,
) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 14.dp, top = islandGap(island), bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(islandGap(island)),
    ) {
        IconAction(GalleryIcons.Back, onClick = onBack, modifier = Modifier.island(island))
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                title,
                style = GalleryTheme.type.title.shadowed(island),
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(subtitle, style = GalleryTheme.type.meta.shadowed(island), color = colors.textMuted)
        }
        menu()
    }
}

@Composable
private fun SelectionBar(count: Int, island: Boolean, onClose: () -> Unit, onSelectAll: () -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = Spacing.screen, top = islandGap(island), bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(islandGap(island)),
    ) {
        IconAction(GalleryIcons.Back, onClick = onClose, modifier = Modifier.island(island))
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(stringResource(R.string.selection_count, count), style = GalleryTheme.type.title.shadowed(island), color = colors.accent)
            Text(stringResource(R.string.selection_hint_tap), style = GalleryTheme.type.meta.shadowed(island), color = colors.textMuted)
        }
        Text(
            stringResource(R.string.action_select_all),
            style = GalleryTheme.type.label,
            color = colors.text,
            modifier = Modifier.island(island).clickable(onClick = onSelectAll).padding(8.dp),
        )
    }
}

@Composable
private fun ActionBar(onShare: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(vertical = Spacing.base),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(GalleryIcons.Share, onClick = onShare, label = stringResource(R.string.action_share))
        IconAction(GalleryIcons.Heart, onClick = onFavorite, label = stringResource(R.string.action_favorite))
        IconAction(GalleryIcons.Trash, onClick = onDelete, label = stringResource(R.string.action_delete))
    }
}

@Composable
private fun GroupHeader(header: GridItem.Header) {
    val locale = LocalConfiguration.current.locales[0]
    val label = when (header.kind) {
        HeaderKind.Today -> stringResource(R.string.group_today)
        HeaderKind.Yesterday -> stringResource(R.string.group_yesterday)
        HeaderKind.Month -> header.date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        HeaderKind.Day -> header.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
    }
    Row(
        modifier = Modifier.padding(start = 13.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        Text(label, style = GalleryTheme.type.label, color = GalleryTheme.colors.text)
        Text(formatCount(header.count), style = GalleryTheme.type.meta, color = GalleryTheme.colors.textFaint)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaTile(
    medium: Medium,
    selected: Boolean,
    selecting: Boolean,
    showDuration: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = GalleryTheme.colors
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(if (selected) colors.accent else Color.Transparent, RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        MediaThumbnail(
            model = medium.uri,
            description = medium.name,
            modifier = Modifier.fillMaxSize().then(if (selected) Modifier.scale(0.86f) else Modifier),
        )
        if (medium.type == MediaType.Video && showDuration) {
            Text(
                formatDuration(medium.durationMs),
                style = GalleryTheme.type.meta,
                color = Color(0xFFEDEDEF),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .background(Color(0x990B0B0C), RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp),
            )
        }
        if (medium.isFavorite) {
            Icon(
                GalleryIcons.Heart,
                contentDescription = null,
                tint = Color(0xFFEDEDEF),
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(12.dp),
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(17.dp)
                    .background(colors.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(GalleryIcons.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(11.dp))
            }
        }
        if (selecting && !selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(Color(0x660B0B0C)),
            )
        }
    }
}

private val COLUMN_RANGE = 2..6

private fun shareIntent(media: List<Medium>) = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
    type = if (media.all { it.type == MediaType.Video }) "video/*" else "*/*"
    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(media.map(Medium::uri)))
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
