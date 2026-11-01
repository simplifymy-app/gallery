package app.simplifymy.gallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellQuoteTest {

    @Test
    fun joinsQuotedPathsOnOneLine() {
        assertEquals(
            "'/storage/emulated/0/DCIM/a.jpg' '/storage/emulated/0/DCIM/b c.mp4'",
            shellQuoted(listOf("/storage/emulated/0/DCIM/a.jpg", "/storage/emulated/0/DCIM/b c.mp4")),
        )
    }

    @Test
    fun escapesApostrophes() {
        assertEquals("'/sdcard/Mom'\\''s party.jpg'", shellQuoted(listOf("/sdcard/Mom's party.jpg")))
    }

    @Test
    fun keepsNonAsciiNames() {
        assertEquals("'/sdcard/Фото/кіт.png'", shellQuoted(listOf("/sdcard/Фото/кіт.png")))
    }

    @Test
    fun emptyListGivesEmptyString() {
        assertEquals("", shellQuoted(emptyList()))
    }
}
