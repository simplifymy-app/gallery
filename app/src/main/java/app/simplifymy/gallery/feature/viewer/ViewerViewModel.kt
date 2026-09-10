package app.simplifymy.gallery.feature.viewer

import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.data.MediaWriter
import app.simplifymy.gallery.core.media.MediaDetails
import app.simplifymy.gallery.core.media.MediaDetailsLoader
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.core.model.sortedBy
import app.simplifymy.gallery.core.settings.SettingsRepository
import app.simplifymy.gallery.navigation.Viewer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val media: List<Medium> = emptyList(),
    val startIndex: Int = 0,
    val details: MediaDetails = MediaDetails(),
    val autoplayNextVideo: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val writer: MediaWriter,
    private val detailsLoader: MediaDetailsLoader,
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Viewer>()
    private val details = MutableStateFlow(MediaDetails())

    val state: StateFlow<ViewerUiState> =
        combine(settings.settings.flatMapLatest { repository.mediaIn(route.bucketId, it.mediaFilter) }, settings.settings, details) { media, config, info ->
            val sorted = media.sortedBy(config.sorting)
            val index = sorted.indexOfFirst { it.id == route.mediumId }.coerceAtLeast(0)
            ViewerUiState(sorted, index, info, config.autoplayNextVideo)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewerUiState())

    fun loadDetails(medium: Medium) = viewModelScope.launch {
        details.value = detailsLoader.load(medium.id, medium.type)
    }

    fun toggleFavorite(medium: Medium) = viewModelScope.launch {
        repository.setFavorite(medium, !medium.isFavorite)
    }

    fun trash(medium: Medium): IntentSender = writer.trashRequest(listOf(medium), trashed = true)
}
