package app.simplifymy.gallery

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore

object MediaSeed {

    const val FOLDER = "SimplifyE2E"

    private val COLLECTION = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    fun insert(
        context: Context,
        names: List<String>,
        tint: Int = Color.rgb(110, 139, 255),
        folder: String = "Pictures/$FOLDER",
        collection: Uri = COLLECTION,
    ) {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(tint) }
        names.forEach { name ->
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(collection, values) ?: return@forEach
            context.contentResolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
        }
        bitmap.recycle()
    }

    fun count(context: Context, nameLike: String, folderLike: String = "%$FOLDER%"): Int =
        context.contentResolver.query(
            COLLECTION,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf(folderLike, nameLike),
            null,
        )?.use { it.count } ?: 0

    fun editedSize(context: Context): Pair<Int, Int> {
        val uri = context.contentResolver.query(
            COLLECTION,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("%$FOLDER%", "%_edited_%"),
            null,
        )?.use { if (it.moveToFirst()) android.content.ContentUris.withAppendedId(COLLECTION, it.getLong(0)) else null }
            ?: return 0 to 0
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
        return options.outWidth to options.outHeight
    }

    fun uriOf(context: Context, name: String): Uri? =
        context.contentResolver.query(
            COLLECTION,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf("%$FOLDER%", name),
            null,
        )?.use { if (it.moveToFirst()) ContentUris.withAppendedId(COLLECTION, it.getLong(0)) else null }

    fun trash(context: Context, name: String) {
        val uri = uriOf(context, name) ?: return
        context.contentResolver.update(
            uri,
            ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 1) },
            null,
            null,
        )
    }

    fun clear(context: Context) {
        context.contentResolver.delete(
            COLLECTION,
            Bundle().apply {
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                )
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("%$FOLDER%", "e2e_%"))
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            },
        )
    }
}
