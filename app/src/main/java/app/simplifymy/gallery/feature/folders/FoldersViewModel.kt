package app.simplifymy.gallery.feature.folders

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.data.MediaWriter
import app.simplifymy.gallery.core.model.Folder
import app.simplifymy.gallery.core.model.MediaFilter
import app.simplifymy.gallery.core.model.RECENT_BUCKET_ID
import app.simplifymy.gallery.core.model.recent
import app.simplifymy.gallery.core.model.sortedBy
import app.simplifymy.gallery.core.settings.GallerySettings
import app.simplifymy.gallery.core.settings.SettingsRepository
import app.simplifymy.gallery.core.settings.cycled
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoldersUiState(
    val folders: List<Folder> = emptyList(),
    val settings: GallerySettings = GallerySettings(),
    val selected: Set<Long> = emptySet(),
    val refreshing: Boolean = false,
) {
    val selecting: Boolean get() = selected.isNotEmpty()

    val selectionPinned: Boolean get() = folders.filter { it.bucketId in selected }.all(Folder::isPinned)
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val writer: MediaWriter,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val selected = MutableStateFlow(emptySet<Long>())
    private val refreshing = MutableStateFlow(false)
    private var loaded = emptyList<Folder>()

    val state: StateFlow<FoldersUiState> =
        combine(
            settings.settings.flatMapLatest { repository.folders(it.mediaFilter) },
            settings.settings,
            selected,
            refreshing,
        ) { folders, config, picked, busy ->
            val sorted = folders.sortedBy(config.sorting).sortedByDescending(Folder::isPinned)
            loaded = sorted
            val shown = listOfNotNull(folders.recent().takeIf { config.showRecent }) + sorted
            FoldersUiState(shown, config, picked.intersect(sorted.map(Folder::bucketId).toSet()), busy)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoldersUiState())

    init {
        sync()
        viewModelScope.launch { repository.changes.debounce(500).collect { repository.sync() } }
    }

    fun sync() = viewModelScope.launch { repository.sync() }

    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try { repository.refresh(null) } finally { refreshing.value = false }
        }
    }

    fun toggle(bucketId: Long) {
        if (bucketId == RECENT_BUCKET_ID) return
        selected.value = selected.value.let { if (bucketId in it) it - bucketId else it + bucketId }
    }

    fun selectAll() { selected.value = loaded.map(Folder::bucketId).toSet() }

    fun clearSelection() { selected.value = emptySet() }

    fun ignoreSelected() = viewModelScope.launch {
        repository.ignoreFolders(loaded.filter { it.bucketId in selected.value })
        clearSelection()
    }

    fun pinSelected() = viewModelScope.launch {
        repository.setPinned(selected.value.toList(), pin = !state.value.selectionPinned)
        clearSelection()
    }

    fun trashSelected(onReady: (IntentSender) -> Unit) = viewModelScope.launch {
        val media = repository.mediaForFolders(selected.value.toList())
        if (media.isNotEmpty()) onReady(writer.trashRequest(media, trashed = true))
    }

    fun setColumns(count: Int) = viewModelScope.launch { settings.setFolderColumns(count) }

    fun cycleSorting() = viewModelScope.launch {
        settings.setSorting(settings.settings.first().sorting.cycled())
    }

    fun cycleFilter() = viewModelScope.launch {
        val current = settings.settings.first().mediaFilter
        settings.setMediaFilter(MediaFilter.entries[(current.ordinal + 1) % MediaFilter.entries.size])
    }
}
