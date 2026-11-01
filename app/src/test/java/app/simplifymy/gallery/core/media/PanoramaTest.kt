package app.simplifymy.gallery.core.media

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PanoramaTest {

    @Test
    fun readsFullSphereFromAttributes() {
        assertEquals(Panorama(), panoramaFromXmp("""<rdf:Description GPano:ProjectionType="equirectangular" GPano:UsePanoramaViewer="True"/>"""))
    }

    @Test
    fun readsCroppedPanoramaFromElements() {
        val xmp = """
            <GPano:ProjectionType>equirectangular</GPano:ProjectionType>
            <GPano:FullPanoWidthPixels>8000</GPano:FullPanoWidthPixels>
            <GPano:FullPanoHeightPixels>4000</GPano:FullPanoHeightPixels>
            <GPano:CroppedAreaImageWidthPixels>4000</GPano:CroppedAreaImageWidthPixels>
            <GPano:CroppedAreaImageHeightPixels>1000</GPano:CroppedAreaImageHeightPixels>
            <GPano:CroppedAreaLeftPixels>2000</GPano:CroppedAreaLeftPixels>
            <GPano:CroppedAreaTopPixels>1500</GPano:CroppedAreaTopPixels>
        """
        assertEquals(Panorama(left = 0.25f, top = 0.375f, width = 0.5f, height = 0.25f), panoramaFromXmp(xmp))
    }

    @Test
    fun ignoresOtherProjections() {
        assertNull(panoramaFromXmp("""GPano:ProjectionType="cylindrical""""))
        assertNull(panoramaFromXmp("""<x:xmpmeta><dc:title>beach</dc:title></x:xmpmeta>"""))
    }

    @Test
    fun findsSphericalBoxInsideMoov() {
        assertTrue(scan(box("ftyp", ByteArray(8)), box("mdat", ByteArray(64)), box("moov", box("trak", box("sv3d", ByteArray(4))))))
        assertTrue(scan(box("moov", box("uuid", "<GSpherical:Spherical>true".toByteArray()))))
    }

    @Test
    fun plainVideoIsFlat() {
        assertFalse(scan(box("ftyp", ByteArray(8)), box("moov", box("trak", ByteArray(32))), box("mdat", "sv3d".toByteArray())))
    }

    private fun box(type: String, body: ByteArray): ByteArray =
        ByteBuffer.allocate(8 + body.size).putInt(8 + body.size).put(type.toByteArray()).put(body).array()

    private fun scan(vararg boxes: ByteArray): Boolean {
        val file = File.createTempFile("panorama", ".mp4").apply { deleteOnExit() }
        file.writeBytes(boxes.reduce(ByteArray::plus))
        return RandomAccessFile(file, "r").use { isSphericalMp4(it.channel) }
    }
}
