package app.simplifymy.gallery.feature.editor

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.simplifymy.gallery.R
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.data.MediaWriter
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.navigation.Editor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class EffectTab(@StringRes val labelRes: Int, val kind: EffectKind?) {
    Leak(R.string.effect_leak, EffectKind.Leak),
    Dust(R.string.effect_dust, EffectKind.Dust),
    Scratches(R.string.effect_scratches, EffectKind.Scratches),
    Burn(R.string.effect_burn, EffectKind.Burn),
    Vhs(R.string.effect_vhs, EffectKind.Vhs),
    Date(R.string.effect_date, null),
}

enum class CameraTab(@StringRes val labelRes: Int) {
    Film(R.string.camera_film),
    Glare(R.string.camera_glare),
    Grain(R.string.editor_grain),
    Fade(R.string.editor_fade),
    Date(R.string.effect_date),
}

data class EditorUiState(
    val medium: Medium? = null,
    val original: Bitmap? = null,
    val preview: Bitmap? = null,
    val thumb: Bitmap? = null,
    val presetThumbs: Map<Preset, Bitmap> = emptyMap(),
    val cameraThumbs: Map<Camera, Bitmap> = emptyMap(),
    val tab: EditorTab = EditorTab.Presets,
    val tool: EditorTool? = null,
    val pack: PresetPack? = null,
    val adjustment: Adjustment = Adjustment.Exposure,
    val cropMode: CropMode = CropMode.Crop,
    val curvesMode: CurvesMode = CurvesMode.Curves,
    val channel: CurveChannel = CurveChannel.Rgb,
    val band: HueBand = HueBand.Orange,
    val effectTab: EffectTab = EffectTab.Leak,
    val cameraTab: CameraTab = CameraTab.Film,
    val markup: MarkupTool = MarkupTool.Pen,
    val selectedText: Long? = null,
    val edits: PhotoEdits = PhotoEdits(),
    val ink: Int = INKS.first(),
    val nib: Float = NIBS[1],
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val comparing: Boolean = false,
    val closeMenu: Boolean = false,
    val saving: Boolean = false,
    val finished: Boolean = false,
) {
    val changed: Boolean get() = edits != PhotoEdits()
    val date: Long get() = medium?.dateTaken?.takeIf { it > 0 } ?: System.currentTimeMillis()
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val writer: MediaWriter,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Editor>()
    private val history = EditHistory()
    private var gestureStart: PhotoEdits? = null
    private var toolStart: PhotoEdits? = null
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()
    val fonts = EditorFonts(context)

    init {
        viewModelScope.launch {
            val medium = repository.medium(route.mediumId) ?: return@launch
            _state.update { it.copy(medium = medium) }
            val original = withContext(Dispatchers.IO) {
                runCatching { decodePhoto(context.contentResolver, medium.uri, PROXY_PIXELS) }.getOrNull()
            } ?: return@launch
            _state.update { it.copy(original = original, preview = original) }
            launch { renderThumbs(original) }
            _state.map { it.edits.grading }
                .distinctUntilChanged()
                .conflate()
                .collect { grading ->
                    val preview = withContext(Dispatchers.Default) { renderPreview(original, grading) }
                    _state.update { it.copy(preview = preview) }
                }
        }
    }

    private suspend fun renderThumbs(original: Bitmap) {
        val thumb = withContext(Dispatchers.Default) {
            val shrink = THUMB_EDGE / maxOf(original.width, original.height).toFloat()
            Bitmap.createScaledBitmap(
                original,
                (original.width * shrink).toInt().coerceAtLeast(1),
                (original.height * shrink).toInt().coerceAtLeast(1),
                true,
            )
        }
        _state.update { it.copy(thumb = thumb) }
        val presets = withContext(Dispatchers.Default) {
            Preset.entries.associateWith { renderPreview(thumb, PhotoEdits(preset = it)) }
        }
        _state.update { it.copy(presetThumbs = presets) }
        val cameras = withContext(Dispatchers.Default) {
            Camera.entries.associateWith { renderPreview(thumb, PhotoEdits(camera = it, cameraSettings = it.defaults()).grading) }
        }
        _state.update { it.copy(cameraThumbs = cameras) }
    }

    fun pickTab(tab: EditorTab) {
        when (tab) {
            EditorTab.Crop -> openTool(EditorTool.Crop)
            EditorTab.Markup -> openTool(EditorTool.Markup)
            EditorTab.Text -> {
                openTool(EditorTool.Text)
                if (_state.value.edits.texts.isEmpty()) addText()
            }
            else -> _state.update { it.copy(tab = tab, comparing = false) }
        }
    }

    fun pickPack(pack: PresetPack?) = _state.update { it.copy(pack = pack) }
    fun pickAdjustmentTool(adjustment: Adjustment) = _state.update { it.copy(adjustment = adjustment) }
    fun pickCropMode(mode: CropMode) = _state.update { it.copy(cropMode = mode) }
    fun pickCurvesMode(mode: CurvesMode) = _state.update { it.copy(curvesMode = mode) }
    fun pickChannel(channel: CurveChannel) = _state.update { it.copy(channel = channel) }
    fun pickBand(band: HueBand) = _state.update { it.copy(band = band) }
    fun pickEffectTab(tab: EffectTab) = _state.update { it.copy(effectTab = tab) }
    fun pickCameraTab(tab: CameraTab) = _state.update { it.copy(cameraTab = tab) }
    fun pickMarkup(tool: MarkupTool) = _state.update { it.copy(markup = tool) }
    fun pickInk(color: Int) = _state.update { it.copy(ink = color) }
    fun pickNib(width: Float) = _state.update { it.copy(nib = width) }
    fun compare(on: Boolean) = _state.update { it.copy(comparing = on) }

    fun openCurves(mode: CurvesMode) {
        _state.update { it.copy(curvesMode = mode) }
        openTool(EditorTool.Curves)
    }

    fun openTool(tool: EditorTool) {
        if (_state.value.tool != null) return
        toolStart = _state.value.edits
        gestureStart = null
        _state.update { it.copy(tool = tool, comparing = false, closeMenu = false) }
    }

    fun cancelTool() {
        val start = toolStart ?: return
        toolStart = null
        _state.update { it.copy(tool = null, edits = start, selectedText = null).withHistory() }
    }

    fun finishTool() {
        val start = toolStart ?: return
        toolStart = null
        _state.update { state ->
            val kept = state.edits.copy(texts = state.edits.texts.filter { it.text.isNotBlank() })
            if (start != kept) history.record(start)
            state.copy(tool = null, selectedText = null, edits = kept).withHistory()
        }
    }

    fun resetTool() = edit { edits ->
        when (_state.value.tool) {
            EditorTool.Crop -> edits.withGeometry(PhotoEdits()).copy(strokes = edits.strokes)
            EditorTool.Curves -> edits.copy(curves = emptyMap(), hsl = emptyMap(), split = SplitTone())
            EditorTool.Camera -> edits.camera?.let { edits.copy(cameraSettings = it.defaults()) } ?: edits
            EditorTool.Markup, EditorTool.Text, null -> edits
        }
    }

    fun pickPreset(preset: Preset?) = edit { it.copy(preset = preset) }
    fun setPresetAmount(amount: Int) = edit { it.copy(presetAmount = amount.coerceIn(0, 100)) }

    fun pickCamera(camera: Camera?) {
        if (camera != null && camera == _state.value.edits.camera) return openTool(EditorTool.Camera)
        edit { it.copy(camera = camera, cameraSettings = camera?.defaults() ?: CameraSettings()) }
    }

    fun updateCamera(block: (CameraSettings) -> CameraSettings) =
        edit { it.copy(cameraSettings = block(it.cameraSettings)) }

    fun pickEffect(kind: EffectKind, variant: Int?) = edit { edits ->
        if (variant == null) return@edit edits.copy(effects = edits.effects - kind)
        val layer = edits.effects[kind]?.copy(variant = variant) ?: EffectLayer(variant, kind.opacity, kind.blend)
        edits.copy(effects = edits.effects + (kind to layer))
    }

    fun updateEffect(kind: EffectKind, block: (EffectLayer) -> EffectLayer) = edit { edits ->
        val layer = edits.effects[kind] ?: return@edit edits
        edits.copy(effects = edits.effects + (kind to block(layer)))
    }

    fun pickDate(style: DateStyle?) = edit { it.copy(dateStamp = style) }

    fun pickRatio(ratio: CropRatio) = edit { edits ->
        val crop = orientedFrame(edits)?.let { (w, h) -> centredCrop(w, h, ratio.aspect(w, h)) } ?: edits.crop
        edits.copy(ratio = ratio, crop = crop)
    }

    fun setCrop(crop: Rect) = edit { it.copy(crop = crop) }
    fun setAdjustment(adjustment: Adjustment, value: Int) = edit { it.with(adjustment, value) }

    fun resetAdjustments() =
        edit { it.copy(adjustments = emptyMap(), curves = emptyMap(), hsl = emptyMap(), split = SplitTone()) }

    fun addStroke(stroke: Stroke) = edit { it.copy(strokes = it.strokes + stroke) }
    fun undoStroke() = edit { it.copy(strokes = it.strokes.dropLast(1)) }
    fun clearStrokes() = edit { it.copy(strokes = emptyList()) }

    fun erase(at: Offset, radius: Float) = edit { edits ->
        val (width, height) = orientedFrame(edits) ?: return@edit edits
        val aspect = height.toFloat() / width
        edits.copy(strokes = edits.strokes.filterNot { it.touches(at, radius, aspect) })
    }

    fun setCurve(channel: CurveChannel, points: List<Offset>) = edit { it.withCurve(channel, points) }
    fun setShift(band: HueBand, shift: HslShift) = edit { it.withShift(band, shift) }
    fun setSplit(split: SplitTone) = edit { it.copy(split = split) }

    fun rotate(clockwise: Boolean) = edit { edits ->
        val (width, height) = orientedFrame(edits) ?: return@edit edits
        val turned = edits.turned(clockwise, width.toFloat() / height)
        val ratio = edits.ratio.aspect(height, width) ?: return@edit turned
        turned.copy(crop = centredCrop(height, width, ratio))
    }

    fun mirror() = edit { it.mirrored() }

    fun flipVertically() = edit { edits ->
        val (width, height) = orientedFrame(edits) ?: return@edit edits
        edits.flippedVertically(width.toFloat() / height)
    }

    fun setStraighten(degrees: Float) = edit { it.copy(straighten = straightened(degrees)) }
    fun moveCorner(corner: Int, to: Offset) = edit { it.copy(quad = movedCorner(it.quad, corner, to)) }
    fun resetPerspective() = edit { it.copy(quad = FULL_QUAD) }

    fun addText() {
        val layer = TextLayer(id = System.nanoTime(), color = _state.value.ink)
        edit { it.copy(texts = it.texts + layer) }
        _state.update { it.copy(selectedText = layer.id) }
    }

    fun selectText(id: Long?) = _state.update { it.copy(selectedText = id) }

    fun updateText(id: Long, block: (TextLayer) -> TextLayer) = edit { edits ->
        edits.copy(texts = edits.texts.map { if (it.id == id) block(it) else it })
    }

    fun deleteText(id: Long) {
        edit { edits -> edits.copy(texts = edits.texts.filterNot { it.id == id }) }
        _state.update { it.copy(selectedText = null) }
    }

    fun duplicateText(id: Long) {
        val source = _state.value.edits.texts.firstOrNull { it.id == id } ?: return
        val copy = source.copy(id = System.nanoTime(), center = source.center + Offset(0.04f, 0.04f), locked = false)
        edit { it.copy(texts = it.texts + copy) }
        _state.update { it.copy(selectedText = copy.id) }
    }

    fun reset() = edit { PhotoEdits() }

    fun beginGesture() {
        if (toolStart == null && gestureStart == null) gestureStart = _state.value.edits
    }

    fun endGesture() {
        val start = gestureStart ?: return
        gestureStart = null
        if (start != _state.value.edits) history.record(start)
        _state.update { it.withHistory() }
    }

    fun undo() = _state.update { state ->
        val previous = history.undo(state.edits) ?: return@update state
        state.copy(edits = previous).withHistory()
    }

    fun redo() = _state.update { state ->
        val next = history.redo(state.edits) ?: return@update state
        state.copy(edits = next).withHistory()
    }

    fun requestClose() {
        if (_state.value.tool != null) return cancelTool()
        _state.update {
            if (it.changed && !it.saving) it.copy(closeMenu = true) else it.copy(finished = true)
        }
    }

    fun dismissCloseMenu() = _state.update { it.copy(closeMenu = false) }
    fun discard() = _state.update { it.copy(closeMenu = false, finished = true) }

    fun save() {
        val current = _state.value
        val medium = current.medium ?: return
        if (current.saving || current.original == null) return
        _state.update { it.copy(saving = true, closeMenu = false) }
        viewModelScope.launch {
            val saved = runCatching {
                val full = withContext(Dispatchers.IO) {
                    decodePhoto(context.contentResolver, medium.uri, EXPORT_PIXELS)
                }
                val bitmap = withContext(Dispatchers.Default) { render(full, current.edits, current.date, fonts) }
                full.recycle()
                writer.saveCopy(medium, bitmap).also { bitmap.recycle() }
            }.getOrNull() != null
            _state.update { it.copy(saving = false, finished = saved) }
        }
    }

    private fun orientedFrame(edits: PhotoEdits): Pair<Int, Int>? =
        _state.value.original?.let { orientedSize(it.width, it.height, edits.turns) }

    private fun edit(block: (PhotoEdits) -> PhotoEdits) {
        val before = _state.value.edits
        val after = block(before)
        if (after == before) return
        if (gestureStart == null && toolStart == null) history.record(before)
        _state.update { it.copy(edits = after).withHistory() }
    }

    private fun EditorUiState.withHistory() = copy(canUndo = history.canUndo, canRedo = history.canRedo)

    private companion object {
        const val PROXY_PIXELS = 4_000_000L
        const val EXPORT_PIXELS = 48_000_000L
        const val THUMB_EDGE = 180
    }
}
