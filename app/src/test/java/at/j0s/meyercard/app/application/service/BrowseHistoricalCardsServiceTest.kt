package at.j0s.meyercard.app.application.service

import at.j0s.meyercard.app.application.port.spi.CardRepository
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.Instruction
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** A fixed in-memory stand-in for Room — no database, just a fixed card list. */
private class FakeCardRepository(private val cards: List<MeyerCard>) : CardRepository {
    override suspend fun allCards(): List<MeyerCard> = cards
}

/**
 * 1-44 right hand, 45-88 the left-hand mirrors, 89-109 the technique cards
 * (90-109 carrying an instruction) — the same shape T3.1/T3.2 already proved
 * the real dataset has, built synthetically here rather than parsed from the
 * bundled JSON: pulling `adapter.persistence.OriginalCardsDataSource` into an
 * `application.service` test file is exactly what
 * `ArchitectureTest`'s "service depends on ports, not adapters" check exists
 * to catch — and it did, on the first attempt at this file.
 */
private fun syntheticCard(number: Int): MeyerCard {
    val hand = when {
        number <= 44 -> Hand.RIGHT
        number <= 88 -> Hand.LEFT
        else -> Hand.NEUTRAL
    }
    return MeyerCard(
        id = CardId(number.toLong()),
        actions = listOf(Action(1, Slot(Direction.N, Radius.OUTER), isThrust = false)),
        hand = hand,
        palette = CardPalette.default(hand),
        instruction = if (number in 90..109) Instruction.DOUBLE_FEINT else null,
        origin = CardOrigin.Historical(number = number),
    )
}

private val syntheticCards = (1..109).map { syntheticCard(it) }

class BrowseHistoricalCardsServiceTest {

    private val service = BrowseHistoricalCardsService(FakeCardRepository(syntheticCards))

    @Test
    @DisplayName("there are exactly 44 drills, numbered 1..44")
    fun `there are 44 drills numbered 1 to 44`() = runBlocking {
        assertEquals((1..44).toList(), service.drills().map { it.number })
    }

    @Test
    @DisplayName("each drill's right and left cards are the historical mirror pair")
    fun `each drill pairs card n with card n+44`() = runBlocking {
        for (drill in service.drills()) {
            assertEquals(Hand.RIGHT, drill.rightHandCard.hand)
            assertEquals(Hand.LEFT, drill.leftHandCard.hand)
        }
    }

    @Test
    @DisplayName("there are exactly 21 technique cards, in ascending order")
    fun `there are 21 technique cards in ascending order`() = runBlocking {
        val techniqueCards = service.techniqueCards()
        assertEquals(21, techniqueCards.size)
        assertTrue(techniqueCards.all { it.hand == Hand.NEUTRAL })
        assertEquals((89..109).toList(), techniqueCards.map { (it.origin as CardOrigin.Historical).number })
    }
}
