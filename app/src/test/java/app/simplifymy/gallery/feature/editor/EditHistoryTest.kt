package app.simplifymy.gallery.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditHistoryTest {

    private val first = PhotoEdits()
    private val second = first.with(Adjustment.Exposure, 10)
    private val third = second.with(Adjustment.Contrast, 20)

    @Test
    fun undoAndRedoWalkTheHistory() {
        val history = EditHistory()
        history.record(first)
        history.record(second)
        assertEquals(second, history.undo(third))
        assertEquals(first, history.undo(second))
        assertFalse(history.canUndo)
        assertEquals(second, history.redo(first))
        assertEquals(third, history.redo(second))
        assertFalse(history.canRedo)
    }

    @Test
    fun aNewEditClearsRedo() {
        val history = EditHistory()
        history.record(first)
        history.undo(second)
        assertTrue(history.canRedo)
        history.record(first)
        assertFalse(history.canRedo)
    }

    @Test
    fun historyIsCapped() {
        val history = EditHistory(limit = 3)
        repeat(5) { history.record(PhotoEdits().with(Adjustment.Exposure, it)) }
        var undone = 0
        var current = PhotoEdits()
        while (true) current = history.undo(current)?.also { undone++ } ?: break
        assertEquals(3, undone)
        assertNull(history.undo(current))
    }
}
