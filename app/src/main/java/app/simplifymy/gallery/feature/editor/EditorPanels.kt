package app.simplifymy.gallery.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.R
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.SegmentedPicker
import app.simplifymy.gallery.ui.theme.GalleryTheme

@Composable
fun PresetsPanel(state: EditorUiState, viewModel: EditorViewModel) {
    val edits = state.edits
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChipRow(
            options = listOf<PresetPack?>(null) + PresetPack.entries,
            selected = state.pack,
            onSelect = viewModel::pickPack,
            label = { pack -> pack?.let { stringResource(it.labelRes) } ?: stringResource(R.string.pack_all) },
        )
        if (state.pack == PresetPack.Cameras) {
            CameraShelf(state, viewModel)
            return@Column
        }
        TileRow {
            ResetTile { viewModel.pickPreset(null) }
            Preset.entries.filter { state.pack == null || it.pack == state.pack }.forEach { preset ->
                PhotoTile(
                    bitmap = state.presetThumbs[preset] ?: state.thumb,
                    label = stringResource(preset.labelRes),
                    selected = edits.preset == preset,
                    onClick = { viewModel.pickPreset(preset) },
                )
            }
        }
        if (edits.preset != null) {
            ValueSlider(
                label = stringResource(R.string.editor_intensity),
                value = edits.presetAmount,
                range = 0..100,
                onChange = viewModel::setPresetAmount,
                onStart = viewModel::beginGesture,
                onEnd = viewModel::endGesture,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
fun AdjustPanel(state: EditorUiState, viewModel: EditorViewModel) {
    val edits = state.edits
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TileRow {
            ToolItem(GalleryIcons.Reset, stringResource(R.string.action_reset), selected = false, onClick = viewModel::resetAdjustments)
            Adjustment.entries.forEach { adjustment ->
                ToolItem(
                    icon = adjustment.icon,
                    label = stringResource(adjustment.labelRes),
                    selected = state.adjustment == adjustment,
                    onClick = { viewModel.pickAdjustmentTool(adjustment) },
                )
            }
            CurvesMode.entries.forEach { mode ->
                ToolItem(
                    icon = mode.icon,
                    label = stringResource(mode.labelRes),
                    selected = false,
                    onClick = { viewModel.openCurves(mode) },
                )
            }
        }
        ValueSlider(
            label = stringResource(state.adjustment.labelRes),
            value = edits[state.adjustment],
            range = state.adjustment.range,
            onChange = { viewModel.setAdjustment(state.adjustment, it) },
            onStart = viewModel::beginGesture,
            onEnd = viewModel::endGesture,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun CameraShelf(state: EditorUiState, viewModel: EditorViewModel) {
    val camera = state.edits.camera
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TileRow {
            ResetTile { viewModel.pickCamera(null) }
            Camera.entries.forEach { entry ->
                PhotoTile(
                    bitmap = state.cameraThumbs[entry] ?: state.thumb,
                    label = stringResource(entry.labelRes),
                    selected = camera == entry,
                    onClick = { viewModel.pickCamera(entry) },
                )
            }
        }
        Text(
            stringResource(if (camera == null) R.string.camera_hint_pick else R.string.camera_hint_settings),
            style = GalleryTheme.type.meta,
            color = GalleryTheme.colors.textFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
    }
}

@Composable
fun EffectsPanel(state: EditorUiState, viewModel: EditorViewModel) {
    val edits = state.edits
    val kind = state.effectTab.kind
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChipRow(
            options = EffectTab.entries,
            selected = state.effectTab,
            onSelect = viewModel::pickEffectTab,
            label = { stringResource(it.labelRes) },
        )
        TileRow {
            ResetTile { if (kind != null) viewModel.pickEffect(kind, null) else viewModel.pickDate(null) }
            if (kind != null) {
                val name = stringResource(kind.labelRes)
                (0 until EFFECT_VARIANTS).forEach { variant ->
                    PhotoTile(
                        bitmap = state.thumb,
                        label = "$name ${variant + 1}",
                        selected = edits.effects[kind]?.variant == variant,
                        onClick = { viewModel.pickEffect(kind, variant) },
                        overlay = {
                            drawIntoCanvas {
                                drawEffect(it.nativeCanvas, size.width, size.height, kind, EffectLayer(variant, 100, kind.blend))
                            }
                        },
                    )
                }
            } else {
                DateStyle.entries.forEach { style ->
                    PhotoTile(
                        bitmap = state.thumb,
                        label = stringResource(style.labelRes),
                        selected = edits.dateStamp == style,
                        onClick = { viewModel.pickDate(style) },
                        overlay = {
                            drawIntoCanvas {
                                drawFinish(it.nativeCanvas, size.width, size.height, PhotoEdits(dateStamp = style), state.date, viewModel.fonts)
                            }
                        },
                    )
                }
            }
        }
        val layer = kind?.let { edits.effects[it] }
        if (kind != null && layer != null) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                SegmentedPicker(
                    options = Blend.entries,
                    selected = layer.blend,
                    onSelect = { blend -> viewModel.updateEffect(kind) { it.copy(blend = blend) } },
                    label = { stringResource(it.labelRes) },
                )
                ValueSlider(
                    label = stringResource(R.string.editor_opacity),
                    value = layer.opacity,
                    range = 0..100,
                    onChange = { value -> viewModel.updateEffect(kind) { it.copy(opacity = value) } },
                    onStart = viewModel::beginGesture,
                    onEnd = viewModel::endGesture,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    labelWidth = 0.dp,
                )
            }
        }
    }
}

val Adjustment.icon: ImageVector
    get() = when (this) {
        Adjustment.Exposure -> GalleryIcons.Exposure
        Adjustment.Brightness -> GalleryIcons.Brightness
        Adjustment.Contrast -> GalleryIcons.Contrast
        Adjustment.Highlights -> GalleryIcons.Highlights
        Adjustment.Shadows -> GalleryIcons.Shadows
        Adjustment.Whites -> GalleryIcons.Whites
        Adjustment.Blacks -> GalleryIcons.Blacks
        Adjustment.Saturation -> GalleryIcons.Saturation
        Adjustment.Vibrance -> GalleryIcons.Vibrance
        Adjustment.Temperature -> GalleryIcons.Temperature
        Adjustment.Tint -> GalleryIcons.Tint
        Adjustment.Fade -> GalleryIcons.Fade
        Adjustment.Clarity -> GalleryIcons.Clarity
        Adjustment.Sharpen -> GalleryIcons.Sharpen
        Adjustment.Glow -> GalleryIcons.Glow
        Adjustment.Grain -> GalleryIcons.Grain
        Adjustment.Vignette -> GalleryIcons.Vignette
    }

val CurvesMode.icon: ImageVector
    get() = when (this) {
        CurvesMode.Curves -> GalleryIcons.Curves
        CurvesMode.Hsl -> GalleryIcons.Hsl
        CurvesMode.Split -> GalleryIcons.Split
    }
