package app.simplifymy.gallery.core.media

import androidx.exifinterface.media.ExifInterface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MetadataEdit(
    val taken: Long? = null,
    val description: String? = null,
    val author: String? = null,
    val removeLocation: Boolean = false,
)

val EDITABLE_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")

private val STAMP = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)
private val OFFSET = DateTimeFormatter.ofPattern("xxx", Locale.US)

private val DATE_TAGS = listOf(
    ExifInterface.TAG_DATETIME_ORIGINAL to ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED to ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
    ExifInterface.TAG_DATETIME to ExifInterface.TAG_OFFSET_TIME,
)

private val GPS_TAGS = listOf(
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_IMG_DIRECTION,
    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_SPEED_REF,
    ExifInterface.TAG_GPS_TRACK,
    ExifInterface.TAG_GPS_TRACK_REF,
)

fun exifStamp(millis: Long, zone: ZoneId): Pair<String, String> {
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    return STAMP.format(time) to OFFSET.format(time)
}

fun ExifInterface.write(edit: MetadataEdit, zone: ZoneId = ZoneId.systemDefault()) {
    edit.taken?.let { taken ->
        val (stamp, offset) = exifStamp(taken, zone)
        DATE_TAGS.forEach { (date, offsetTag) ->
            setAttribute(date, stamp)
            setAttribute(offsetTag, offset)
        }
    }
    edit.description?.let { setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, it.trim().ifEmpty { null }) }
    edit.author?.let { setAttribute(ExifInterface.TAG_ARTIST, it.trim().ifEmpty { null }) }
    if (edit.removeLocation) GPS_TAGS.forEach { setAttribute(it, null) }
}
