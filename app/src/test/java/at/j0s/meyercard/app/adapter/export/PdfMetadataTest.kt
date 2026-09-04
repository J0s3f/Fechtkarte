package at.j0s.meyercard.app.adapter.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [withPdfInfoDictionary] is checked here against a hand-built minimal PDF (the same shape a
 * one-page document from `android.graphics.pdf.PdfDocument` has: a Catalog, a Pages tree, one
 * Page, and a trailer with `/Root`/`/Size` and no `/Info`) rather than a real Android-rendered
 * one — Robolectric ships no shadow for `PdfDocument` (see `MediaStoreCardExporterTest`'s own
 * doc comment), and this function's own logic only ever looks at the trailer/xref/Catalog bytes,
 * not the page content, so a minimal fixture exercises exactly what it reads. Independently
 * verified against a real PDF with `exiftool` during development — see [withPdfInfoDictionary].
 */
class PdfMetadataTest {

    private fun testInfo() = PdfInfo(title = "t", author = "a", subject = "s", creator = "c")
    private fun testRights() = XmpRights(
        marked = false,
        rightsStatement = "CC0 1.0 Universal — no rights reserved",
        webStatementUrl = "https://creativecommons.org/publicdomain/zero/1.0/",
        usageTerms = "No rights reserved (CC0 1.0 Universal). See the linked deed for details.",
    )

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
        val result = original.withPdfInfoDictionary(testInfo(), testRights())
        assertTrue(result.copyOfRange(0, original.size).contentEquals(original))
    }

    @Test
    @DisplayName("the appended Info object carries the exact title, author, subject and creator given")
    fun `appended object carries the given fields`() {
        val info = PdfInfo(title = "Fechtkarte card ABCDEFGHIJ", author = "Fechtkarte 1.0.9", subject = "https://fechtkarte.j0s.at/", creator = "Fechtkarte 1.0.9")
        val result = String(minimalPdf().withPdfInfoDictionary(info, testRights()), Charsets.ISO_8859_1)

        assertTrue(result.contains("/Title (Fechtkarte card ABCDEFGHIJ)"))
        assertTrue(result.contains("/Author (Fechtkarte 1.0.9)"))
        assertTrue(result.contains("/Subject (https://fechtkarte.j0s.at/)"))
        assertTrue(result.contains("/Creator (Fechtkarte 1.0.9)"))
        assertTrue(result.contains("/Producer (Fechtkarte 1.0.9)"))
    }

    @Test
    @DisplayName("backslashes and parentheses in a field are escaped, not left to break the literal string")
    fun `parentheses and backslashes are escaped`() {
        val info = PdfInfo(title = "a (b) c \\ d", author = "x", subject = "y", creator = "z")
        val result = String(minimalPdf().withPdfInfoDictionary(info, testRights()), Charsets.ISO_8859_1)

        assertTrue(result.contains("""/Title (a \(b\) c \\ d)"""), "expected escaped title in: $result")
    }

    @Test
    @DisplayName("the new trailer's /Root matches the original, /Size grows by two (Info + Metadata), and /Prev points at the original xref")
    fun `new trailer carries over Root, grows Size by two, and chains Prev to the original xref`() {
        val original = minimalPdf(objectCount = 3)
        val originalStartxref = Regex("""startxref\s+(\d+)""").findAll(String(original, Charsets.ISO_8859_1)).last().groupValues[1].toInt()

        val result = String(original.withPdfInfoDictionary(testInfo(), testRights()), Charsets.ISO_8859_1)
        val newTrailer = Regex("""trailer\s*<<(.*?)>>""", RegexOption.DOT_MATCHES_ALL).findAll(result).last().groupValues[1]

        assertTrue(newTrailer.contains("/Root 1 0 R"))
        // 3 real objects (1..3) means the original /Size was 4 (the next free object number);
        // the new Info object takes that free number 4, the new Metadata stream takes 5, so
        // /Size grows to 6.
        assertTrue(newTrailer.contains("/Size 6"), "expected /Size 6, got: $newTrailer")
        assertTrue(newTrailer.contains("/Prev $originalStartxref"))
        assertTrue(newTrailer.contains("/Info 4 0 R"))
    }

    @Test
    @DisplayName("each new xref entry's byte offset actually points at that object's own \"N 0 obj\" line")
    fun `new xref entries point at their own objects`() {
        val result = minimalPdf().withPdfInfoDictionary(testInfo(), testRights())
        val text = String(result, Charsets.ISO_8859_1)

        val newXrefOffset = Regex("""startxref\s+(\d+)""").findAll(text).last().groupValues[1].toInt()
        val xrefSection = text.substring(newXrefOffset)

        // Three objects touched by this update: the new Info object (4), the new Metadata
        // stream (5), and the rewritten Catalog (1, reusing its existing number) — each gets
        // its own "N 1\n<offset> ..." subsection in the xref, per the incremental-update format.
        for (objectNumber in listOf(1, 4, 5)) {
            val subsection = Regex("""$objectNumber 1\n(\d{10}) \d{5} n""").find(xrefSection)
                ?: error("no xref subsection for object $objectNumber in: $xrefSection")
            val objectOffset = subsection.groupValues[1].toInt()
            assertEquals("$objectNumber 0 obj", text.substring(objectOffset, objectOffset + "$objectNumber 0 obj".length))
        }
    }

    @Test
    @DisplayName("when the trailer already names an /Info object, its identity is reused and /Size grows by only one (the Metadata stream)")
    fun `an existing Info object's identity is reused`() {
        val result = String(minimalPdfWithExistingInfo().withPdfInfoDictionary(testInfo(), testRights()), Charsets.ISO_8859_1)
        val newTrailer = Regex("""trailer\s*<<(.*?)>>""", RegexOption.DOT_MATCHES_ALL).findAll(result).last().groupValues[1]

        // /Size grows from 5 to 6 — object 4 (the existing Info object) is given a new
        // definition rather than a new object allocated, the same way a real PDF editor amends
        // a dictionary that's already there instead of leaving the old one orphaned; only the
        // Metadata stream (new, PdfDocument never writes one) takes a fresh number.
        assertTrue(newTrailer.contains("/Size 6"), "expected /Size 6 (one new object, the Metadata stream), got: $newTrailer")
        assertTrue(newTrailer.contains("/Info 4 0 R"))
        assertTrue(result.contains("4 0 obj\n<< /Title (t)"), "expected the new definition to reuse object 4, got: $result")
    }

    @Test
    @DisplayName("the Root/Catalog object keeps its existing content and gains a /Metadata reference to the new stream object")
    fun `Catalog gains a Metadata reference without losing its existing content`() {
        val result = String(minimalPdf().withPdfInfoDictionary(testInfo(), testRights()), Charsets.ISO_8859_1)

        // Object 1 is the Catalog here (/Root 1 0 R); its original content (/Type /Test1) must
        // survive into the rewritten object, alongside the new /Metadata reference — this is a
        // full replacement of the object, not a smaller in-place edit, so losing the original
        // content would be silent data loss no test of the Info/xref logic alone would catch.
        val rewrittenCatalog = Regex("""1 0 obj\s*<<(.*?)>>\s*endobj""", RegexOption.DOT_MATCHES_ALL)
            .findAll(result).last().groupValues[1]
        assertTrue(rewrittenCatalog.contains("/Type /Test1"), "expected original Catalog content preserved, got: $rewrittenCatalog")
        assertTrue(rewrittenCatalog.contains("/Metadata 5 0 R"), "expected a /Metadata reference to the new stream object, got: $rewrittenCatalog")
    }

    @Test
    @DisplayName("the Metadata stream object is a well-formed XML XMP stream carrying the given rights fields")
    fun `Metadata stream carries the given rights fields`() {
        val rights = XmpRights(
            marked = false,
            rightsStatement = "CC0 1.0 Universal — no rights reserved",
            webStatementUrl = "https://creativecommons.org/publicdomain/zero/1.0/",
            usageTerms = "No rights reserved (CC0 1.0 Universal).",
        )
        val result = minimalPdf().withPdfInfoDictionary(testInfo(), rights)
        // The dict header is pure ASCII, safe to locate via a Latin-1 decode; the stream content
        // itself is UTF-8 (the rights statement's em dash is multi-byte) and must be sliced from
        // the *original bytes* by the declared /Length, not re-derived from a Latin-1 string —
        // decoding non-ASCII UTF-8 as Latin-1 first would corrupt it before it's ever measured.
        val text = String(result, Charsets.ISO_8859_1)
        val header = Regex("""5 0 obj\s*<< /Type /Metadata /Subtype /XML /Length (\d+) >>\nstream\n""")
            .find(text) ?: error("Metadata stream object 5 not found in: $text")
        val length = header.groupValues[1].toInt()
        val streamStart = header.range.last + 1
        val xmp = String(result, streamStart, length, Charsets.UTF_8)

        assertEquals(length, xmp.toByteArray(Charsets.UTF_8).size, "declared /Length should match the actual stream byte count")
        assertTrue(
            text.startsWith("\nendstream\nendobj\n", streamStart + length),
            "expected \"endstream\" immediately after the declared /Length worth of bytes",
        )

        assertTrue(xmp.contains("<xmpRights:Marked>False</xmpRights:Marked>"))
        assertTrue(xmp.contains("<xmpRights:WebStatement>https://creativecommons.org/publicdomain/zero/1.0/</xmpRights:WebStatement>"))
        assertTrue(xmp.contains("CC0 1.0 Universal — no rights reserved"), "expected the rights statement in dc:rights, got: $xmp")
        assertTrue(xmp.contains("No rights reserved (CC0 1.0 Universal)."), "expected the usage terms, got: $xmp")
    }

    @Test
    @DisplayName("the result ends with %%EOF")
    fun `result ends with the EOF marker`() {
        val result = String(minimalPdf().withPdfInfoDictionary(testInfo(), testRights()), Charsets.ISO_8859_1)
        assertTrue(result.endsWith("%%EOF"))
    }

    /** [minimalPdf] with [replacement] substituted for [target] in its trailer/xref tail — a malformed variant for the guard tests below. */
    private fun minimalPdf(target: String, replacement: String): ByteArray {
        val text = String(minimalPdf(), Charsets.ISO_8859_1)
        require(text.contains(target)) { "'$target' not found in the fixture to mutate" }
        return text.replace(target, replacement).toByteArray(Charsets.ISO_8859_1)
    }

    @Test
    @DisplayName("a PDF with no startxref is rejected rather than silently miscoded")
    fun `missing startxref is rejected`() {
        val malformed = minimalPdf("startxref", "xxxxxxxxx")
        assertThrows(IllegalStateException::class.java) { malformed.withPdfInfoDictionary(testInfo(), testRights()) }
    }

    @Test
    @DisplayName("a PDF with no trailer dictionary is rejected rather than silently miscoded")
    fun `missing trailer is rejected`() {
        val malformed = minimalPdf("trailer\n<<", "xxxxxxx\n<<")
        assertThrows(IllegalStateException::class.java) { malformed.withPdfInfoDictionary(testInfo(), testRights()) }
    }

    @Test
    @DisplayName("a trailer with no /Root is rejected rather than silently miscoded")
    fun `missing Root in trailer is rejected`() {
        val malformed = minimalPdf("/Root 1 0 R", "")
        assertThrows(IllegalStateException::class.java) { malformed.withPdfInfoDictionary(testInfo(), testRights()) }
    }

    @Test
    @DisplayName("a trailer with no /Size is rejected rather than silently miscoded")
    fun `missing Size in trailer is rejected`() {
        val malformed = minimalPdf("/Size 4 ", "")
        assertThrows(IllegalStateException::class.java) { malformed.withPdfInfoDictionary(testInfo(), testRights()) }
    }

    @Test
    @DisplayName("a /Root pointing at an object that doesn't exist is rejected rather than silently miscoded")
    fun `Root pointing at a missing object is rejected`() {
        val malformed = minimalPdf("/Root 1 0 R", "/Root 99 0 R")
        assertThrows(IllegalStateException::class.java) { malformed.withPdfInfoDictionary(testInfo(), testRights()) }
    }
}
