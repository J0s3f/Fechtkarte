package at.j0s.meyercard.app.adapter.export

import at.j0s.meyercard.app.BuildConfig
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.contentCode

/**
 * Not live yet — the subdomain is reserved but unpointed. Embedded regardless, so every file
 * exported from here on is already tagged with it once it does resolve, rather than only the
 * ones exported after that point.
 */
internal const val FECHTKARTE_URL = "https://fechtkarte.j0s.at/"

/**
 * A generated card's rights statement is CC0, deliberately distinct from the app's own
 * Apache-2.0 licence (`NOTICE`) — see [XmpRights]'s doc comment for why a rendered drill diagram
 * someone prints and shares doesn't carry a personal copyright claim the way the app's own
 * source code does.
 */
private const val CC0_DEED_URL = "https://creativecommons.org/publicdomain/zero/1.0/"

internal fun creationSoftware(): String = "Fechtkarte ${BuildConfig.VERSION_NAME}"

/**
 * The PNG tEXt keyword/text pairs embedded in every exported or shared image: the same content
 * code used for the filename (see [contentCode]) — carried into the image itself so it survives
 * a rename — which software produced it, and where to learn more.
 */
internal fun MeyerCard.pngMetadataEntries(lineStyle: CardLineStyle): List<Pair<String, String>> = listOf(
    "Software" to creationSoftware(),
    "Comment" to contentCode(lineStyle),
    "URL" to FECHTKARTE_URL,
)

/**
 * The PDF Document Info dictionary embedded in every exported PDF — see [pngMetadataEntries]
 * for the PNG equivalent, and [withPdfInfoDictionary] for why this is applied after the fact
 * rather than during rendering.
 */
internal fun MeyerCard.pdfInfo(lineStyle: CardLineStyle): PdfInfo {
    val code = contentCode(lineStyle)
    val software = creationSoftware()
    return PdfInfo(title = "Fechtkarte card $code", author = software, subject = FECHTKARTE_URL, creator = software)
}

/**
 * The XMP packet embedded in every exported PNG's `iTXt` chunk — see [withPngXmpChunk] for why
 * PNG carries it that way, and [pdfInfo]/[withPdfInfoDictionary] for the PDF equivalent. PNG has
 * no `/Info`-dictionary-shaped concept of its own, so [xmpPacket] is called directly with the
 * same title/creator/description convention [pdfInfo] uses, rather than routing through a
 * PNG-specific "info" type that would only exist to hold three strings.
 */
internal fun MeyerCard.pngXmpPacket(lineStyle: CardLineStyle): String {
    val code = contentCode(lineStyle)
    val software = creationSoftware()
    return xmpPacket(
        title = "Fechtkarte card $code",
        creator = software,
        description = FECHTKARTE_URL,
        format = "image/png",
        rights = xmpRights(),
    )
}

/**
 * The rights-management fields embedded in every exported PNG/PDF's XMP packet — see
 * [XmpRights]'s own doc comment for why these are CC0, not the app's own Apache-2.0.
 */
internal fun xmpRights(): XmpRights = XmpRights(
    marked = false,
    rightsStatement = "CC0 1.0 Universal — no rights reserved",
    webStatementUrl = CC0_DEED_URL,
    usageTerms = "No rights reserved (CC0 1.0 Universal). See the linked deed for details.",
)

/**
 * Dublin Core (title/creator/description/rights) and XMP Basic (CreatorTool) fields apply
 * regardless of file format; [producer] is PDF-specific (the classic PDF `/Producer` concept —
 * see `withPdfInfoDictionary`'s own doc comment) and omitted entirely for any other [format],
 * rather than stamping a PDF-only field onto, say, a PNG's XMP packet where it wouldn't apply.
 *
 * **A generated card is CC0, deliberately distinct from the app's own Apache-2.0 licence.**
 * Apache-2.0 covers Fechtkarte's source code; it doesn't automatically extend a personal
 * copyright claim to whatever the software produces when run. A generated card is a rendered
 * drill diagram someone prints and hands to a training partner — attaching an app author's name
 * as copyright holder to that output doesn't fit, so [XmpRights.marked] is `false` ("not a
 * rights-managed resource," the correct XMP value for public-domain-dedicated content, not
 * `true`), [XmpRights.rightsStatement] names the CC0 dedication rather than a copyright notice,
 * and [XmpRights.webStatementUrl] points at the CC0 deed.
 */
internal fun xmpPacket(title: String, creator: String, description: String, format: String, rights: XmpRights, producer: String? = null): String {
    fun String.xmlEscaped() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    val producerNamespace = if (producer != null) "\n  xmlns:pdf=\"http://ns.adobe.com/pdf/1.3/\"" else ""
    val producerElement = if (producer != null) "\n<pdf:Producer>${producer.xmlEscaped()}</pdf:Producer>" else ""
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
  xmlns:xmp="http://ns.adobe.com/xap/1.0/"$producerNamespace
  xmlns:xmpRights="http://ns.adobe.com/xap/1.0/rights/">
<dc:title><rdf:Alt><rdf:li xml:lang="x-default">${title.xmlEscaped()}</rdf:li></rdf:Alt></dc:title>
<dc:creator><rdf:Seq><rdf:li>${creator.xmlEscaped()}</rdf:li></rdf:Seq></dc:creator>
<dc:description><rdf:Alt><rdf:li xml:lang="x-default">${description.xmlEscaped()}</rdf:li></rdf:Alt></dc:description>
<dc:rights><rdf:Alt><rdf:li xml:lang="x-default">${rights.rightsStatement.xmlEscaped()}</rdf:li></rdf:Alt></dc:rights>
<dc:format>${format.xmlEscaped()}</dc:format>
<xmp:CreatorTool>${creator.xmlEscaped()}</xmp:CreatorTool>$producerElement
<xmpRights:Marked>${if (rights.marked) "True" else "False"}</xmpRights:Marked>
<xmpRights:WebStatement>${rights.webStatementUrl.xmlEscaped()}</xmpRights:WebStatement>
<xmpRights:UsageTerms><rdf:Alt><rdf:li xml:lang="x-default">${rights.usageTerms.xmlEscaped()}</rdf:li></rdf:Alt></xmpRights:UsageTerms>
</rdf:Description>
</rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>"""
}

/** The rights-management fields carried in an XMP packet — see [xmpPacket]'s own doc comment for why a generated card is CC0, distinct from the app's own Apache-2.0 licence. */
internal data class XmpRights(val marked: Boolean, val rightsStatement: String, val webStatementUrl: String, val usageTerms: String)
