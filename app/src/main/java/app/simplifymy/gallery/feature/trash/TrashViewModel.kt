package app.simplifymy.gallery.feature.trash

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.data.MediaWriter
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.core.settings.GallerySettings
import app.simplifymy.gallery.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TrashUiState(
    val items: List<Medium> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val settings: GallerySettings = GallerySettings(),
) {
    val selecting: Boolean get() = selected.isNotEmpty()
}

@HiltViewModel
class TrashViewModel @Inject constructor(
    repository: MediaRepository,
    settings: SettingsRepository,
    private val writer: MediaWriter,
) : ViewModel() {

    private val selected = MutableStateFlow(emptySet<Long>())

    val state: StateFlow<TrashUiState> =
        combine(repository.trash, selected, settings.settings) { items, picked, config ->
            TrashUiState(items, picked.intersect(items.map(Medium::id).toSet()), config)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrashUiState())

    fun toggle(id: Long) {
        selected.value = selected.value.let { if (id in it) it - id else it + id }
    }

    fun selectAll() { selected.value = state.value.items.map(Medium::id).toSet() }

    fun clearSelection() { selected.value = emptySet() }

    fun restoreSelected(): IntentSender = writer.trashRequest(selectedMedia(), trashed = false)

    fun deleteSelected(): IntentSender = writer.deleteRequest(selectedMedia())

    private fun selectedMedia(): List<Medium> = state.value.items.filter { it.id in selected.value }
}
