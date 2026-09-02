package at.j0s.meyercard.app.adapter.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [withPdfInfoDictionary] is checked here against a hand-built minimal PDF (the same shape a
 * one-page document from `android.graphics.pdf.PdfDocument` has: a Catalog, a Pages tree, one
 * Page, and a trailer with `/Root`/`/Size` and no `/Info`) rather than a real Android-rendered
 * one — Robolectric ships no shadow for `PdfDocument` (see `MediaStoreCardExporterTest`'s own
 * doc comment), and this function's own logic only ever looks at the trailer/xref bytes, not
 * the page content, so a minimal fixture exercises exactly what it reads. Independently
 * verified against a real PDF with `exiftool` during development — see [withPdfInfoDictionary].
 */
class PdfMetadataTest {

    /** [objectCount] objects, each an empty dictionary — enough structure for a trailer/xref without needing real page content. */
    private fun minimalPdf(objectCount: Int = 3): ByteArray {
        val header = "%PDF-1.4\n"
        val bodies = (1..objectCount).map { n -> "$n 0 obj\n<< /Type /Test$n >>\nendobj\n" }

        var offset = header.toByteArray(Charsets.ISO_8859_1).size
        val offsets = mutableListOf<Int>()
        for (body in bodies) {
            offsets += offset
            offset += body.toByteArray(Charsets.ISO_8859_1).size
        }

        val size = objectCount + 1
        val xrefOffset = offset
        val xref = buildString {
            append("xref\n0 $size\n")
            append("0000000000 65535 f\r\n")
            for (objOffset in offsets) append("${"%010d".format(objOffset)} 00000 n\r\n")
        }
        val trailer = "trailer\n<< /Size $size /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF"

        return (header + bodies.joinToString("") + xref + trailer).toByteArray(Charsets.ISO_8859_1)
    }

    /**
     * Shaped like what `android.graphics.pdf.PdfDocument` (Skia) actually produces on a real
     * device: a trailer that *already* names an `/Info` object — confirmed by the "[Minor]
     * Ignored duplicate Info dictionary" warning a real PDF viewer raised against the first
     * version of [withPdfInfoDictionary], which always allocated a fresh object number instead
     * of checking for one already there.
     */
    private fun minimalPdfWithExistingInfo(): ByteArray {
        val header = "%PDF-1.4\n"
        val bodies = (1..3).map { n -> "$n 0 obj\n<< /Type /Test$n >>\nendobj\n" } +
            "4 0 obj\n<< /Producer (Skia/PDF) >>\nendobj\n"

        var offset = header.toByteArray(Charsets.ISO_8859_1).size
        val offsets = mutableListOf<Int>()
        for (body in bodies) {
            offsets += offset
            offset += body.toByteArray(Charsets.ISO_8859_1).size
        }

        val size = 5
        val xrefOffset = offset
        val xref = buildString {
            append("xref\n0 $size\n")
            append("0000000000 65535 f\r\n")
            for (objOffset in offsets) append("${"%010d".format(objOffset)} 00000 n\r\n")
        }
        val trailer = "trailer\n<< /Size $size /Root 1 0 R /Info 4 0 R >>\nstartxref\n$xrefOffset\n%%EOF"

        return (header + bodies.joinToString("") + xref + trailer).toByteArray(Charsets.ISO_8859_1)
    }

    @Test
    @DisplayName("the original bytes are an untouched prefix of the result")
    fun `original bytes are preserved as a prefix`() {
        val original = minimalPdf()
        val result = original.withPdfInfoDictionary(PdfInfo("t", "a", "s", "c"))
        assertTrue(result.copyOfRange(0, original.size).contentEquals(original))
    }

    @Test
    @DisplayName("the appended Info object carries the exact title, author, subject and creator given")
    fun `appended object carries the given fields`() {
        val info = PdfInfo(title = "Fechtkarte card ABCDEFGHIJ", author = "Fechtkarte 1.0.8", subject = "https://fechtkarte.j0s.at/", creator = "Fechtkarte 1.0.8")
        val result = String(minimalPdf().withPdfInfoDictionary(info), Charsets.ISO_8859_1)

        assertTrue(result.contains("/Title (Fechtkarte card ABCDEFGHIJ)"))
        assertTrue(result.contains("/Author (Fechtkarte 1.0.8)"))
        assertTrue(result.contains("/Subject (https://fechtkarte.j0s.at/)"))
        assertTrue(result.contains("/Creator (Fechtkarte 1.0.8)"))
        assertTrue(result.contains("/Producer (Fechtkarte 1.0.8)"))
    }

    @Test
    @DisplayName("backslashes and parentheses in a field are escaped, not left to break the literal string")
    fun `parentheses and backslashes are escaped`() {
        val info = PdfInfo(title = "a (b) c \\ d", author = "x", subject = "y", creator = "z")
        val result = String(minimalPdf().withPdfInfoDictionary(info), Charsets.ISO_8859_1)

        assertTrue(result.contains("""/Title (a \(b\) c \\ d)"""), "expected escaped title in: $result")
    }

    @Test
    @DisplayName("the new trailer's /Root matches the original, /Size grows by exactly one, and /Prev points at the original xref")
    fun `new trailer carries over Root, grows Size by one, and chains Prev to the original xref`() {
        val original = minimalPdf(objectCount = 3)
        val originalStartxref = Regex("""startxref\s+(\d+)""").findAll(String(original, Charsets.ISO_8859_1)).last().groupValues[1].toInt()

        val result = String(original.withPdfInfoDictionary(PdfInfo("t", "a", "s", "c")), Charsets.ISO_8859_1)
        val newTrailer = Regex("""trailer\s*<<(.*?)>>""", RegexOption.DOT_MATCHES_ALL).findAll(result).last().groupValues[1]

        assertTrue(newTrailer.contains("/Root 1 0 R"))
        // 3 real objects (1..3) means the original /Size was 4 (the next free object number);
        // the new Info object takes that free number 4, so /Size grows to 5.
        assertTrue(newTrailer.contains("/Size 5"))
        assertTrue(newTrailer.contains("/Prev $originalStartxref"))
        assertTrue(newTrailer.contains("/Info 4 0 R"))
    }

    @Test
    @DisplayName("the new xref entry's byte offset actually points at the new object's own \"N 0 obj\" line")
    fun `new xref entry points at the new object`() {
        val result = minimalPdf().withPdfInfoDictionary(PdfInfo("t", "a", "s", "c"))
        val text = String(result, Charsets.ISO_8859_1)

        val newXrefOffset = Regex("""startxref\s+(\d+)""").findAll(text).last().groupValues[1].toInt()
        val xrefSection = text.substring(newXrefOffset)
        val objectOffset = Regex("""(\d{10}) 00000 n""").find(xrefSection)!!.groupValues[1].toInt()

        assertEquals("4 0 obj", text.substring(objectOffset, objectOffset + "4 0 obj".length))
    }

    @Test
    @DisplayName("when the trailer already names an /Info object, its identity is reused rather than a new object allocated")
    fun `an existing Info object's identity is reused`() {
        val result = String(minimalPdfWithExistingInfo().withPdfInfoDictionary(PdfInfo("t", "a", "s", "c")), Charsets.ISO_8859_1)
        val newTrailer = Regex("""trailer\s*<<(.*?)>>""", RegexOption.DOT_MATCHES_ALL).findAll(result).last().groupValues[1]

        // /Size stays at 5 — no new object was allocated, only object 4 (the existing Info
        // object) was given a new definition, the same way a real PDF editor amends a
        // dictionary that's already there instead of leaving the old one orphaned.
        assertTrue(newTrailer.contains("/Size 5"), "expected /Size to stay 5 (no new object), got: $newTrailer")
        assertTrue(newTrailer.contains("/Info 4 0 R"))
        assertTrue(result.contains("4 0 obj\n<< /Title (t)"), "expected the new definition to reuse object 4, got: $result")
    }

    @Test
    @DisplayName("the result ends with %%EOF")
    fun `result ends with the EOF marker`() {
        val result = String(minimalPdf().withPdfInfoDictionary(PdfInfo("t", "a", "s", "c")), Charsets.ISO_8859_1)
        assertTrue(result.endsWith("%%EOF"))
    }
}
