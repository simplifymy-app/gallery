package app.simplifymy.gallery.core.media

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataEditTest {

    @Test
    fun stampsLocalTimeWithItsOffset() {
        val kyiv = ZoneId.of("Europe/Kyiv")
        val taken = ZonedDateTime.of(2024, 7, 14, 18, 5, 9, 0, kyiv).toInstant().toEpochMilli()
        assertEquals("2024:07:14 18:05:09" to "+03:00", exifStamp(taken, kyiv))
    }

    @Test
    fun writesUtcOffsetAsDigits() {
        val taken = ZonedDateTime.of(2023, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("2023:01:02 03:04:05" to "+00:00", exifStamp(taken, ZoneOffset.UTC))
    }
}
