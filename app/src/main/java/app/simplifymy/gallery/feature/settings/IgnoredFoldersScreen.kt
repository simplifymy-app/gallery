package app.simplifymy.gallery.feature.settings

import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.model.FolderEntry
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.formatCount
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IgnoredFoldersUiState(
    val folders: List<FolderEntry> = emptyList(),
    val ignored: Set<Long> = emptySet(),
    val edited: Set<Long>? = null,
) {
    val checked: Set<Long> get() = edited ?: ignored
    val dirty: Boolean get() = edited != null && edited != ignored
}

@HiltViewModel
class IgnoredFoldersViewModel @Inject constructor(
    private val repository: MediaRepository,
) : ViewModel() {

    private val edits = MutableStateFlow<Set<Long>?>(null)

    val state: StateFlow<IgnoredFoldersUiState> =
        combine(
            repository.folderEntries,
            repository.ignoredFolders.map { list -> list.map { it.bucketId }.toSet() },
            edits,
        ) { folders, ignored, edited -> IgnoredFoldersUiState(folders, ignored, edited) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IgnoredFoldersUiState())

    fun toggle(bucketId: Long) {
        val current = state.value.checked
        edits.value = if (bucketId in current) current - bucketId else current + bucketId
    }

    fun save() = viewModelScope.launch {
        val current = state.first()
        repository.setIgnoredFolders(current.folders.filter { it.bucketId in current.checked })
        edits.value = null
    }
}

@Composable
fun IgnoredFoldersScreen(onBack: () -> Unit, viewModel: IgnoredFoldersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = Spacing.screen, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(GalleryIcons.Back, onClick = onBack)
            Text(stringResource(R.string.ignored_title), style = GalleryTheme.type.title, color = colors.text)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.action_save),
                style = GalleryTheme.type.label,
                color = colors.onAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(if (state.dirty) colors.accent else colors.textFaint)
                    .clickable(enabled = state.dirty, onClick = viewModel::save)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Text(
            stringResource(R.string.ignored_hint, state.checked.size),
            style = GalleryTheme.type.meta,
            color = colors.textMuted,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.base),
        )

        LazyColumn {
            items(state.folders, key = { it.bucketId }) { folder ->
                IgnoreRow(folder, ignored = folder.bucketId in state.checked) { viewModel.toggle(folder.bucketId) }
            }
        }
    }
}

@Composable
private fun IgnoreRow(folder: FolderEntry, ignored: Boolean, onClick: () -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.screen, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(ControlRadius))
                .background(if (ignored) colors.accent else colors.background)
                .border(1.5.dp, if (ignored) colors.accent else colors.textFaint, RoundedCornerShape(ControlRadius)),
            contentAlignment = Alignment.Center,
        ) {
            if (ignored) Icon(GalleryIcons.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                folder.name,
                style = GalleryTheme.type.body,
                color = if (ignored) colors.textMuted else colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (folder.path.isNotEmpty()) {
                Text(
                    folder.path,
                    style = GalleryTheme.type.meta,
                    color = colors.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(formatCount(folder.itemCount), style = GalleryTheme.type.meta, color = colors.textMuted)
    }
}
