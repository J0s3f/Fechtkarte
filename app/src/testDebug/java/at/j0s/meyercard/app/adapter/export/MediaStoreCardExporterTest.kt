package at.j0s.meyercard.app.adapter.export

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Robolectric, JUnit 4 bridged via the Vintage engine, debug-only — same
 * reasoning as the other adapter tests. `@Config(sdk = ...)` overrides
 * Robolectric's simulated API level per test, which is exactly what's
 * needed here: [MediaStoreCardExporter]'s whole behaviour split is
 * API-level-dependent.
 *
 * There's no test here for the actual bytes `exportPdf` produces:
 * Robolectric ships no shadow for `android.graphics.pdf.PdfDocument` (its
 * shadow set has none — confirmed by inspecting shadows-framework.jar), so
 * it falls through to the real platform class, which calls native code the
 * desktop JVM doesn't have. `PdfDocument.startPage` fails immediately with
 * `IllegalStateException: document is closed!` regardless of graphics mode
 * (`@GraphicsMode(NATIVE)` was tried and made no difference, since that
 * only affects Canvas/Bitmap shadows, not this class). PDF content is
 * verified manually instead.
 */
@RunWith(RobolectricTestRunner::class)
class MediaStoreCardExporterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val card = MeyerCard(
        id = CardId(1L),
        actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
        hand = Hand.RIGHT,
        palette = CardPalette.default(Hand.RIGHT),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    @Test
    @Config(sdk = [29])
    fun `exports a PNG at the expected resolution on Android 10+`() = runBlocking {
        val exporter = MediaStoreCardExporter(context.contentResolver, context.resources, Typeface.DEFAULT)

        val result = exporter.exportPng(card, CardLineStyle.COMPASS)

        assertEquals("fechtkarte-${card.contentCode(CardLineStyle.COMPASS)}.png", result.displayName)
        context.contentResolver.openInputStream(result.uri)!!.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            assertEquals(2048, bitmap.width)
        }
    }

    @Test
    @Config(sdk = [29])
    fun `exporting the same card under the same line style twice reuses the same file`() = runBlocking {
        val exporter = MediaStoreCardExporter(context.contentResolver, context.resources, Typeface.DEFAULT)

        val first = exporter.exportPng(card, CardLineStyle.COMPASS)
        val second = exporter.exportPng(card, CardLineStyle.COMPASS)

        assertEquals(first.uri, second.uri)
        assertEquals(first.displayName, second.displayName)
    }

    @Test
    @Config(sdk = [29])
    fun `exporting the same card under a different line style saves a distinctly named file`() = runBlocking {
        val exporter = MediaStoreCardExporter(context.contentResolver, context.resources, Typeface.DEFAULT)

        val compass = exporter.exportPng(card, CardLineStyle.COMPASS)
        val sequence = exporter.exportPng(card, CardLineStyle.SEQUENCE)

        assertNotEquals(compass.displayName, sequence.displayName)
    }

    @Test
    @Config(sdk = [28])
    fun `refuses to export either format on Android versions before 10`() {
        val exporter = MediaStoreCardExporter(context.contentResolver, context.resources, Typeface.DEFAULT)
        assertThrows(IllegalStateException::class.java) { runBlocking { exporter.exportPng(card, CardLineStyle.COMPASS) } }
        assertThrows(IllegalStateException::class.java) { runBlocking { exporter.exportPdf(card, CardLineStyle.COMPASS) } }
    }
}
