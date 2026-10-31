package app.simplifymy.gallery.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.R
import app.simplifymy.gallery.ui.component.SegmentedPicker

@Composable
fun CameraPanel(state: EditorUiState, viewModel: EditorViewModel) {
    val settings = state.edits.cameraSettings
    Column(
        modifier = Modifier.fillMaxWidth().height(PANEL_HEIGHT).padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SegmentedPicker(
            options = CameraTab.entries,
            selected = state.cameraTab,
            onSelect = viewModel::pickCameraTab,
            label = { stringResource(it.labelRes) },
        )
        val slider: @Composable (Int, Int, (Int) -> CameraSettings) -> Unit = { label, value, update ->
            ValueSlider(
                label = stringResource(label),
                value = value,
                range = 0..100,
                onChange = { next -> viewModel.updateCamera { update(next) } },
                onStart = viewModel::beginGesture,
                onEnd = viewModel::endGesture,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        when (state.cameraTab) {
            CameraTab.Film -> {
                ChipRow(
                    options = FilmStock.entries,
                    selected = FilmStock.entries[settings.stock.coerceIn(FilmStock.entries.indices)],
                    onSelect = { stock -> viewModel.updateCamera { it.copy(stock = stock.ordinal) } },
                    label = { stringResource(it.labelRes) },
                )
                slider(R.string.editor_intensity, settings.strength) { settings.copy(strength = it) }
            }
            CameraTab.Glare -> slider(R.string.camera_glare, settings.glare) { settings.copy(glare = it) }
            CameraTab.Grain -> slider(R.string.editor_grain, settings.grain) { settings.copy(grain = it) }
            CameraTab.Fade -> slider(R.string.editor_fade, settings.fade) { settings.copy(fade = it) }
            CameraTab.Date -> ChipRow(
                options = listOf<DateStyle?>(null) + DateStyle.entries,
                selected = state.edits.dateStamp,
                onSelect = viewModel::pickDate,
                label = { style -> style?.let { stringResource(it.labelRes) } ?: stringResource(R.string.date_off) },
            )
        }
    }
}
