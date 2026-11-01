package app.simplifymy.gallery.feature.viewer

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.settings.SettingsRepository
import app.simplifymy.gallery.navigation.GalleryNavHost
import app.simplifymy.gallery.navigation.Viewer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpenedMedium(val uri: Uri, val isVideo: Boolean)

private val VIEW_ACTIONS = setOf(Intent.ACTION_VIEW, MediaStore.ACTION_REVIEW, "com.android.camera.action.REVIEW")

fun Intent.openedMedium(type: String?): OpenedMedium? {
    val uri = data ?: return null
    if (action !in VIEW_ACTIONS || type == null) return null
    if (!type.startsWith("image/") && !type.startsWith("video/")) return null
    return OpenedMedium(uri, isVideo = type.startsWith("video/"))
}

sealed interface OpenedState {
    data object Resolving : OpenedState
    data class InGallery(val route: Viewer) : OpenedState
    data class Standalone(val panoramaForAll: Boolean) : OpenedState
}

@HiltViewModel
class OpenedMediaViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<OpenedState>(OpenedState.Resolving)
    val state: StateFlow<OpenedState> = _state

    fun resolve(uri: Uri) {
        if (_state.value != OpenedState.Resolving) return
        viewModelScope.launch {
            val medium = runCatching { repository.locate(uri) }.getOrNull()
            val config = settings.settings.first()
            val browsable = medium != null &&
                repository.mediaIn(medium.bucketId, config.mediaFilter).first().any { it.id == medium.id }
            _state.value = if (browsable) OpenedState.InGallery(Viewer(medium.bucketId, medium.id))
            else OpenedState.Standalone(config.panoramaForAll)
        }
    }
}

@Composable
fun OpenedMediaScreen(
    opened: OpenedMedium,
    onClose: () -> Unit,
    viewModel: OpenedMediaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(opened.uri) { viewModel.resolve(opened.uri) }
    when (val current = state) {
        OpenedState.Resolving -> Box(Modifier.fillMaxSize().background(Color.Black))
        is OpenedState.InGallery -> GalleryNavHost(start = current.route, onExit = onClose)
        is OpenedState.Standalone -> StandaloneViewer(opened.uri, opened.isVideo, current.panoramaForAll, onBack = onClose)
    }
}
