package app.simplifymy.gallery.feature.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.provider.MediaStore
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.data.MediaWriter
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.navigation.Editor
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class EditorUiState(
    val medium: Medium? = null,
    val source: Bitmap? = null,
    val tab: EditorTab = EditorTab.Crop,
    val edits: PhotoEdits = PhotoEdits(),
    val ink: Int = INKS.first(),
    val nib: Float = NIBS[1],
    val saving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val writer: MediaWriter,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Editor>()
    private var original: Bitmap? = null
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val medium = repository.medium(route.mediumId) ?: return@launch
            _state.update { it.copy(medium = medium) }
            val bitmap = load(medium)
            original = bitmap
            _state.update { state ->
                val crop = bitmap?.let { centredCrop(it.width, it.height, state.edits.ratio.ratio) }
                state.copy(source = bitmap, edits = state.edits.copy(crop = crop ?: state.edits.crop))
            }
        }
    }

    private suspend fun load(medium: Medium): Bitmap? = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(medium.uri)
            .size(minOf(MAX_EDGE_PX, sourceEdge(medium)))
            .allowHardware(false)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
    }

    private fun sourceEdge(medium: Medium): Int = context.contentResolver
        .query(
            medium.uri,
            arrayOf(MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT),
            null,
            null,
            null,
        )
        ?.use { if (it.moveToFirst()) maxOf(it.getInt(0), it.getInt(1)) else 0 }
        ?.takeIf { it > 0 }
        ?: MAX_EDGE_PX

    fun pickTab(tab: EditorTab) = _state.update { it.copy(tab = tab) }

    fun pickRatio(ratio: CropRatio) = _state.update { state ->
        val source = state.source ?: return@update state.copy(edits = state.edits.copy(ratio = ratio))
        state.copy(
            edits = state.edits.copy(
                ratio = ratio,
                crop = centredCrop(source.width, source.height, ratio.ratio),
            )
        )
    }

    fun setCrop(crop: Rect) = edit { it.copy(crop = crop) }
    fun pickFilter(filter: PhotoFilter) = edit { it.copy(filter = filter) }
    fun pickInk(color: Int) = _state.update { it.copy(ink = color) }
    fun pickNib(width: Float) = _state.update { it.copy(nib = width) }

    fun stepBrightness(delta: Int) = edit { it.copy(brightness = it.brightness.stepped(delta)) }
    fun stepContrast(delta: Int) = edit { it.copy(contrast = it.contrast.stepped(delta)) }
    fun stepSaturation(delta: Int) = edit { it.copy(saturation = it.saturation.stepped(delta)) }

    fun addStroke(stroke: Stroke) = edit { it.copy(strokes = it.strokes + stroke) }

    fun rotate(clockwise: Boolean) = _state.update { state ->
        val bitmap = state.source ?: return@update state
        val shape = bitmap.width.toFloat() / bitmap.height
        val turned = state.edits.turned(clockwise, shape)
        val rotated = bitmap.transformed(if (clockwise) 90f else -90f, mirror = false)
        state.copy(
            source = rotated,
            edits = if (state.edits.ratio.ratio == null) turned else turned.copy(
                crop = centredCrop(rotated.width, rotated.height, state.edits.ratio.ratio)
            ),
        )
    }

    fun mirror() = _state.update { state ->
        val bitmap = state.source ?: return@update state
        state.copy(source = bitmap.transformed(0f, mirror = true), edits = state.edits.mirrored())
    }

    fun reset() = _state.update { it.copy(source = original ?: it.source, edits = PhotoEdits()) }

    fun save() {
        val current = _state.value
        val medium = current.medium ?: return
        val source = current.source ?: return
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) { render(source, current.edits) }
            val saved = writer.saveCopy(medium, bitmap) != null
            bitmap.recycle()
            _state.update { it.copy(saving = false, saved = saved) }
        }
    }

    private fun Bitmap.transformed(degrees: Float, mirror: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (mirror) postScale(-1f, 1f)
            if (degrees != 0f) postRotate(degrees)
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun edit(block: (PhotoEdits) -> PhotoEdits) =
        _state.update { it.copy(edits = block(it.edits)) }

    private fun Int.stepped(delta: Int) =
        (this + delta).coerceIn(SLIDER_RANGE.first, SLIDER_RANGE.last)

    private companion object {
        const val MAX_EDGE_PX = 2048
    }
}
