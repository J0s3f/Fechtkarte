package at.j0s.meyercard.app.adapter.ui

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * A [MeyerCard] is pure vector with no text nodes, so without a spoken description a screen
 * reader gets nothing (T7.5).
 *
 * Moved from `src/test/` to `src/testDebug/` under Robolectric when the wording became string
 * resources (T9.8) — it needs real [Resources] now. That's a gain, not just a cost: the
 * assertions below run against the *actual* shipped resources, so a broken placeholder or a
 * missing translation fails here rather than on a device.
 */
@RunWith(RobolectricTestRunner::class)
class CardContentDescriptionTest {

    private val resources: Resources
        get() = ApplicationProvider.getApplicationContext<Context>().resources

    private fun card(actions: List<Action>, hand: Hand = Hand.RIGHT) = MeyerCard(
        id = CardId(1L),
        actions = actions,
        hand = hand,
        palette = CardPalette.default(Hand.RIGHT),
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    @Test
    fun `describes hand, action count, and each action`() {
        val description = card(
            actions = listOf(
                Action(1, Slot(Direction.NW, Radius.OUTER), isThrust = false),
                Action(2, Slot(Direction.SE, Radius.INNER), isThrust = true),
            ),
        ).contentDescription(resources)

        assertEquals("Right hand, two actions: one, upper left outer; two, thrust to lower right inner.", description)
    }

    @Test
    fun `singular action count`() {
        val description = card(actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)))
            .contentDescription(resources)
        assertEquals("Right hand, one action: one, top outer.", description)
    }

    @Test
    fun `hand naming`() {
        val leftDescription = card(
            actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
            hand = Hand.LEFT,
        ).contentDescription(resources)
        assertEquals("Left hand, one action: one, top outer.", leftDescription)

        val neutralDescription = card(
            actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
            hand = Hand.NEUTRAL,
        ).contentDescription(resources)
        assertEquals("Technique card, one action: one, top outer.", neutralDescription)
    }

    @Test
    fun `centre radius has no compass direction`() {
        val description = card(actions = listOf(Action(1, Slot(Direction.E, Radius(0.05f)), isThrust = false)))
            .contentDescription(resources)
        assertEquals("Right hand, one action: one, centre.", description)
    }

    @Test
    @Config(qualifiers = "de")
    fun `german spoken description`() {
        // Not just "it isn't English": the exact sentence, because the German translation had
        // to solve two things a literal translation gets wrong, and both are easy to regress.
        // The thrust marker is set off by commas ("zwei, Stich, unten rechts innen") rather
        // than using a preposition, because no single German preposition governs both an
        // adverbial position ("oben links außen") and the noun "Mitte". And the singular
        // plural form is "eine Aktion", not "eins Aktion" — German needs the article form
        // before a noun while the same spoken-number resource must stay "eins" as a bare
        // sequence number, so that one form deliberately drops the placeholder.
        val description = card(
            actions = listOf(
                Action(1, Slot(Direction.NW, Radius.OUTER), isThrust = false),
                Action(2, Slot(Direction.SE, Radius.INNER), isThrust = true),
            ),
        ).contentDescription(resources)

        assertEquals("Rechte Hand, zwei Aktionen: eins, oben links außen; zwei, Stich, unten rechts innen.", description)
    }

    @Test
    @Config(qualifiers = "de")
    fun `german singular action count uses the article form`() {
        val description = card(actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)))
            .contentDescription(resources)
        assertEquals("Rechte Hand, eine Aktion: eins, oben außen.", description)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `french spoken description`() {
        // French can't stack bare adjectives the way "upper left outer" does, so the ring
        // carries its own noun ("anneau extérieur") and the template joins with a comma. That
        // also fixes the gender agreement, which would otherwise have to vary with the
        // direction phrase.
        val description = card(
            actions = listOf(
                Action(1, Slot(Direction.NW, Radius.OUTER), isThrust = false),
                Action(2, Slot(Direction.SE, Radius.INNER), isThrust = true),
            ),
        ).contentDescription(resources)

        assertEquals(
            "Main droite, deux actions : un, en haut à gauche, anneau extérieur ; " +
                "deux, estoc en bas à droite, anneau intérieur.",
            description,
        )
    }

    @Test
    fun `actions are ordered by sequence number`() {
        val description = card(
            actions = listOf(
                Action(2, Slot(Direction.S, Radius.OUTER), isThrust = false),
                Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false),
            ),
        ).contentDescription(resources)

        assertEquals("Right hand, two actions: one, top outer; two, bottom outer.", description)
    }
}
