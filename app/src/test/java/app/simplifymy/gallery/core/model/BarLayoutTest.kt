package app.simplifymy.gallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BarLayoutTest {

    @Test
    fun roundTripsThroughEncoding() {
        val layout = BarLayout(
            bar = listOf(BarAction.Move, BarAction.Rename),
            menu = listOf(BarAction.Share, BarAction.Favorite, BarAction.Copy, BarAction.Delete, BarAction.CopyPath),
            hidden = setOf(BarAction.Favorite),
        )
        assertEquals(layout, BarLayout.decode(layout.encode(), BarLayout.Selection))
    }

    @Test
    fun missingStoredValueGivesFallback() {
        assertEquals(BarLayout.Selection, BarLayout.decode(null, BarLayout.Selection))
    }

    @Test
    fun dropsUnknownAndUnsupportedActionsAndAppendsMissingOnesToMenu() {
        val decoded = BarLayout.decode("Move,Bogus,Metadata|Move,Delete", BarLayout.Selection)
        assertEquals(listOf(BarAction.Move), decoded.bar)
        assertEquals(
            listOf(BarAction.Delete, BarAction.Share, BarAction.Favorite, BarAction.Copy, BarAction.Rename, BarAction.CopyPath),
            decoded.menu,
        )
    }

    @Test
    fun keepsLayoutThatFits() {
        assertEquals(BarLayout.Selection, BarLayout.Selection.fitting(5))
    }

    @Test
    fun overflowsTailOfBarIntoMenuWhenOutOfRoom() {
        val fitted = BarLayout.Selection.fitting(3)
        assertEquals(listOf(BarAction.Share, BarAction.Favorite), fitted.bar)
        assertEquals(
            listOf(BarAction.Copy, BarAction.Delete, BarAction.Move, BarAction.Rename, BarAction.CopyPath),
            fitted.menu,
        )
    }

    @Test
    fun copyPathIsHiddenByDefault() {
        val visible = BarLayout.Selection.only { true }
        assertEquals(listOf(BarAction.Move, BarAction.Rename), visible.menu)
    }

    @Test
    fun hiddenActionsDisappearFromBarAndMenu() {
        val visible = BarLayout.Viewer.shown(BarAction.Edit, false).shown(BarAction.Metadata, false).only { true }
        assertEquals(listOf(BarAction.Share, BarAction.SetAs, BarAction.Delete), visible.bar)
        assertEquals(listOf(BarAction.Copy, BarAction.Move, BarAction.Rename), visible.menu)
    }

    @Test
    fun hiddenActionMissingFromStoredValueStaysHidden() {
        assertEquals(setOf(BarAction.CopyPath), BarLayout.decode("Share|Move", BarLayout.Selection).hidden)
    }

    @Test
    fun barWithoutMenuUsesEverySlot() {
        val layout = BarLayout(listOf(BarAction.Share, BarAction.Delete), emptyList())
        assertEquals(layout, layout.fitting(2))
    }

    @Test
    fun filtersUnavailableActions() {
        val single = BarLayout.Selection.shown(BarAction.CopyPath, true).only { it != BarAction.Rename }
        assertEquals(listOf(BarAction.Move, BarAction.CopyPath), single.menu)
    }
}
