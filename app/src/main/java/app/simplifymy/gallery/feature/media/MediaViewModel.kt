package app.simplifymy.gallery.feature.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.data.MediaWriter
import app.simplifymy.gallery.core.model.GridItem
import app.simplifymy.gallery.core.model.Grouping
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.core.model.grouped
import app.simplifymy.gallery.core.model.sortedBy
import app.simplifymy.gallery.core.settings.GallerySettings
import app.simplifymy.gallery.core.settings.SettingsRepository
import app.simplifymy.gallery.core.settings.cycled
import app.simplifymy.gallery.navigation.Media
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
) {
    val selecting: Boolean get() = selected.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MediaViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val settings: SettingsRepository,
    private val writer: MediaWriter,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Media>()
    private val selected = MutableStateFlow(emptySet<Long>())
    private var loaded = emptyList<Medium>()

    val state: StateFlow<MediaUiState> =
        combine(settings.settings.flatMapLatest { repository.mediaIn(route.bucketId, it.mediaFilter) }, settings.settings, selected) { media, config, picked ->
            loaded = media
            val sorted = media.sortedBy(config.sorting)
            MediaUiState(
                title = route.title,
                items = sorted.grouped(config.grouping, config.sorting),
                totalCount = media.size,
                totalSize = media.sumOf { it.size },
                selected = picked,
                settings = config,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaUiState(title = route.title))

    fun toggle(id: Long) {
        selected.value = selected.value.let { if (id in it) it - id else it + id }
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
