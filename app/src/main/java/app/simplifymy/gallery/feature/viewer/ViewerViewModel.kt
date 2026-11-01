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
import app.simplifymy.gallery.core.media.MetadataEdit
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.core.model.sortedBy
import app.simplifymy.gallery.core.settings.SettingsRepository
import app.simplifymy.gallery.navigation.Viewer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val media: List<Medium> = emptyList(),
    val startIndex: Int = 0,
    val details: MediaDetails = MediaDetails(),
    val autoplayNextVideo: Boolean = false,
    val backgroundPlayback: Boolean = false,
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

    private val library = settings.settings.flatMapLatest { config ->
        repository.mediaIn(route.bucketId, config.mediaFilter).map { media ->
            val sorted = media.sortedBy(config.sorting)
            ViewerUiState(
                media = sorted,
                startIndex = sorted.indexOfFirst { it.id == route.mediumId }.coerceAtLeast(0),
                autoplayNextVideo = config.autoplayNextVideo,
                backgroundPlayback = config.backgroundPlayback,
            )
        }
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<ViewerUiState> =
        combine(library, details) { viewer, info -> viewer.copy(details = info) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewerUiState())

    fun loadDetails(medium: Medium) = viewModelScope.launch {
        details.value = detailsLoader.load(medium.id, medium.type)
    }

    fun toggleFavorite(medium: Medium) = viewModelScope.launch {
        repository.setFavorite(medium, !medium.isFavorite)
    }

    fun writeRequest(medium: Medium): IntentSender = writer.writeRequest(listOf(medium))

    fun editMetadata(medium: Medium, edit: MetadataEdit, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val saved = repository.editMetadata(medium, edit)
        if (saved) details.value = detailsLoader.load(medium.id, medium.type)
        onDone(saved)
    }

    fun trash(medium: Medium): IntentSender = writer.trashRequest(listOf(medium), trashed = true)
}
