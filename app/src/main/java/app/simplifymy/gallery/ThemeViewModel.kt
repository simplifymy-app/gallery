package app.simplifymy.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.settings.GallerySettings
import app.simplifymy.gallery.core.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val settings: StateFlow<GallerySettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, GallerySettings())
}
