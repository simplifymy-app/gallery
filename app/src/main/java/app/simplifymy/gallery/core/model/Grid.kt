package app.simplifymy.gallery.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class HeaderKind { Today, Yesterday, Day, Month }

sealed interface GridItem {
    /** Carries the date, not a formatted label, so the text can follow the app's language. */
    data class Header(val date: LocalDate, val kind: HeaderKind, val count: Int) : GridItem
    data class Item(val medium: Medium) : GridItem
}

fun List<Medium>.grouped(grouping: Grouping, sorting: Sorting): List<GridItem> {
    if (grouping == Grouping.None) return map(GridItem::Item)

    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val stamp = { m: Medium -> if (sorting.key == SortKey.DateModified) m.dateModified else m.dateTaken }

    return groupBy { LocalDate.ofInstant(Instant.ofEpochMilli(stamp(it)), zone).truncate(grouping) }
        .flatMap { (date, items) ->
            buildList {
                add(GridItem.Header(date, date.kind(today, grouping), items.size))
                items.forEach { add(GridItem.Item(it)) }
            }
        }
}

private fun LocalDate.truncate(grouping: Grouping) =
    if (grouping == Grouping.Month) withDayOfMonth(1) else this

private fun LocalDate.kind(today: LocalDate, grouping: Grouping): HeaderKind = when {
    grouping == Grouping.Month -> HeaderKind.Month
    this == today -> HeaderKind.Today
    this == today.minusDays(1) -> HeaderKind.Yesterday
    else -> HeaderKind.Day
}
