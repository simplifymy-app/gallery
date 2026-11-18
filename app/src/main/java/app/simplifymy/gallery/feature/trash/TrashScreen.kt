package app.simplifymy.gallery.feature.trash

import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.gallery.R
import app.simplifymy.gallery.feature.media.MediaTile
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing

@Composable
fun TrashScreen(onBack: () -> Unit, viewModel: TrashViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { if (it.resultCode == Activity.RESULT_OK) viewModel.clearSelection() }
    val launch: (IntentSender) -> Unit = { launcher.launch(IntentSenderRequest.Builder(it).build()) }
    BackHandler(enabled = state.selecting, onBack = viewModel::clearSelection)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = Spacing.screen, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(GalleryIcons.Back, onClick = if (state.selecting) viewModel::clearSelection else onBack)
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(stringResource(R.string.trash_title), style = GalleryTheme.type.title, color = colors.text)
                Text(
                    if (state.selecting) stringResource(R.string.selection_count, state.selected.size)
                    else stringResource(R.string.trash_hint),
                    style = GalleryTheme.type.meta,
                    color = if (state.selecting) colors.accent else colors.textMuted,
                )
            }
            if (state.selecting) {
                IconAction(GalleryIcons.Undo, label = stringResource(R.string.action_restore), onClick = { launch(viewModel.restoreSelected()) })
                IconAction(GalleryIcons.Trash, label = stringResource(R.string.action_delete_forever), onClick = { launch(viewModel.deleteSelected()) })
            }
            if (state.items.isNotEmpty()) {
                Text(
                    stringResource(R.string.action_select_all),
                    style = GalleryTheme.type.label,
                    color = colors.text,
                    modifier = Modifier.clickable(onClick = viewModel::selectAll).padding(8.dp),
                )
            }
        }

        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.trash_empty), style = GalleryTheme.type.body, color = colors.textMuted)
            }
        } else {
            val gap = state.settings.thumbnailSpacing.gap
            LazyVerticalGrid(
                columns = GridCells.Fixed(state.settings.mediaColumns),
                contentPadding = PaddingValues(horizontal = gap),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                items(state.items, key = { it.id }) { medium ->
                    Box(Modifier.clickable { viewModel.toggle(medium.id) }) {
                        MediaTile(
                            medium = medium,
                            selected = medium.id in state.selected,
                            selecting = true,
                            showDuration = state.settings.showVideoDuration,
                            onTap = { viewModel.toggle(medium.id) },
                            onHold = { viewModel.toggle(medium.id) },
                        )
                    }
                }
            }
        }
    }
}
