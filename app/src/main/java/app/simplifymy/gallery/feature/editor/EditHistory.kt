package app.simplifymy.gallery.feature.editor

class EditHistory(private val limit: Int = 50) {

    private val past = ArrayDeque<PhotoEdits>()
    private val future = ArrayDeque<PhotoEdits>()

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    fun record(before: PhotoEdits) {
        past.addLast(before)
        if (past.size > limit) past.removeFirst()
        future.clear()
    }

    fun undo(current: PhotoEdits): PhotoEdits? = past.removeLastOrNull()?.also { future.addLast(current) }

    fun redo(current: PhotoEdits): PhotoEdits? = future.removeLastOrNull()?.also { past.addLast(current) }
}
