package app.simplifymy.gallery.feature.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.data.MediaWriter
import app.simplifymy.gallery.core.media.MediaPaths
import app.simplifymy.gallery.core.model.GridItem
import app.simplifymy.gallery.core.model.Grouping
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.core.model.RECENT_BUCKET_ID
import app.simplifymy.gallery.core.model.ScrollDirection
import app.simplifymy.gallery.core.model.grouped
import app.simplifymy.gallery.core.model.shellQuoted
import app.simplifymy.gallery.core.model.sortedBy
import app.simplifymy.gallery.core.settings.GallerySettings
import app.simplifymy.gallery.core.settings.SettingsRepository
import app.simplifymy.gallery.core.settings.cycled
import app.simplifymy.gallery.navigation.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.navigation.toRoute
import javax.inject.Inject

data class MediaUiState(
    val title: String = "",
    val items: List<GridItem> = emptyList(),
    val totalCount: Int = 0,
    val totalSize: Long = 0,
    val selected: Set<Long> = emptySet(),
    val settings: GallerySettings = GallerySettings(),
    val recent: Boolean = false,
    val refreshing: Boolean = false,
) {
    val selecting: Boolean get() = selected.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MediaViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val settings: SettingsRepository,
    private val writer: MediaWriter,
    private val paths: MediaPaths,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Media>()
    private val selected = MutableStateFlow(emptySet<Long>())
    private val refreshing = MutableStateFlow(false)
    private var loaded = emptyList<Medium>()
    private var positions = emptyMap<Long, Int>()
    private var rangeBase = emptySet<Long>()

    private val arranged = settings.settings.flatMapLatest { config ->
        repository.mediaIn(route.bucketId, config.mediaFilter).map { media -> media.arranged(config) }
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<MediaUiState> =
        combine(arranged, selected, refreshing) { library, picked, busy ->
            loaded = library.sorted
            positions = library.positions
            MediaUiState(
                title = route.title,
                items = library.items,
                totalCount = library.sorted.size,
                totalSize = library.totalSize,
                selected = picked,
                settings = library.settings,
                recent = route.bucketId == RECENT_BUCKET_ID,
                refreshing = busy,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaUiState(title = route.title))

    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try {
                repository.refresh(route.bucketId.takeUnless { it == RECENT_BUCKET_ID }?.let(::setOf))
            } finally {
                refreshing.value = false
            }
        }
    }

    fun toggle(id: Long) {
        selected.value = selected.value.let { if (id in it) it - id else it + id }
    }

    fun beginRange() { rangeBase = selected.value }

    fun selectRange(from: Long, to: Long) {
        val start = positions[from] ?: return
        val end = positions[to] ?: return
        selected.value = rangeBase + loaded.subList(minOf(start, end), maxOf(start, end) + 1).map(Medium::id)
    }

    fun selectAll() { selected.value = loaded.map(Medium::id).toSet() }

    fun clearSelection() { selected.value = emptySet() }

    fun trashSelected(): android.content.IntentSender =
        writer.trashRequest(loaded.filter { it.id in selected.value }, trashed = true)

    fun cycleGrouping() = viewModelScope.launch {
        val next = Grouping.entries[(settings.settings.first().grouping.ordinal + 1) % Grouping.entries.size]
        settings.setGrouping(next)
    }

    fun cycleSorting() = viewModelScope.launch {
        settings.setSorting(settings.settings.first().sorting.cycled())
    }

    fun setColumns(count: Int) = viewModelScope.launch { settings.setMediaColumns(count) }

    fun selectedMedia(): List<Medium> = loaded.filter { it.id in selected.value }

    fun copySelectedPaths(copy: (String) -> Unit) = viewModelScope.launch {
        val found = paths.absolute(selectedMedia())
        if (found.isNotEmpty()) copy(shellQuoted(found))
        clearSelection()
    }

    fun ignoreFolder() = viewModelScope.launch {
        repository.ignoreFolder(route.bucketId, route.title)
    }

    fun toggleFavorite() = viewModelScope.launch {
        val picked = loaded.filter { it.id in selected.value }
        val makeFavorite = picked.any { !it.isFavorite }
        picked.forEach { repository.setFavorite(it, makeFavorite) }
        clearSelection()
    }
}

private class ArrangedMedia(
    val sorted: List<Medium>,
    val items: List<GridItem>,
    val positions: Map<Long, Int>,
    val totalSize: Long,
    val settings: GallerySettings,
)

private fun List<Medium>.arranged(config: GallerySettings): ArrangedMedia {
    val order = config.sorting.copy(
        descending = config.sorting.descending != (config.scrollDirection == ScrollDirection.Up)
    )
    val items = sortedBy(order).grouped(config.grouping, order)
    val shown = items.mapNotNull { (it as? GridItem.Item)?.medium }
    return ArrangedMedia(
        sorted = shown,
        items = items,
        positions = HashMap<Long, Int>(shown.size * 2).apply { shown.forEachIndexed { index, medium -> put(medium.id, index) } },
        totalSize = sumOf { it.size },
        settings = config,
    )
}
