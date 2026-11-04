package app.simplifymy.gallery.core.model

import androidx.annotation.StringRes
import app.simplifymy.gallery.R

enum class BarAction(@StringRes val labelRes: Int) {
    Share(R.string.action_share),
    Favorite(R.string.action_favorite),
    Edit(R.string.action_edit),
    SetAs(R.string.action_set_as),
    Copy(R.string.action_copy_to),
    Move(R.string.action_move_to),
    Rename(R.string.action_rename),
    CopyPath(R.string.action_copy_path),
    Metadata(R.string.action_metadata),
    Delete(R.string.action_delete),
}

data class BarLayout(
    val bar: List<BarAction>,
    val menu: List<BarAction>,
    val hidden: Set<BarAction> = emptySet(),
) {

    val all: List<BarAction> get() = bar + menu

    fun encode(): String = listOf(bar, menu, hidden).joinToString("|") { it.joinToString(",") }

    fun shown(action: BarAction, on: Boolean): BarLayout = copy(hidden = if (on) hidden - action else hidden + action)

    fun only(available: (BarAction) -> Boolean): BarLayout {
        val keep = { action: BarAction -> action !in hidden && available(action) }
        return BarLayout(bar.filter(keep), menu.filter(keep))
    }

    fun fitting(slots: Int): BarLayout {
        val room = slots.coerceAtLeast(1)
        val needed = if (menu.isEmpty()) bar.size else bar.size + 1
        if (needed <= room) return this
        val shown = (room - 1).coerceAtLeast(0)
        return BarLayout(bar.take(shown), bar.drop(shown) + menu, hidden)
    }

    companion object {
        val Selection = BarLayout(
            bar = listOf(BarAction.Share, BarAction.Favorite, BarAction.Copy, BarAction.Delete),
            menu = listOf(BarAction.Move, BarAction.Rename, BarAction.CopyPath),
            hidden = setOf(BarAction.CopyPath),
        )
        val Viewer = BarLayout(
            bar = listOf(BarAction.Share, BarAction.Edit, BarAction.SetAs, BarAction.Delete),
            menu = listOf(BarAction.Copy, BarAction.Move, BarAction.Rename, BarAction.Metadata),
        )

        fun decode(raw: String?, fallback: BarLayout): BarLayout {
            if (raw == null) return fallback
            val supported = fallback.all.toSet()
            val parts = raw.split("|")
            fun parse(index: Int) = parts.getOrNull(index).orEmpty().split(",")
                .mapNotNull { name -> BarAction.entries.firstOrNull { it.name == name } }
                .filter { it in supported }
                .distinct()
            val bar = parse(0)
            val menu = parse(1).filterNot { it in bar }
            val placed = (bar + menu).toSet()
            val missing = fallback.all.filterNot { it in placed }
            val hidden = parse(2).toSet() + missing.filter { it in fallback.hidden }
            return BarLayout(bar, menu + missing, hidden)
        }
    }
}
