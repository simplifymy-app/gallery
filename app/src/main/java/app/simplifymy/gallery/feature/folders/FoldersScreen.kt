package app.simplifymy.gallery.feature.folders

import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.gallery.core.model.Folder
import app.simplifymy.gallery.core.model.MediaFilter
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.component.MediaThumbnail
import app.simplifymy.gallery.ui.component.MenuEntry
import app.simplifymy.gallery.ui.component.MenuStepper
import app.simplifymy.gallery.ui.component.OverflowMenu
import app.simplifymy.gallery.ui.component.island
import app.simplifymy.gallery.ui.component.islandGap
import app.simplifymy.gallery.ui.component.shadowed
import app.simplifymy.gallery.ui.formatCount
import app.simplifymy.gallery.ui.formatSize
import app.simplifymy.gallery.ui.theme.DetailsPlacement
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing

private val COLUMN_RANGE = 2..6

@Composable
fun FoldersScreen(
    onOpenFolder: (Folder) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: FoldersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors
    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { viewModel.clearSelection() }
    val labelled = state.settings.showFolderNames
    val below = state.settings.detailsPlacement == DetailsPlacement.Below
    val gap = state.settings.thumbnailSpacing.gap
    val captioned = labelled || state.settings.showFolderDetails

    val overlay = state.settings.overlayUi
    var barHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    val bar: @Composable () -> Unit = {
        if (state.selecting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = Spacing.screen, top = islandGap(overlay), bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(islandGap(overlay)),
            ) {
                IconAction(GalleryIcons.Close, onClick = viewModel::clearSelection, modifier = Modifier.island(overlay))
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(
                        stringResource(R.string.selection_count, state.selected.size),
                        style = GalleryTheme.type.title.shadowed(overlay),
                        color = colors.accent,
                    )
                    Text(
                        stringResource(R.string.selection_hint_long_press),
                        style = GalleryTheme.type.meta.shadowed(overlay),
                        color = colors.textMuted,
                    )
                }
                Text(
                    stringResource(R.string.action_select_all),
                    style = GalleryTheme.type.label,
                    color = colors.text,
                    modifier = Modifier.island(overlay).clickable(onClick = viewModel::selectAll).padding(8.dp),
                )
            }
        } else Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.screen, end = 14.dp, top = islandGap(overlay), bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(islandGap(overlay)),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = GalleryTheme.type.display.shadowed(overlay),
                color = colors.text,
            )
            Spacer(Modifier.weight(1f))
            IconAction(
                GalleryIcons.Search,
                onClick = onOpenSearch,
                modifier = Modifier.island(overlay),
                label = stringResource(R.string.action_search),
            )
            OverflowMenu(Modifier.island(overlay)) { dismiss ->
                MenuEntry(stringResource(R.string.menu_sort_by), value = stringResource(state.settings.sorting.key.labelRes)) { viewModel.cycleSorting() }
                MenuEntry(stringResource(R.string.menu_show), value = stringResource(state.settings.mediaFilter.labelRes)) { viewModel.cycleFilter() }
                MenuStepper(
                    label = stringResource(R.string.menu_columns),
                    value = state.settings.folderColumns,
                    range = COLUMN_RANGE,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    onChange = viewModel::setColumns,
                )
                MenuEntry(stringResource(R.string.menu_settings)) { dismiss(); onOpenSettings() }
            }
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
                columns = GridCells.Fixed(state.settings.folderColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = gap, end = gap, top = if (overlay) barHeight else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(if (captioned && below) gap + 14.dp else gap),
            ) {
                items(state.folders, key = { it.bucketId }) { folder ->
                    FolderCard(
                        folder = folder,
                        selected = folder.bucketId in state.selected,
                        selecting = state.selecting,
                        onLongClick = { viewModel.toggle(folder.bucketId) },
                        crop = state.settings.cropThumbnails,
                        showName = labelled,
                        showDetails = state.settings.showFolderDetails,
                        below = below,
                        compact = state.settings.folderColumns >= 4,
                        onClick = {
                            if (state.selecting) viewModel.toggle(folder.bucketId) else onOpenFolder(folder)
                        },
                    )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(vertical = Spacing.base),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconAction(GalleryIcons.Hide, onClick = viewModel::ignoreSelected, label = stringResource(R.string.action_ignore))
                IconAction(GalleryIcons.Trash, label = stringResource(R.string.action_delete), onClick = {
                    viewModel.trashSelected { sender ->
                        trashLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                })
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: Folder,
    selected: Boolean,
    selecting: Boolean,
    onLongClick: () -> Unit,
    crop: Boolean,
    showName: Boolean,
    showDetails: Boolean,
    below: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val colors = GalleryTheme.colors
    val captioned = showName || showDetails

    if (captioned && !below) {
        Box(Modifier.selectable(selected, onClick, onLongClick)) {
            MediaThumbnail(model = folder.coverUri, crop = crop, modifier = Modifier.fillMaxWidth())
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xC7000000)))
                    )
                    .padding(horizontal = 6.dp, vertical = 5.dp),
            ) {
                Caption(folder, showName, showDetails, compact, Color(0xFFEDEDEF), Color(0xB3EDEDEF))
            }
        }
        return
    }

    Column(
        modifier = Modifier.selectable(selected, onClick, onLongClick),
        verticalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        MediaThumbnail(model = folder.coverUri, crop = crop, modifier = Modifier.fillMaxWidth())
        if (captioned) {
            Caption(folder, showName, showDetails, compact, colors.text, colors.textMuted)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.selectable(selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit): Modifier {
    val accent = GalleryTheme.colors.accent
    return this
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .then(if (selected) Modifier.border(2.dp, accent, RoundedCornerShape(4.dp)).padding(3.dp) else Modifier)
}

@Composable
private fun Caption(
    folder: Folder,
    showName: Boolean,
    showDetails: Boolean,
    compact: Boolean,
    primary: Color,
    secondary: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (showName) {
            Text(
                folder.name,
                style = if (compact) GalleryTheme.type.meta else GalleryTheme.type.label,
                color = primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDetails) {
            Text(
                "${formatCount(folder.itemCount)} · ${formatSize(folder.size)}",
                style = GalleryTheme.type.meta,
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
