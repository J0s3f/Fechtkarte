package at.j0s.meyercard.app.adapter.export

/**
 * [android.graphics.pdf.PdfDocument] has no API to set Title/Author/Subject/Creator, or any
 * rights-related metadata — nothing on `PdfDocument`, `PdfDocument.PageInfo` or `PdfDocument.Page`
 * touches the Document Info dictionary or an XMP packet at all. Rather than write a PDF from
 * scratch, this patches a finished PDF's bytes afterward, the standard way a PDF is amended
 * without rewriting it: an *incremental update* (ISO 32000-1 §7.5.6) — append new objects, a
 * cross-reference section covering just those objects, and a new trailer whose `/Prev` points at
 * the original cross-reference table. A compliant reader follows the trailer chain and sees the
 * new content as if it had always been there; the original bytes are never touched, so nothing
 * about the rendered page itself is at risk from this.
 *
 * Two things get added, for two different reasons:
 *
 * 1. **The classic Document Info dictionary** (`/Title`/`/Author`/`/Subject`/`/Creator`/
 *    `/Producer`) — the same as before. If the original trailer already names an `/Info`
 *    object — confirmed on a real device: `PdfDocument`'s own Skia backend already writes one,
 *    just a bare `/Producer` — that object's identity is *reused* rather than a new object
 *    number allocated. Allocating a new number would leave two distinct objects both claiming to
 *    be "the" Info dictionary reachable by following `/Prev` through both trailers; at least one
 *    real PDF reader flagged that as "[Minor] Ignored duplicate Info dictionary" on a real
 *    export.
 * 2. **An XMP metadata stream** (ISO 32000-1 §14.3.2), referenced from the document Catalog's own
 *    `/Metadata` entry. This is the only way to carry rights-management fields at all —
 *    `xmpRights:Marked`/`WebStatement`/`UsageTerms` and `dc:rights` have no equivalent in the
 *    classic Info dictionary, which only ever had the five keys above. Adding this means
 *    *modifying* an existing object (the Catalog) rather than only adding new ones: the standard
 *    way to do that in an incremental update is to append a full replacement of that object under
 *    the same object number — any reader resolving that number finds the newest definition, the
 *    same mechanism that makes reusing the Info object's identity above work.
 *
 * Verified against real PDFs with `exiftool` during development (not shipped as a test — no
 * PDF-parsing library lives here to assert against; the byte-level tests instead check that this
 * function's *output structure* is well-formed, which is what a JVM test without a PDF reader
 * actually can check) — a trailer with no pre-existing `/Info`, one shaped like `PdfDocument`'s
 * own output (trailer already naming an `/Info` object, Catalog with no `/Metadata`), and a real
 * device-exported PDF that had already been through this function once.
 */
internal fun ByteArray.withPdfInfoDictionary(info: PdfInfo, rights: PdfRights): ByteArray {
    val text = toString(Charsets.ISO_8859_1)

    val prevXrefOffset = STARTXREF.findAll(text).lastOrNull()?.groupValues?.get(1)?.toInt()
        ?: error("No startxref found — not a well-formed PDF")
    val trailerDictionary = TRAILER.findAll(text).lastOrNull()?.groupValues?.get(1)
        ?: error("No trailer dictionary found — not a well-formed PDF")
    val rootRef = ROOT_REF.find(trailerDictionary)
        ?: error("Trailer has no /Root — not a well-formed PDF")
    val rootNumber = rootRef.groupValues[1].toInt()
    val rootGeneration = rootRef.groupValues[2].toInt()
    val size = TRAILER_SIZE.find(trailerDictionary)?.groupValues?.get(1)?.toInt()
        ?: error("Trailer has no /Size — not a well-formed PDF")

    val existingInfoRef = INFO_REF.find(trailerDictionary)
    // Reuse the existing /Info object's number/generation when there is one; otherwise a fresh,
    // currently-unused number — valid object numbers in a PDF whose trailer declares /Size N
    // run 1..N-1, so N itself is guaranteed free without inspecting every object in the file.
    val infoNumber = existingInfoRef?.groupValues?.get(1)?.toInt() ?: size
    val infoGeneration = existingInfoRef?.groupValues?.get(2)?.toInt() ?: 0
    // The Metadata stream is always new — PdfDocument never writes one — so it takes whichever
    // free number the Info object didn't.
    val metadataNumber = if (existingInfoRef != null) size else size + 1
    val metadataGeneration = 0
    val newSize = metadataNumber + 1

    val rootObject = OBJECT.find(text, rootNumber, rootGeneration)
        ?: error("/Root $rootNumber $rootGeneration R not found — not a well-formed PDF")

    val infoDictionary = "<< /Title (${info.title.pdfEscaped()}) /Author (${info.author.pdfEscaped()}) " +
        "/Subject (${info.subject.pdfEscaped()}) /Creator (${info.creator.pdfEscaped()}) " +
        "/Producer (${info.creator.pdfEscaped()}) >>"

    val xmpPacket = xmpPacket(info, rights)
    val xmpBytes = xmpPacket.toByteArray(Charsets.UTF_8)

    // Same content as before, with a /Metadata reference to the new stream object appended —
    // the Catalog keeps every key it already had, nothing here removes or reorders anything.
    val newRootDictionary = "<<${rootObject.trimEnd()} /Metadata $metadataNumber $metadataGeneration R >>"

    // The leading newline is a separator from whatever the original PDF's last byte was (so
    // this never glues onto, say, a trailing "%%EOF" with no line break of its own) — it is
    // deliberately *not* part of any object's own bytes, so each xref entry below can point
    // exactly at "N G obj", not at a separator.
    var bytes = this + "\n".toByteArray(Charsets.ISO_8859_1)

    val infoOffset = bytes.size
    bytes += "$infoNumber $infoGeneration obj\n$infoDictionary\nendobj\n".toByteArray(Charsets.ISO_8859_1)

    val metadataOffset = bytes.size
    bytes += "$metadataNumber $metadataGeneration obj\n<< /Type /Metadata /Subtype /XML /Length ${xmpBytes.size} >>\nstream\n"
        .toByteArray(Charsets.ISO_8859_1) + xmpBytes + "\nendstream\nendobj\n".toByteArray(Charsets.ISO_8859_1)

    val rootOffset = bytes.size
    bytes += "$rootNumber $rootGeneration obj\n$newRootDictionary\nendobj\n".toByteArray(Charsets.ISO_8859_1)

    val newXrefOffset = bytes.size
    // In ascending object-number order — not the write order above — matching how a real PDF's
    // own xref section is conventionally laid out; readers don't require this, but nothing here
    // benefits from being different from convention either.
    val entries = listOf(infoNumber to infoOffset, metadataNumber to metadataOffset, rootNumber to rootOffset)
        .sortedBy { it.first }
    val xrefSection = buildString {
        append("xref\n")
        for ((number, offset) in entries) {
            val generation = if (number == rootNumber) rootGeneration else 0
            append("$number 1\n${"%010d".format(offset)} ${"%05d".format(generation)} n\r\n")
        }
    }
    val trailer = "trailer\n<< /Size $newSize /Root $rootNumber $rootGeneration R " +
        "/Info $infoNumber $infoGeneration R /Prev $prevXrefOffset >>\n" +
        "startxref\n$newXrefOffset\n%%EOF"

    return bytes + xrefSection.toByteArray(Charsets.ISO_8859_1) + trailer.toByteArray(Charsets.ISO_8859_1)
}

/**
 * Dublin Core (title/creator/description/rights), XMP Basic (CreatorTool) and PDF (Producer)
 * mirror the classic Info dictionary fields — a reader that prefers XMP over Info still sees the
 * same values. XMP Rights Management (`xmpRights:*`) carries what Info has no room for at all.
 *
 * **A generated card is CC0, deliberately distinct from the app's own Apache-2.0 licence.**
 * Apache-2.0 covers Fechtkarte's source code; it doesn't automatically extend a personal
 * copyright claim to whatever the software produces when run. A generated card is a rendered
 * drill diagram someone prints and hands to a training partner — attaching an app author's name
 * as copyright holder to that output doesn't fit, so [PdfRights.marked] is `false` ("not a
 * rights-managed resource," the correct XMP value for public-domain-dedicated content, not
 * `true`), [PdfRights.rightsStatement] names the CC0 dedication rather than a copyright notice,
 * and [PdfRights.webStatementUrl] points at the CC0 deed.
 */
private fun xmpPacket(info: PdfInfo, rights: PdfRights): String {
    fun String.xmlEscaped() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    // ISO 16684-1 §7.3.2's own `xpacket begin` attribute requires a literal BOM (U+FEFF) as its
    // value — that's the packet declaring its own byte order to a reader, not file encoding
    // noise, but a literal BOM *byte sequence* sitting in this .kt source file trips Android
    // Lint's ByteOrderMark check regardless of context. The escape is the same character, written
    // as a Unicode escape so the file itself contains only ASCII, not the raw BOM bytes.
    return """<?xpacket begin="${"\uFEFF"}" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Fechtkarte">
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
<rdf:Description rdf:about=""
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
  xmlns:pdf="http://ns.adobe.com/pdf/1.3/"
  xmlns:xmpRights="http://ns.adobe.com/xap/1.0/rights/">
<dc:title><rdf:Alt><rdf:li xml:lang="x-default">${info.title.xmlEscaped()}</rdf:li></rdf:Alt></dc:title>
<dc:creator><rdf:Seq><rdf:li>${info.creator.xmlEscaped()}</rdf:li></rdf:Seq></dc:creator>
<dc:description><rdf:Alt><rdf:li xml:lang="x-default">${info.subject.xmlEscaped()}</rdf:li></rdf:Alt></dc:description>
<dc:rights><rdf:Alt><rdf:li xml:lang="x-default">${rights.rightsStatement.xmlEscaped()}</rdf:li></rdf:Alt></dc:rights>
<dc:format>application/pdf</dc:format>
<xmp:CreatorTool>${info.creator.xmlEscaped()}</xmp:CreatorTool>
<pdf:Producer>${info.creator.xmlEscaped()}</pdf:Producer>
<xmpRights:Marked>${if (rights.marked) "True" else "False"}</xmpRights:Marked>
<xmpRights:WebStatement>${rights.webStatementUrl.xmlEscaped()}</xmpRights:WebStatement>
<xmpRights:UsageTerms><rdf:Alt><rdf:li xml:lang="x-default">${rights.usageTerms.xmlEscaped()}</rdf:li></rdf:Alt></xmpRights:UsageTerms>
</rdf:Description>
</rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>"""
}

/** Escapes the three literal-string special characters ISO 32000-1 §7.3.4.2 requires — `\`, `(` and `)`. */
private fun String.pdfEscaped(): String = replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

private val STARTXREF = Regex("""startxref\s+(\d+)""")
private val TRAILER = Regex("""trailer\s*<<(.*?)>>""", RegexOption.DOT_MATCHES_ALL)
private val ROOT_REF = Regex("""/Root\s+(\d+)\s+(\d+)\s+R""")
private val TRAILER_SIZE = Regex("""/Size\s+(\d+)""")
private val INFO_REF = Regex("""/Info\s+(\d+)\s+(\d+)\s+R""")

/** Finds `number generation obj << ... >> endobj` and returns the dictionary's own inner content. */
private object OBJECT {
    fun find(text: String, number: Int, generation: Int): String? =
        Regex("""$number\s+$generation\s+obj\s*<<(.*?)>>\s*endobj""", RegexOption.DOT_MATCHES_ALL)
            .find(text)?.groupValues?.get(1)
}

/** A PDF's Document Info dictionary — see [withPdfInfoDictionary]'s own doc comment for why this is applied after the fact rather than during rendering. */
internal data class PdfInfo(val title: String, val author: String, val subject: String, val creator: String)

/** The rights-management fields carried in the XMP packet — see [xmpPacket]'s own doc comment for why a generated card is CC0, distinct from the app's own Apache-2.0 licence. */
internal data class PdfRights(val marked: Boolean, val rightsStatement: String, val webStatementUrl: String, val usageTerms: String)
