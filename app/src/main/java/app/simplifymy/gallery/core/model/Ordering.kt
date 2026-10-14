package app.simplifymy.gallery.core.model

import androidx.annotation.StringRes
import app.simplifymy.gallery.R

enum class SortKey(@StringRes val labelRes: Int) {
    DateTaken(R.string.sort_date_taken),
    DateModified(R.string.sort_modified),
    Name(R.string.sort_name),
    Size(R.string.sort_size),
    Type(R.string.sort_type),
}

data class Sorting(val key: SortKey = SortKey.DateTaken, val descending: Boolean = true)

enum class Grouping(@StringRes val labelRes: Int) {
    None(R.string.group_none),
    Day(R.string.group_day),
    Month(R.string.group_month),
}

enum class ScrollDirection(@StringRes val labelRes: Int) {
    Down(R.string.scroll_down),
    Up(R.string.scroll_up),
}

enum class MediaFilter(@StringRes val labelRes: Int) {
    All(R.string.filter_all),
    Images(R.string.filter_images),
    Videos(R.string.filter_videos),
    ;

    val types: List<Int>
        get() = when (this) {
            All -> MediaType.entries.map(MediaType::ordinal)
            Images -> listOf(MediaType.Image.ordinal, MediaType.Gif.ordinal)
            Videos -> listOf(MediaType.Video.ordinal)
        }
}

interface Sortable {
    val name: String
    val size: Long
    val dateTaken: Long
    val dateModified: Long

    val typeRank: Int
}

fun <T : Sortable> List<T>.sortedBy(sorting: Sorting): List<T> {
    val ordered = when (sorting.key) {
        SortKey.DateTaken -> sortedBy { it.dateTaken }
        SortKey.DateModified -> sortedBy { it.dateModified }
        SortKey.Name -> sortedBy { it.name.lowercase() }
        SortKey.Size -> sortedBy { it.size }
        SortKey.Type -> sortedWith(compareBy({ it.typeRank }, { it.name.lowercase() }))
    }
    return if (sorting.descending) ordered.asReversed() else ordered
}
