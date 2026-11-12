package app.simplifymy.gallery.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.core.settings.GallerySettings
import app.simplifymy.gallery.core.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: MediaRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<Medium>> = _query
        .debounce(200)
        .flatMapLatest { if (it.isBlank()) flowOf(emptyList()) else repository.search(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val columns: StateFlow<Int> = settings.settings
        .map { it.mediaColumns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GallerySettings().mediaColumns)

    fun setQuery(value: String) { _query.value = value }
}
