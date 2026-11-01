package app.simplifymy.gallery.feature.media

import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
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
import app.simplifymy.gallery.feature.files.FileAction
import app.simplifymy.gallery.feature.files.FileActions
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
import kotlinx.coroutines.delay

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
    var fileAction by remember { mutableStateOf<FileAction?>(null) }
    var dragAnchor by remember { mutableStateOf<Long?>(null) }
    var dragPoint by remember { mutableStateOf(Offset.Zero) }
    var autoScroll by remember { mutableFloatStateOf(0f) }
    var slid by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val openOrToggle: (Medium) -> Unit = { if (state.selecting) viewModel.toggle(it.id) else onOpenMedium(it) }
    val currentTap by rememberUpdatedState<(Long) -> Unit> { id ->
        state.items.firstNotNullOfOrNull { (it as? GridItem.Item)?.medium?.takeIf { m -> m.id == id } }?.let(openOrToggle)
    }

    LaunchedEffect(autoScroll) {
        while (autoScroll != 0f) {
            gridState.scrollBy(autoScroll)
            val anchor = dragAnchor ?: break
            if (slid) gridState.mediumAt(dragPoint)?.let { viewModel.selectRange(anchor, it) }
            delay(16)
        }
    }

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
                        if (!state.recent) {
                            MenuEntry(stringResource(R.string.menu_ignore_folder)) { dismiss(); viewModel.ignoreFolder(); onBack() }
                        }
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
                modifier = Modifier.fillMaxSize().gridGestures(
                    grid = gridState,
                    onTap = { id -> currentTap(id) },
                    onLongPress = { id ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggle(id)
                        viewModel.beginRange()
                        dragAnchor = id
                    },
                    onSlide = { point, height ->
                        dragPoint = point
                        dragAnchor?.let { anchor -> gridState.mediumAt(point)?.let { if (it != anchor) slid = true } }
                        if (slid) dragAnchor?.let { anchor -> gridState.mediumAt(point)?.let { viewModel.selectRange(anchor, it) } }
                        val edge = with(density) { AUTO_SCROLL_EDGE.toPx() }
                        autoScroll = when {
                            point.y < edge -> -(edge - point.y) / edge * AUTO_SCROLL_SPEED
                            point.y > height - edge -> (point.y - height + edge) / edge * AUTO_SCROLL_SPEED
                            else -> 0f
                        }
                    },
                    onSlideEnd = {
                        dragAnchor = null
                        slid = false
                        autoScroll = 0f
                    },
                ),
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
                                onTap = { openOrToggle(item.medium) },
                                onHold = { viewModel.toggle(item.medium.id) },
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
                onCopy = { fileAction = FileAction.Copy },
                onMove = { fileAction = FileAction.Move },
                onRename = if (state.selected.size == 1) ({ fileAction = FileAction.Rename }) else null,
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

    FileActions(
        action = fileAction,
        media = if (fileAction == null) emptyList() else viewModel.selectedMedia(),
        onDone = {
            fileAction = null
            viewModel.clearSelection()
        },
        onDismiss = { fileAction = null },
    )
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
private fun ActionBar(
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: () -> Unit,
) {
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
        IconAction(GalleryIcons.Duplicate, onClick = onCopy, label = stringResource(R.string.action_copy))
        IconAction(GalleryIcons.Trash, onClick = onDelete, label = stringResource(R.string.action_delete))
        OverflowMenu { dismiss ->
            MenuEntry(stringResource(R.string.action_move_to)) { dismiss(); onMove() }
            if (onRename != null) MenuEntry(stringResource(R.string.action_rename)) { dismiss(); onRename() }
        }
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

@Composable
private fun MediaTile(
    medium: Medium,
    selected: Boolean,
    selecting: Boolean,
    showDuration: Boolean,
    onTap: () -> Unit,
    onHold: () -> Unit,
) {
    val colors = GalleryTheme.colors
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(if (selected) colors.accent else Color.Transparent, RoundedCornerShape(4.dp))
            .semantics {
                onClick { onTap(); true }
                onLongClick { onHold(); true }
            },
    ) {
        MediaThumbnail(
            model = medium.uri,
            description = medium.name,
            modifier = Modifier.fillMaxSize().then(if (selected) Modifier.padding(SELECTION_BORDER) else Modifier),
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

private val SELECTION_BORDER = 3.dp
private val COLUMN_RANGE = 2..6
private val AUTO_SCROLL_EDGE = 64.dp
private const val AUTO_SCROLL_SPEED = 24f

private fun Modifier.gridGestures(
    grid: LazyGridState,
    onTap: (Long) -> Unit,
    onLongPress: (Long) -> Unit,
    onSlide: (point: Offset, height: Int) -> Unit,
    onSlideEnd: () -> Unit,
) = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val id = grid.mediumAt(down.position) ?: return@awaitEachGesture
        var held = true
        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                held = false
                if (change == null || change.isConsumed) break
                if (!change.pressed) { onTap(id); break }
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                held = true
            }
        }
        if (!held) return@awaitEachGesture
        onLongPress(id)
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
            change.consume()
            if (!change.pressed) break
            onSlide(change.position, size.height)
        }
        onSlideEnd()
    }
}

private fun LazyGridState.mediumAt(point: Offset): Long? =
    layoutInfo.visibleItemsInfo.firstOrNull { IntRect(it.offset, it.size).contains(point.round()) }?.key as? Long

private fun shareIntent(media: List<Medium>) = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
    type = if (media.all { it.type == MediaType.Video }) "video/*" else "*/*"
    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(media.map(Medium::uri)))
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
