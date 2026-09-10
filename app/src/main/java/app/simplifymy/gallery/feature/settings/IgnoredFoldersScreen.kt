package app.simplifymy.gallery.feature.settings

import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.database.IgnoredFolderEntity
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IgnoredFoldersViewModel @Inject constructor(
    private val repository: MediaRepository,
) : ViewModel() {

    val folders: StateFlow<List<IgnoredFolderEntity>> =
        repository.ignoredFolders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(bucketId: Long) = viewModelScope.launch { repository.unignoreFolder(bucketId) }
}

@Composable
fun IgnoredFoldersScreen(onBack: () -> Unit, viewModel: IgnoredFoldersViewModel = hiltViewModel()) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(GalleryIcons.Back, onClick = onBack)
            Text(stringResource(R.string.ignored_title), style = GalleryTheme.type.title, color = colors.text)
        }

        if (folders.isEmpty()) {
            Text(
                stringResource(R.string.ignored_empty),
                style = GalleryTheme.type.body,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.base),
            )
            return@Column
        }

        LazyColumn {
            items(folders, key = { it.bucketId }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        folder.name,
                        style = GalleryTheme.type.body,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.ignored_restore),
                        style = GalleryTheme.type.label,
                        color = colors.accent,
                        modifier = Modifier
                            .clickable { viewModel.restore(folder.bucketId) }
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}
