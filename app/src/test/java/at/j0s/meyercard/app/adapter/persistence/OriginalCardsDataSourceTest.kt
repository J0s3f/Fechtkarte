package at.j0s.meyercard.app.adapter.persistence

import at.j0s.meyercard.app.adapter.ui.render.BORDER_STROKE_WIDTH_FRACTION
import at.j0s.meyercard.app.adapter.ui.render.DISC_DIAMETER_FRACTION
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.toCardPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Exercises [OriginalCardsDataSource] against the real recovered dataset, not
 * a fixture — the point of these assertions is to guard invariants D3 and the
 * mirror-pair claim make about the actual 109 cards, not about parsing logic
 * in isolation.
 */
class OriginalCardsDataSourceTest {

    private val cards: List<MeyerCard> by lazy {
        val path = System.getProperty("fechtkarte.originalCardsDataset")
            ?: error("fechtkarte.originalCardsDataset system property not set - see app/build.gradle.kts testOptions")
        OriginalCardsDataSource.parse(File(path).readText())
    }

    @Test
    @DisplayName("all 109 historical cards parse")
    fun `all 109 cards parse`() {
        assertEquals(109, cards.size)
    }

    @Test
    @DisplayName("the total action count across all cards is 542")
    fun `total action count is 542`() {
        assertEquals(542, cards.sumOf { it.actions.size })
    }

    @Test
    @DisplayName("every card's sequence numbers are 1..n, each once")
    fun `every card's sequence numbers are consecutive`() {
        for (card in cards) {
            val expected = (1..card.actions.size).toList()
            val actual = card.actions.map { it.sequenceNumber }.sorted()
            assertEquals(expected, actual, "card ${card.id} has sequence numbers $actual")
        }
    }

    @Test
    @DisplayName("cards 25 and 69 carry a sourceNote recording their renumbering")
    fun `cards 25 and 69 carry a sourceNote`() {
        for (id in listOf(25L, 69L)) {
            val card = cards.single { it.id == CardId(id) }
            val origin = card.origin as CardOrigin.Historical
            assertTrue(!origin.sourceNote.isNullOrBlank(), "card $id has no sourceNote")
        }
    }

    @Test
    @DisplayName("cards 1-44 mirror to 45-88 for at least 42 of the 44 pairs")
    fun `cards 1-44 mirror to 45-88 for at least 42 of 44 pairs`() {
        val byId = cards.associateBy { it.id }
        // 0.05 is the tightest tolerance that reproduces exactly the 2 remaining known
        // exceptions (5, 28 — hand-placed numerals a few pixels off the ray) and no
        // others; tighter values start failing pairs that are ordinary manual-digitisation
        // noise, not real exceptions. Found by sweeping tolerance values against the real
        // dataset, not assumed. Two further exceptions (11, 42) were corrected directly in
        // the dataset — see each card's sourceNote — rather than tolerated here.
        val radiusTolerance = 0.05f

        fun MeyerCard.actionSignatures(mirrorDirection: Boolean) = actions
            .sortedBy { it.sequenceNumber }
            .map { action ->
                val direction = if (mirrorDirection) action.slot.direction.mirrored() else action.slot.direction
                Triple(action.sequenceNumber, direction, action.isThrust) to action.slot.radius.value
            }

        fun matchesWithinTolerance(right: MeyerCard, left: MeyerCard): Boolean {
            val rightSignatures = right.actionSignatures(mirrorDirection = true)
            val leftSignatures = left.actionSignatures(mirrorDirection = false)
            if (rightSignatures.map { it.first } != leftSignatures.map { it.first }) return false
            return rightSignatures.zip(leftSignatures).all { (r, l) -> abs(r.second - l.second) <= radiusTolerance }
        }

        val matchingPairs = (1L..44L).count { rightId ->
            matchesWithinTolerance(byId.getValue(CardId(rightId)), byId.getValue(CardId(rightId + 44)))
        }

        assertTrue(matchingPairs >= 42, "only $matchingPairs/44 pairs mirrored within tolerance")
    }

    @Test
    @DisplayName("no two actions on the same card render with overlapping discs")
    fun `no two actions overlap on screen`() {
        // The stroke extends outward from each disc's nominal edge, so two discs whose
        // nominal radii are merely touching still visibly merge - found on cards 12/56,
        // where a same-ray pair sat just outside a naive "disc radii sum" check
        // (0.1406 vs a 0.140 threshold) yet clearly overlapped once the stroke was
        // accounted for. Uses actual CardRenderer positions/sizes, not a reimplementation
        // of its ray-projection math, so this can't silently drift out of sync with what
        // actually gets drawn.
        val minCentreDistance = DISC_DIAMETER_FRACTION + BORDER_STROKE_WIDTH_FRACTION

        for (card in cards) {
            val points = card.actions.map { it.sequenceNumber to it.slot.toCardPoint() }
            for (i in points.indices) {
                for (j in i + 1 until points.size) {
                    val (seqA, a) = points[i]
                    val (seqB, b) = points[j]
                    val distance = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())
                    assertTrue(
                        distance >= minCentreDistance,
                        "card ${card.id} actions $seqA and $seqB are $distance apart, " +
                            "closer than $minCentreDistance - their discs would visibly overlap",
                    )
                }
            }
        }
    }
}
