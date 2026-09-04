package at.j0s.meyercard.app.adapter.export

import at.j0s.meyercard.app.BuildConfig
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import at.j0s.meyercard.app.domain.contentCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * [PngMetadataTest] and [PdfMetadataTest] check the low-level chunk/object writers against
 * hand-supplied [PdfInfo]/[XmpRights] values; nothing until this file checked that the *real*
 * values this app actually builds from a [MeyerCard] — [MeyerCard.pngMetadataEntries],
 * [MeyerCard.pdfInfo], [MeyerCard.pngXmpPacket], [xmpRights] — are themselves correct, or that
 * the shared [xmpPacket] builder handles the PDF/PNG difference ([producer]) and XML escaping
 * right. A typo in one of those real call sites would pass every existing test and still ship
 * wrong metadata.
 */
class ExportMetadataTest {

    private val card = MeyerCard(
        id = CardId(1L),
        actions = listOf(
            Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false),
            Action(2, Slot(Direction.SE, Radius.INNER), isThrust = true),
        ),
        hand = Hand.RIGHT,
        palette = CardPalette.WOAD,
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    private val software = "Fechtkarte ${BuildConfig.VERSION_NAME}"

    @Test
    @DisplayName("pngMetadataEntries carries the card's own content code, the running software, and the Fechtkarte URL")
    fun `pngMetadataEntries reflects the real card`() {
        val entries = card.pngMetadataEntries(CardLineStyle.COMPASS)
        assertEquals(
            listOf("Software" to software, "Comment" to card.contentCode(CardLineStyle.COMPASS), "URL" to FECHTKARTE_URL),
            entries,
        )
    }

    @Test
    @DisplayName("pdfInfo's Title embeds the card's own content code, and Author/Creator/Producer all agree on the running software")
    fun `pdfInfo reflects the real card`() {
        val info = card.pdfInfo(CardLineStyle.SEQUENCE)
        assertEquals("Fechtkarte card ${card.contentCode(CardLineStyle.SEQUENCE)}", info.title)
        assertEquals(software, info.author)
        assertEquals(software, info.creator)
        assertEquals(FECHTKARTE_URL, info.subject)
    }

    @Test
    @DisplayName("pngXmpPacket names the same title, creator and description pdfInfo does, tagged as image/png")
    fun `pngXmpPacket reflects the real card, tagged as image-png`() {
        val packet = card.pngXmpPacket(CardLineStyle.COMPASS)
        val info = card.pdfInfo(CardLineStyle.COMPASS)

        assertTrue(packet.contains("<dc:title><rdf:Alt><rdf:li xml:lang=\"x-default\">${info.title}</rdf:li></rdf:Alt></dc:title>"))
        assertTrue(packet.contains("<dc:creator><rdf:Seq><rdf:li>${info.creator}</rdf:li></rdf:Seq></dc:creator>"))
        assertTrue(packet.contains("<dc:description><rdf:Alt><rdf:li xml:lang=\"x-default\">${info.subject}</rdf:li></rdf:Alt></dc:description>"))
        assertTrue(packet.contains("<dc:format>image/png</dc:format>"))
    }

    @Test
    @DisplayName("pngXmpPacket carries no PDF Producer element or namespace — that field doesn't apply to a PNG")
    fun `pngXmpPacket omits the PDF-specific Producer field`() {
        val packet = card.pngXmpPacket(CardLineStyle.COMPASS)
        assertFalse(packet.contains("pdf:Producer"))
        assertFalse(packet.contains("xmlns:pdf"))
    }

    @Test
    @DisplayName("xmpRights is the app's CC0 dedication, not its own Apache-2.0 source licence")
    fun `xmpRights is CC0, not Apache-2_0`() {
        val rights = xmpRights()
        assertFalse(rights.marked, "a CC0-dedicated work is not a rights-managed resource")
        assertEquals("CC0 1.0 Universal — no rights reserved", rights.rightsStatement)
        assertEquals("https://creativecommons.org/publicdomain/zero/1.0/", rights.webStatementUrl)
        assertTrue(rights.usageTerms.contains("CC0"))
        assertFalse(rights.rightsStatement.contains("Apache"), "must not claim the app's own source licence for generated output")
    }

    @Test
    @DisplayName("xmpPacket includes the PDF Producer element and its namespace only when a producer is given")
    fun `xmpPacket includes Producer only when given`() {
        val rights = xmpRights()
        val withProducer = xmpPacket(title = "t", creator = "c", description = "d", format = "application/pdf", rights = rights, producer = "c")
        val withoutProducer = xmpPacket(title = "t", creator = "c", description = "d", format = "image/png", rights = rights, producer = null)

        assertTrue(withProducer.contains("<pdf:Producer>c</pdf:Producer>"))
        assertTrue(withProducer.contains("xmlns:pdf="))
        assertFalse(withoutProducer.contains("pdf:Producer"))
        assertFalse(withoutProducer.contains("xmlns:pdf"))
    }

    @Test
    @DisplayName("xmpPacket XML-escapes ampersands and angle brackets in every field, not just some")
    fun `xmpPacket escapes XML special characters in every field`() {
        val rights = XmpRights(marked = false, rightsStatement = "a & b", webStatementUrl = "https://example/?a=1&b=2", usageTerms = "<terms>")
        val packet = xmpPacket(title = "A & B", creator = "<C>", description = "D & E", format = "F<G", rights = rights, producer = "P&Q")

        assertTrue(packet.contains("A &amp; B"), "expected the title's ampersand escaped, got: $packet")
        assertTrue(packet.contains("&lt;C&gt;"), "expected the creator's angle brackets escaped, got: $packet")
        assertTrue(packet.contains("D &amp; E"), "expected the description's ampersand escaped, got: $packet")
        assertTrue(packet.contains("F&lt;G"), "expected the format's angle bracket escaped, got: $packet")
        assertTrue(packet.contains("a &amp; b"), "expected the rights statement's ampersand escaped, got: $packet")
        assertTrue(packet.contains("https://example/?a=1&amp;b=2"), "expected the web statement URL's ampersand escaped, got: $packet")
        assertTrue(packet.contains("&lt;terms&gt;"), "expected the usage terms' angle brackets escaped, got: $packet")
        assertTrue(packet.contains("P&amp;Q"), "expected the producer's ampersand escaped, got: $packet")

        // None of the raw, unescaped special characters should survive into the output at all —
        // a partial escape (e.g. only the title) would still leave the packet non-well-formed XML.
        assertFalse(packet.contains(" & "), "a raw, unescaped ampersand would make this invalid XML: $packet")
    }
}
