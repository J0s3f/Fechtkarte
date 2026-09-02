package at.j0s.meyercard.app.adapter.export

/**
 * [android.graphics.pdf.PdfDocument] has no API to set Title/Author/Subject/Creator — nothing
 * on `PdfDocument`, `PdfDocument.PageInfo` or `PdfDocument.Page` touches the Document Info
 * dictionary at all. Rather than write a PDF from scratch to get three text fields, this patches
 * a finished PDF's bytes afterward, the standard way a PDF is amended without rewriting it: an
 * *incremental update* (ISO 32000-1 §7.5.6) — append one new object (the Info dictionary), a new
 * cross-reference section covering just that object, and a new trailer whose `/Prev` points at
 * the original cross-reference table. A compliant reader follows the trailer chain and sees the
 * new `/Info` as if it had always been there; the original bytes are never touched, so nothing
 * about the rendered page itself is at risk from this.
 *
 * If the original trailer already names an `/Info` object — confirmed on a real device:
 * `PdfDocument`'s own Skia backend already writes one, just a bare `/Producer` — that object's
 * identity is *reused* rather than a new object number allocated. Allocating a new number would
 * leave two distinct objects both claiming to be "the" Info dictionary reachable by following
 * `/Prev` through both trailers (this app's new one, and the original's); at least one real PDF
 * reader flagged that as "[Minor] Ignored duplicate Info dictionary" on a real export. Reusing
 * the existing object number means every trailer in the chain agrees on one Info identity — the
 * newest definition of it simply wins, which is the whole point of a `/Prev` chain — the same
 * way a real editor amends an existing dictionary instead of leaving the old one as orphaned,
 * still-referenced-looking bytes.
 *
 * Verified against real PDFs with `exiftool` during development (not shipped as a test — no
 * PDF-parsing library lives here to assert against; the byte-level tests instead check that this
 * function's *output structure* is well-formed, which is what a JVM test without a PDF reader
 * actually can check) — both a trailer with no pre-existing `/Info` and one shaped like
 * `PdfDocument`'s own output (trailer already naming an `/Info` object).
 */
internal fun ByteArray.withPdfInfoDictionary(info: PdfInfo): ByteArray {
    val text = toString(Charsets.ISO_8859_1)

    val prevXrefOffset = STARTXREF.findAll(text).lastOrNull()?.groupValues?.get(1)?.toInt()
        ?: error("No startxref found — not a well-formed PDF")
    val trailerDictionary = TRAILER.findAll(text).lastOrNull()?.groupValues?.get(1)
        ?: error("No trailer dictionary found — not a well-formed PDF")
    val rootRef = ROOT_REF.find(trailerDictionary)?.groupValues?.get(1)
        ?: error("Trailer has no /Root — not a well-formed PDF")
    val size = TRAILER_SIZE.find(trailerDictionary)?.groupValues?.get(1)?.toInt()
        ?: error("Trailer has no /Size — not a well-formed PDF")

    val existingInfoRef = INFO_REF.find(trailerDictionary)
    // Reuse the existing /Info object's number/generation when there is one; otherwise a fresh,
    // currently-unused number — valid object numbers in a PDF whose trailer declares /Size N
    // run 1..N-1, so N itself is guaranteed free without inspecting every object in the file.
    val objectNumber = existingInfoRef?.groupValues?.get(1)?.toInt() ?: size
    val objectGeneration = existingInfoRef?.groupValues?.get(2)?.toInt() ?: 0
    val newSize = if (existingInfoRef != null) size else size + 1

    val infoDictionary = "<< /Title (${info.title.pdfEscaped()}) /Author (${info.author.pdfEscaped()}) " +
        "/Subject (${info.subject.pdfEscaped()}) /Creator (${info.creator.pdfEscaped()}) " +
        "/Producer (${info.creator.pdfEscaped()}) >>"

    // The leading newline is a separator from whatever the original PDF's last byte was (so
    // this never glues onto, say, a trailing "%%EOF" with no line break of its own) — it is
    // deliberately *not* part of the object's own bytes, so the xref entry below can point
    // exactly at "N G obj", not at that separator.
    val withSeparator = this + "\n".toByteArray(Charsets.ISO_8859_1)
    val objectOffset = withSeparator.size
    val withObject = withSeparator + "$objectNumber $objectGeneration obj\n$infoDictionary\nendobj\n".toByteArray(Charsets.ISO_8859_1)
    val newXrefOffset = withObject.size

    val xrefSection = "xref\n$objectNumber 1\n${"%010d".format(objectOffset)} ${"%05d".format(objectGeneration)} n\r\n"
    val trailer = "trailer\n<< /Size $newSize /Root $rootRef /Info $objectNumber $objectGeneration R /Prev $prevXrefOffset >>\n" +
        "startxref\n$newXrefOffset\n%%EOF"

    return withObject + xrefSection.toByteArray(Charsets.ISO_8859_1) + trailer.toByteArray(Charsets.ISO_8859_1)
}

/** Escapes the three literal-string special characters ISO 32000-1 §7.3.4.2 requires — `\`, `(` and `)`. */
private fun String.pdfEscaped(): String = replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

private val STARTXREF = Regex("""startxref\s+(\d+)""")
private val TRAILER = Regex("""trailer\s*<<(.*?)>>""", RegexOption.DOT_MATCHES_ALL)
private val ROOT_REF = Regex("""/Root\s+(\d+\s+\d+\s+R)""")
private val TRAILER_SIZE = Regex("""/Size\s+(\d+)""")
private val INFO_REF = Regex("""/Info\s+(\d+)\s+(\d+)\s+R""")

/** A PDF's Document Info dictionary — see [withPdfInfoDictionary]'s own doc comment for why this is applied after the fact rather than during rendering. */
internal data class PdfInfo(val title: String, val author: String, val subject: String, val creator: String)
