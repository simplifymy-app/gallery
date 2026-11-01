package app.simplifymy.gallery.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.model.Grouping
import app.simplifymy.gallery.core.model.Sorting
import app.simplifymy.gallery.core.settings.AppIconManager
import app.simplifymy.gallery.core.settings.GallerySettings
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.model.Folder
import app.simplifymy.gallery.core.model.MediaFilter
import app.simplifymy.gallery.core.model.ScrollDirection
import app.simplifymy.gallery.core.settings.Language
import app.simplifymy.gallery.core.settings.SettingsRepository
import app.simplifymy.gallery.ui.theme.AppIcon
import app.simplifymy.gallery.ui.theme.Contrast
import app.simplifymy.gallery.ui.theme.CornerStyle
import app.simplifymy.gallery.ui.theme.DetailsPlacement
import app.simplifymy.gallery.ui.theme.ThumbnailSpacing
import app.simplifymy.gallery.ui.theme.GalleryFont
import app.simplifymy.gallery.ui.theme.TextScale
import app.simplifymy.gallery.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val appIcons: AppIconManager,
    media: MediaRepository,
) : ViewModel() {

    val previewFolders: StateFlow<List<Folder>> = media.folders(MediaFilter.All)
        .map { it.take(6) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<GallerySettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GallerySettings())

    fun setThemeMode(value: ThemeMode) = update { repository.setThemeMode(value) }
    fun setAccent(argb: Int) = update { repository.setAccent(argb) }
    fun setContrast(value: Contrast) = update { repository.setContrast(value) }
    fun setFont(value: GalleryFont) = update { repository.setFont(value) }
    fun setTextScale(value: TextScale) = update { repository.setTextScale(value) }
    fun setCorners(value: CornerStyle) = update { repository.setCorners(value) }

    fun setAppIcon(value: AppIcon) = update {
        repository.setAppIcon(value)
        appIcons.request(value)
    }

    fun setGrouping(value: Grouping) = update { repository.setGrouping(value) }
    fun setSorting(value: Sorting) = update { repository.setSorting(value) }
    fun setFolderColumns(value: Int) = update { repository.setFolderColumns(value) }
    fun setMediaColumns(value: Int) = update { repository.setMediaColumns(value) }
    fun setScrollDirection(value: ScrollDirection) = update { repository.setScrollDirection(value) }
    fun setOverlayUi(value: Boolean) = update { repository.setOverlayUi(value) }
    fun setCropThumbnails(value: Boolean) = update { repository.setCropThumbnails(value) }
    fun setShowFolderNames(value: Boolean) = update { repository.setShowFolderNames(value) }
    fun setShowFolderDetails(value: Boolean) = update { repository.setShowFolderDetails(value) }
    fun setShowRecent(value: Boolean) = update { repository.setShowRecent(value) }
    fun setDetailsPlacement(value: DetailsPlacement) = update { repository.setDetailsPlacement(value) }
    fun setThumbnailSpacing(value: ThumbnailSpacing) = update { repository.setThumbnailSpacing(value) }
    fun setShowVideoDuration(value: Boolean) = update { repository.setShowVideoDuration(value) }
    fun setAutoplayNextVideo(value: Boolean) = update { repository.setAutoplayNextVideo(value) }
    fun setBackgroundPlayback(value: Boolean) = update { repository.setBackgroundPlayback(value) }
    fun setCopyPathAction(value: Boolean) = update { repository.setCopyPathAction(value) }
    fun setLanguage(value: Language, onApplied: () -> Unit) = viewModelScope.launch {
        repository.setLanguage(value)
        onApplied()
    }

    private fun update(block: suspend () -> Unit) = viewModelScope.launch { block() }
}
