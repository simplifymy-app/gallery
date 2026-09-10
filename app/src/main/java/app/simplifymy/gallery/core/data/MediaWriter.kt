package app.simplifymy.gallery.core.data

import android.content.Context
import android.content.IntentSender
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import javax.inject.Inject
import javax.inject.Singleton

/** MediaStore's own trash is the recycle bin: system-managed, auto-purged, no extra permission. */
@Singleton
class MediaWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun trashRequest(media: List<Medium>, trashed: Boolean): IntentSender =
        MediaStore.createTrashRequest(context.contentResolver, media.map(Medium::uri), trashed).intentSender

    fun deleteRequest(media: List<Medium>): IntentSender =
        MediaStore.createDeleteRequest(context.contentResolver, media.map(Medium::uri)).intentSender
}
