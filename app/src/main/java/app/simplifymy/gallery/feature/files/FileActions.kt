package app.simplifymy.gallery.feature.files

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.gallery.R
import app.simplifymy.gallery.core.model.FolderEntry
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.GallerySheet
import app.simplifymy.gallery.ui.component.TextDialog
import app.simplifymy.gallery.ui.formatCount
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing

enum class FileAction(@StringRes val labelRes: Int, @StringRes val failedRes: Int) {
    Copy(R.string.action_copy_to, R.string.files_copy_failed),
    Move(R.string.action_move_to, R.string.files_move_failed),
    Rename(R.string.action_rename, R.string.files_rename_failed),
}

@Composable
fun FileActions(
    action: FileAction?,
    media: List<Medium>,
    onDone: (FileAction) -> Unit,
    onDismiss: () -> Unit,
    viewModel: FileActionsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var busy by remember(action) { mutableStateOf(false) }
    var naming by remember(action) { mutableStateOf(action == FileAction.Rename) }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) pending?.invoke() else onDismiss()
        pending = null
    }

    if (action == null || media.isEmpty() || busy) return

    val finish: (Boolean) -> Unit = { ok ->
        if (ok) onDone(action)
        else {
            Toast.makeText(context, action.failedRes, Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    fun withConsent(work: () -> Unit) {
        busy = true
        pending = work
        consent.launch(IntentSenderRequest.Builder(viewModel.writeRequest(media)).build())
    }

    fun commit(folder: String) {
        if (action == FileAction.Copy) {
            busy = true
            viewModel.copy(media, folder, finish)
        } else {
            withConsent { viewModel.move(media, folder, finish) }
        }
    }

    when {
        action == FileAction.Rename -> {
            val medium = media.first()
            val extension = medium.name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
            val stem = medium.name.removeSuffix(extension)
            TextDialog(
                title = stringResource(R.string.action_rename),
                initial = stem,
                isValid = { isValidName(it) && it.trim() != stem },
                onConfirm = { stem -> withConsent { viewModel.rename(medium, stem.trim() + extension, finish) } },
                onDismiss = onDismiss,
            )
        }
        naming -> TextDialog(
            title = stringResource(R.string.files_new_folder),
            initial = "",
            isValid = ::isValidName,
            onConfirm = { commit(newFolderPath(it)) },
            onDismiss = { naming = false },
        )
        else -> FolderSheet(
            title = stringResource(action.labelRes),
            folders = destinationsFor(media, folders),
            onPick = { commit(it.path) },
            onNewFolder = { naming = true },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun FolderSheet(
    title: String,
    folders: List<FolderEntry>,
    onPick: (FolderEntry) -> Unit,
    onNewFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    GallerySheet(title, onDismiss) {
        LazyColumn(Modifier.padding(bottom = Spacing.base)) {
            item {
                SheetRow(icon = GalleryIcons.Add, title = stringResource(R.string.files_new_folder), onClick = onNewFolder)
            }
            items(folders, key = FolderEntry::bucketId) { folder ->
                SheetRow(
                    title = folder.name,
                    subtitle = folder.path.removeSuffix("/"),
                    trailing = formatCount(folder.itemCount),
                    onClick = { onPick(folder) },
                )
            }
        }
    }
}

@Composable
private fun SheetRow(
    title: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null,
    trailing: String? = null,
) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = GalleryTheme.type.body, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = GalleryTheme.type.meta, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) Text(trailing, style = GalleryTheme.type.meta, color = colors.textFaint)
    }
}

