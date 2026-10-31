package app.simplifymy.gallery.feature.editor

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.gallery.R
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.component.MenuEntry
import app.simplifymy.gallery.ui.component.OverflowMenu
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import java.util.Locale

const val EDITOR_PREVIEW = "editor_preview"

enum class Overlay { None, Crop, Straighten, Perspective, Text, Draw }

@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors
    var straightening by remember { mutableStateOf(false) }

    LaunchedEffect(state.finished) { if (state.finished) onBack() }
    BackHandler(onBack = viewModel::requestClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        when (state.tool) {
            null -> TopBar(state, viewModel)
            EditorTool.Crop -> ToolTopBar(
                title = stringResource(R.string.editor_crop),
                start = stringResource(R.string.action_reset),
                onStart = viewModel::resetTool,
                onDone = viewModel::finishTool,
            )
            EditorTool.Curves -> ToolTopBar(
                title = stringResource(state.curvesMode.titleRes),
                start = stringResource(R.string.action_cancel),
                onStart = viewModel::cancelTool,
                onDone = viewModel::finishTool,
            )
            EditorTool.Camera, EditorTool.Markup, EditorTool.Text -> Spacer(Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val overlay = overlayOf(state)
            val shown = if (state.comparing) state.original else state.preview
            shown?.let { bitmap ->
                Preview(
                    source = bitmap,
                    edits = when {
                        state.comparing -> PhotoEdits()
                        overlay == Overlay.Perspective -> state.edits.oriented
                        else -> state.edits
                    },
                    overlay = overlay,
                    straightening = straightening,
                    comparable = state.tool == null,
                    state = state,
                    viewModel = viewModel,
                )
            }
        }

        if (state.tool == null) Caption(state)

        when (state.tool) {
            EditorTool.Crop -> CropPanel(state, viewModel) { straightening = it }
            EditorTool.Curves -> CurvesPanel(state, viewModel)
            EditorTool.Camera -> CameraPanel(state, viewModel)
            EditorTool.Markup -> MarkupPanel(state, viewModel)
            EditorTool.Text -> TextPanel(state, viewModel)
            null -> Box(Modifier.fillMaxWidth().height(PANEL_HEIGHT), contentAlignment = Alignment.Center) {
                when (state.tab) {
                    EditorTab.Presets -> PresetsPanel(state, viewModel)
                    EditorTab.Adjust -> AdjustPanel(state, viewModel)
                    EditorTab.Effects -> EffectsPanel(state, viewModel)
                    EditorTab.Markup, EditorTab.Crop, EditorTab.Text -> Unit
                }
            }
        }

        when (state.tool) {
            null -> TabBar(state.tab, viewModel::pickTab)
            EditorTool.Camera -> ToolFooter(
                title = state.edits.camera?.let { stringResource(it.labelRes) } ?: "",
                onCancel = viewModel::cancelTool,
                onDone = viewModel::finishTool,
            )
            EditorTool.Markup -> ToolFooter(
                title = stringResource(R.string.editor_markup),
                onCancel = viewModel::cancelTool,
                onDone = viewModel::finishTool,
            )
            EditorTool.Text -> ToolFooter(
                title = stringResource(R.string.editor_text),
                onCancel = viewModel::cancelTool,
                onDone = viewModel::finishTool,
            )
            EditorTool.Crop, EditorTool.Curves -> Spacer(Modifier.height(8.dp))
        }
    }
}

val PANEL_HEIGHT = 200.dp

private fun overlayOf(state: EditorUiState): Overlay = when {
    state.comparing -> Overlay.None
    state.tool == EditorTool.Crop -> when (state.cropMode) {
        CropMode.Crop -> Overlay.Crop
        CropMode.Straighten -> Overlay.Straighten
        CropMode.Perspective -> Overlay.Perspective
    }
    state.tool == EditorTool.Markup -> Overlay.Draw
    state.tool == EditorTool.Text -> Overlay.Text
    else -> Overlay.None
}

@Composable
private fun TopBar(state: EditorUiState, viewModel: EditorViewModel) {
    val colors = GalleryTheme.colors
    val canSave = state.original != null && !state.saving
    Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp)) {
        Box(Modifier.align(Alignment.CenterStart)) {
            IconAction(GalleryIcons.Close, onClick = viewModel::requestClose, label = stringResource(R.string.action_close))
            DropdownMenu(
                expanded = state.closeMenu,
                onDismissRequest = viewModel::dismissCloseMenu,
                shape = RoundedCornerShape(6.dp),
                containerColor = colors.surfaceAlt,
            ) {
                MenuEntry(stringResource(R.string.editor_save_changes), onClick = viewModel::save)
                CloseEntry(stringResource(R.string.editor_discard_changes), colors.danger, viewModel::discard)
                MenuEntry(stringResource(R.string.action_cancel), onClick = viewModel::dismissCloseMenu)
            }
        }
        Row(Modifier.align(Alignment.Center)) {
            IconAction(
                GalleryIcons.Undo,
                onClick = viewModel::undo,
                label = stringResource(R.string.action_undo),
                tint = colors.textMuted,
                enabled = state.canUndo,
            )
            IconAction(
                GalleryIcons.Redo,
                onClick = viewModel::redo,
                label = stringResource(R.string.action_redo),
                tint = colors.textMuted,
                enabled = state.canRedo,
            )
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            OverflowMenu(tint = colors.textMuted) { dismiss ->
                MenuEntry(stringResource(R.string.editor_reset_all)) {
                    viewModel.reset()
                    dismiss()
                }
            }
            Text(
                stringResource(R.string.action_save),
                style = GalleryTheme.type.label.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onAccent,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .alpha(if (canSave) 1f else DISABLED)
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(colors.accent)
                    .clickable(enabled = canSave, onClick = viewModel::save)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun CloseEntry(label: String, color: Color, onClick: () -> Unit) {
    androidx.compose.material3.DropdownMenuItem(
        text = { Text(label, style = GalleryTheme.type.body, color = color) },
        onClick = onClick,
    )
}

@Composable
private fun Caption(state: EditorUiState) {
    val edits = state.edits
    val parts = buildList {
        edits.preset?.let { add(it.code) ; add("${edits.presetAmount}%") }
        edits.camera?.let { add(stringResource(it.labelRes).uppercase(Locale.getDefault())) }
        if (edits.ratio != CropRatio.Free) add(edits.ratio.labelRes?.let { stringResource(it).uppercase(Locale.getDefault()) } ?: edits.ratio.label)
        add(stringResource(R.string.editor_hold_to_compare).uppercase(Locale.getDefault()))
    }
    Text(
        parts.joinToString(" · "),
        style = GalleryTheme.type.meta,
        color = GalleryTheme.colors.textFaint,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun Preview(
    source: Bitmap,
    edits: PhotoEdits,
    overlay: Overlay,
    straightening: Boolean,
    comparable: Boolean,
    state: EditorUiState,
    viewModel: EditorViewModel,
) {
    val (frameWidth, frameHeight) = orientedSize(source.width, source.height, edits.turns)
    val cropping = overlay == Overlay.Crop || overlay == Overlay.Straighten || overlay == Overlay.Perspective
    val window = if (cropping) FULL_FRAME else edits.crop
    val aspect = frameWidth * window.width / (frameHeight * window.height)
    val paint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }
    var live by remember { mutableStateOf(emptyList<Offset>()) }
    var canvas by remember { mutableStateOf(Size.Zero) }
    val drawing = overlay == Overlay.Draw

    Box(
        Modifier
            .aspectRatio(aspect)
            .then(if (aspect >= 1f) Modifier.fillMaxWidth() else Modifier.fillMaxHeight())
            .clip(RoundedCornerShape(2.dp))
            .testTag(EDITOR_PREVIEW)
            .then(
                if (!comparable) Modifier else Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (awaitLongPressOrCancellation(down.id) != null) {
                            viewModel.compare(true)
                            waitForUpOrCancellation()
                            viewModel.compare(false)
                        }
                    }
                }
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!drawing) Modifier else Modifier.pointerInput(state.markup, state.ink, state.nib, window) {
                        val tool = state.markup
                        val erase = { point: Offset ->
                            val size = canvas
                            if (size.minDimension > 0f) {
                                viewModel.erase(normalized(point, size, window), state.nib * tool.scale / size.width * window.width)
                            }
                        }
                        detectDragGestures(
                            onDragStart = {
                                live = listOf(it)
                                if (tool == MarkupTool.Eraser) erase(it)
                            },
                            onDrag = { change, _ ->
                                live = when {
                                    tool == MarkupTool.Eraser -> listOf(change.position)
                                    tool.shape -> listOf(live.first(), change.position)
                                    else -> live + change.position
                                }
                                if (tool == MarkupTool.Eraser) erase(change.position)
                            },
                            onDragEnd = {
                                val size = canvas
                                if (tool != MarkupTool.Eraser && size.minDimension > 0f && live.size > 1) {
                                    viewModel.addStroke(strokeOf(live, tool, state.ink, state.nib, size, window))
                                }
                                live = emptyList()
                            },
                            onDragCancel = { live = emptyList() },
                        )
                    }
                )
        ) {
            canvas = size
            val matrix = frameMatrix(source.width, source.height, edits).apply {
                postTranslate(-window.left * frameWidth, -window.top * frameHeight)
                postScale(size.width / (window.width * frameWidth), size.height / (window.height * frameHeight))
            }
            drawIntoCanvas { it.nativeCanvas.drawBitmap(source, matrix, paint) }
            if (cropping) return@Canvas
            drawIntoCanvas { drawFinish(it.nativeCanvas, size.width, size.height, edits, state.date, viewModel.fonts) }
            val shown = edits.strokes + liveStroke(live, state.markup, state.ink, state.nib, size, window)
            drawIntoCanvas { drawStrokes(it.nativeCanvas, shown, window, size.width, size.height) }
            if (drawing && state.markup == MarkupTool.Eraser && live.isNotEmpty()) {
                drawCircle(
                    color = Color.White,
                    radius = state.nib * MarkupTool.Eraser.scale,
                    center = live.last(),
                    style = DrawStroke(width = 1.dp.toPx()),
                )
            }
            drawIntoCanvas { drawTexts(it.nativeCanvas, size.width, size.height, edits.texts, viewModel.fonts) }
        }
        when (overlay) {
            Overlay.Crop -> CropBox(
                crop = edits.crop,
                ratio = edits.ratio.aspect(frameWidth, frameHeight),
                interactive = true,
                lines = 3,
                onStart = viewModel::beginGesture,
                onCrop = viewModel::setCrop,
                onEnd = viewModel::endGesture,
            )
            Overlay.Straighten -> CropBox(
                crop = edits.crop,
                ratio = null,
                interactive = false,
                lines = if (straightening) 6 else 3,
                onStart = {},
                onCrop = {},
                onEnd = {},
            )
            Overlay.Perspective -> PerspectiveBox(edits.quad, viewModel::beginGesture, viewModel::moveCorner, viewModel::endGesture)
            Overlay.Text -> TextOverlay(edits.texts, state.selectedText, viewModel)
            Overlay.None, Overlay.Draw -> Unit
        }
    }
}

private fun normalized(point: Offset, canvas: Size, window: Rect) = Offset(
    window.left + point.x / canvas.width * window.width,
    window.top + point.y / canvas.height * window.height,
)

private fun strokeOf(points: List<Offset>, tool: MarkupTool, ink: Int, nib: Float, canvas: Size, window: Rect) = Stroke(
    color = ink,
    width = nib * tool.scale / canvas.width * window.width,
    points = points.map { normalized(it, canvas, window) },
    tool = tool,
)

private fun liveStroke(points: List<Offset>, tool: MarkupTool, ink: Int, nib: Float, canvas: Size, window: Rect) =
    if (tool == MarkupTool.Eraser || points.size < 2 || canvas.minDimension <= 0f) emptyList()
    else listOf(strokeOf(points, tool, ink, nib, canvas, window))

@Composable
private fun TabBar(selected: EditorTab, onPick: (EditorTab) -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawTopHairline(colors.hairline),
    ) {
        EditorTab.entries.forEach { tab ->
            val on = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .then(if (on) Modifier.drawTopHairline(colors.accent, 2.dp) else Modifier)
                    .clickable { onPick(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    tab.icon,
                    contentDescription = stringResource(tab.labelRes),
                    tint = if (on) colors.accent else colors.textFaint,
                    modifier = Modifier.size(19.dp),
                )
                Box(Modifier.height(5.dp))
                Text(
                    stringResource(tab.labelRes),
                    style = GalleryTheme.type.label.scaled(0.74f),
                    color = if (on) colors.accent else colors.textFaint,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun Modifier.drawTopHairline(color: Color, thickness: Dp = 1.dp) = drawWithContent {
    drawContent()
    drawRect(color = color, size = Size(size.width, thickness.toPx()))
}

private val EditorTab.icon
    get() = when (this) {
        EditorTab.Presets -> GalleryIcons.Filter
        EditorTab.Adjust -> GalleryIcons.Adjust
        EditorTab.Markup -> GalleryIcons.Markup
        EditorTab.Effects -> GalleryIcons.Effects
        EditorTab.Crop -> GalleryIcons.Crop
        EditorTab.Text -> GalleryIcons.Text
    }
