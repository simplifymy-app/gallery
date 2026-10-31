package app.simplifymy.gallery.feature.files

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import app.simplifymy.gallery.core.data.MediaRepository
import app.simplifymy.gallery.core.data.MediaWriter
import app.simplifymy.gallery.core.model.FolderEntry
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileActionsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val writer: MediaWriter,
) : ViewModel() {

    val folders: StateFlow<List<FolderEntry>> =
        repository.folderEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun writeRequest(media: List<Medium>): IntentSender = writer.writeRequest(media)

    fun copy(media: List<Medium>, folder: String, onDone: (Boolean) -> Unit) =
        perform(onDone) { repository.copy(media, folder) == media.size }

    fun move(media: List<Medium>, folder: String, onDone: (Boolean) -> Unit) =
        perform(onDone) { repository.move(media, folder) == media.size }

    fun rename(medium: Medium, name: String, onDone: (Boolean) -> Unit) =
        perform(onDone) { repository.rename(medium, name) }

    private fun perform(onDone: (Boolean) -> Unit, work: suspend () -> Boolean) =
        viewModelScope.launch { onDone(work()) }
}

fun destinationsFor(media: List<Medium>, folders: List<FolderEntry>): List<FolderEntry> =
    folders.filter { folder ->
        folder.path.isNotEmpty() &&
            media.any { it.bucketId != folder.bucketId } &&
            media.all { folder.path.substringBefore('/') in rootsFor(it.type) }
    }

fun newFolderPath(name: String) = "Pictures/${name.trim()}/"

fun isValidName(name: String) = name.isNotBlank() && name.none { it in "/\\:*?\"<>|" } && name.trim() !in setOf(".", "..")

private fun rootsFor(type: MediaType) =
    if (type == MediaType.Video) setOf("DCIM", "Movies", "Pictures") else setOf("DCIM", "Pictures")
