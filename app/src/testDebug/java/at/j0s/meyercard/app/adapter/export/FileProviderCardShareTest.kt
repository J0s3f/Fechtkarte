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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/**
 * Robolectric, JUnit 4 bridged via the Vintage engine, debug-only — same reasoning as the
 * other adapter tests. Unlike [MediaStoreCardExporterTest], there's no `@Config(sdk = ...)`
 * split here: sharing writes into the app's own private cache, which needs no scoped-storage
 * permission at any API level — [FileProviderCardShare] doesn't have MediaStore's Q+
 * restriction to test.
 */
@RunWith(RobolectricTestRunner::class)
class FileProviderCardShareTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val card = MeyerCard(
        id = CardId(1L),
        actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
        hand = Hand.RIGHT,
        palette = CardPalette.default(Hand.RIGHT),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    @Test
    fun `prepares a readable PNG at the expected resolution, addressed by a content URI`() = runBlocking {
        val cardShare = FileProviderCardShare(context, Typeface.DEFAULT)

        val result = cardShare.prepareShare(card, CardLineStyle.COMPASS)

        assertEquals("image/png", result.mimeType)
        assertEquals("content", result.uri.scheme)
        context.contentResolver.openInputStream(result.uri)!!.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            assertEquals(2048, bitmap.width)
        }
    }
}
