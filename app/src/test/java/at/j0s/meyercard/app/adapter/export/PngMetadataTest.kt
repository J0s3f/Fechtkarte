package at.j0s.meyercard.app.adapter.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Uses `javax.imageio` to produce and re-read a real PNG rather than `android.graphics.Bitmap`
 * (unavailable outside Robolectric) — a JVM-only stand-in for exactly the same file format, so
 * [withPngTextChunks] can be checked against actual PNG bytes, not a hand-typed fixture.
 */
class PngMetadataTest {

    private fun realPng(width: Int = 4, height: Int = 4): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    /** Walks the PNG chunk stream from just after the signature, collecting every `tEXt` chunk's (keyword, text). */
    private fun textChunksOf(png: ByteArray): List<Pair<String, String>> {
        val chunks = mutableListOf<Pair<String, String>>()
        var offset = 8
        while (offset < png.size) {
            val length = ((png[offset].toInt() and 0xFF) shl 24) or ((png[offset + 1].toInt() and 0xFF) shl 16) or
                ((png[offset + 2].toInt() and 0xFF) shl 8) or (png[offset + 3].toInt() and 0xFF)
            val type = String(png, offset + 4, 4, Charsets.US_ASCII)
            if (type == "tEXt") {
                val data = png.copyOfRange(offset + 8, offset + 8 + length)
                val nullIndex = data.indexOf(0)
                chunks += String(data, 0, nullIndex, Charsets.ISO_8859_1) to
                    String(data, nullIndex + 1, data.size - nullIndex - 1, Charsets.ISO_8859_1)
            }
            offset += 4 + 4 + length + 4
            if (type == "IEND") break
        }
        return chunks
    }

    @Test
    @DisplayName("every (keyword, text) pair passed in comes back out, in order")
    fun `every keyword-text pair round trips in order`() {
        val png = realPng().withPngTextChunks("Software" to "Fechtkarte 1.0.8", "Comment" to "ABCDEFGHIJ", "URL" to "https://fechtkarte.j0s.at/")

        assertEquals(
            listOf("Software" to "Fechtkarte 1.0.8", "Comment" to "ABCDEFGHIJ", "URL" to "https://fechtkarte.j0s.at/"),
            textChunksOf(png),
        )
    }

    @Test
    @DisplayName("the result is still a real, readable PNG at the original dimensions")
    fun `result is still a readable PNG at the original size`() {
        val png = realPng(width = 7, height = 5).withPngTextChunks("Software" to "Fechtkarte 1.0.8")

        val decoded = ImageIO.read(png.inputStream())
        assertEquals(7, decoded.width)
        assertEquals(5, decoded.height)
    }

    @Test
    @DisplayName("text chunks are inserted directly after IHDR, before any other chunk")
    fun `text chunks land directly after IHDR`() {
        val plain = realPng()
        val withText = plain.withPngTextChunks("Software" to "Fechtkarte 1.0.8")

        // IHDR ends at signature(8) + length(4) + "IHDR"(4) + data(13) + crc(4) = 33 bytes from
        // the start of the file; the next chunk's own 4-byte length field follows immediately,
        // then its 4-byte type.
        val ihdrEndOffset = 8 + 4 + 4 + 13 + 4
        val firstChunkAfterIhdr = String(withText, ihdrEndOffset + 4, 4, Charsets.US_ASCII)
        assertEquals("tEXt", firstChunkAfterIhdr)
    }

    @Test
    @DisplayName("passing no entries leaves the PNG unchanged")
    fun `no entries leaves the PNG unchanged`() {
        val plain = realPng()
        assertTrue(plain.contentEquals(plain.withPngTextChunks()))
    }

    @Test
    @DisplayName("a keyword longer than 79 characters is rejected")
    fun `an over-length keyword is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            realPng().withPngTextChunks("x".repeat(80) to "text")
        }
    }

    @Test
    @DisplayName("bytes that aren't a PNG are rejected rather than silently corrupted")
    fun `non-PNG bytes are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            "not a png".toByteArray().withPngTextChunks("Software" to "Fechtkarte 1.0.8")
        }
    }
}
