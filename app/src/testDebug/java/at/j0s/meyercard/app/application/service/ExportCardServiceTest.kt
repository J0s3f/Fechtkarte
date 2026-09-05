package at.j0s.meyercard.app.application.service

import android.net.Uri
import at.j0s.meyercard.app.application.port.spi.CardExporter
import at.j0s.meyercard.app.application.port.spi.ExportResult
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
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/**
 * Debug-only (Robolectric) purely because [ExportResult] carries a real `android.net.Uri` --
 * this service itself is plain delegation with no Android dependency of its own. Never exercised
 * before this: [at.j0s.meyercard.app.adapter.ui.MainActivity] wires it up, but every UI test uses
 * a fake [at.j0s.meyercard.app.application.port.api.ExportCard] instead (see
 * `FechtkarteAppTestFakes.kt`), so the service's own pass-through was untested.
 */
@RunWith(RobolectricTestRunner::class)
class ExportCardServiceTest {

    private val card = MeyerCard(
        id = CardId(1L),
        actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
        hand = Hand.RIGHT,
        palette = CardPalette.default(Hand.RIGHT),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    private class FakeCardExporter : CardExporter {
        var lastCard: MeyerCard? = null
        var lastLineStyle: CardLineStyle? = null
        val pngResult = ExportResult(Uri.EMPTY, "fake.png")
        val pdfResult = ExportResult(Uri.EMPTY, "fake.pdf")

        override suspend fun exportPng(card: MeyerCard, lineStyle: CardLineStyle): ExportResult {
            lastCard = card
            lastLineStyle = lineStyle
            return pngResult
        }

        override suspend fun exportPdf(card: MeyerCard, lineStyle: CardLineStyle): ExportResult {
            lastCard = card
            lastLineStyle = lineStyle
            return pdfResult
        }
    }

    @Test
    fun `asPng passes the card and line style through and returns the exporter's result`() = runBlocking {
        val exporter = FakeCardExporter()
        val service = ExportCardService(exporter)

        val result = service.asPng(card, CardLineStyle.COMPASS)

        assertSame(card, exporter.lastCard)
        assertEquals(CardLineStyle.COMPASS, exporter.lastLineStyle)
        assertSame(exporter.pngResult, result)
    }

    @Test
    fun `asPdf passes the card and line style through and returns the exporter's result`() = runBlocking {
        val exporter = FakeCardExporter()
        val service = ExportCardService(exporter)

        val result = service.asPdf(card, CardLineStyle.SEQUENCE)

        assertSame(card, exporter.lastCard)
        assertEquals(CardLineStyle.SEQUENCE, exporter.lastLineStyle)
        assertSame(exporter.pdfResult, result)
    }
}
