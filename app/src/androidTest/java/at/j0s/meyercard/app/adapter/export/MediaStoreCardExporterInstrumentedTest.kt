package at.j0s.meyercard.app.adapter.export

import android.content.Context
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Runs on a real device/emulator (`.github/workflows/instrumented-tests.yml`), not Robolectric:
 * `android.graphics.pdf.PdfDocument` has no Robolectric shadow at all —
 * [MediaStoreCardExporterTest]'s own doc comment documents this and works around it by verifying
 * only the byte-level `withPdfInfoDictionary` function `exportPdf` calls, against a hand-built
 * fixture, never `exportPdf`'s own real output. This closes that gap directly: the actual bytes
 * a real `PdfDocument`, rendered and patched on a real device, produces.
 */
@RunWith(AndroidJUnit4::class)
class MediaStoreCardExporterInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val card = MeyerCard(
        id = CardId(1L),
        actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
        hand = Hand.RIGHT,
        palette = CardPalette.default(Hand.RIGHT),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    private fun exporter() = MediaStoreCardExporter(context.contentResolver, context.resources, Typeface.DEFAULT)

    @Test
    fun exportedPdfIsAWellFormedFileCarryingItsMetadata() = runBlocking {
        val result = exporter().exportPdf(card, CardLineStyle.COMPASS)

        val bytes = context.contentResolver.openInputStream(result.uri)!!.use { it.readBytes() }
        val text = String(bytes, Charsets.ISO_8859_1)

        assertTrue("expected a PDF header, got: ${text.take(16)}", text.startsWith("%PDF-"))
        assertTrue("expected an EOF marker", text.trimEnd().endsWith("%%EOF"))
        assertTrue("expected the embedded XMP packet", text.contains("adobe:ns:meta/"))
        assertTrue("expected the CC0 rights statement", text.contains("CC0 1.0 Universal"))
    }

    @Test
    fun exportingTheSamePdfTwiceReusesTheSameFile() = runBlocking {
        val exporter = exporter()

        val first = exporter.exportPdf(card, CardLineStyle.COMPASS)
        val second = exporter.exportPdf(card, CardLineStyle.COMPASS)

        assertEquals(first.uri, second.uri)
        assertEquals(first.displayName, second.displayName)
    }
}
