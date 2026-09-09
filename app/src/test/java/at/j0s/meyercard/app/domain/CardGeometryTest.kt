package at.j0s.meyercard.app.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import java.time.Instant

class CardGeometryTest {

    private fun action(seq: Int, direction: Direction, radius: Radius = Radius.OUTER) =
        Action(seq, Slot(direction, radius), isThrust = false)

    /** Matches `BRIDGE_ELBOW_FRACTION` in `CardGeometry.kt`, restated rather than imported (it's
     * private there) so a change to the production constant has to be a deliberate edit here too,
     * not something these tests silently agree with by construction. */
    private val BRIDGE_ELBOW_FRACTION = 0.3f

    private fun lerp(from: CardPoint, to: CardPoint, fraction: Float) =
        CardPoint(from.x + (to.x - from.x) * fraction, from.y + (to.y - from.y) * fraction)

    private fun card(actions: List<Action>) = MeyerCard(
        id = CardId(1L),
        actions = actions,
        hand = Hand.RIGHT,
        palette = CardPalette.WOAD,
        origin = CardOrigin.Generated(Instant.now()),
    )

    @Test
    @DisplayName("COMPASS draws today's four lines - both diagonals plus the centre cross, edge to edge")
    fun `COMPASS draws the four edge-to-edge lines`() {
        val anyCard = card(listOf(action(1, Direction.N)))

        val segments = anyCard.lineSegments(CardLineStyle.COMPASS)

        assertEquals(
            listOf(
                CardSegment(Direction.NW.edgePointNormalised(), Direction.SE.edgePointNormalised()),
                CardSegment(Direction.NE.edgePointNormalised(), Direction.SW.edgePointNormalised()),
                CardSegment(Direction.N.edgePointNormalised(), Direction.S.edgePointNormalised()),
                CardSegment(Direction.E.edgePointNormalised(), Direction.W.edgePointNormalised()),
            ),
            segments,
        )
    }

    @Test
    @DisplayName("SEQUENCE connects each action to the next by sequence number, tracing the strike order")
    fun `SEQUENCE connects consecutive actions in sequence order`() {
        val fourActions = card(
            listOf(
                action(1, Direction.W),
                action(2, Direction.N),
                action(3, Direction.E),
                action(4, Direction.N, Radius.INNER),
            ),
        )

        val segments = fourActions.lineSegments(CardLineStyle.SEQUENCE)

        assertEquals(
            listOf(
                CardSegment(Slot(Direction.W, Radius.OUTER).toCardPoint(), Slot(Direction.N, Radius.OUTER).toCardPoint()),
                CardSegment(Slot(Direction.N, Radius.OUTER).toCardPoint(), Slot(Direction.E, Radius.OUTER).toCardPoint()),
                CardSegment(Slot(Direction.E, Radius.OUTER).toCardPoint(), Slot(Direction.N, Radius.INNER).toCardPoint()),
            ),
            segments,
        )
    }

    @Test
    @DisplayName("SEQUENCE follows sequence number, not the order actions are listed in")
    fun `SEQUENCE follows sequence number rather than list order`() {
        val outOfOrder = card(listOf(action(2, Direction.S), action(1, Direction.N)))

        val segments = outOfOrder.lineSegments(CardLineStyle.SEQUENCE)

        assertEquals(
            listOf(CardSegment(Slot(Direction.N, Radius.OUTER).toCardPoint(), Slot(Direction.S, Radius.OUTER).toCardPoint())),
            segments,
        )
    }

    @Test
    @DisplayName("SEQUENCE draws nothing for a single-action card - there is no next action to connect to")
    fun `SEQUENCE draws nothing for a single action`() {
        val singleAction = card(listOf(action(1, Direction.N)))

        assertEquals(emptyList<CardSegment>(), singleAction.lineSegments(CardLineStyle.SEQUENCE))
    }

    @Test
    @DisplayName("NONE draws no lines regardless of the card's actions")
    fun `NONE draws nothing`() {
        val anyCard = card(listOf(action(1, Direction.N), action(2, Direction.S)))

        assertEquals(emptyList<CardSegment>(), anyCard.lineSegments(CardLineStyle.NONE))
    }

    @Test
    @DisplayName(
        "BRIDGE connects NW-NE (the bar), and each leg runs from its lower corner to a point" +
            " BRIDGE_ELBOW_FRACTION along the bar from the near upper corner, not to the corner itself",
    )
    fun `BRIDGE draws the bar and both legs`() {
        val allFourCorners = card(
            listOf(
                action(1, Direction.SE),
                action(2, Direction.SW),
                action(3, Direction.NE),
                action(4, Direction.NW),
            ),
        )

        val segments = allFourCorners.lineSegments(CardLineStyle.BRIDGE)

        val nw = Slot(Direction.NW, Radius.OUTER).toCardPoint()
        val ne = Slot(Direction.NE, Radius.OUTER).toCardPoint()
        assertEquals(
            listOf(
                CardSegment(nw, ne),
                CardSegment(Slot(Direction.SW, Radius.OUTER).toCardPoint(), lerp(nw, ne, BRIDGE_ELBOW_FRACTION)),
                CardSegment(Slot(Direction.SE, Radius.OUTER).toCardPoint(), lerp(ne, nw, BRIDGE_ELBOW_FRACTION)),
            ),
            segments,
        )
    }

    @Test
    @DisplayName("BRIDGE omits a leg when its lower corner is missing, but keeps the bar and the other leg's elbow")
    fun `BRIDGE omits a missing leg`() {
        val noSouthWest = card(listOf(action(1, Direction.SE), action(2, Direction.NE), action(3, Direction.NW)))

        val segments = noSouthWest.lineSegments(CardLineStyle.BRIDGE)

        val nw = Slot(Direction.NW, Radius.OUTER).toCardPoint()
        val ne = Slot(Direction.NE, Radius.OUTER).toCardPoint()
        assertEquals(
            listOf(
                CardSegment(nw, ne),
                CardSegment(Slot(Direction.SE, Radius.OUTER).toCardPoint(), lerp(ne, nw, BRIDGE_ELBOW_FRACTION)),
            ),
            segments,
        )
    }

    @Test
    @DisplayName(
        "BRIDGE falls back to the badge itself, not an elbow, when the leg's near upper corner has no" +
            " far corner to inset towards - here NE is missing, so the bar and NW-SW's elbow both disappear",
    )
    fun `BRIDGE omits the bar when one upper corner is missing`() {
        val noNorthEast = card(listOf(action(1, Direction.SW), action(2, Direction.SE), action(3, Direction.NW)))

        val segments = noNorthEast.lineSegments(CardLineStyle.BRIDGE)

        assertEquals(
            listOf(CardSegment(Slot(Direction.SW, Radius.OUTER).toCardPoint(), Slot(Direction.NW, Radius.OUTER).toCardPoint())),
            segments,
        )
    }

    @Test
    @DisplayName("BRIDGE draws nothing when the card has no diagonal actions at all")
    fun `BRIDGE draws nothing without diagonal actions`() {
        val axisOnly = card(listOf(action(1, Direction.N), action(2, Direction.S)))

        assertEquals(emptyList<CardSegment>(), axisOnly.lineSegments(CardLineStyle.BRIDGE))
    }

    @Test
    @DisplayName("BRIDGE uses the outermost action when a direction has two, like historical cards 12 and 56")
    fun `BRIDGE uses the outermost action per direction`() {
        val doubledNorthWest = card(
            listOf(
                action(1, Direction.SE),
                action(2, Direction.SW),
                action(3, Direction.NE),
                action(4, Direction.NW, Radius.OUTER),
                action(5, Direction.NW, Radius.INNER),
            ),
        )

        val segments = doubledNorthWest.lineSegments(CardLineStyle.BRIDGE)

        val nw = Slot(Direction.NW, Radius.OUTER).toCardPoint()
        val ne = Slot(Direction.NE, Radius.OUTER).toCardPoint()
        assertEquals(
            listOf(
                CardSegment(nw, ne),
                CardSegment(Slot(Direction.SW, Radius.OUTER).toCardPoint(), lerp(nw, ne, BRIDGE_ELBOW_FRACTION)),
                CardSegment(Slot(Direction.SE, Radius.OUTER).toCardPoint(), lerp(ne, nw, BRIDGE_ELBOW_FRACTION)),
            ),
            segments,
        )
    }

    @Test
    @DisplayName(
        "BRIDGE reproduces the connectivity measured from reference artwork during development" +
            " (docs/LINE_STYLE_DESIGN.md), using each card's own NW/NE radii for the bar and the" +
            " elbow points, per BRIDGE_ELBOW_FRACTION",
    )
    fun `BRIDGE matches the measured reference geometry`() {
        data class ReferenceBridgeCard(val nw: Radius, val ne: Radius, val sw: Radius, val se: Radius, val extra: List<Action>)

        // Radii transcribed from data/original_cards.json (ids 11, 12, 55, 56), used here purely
        // as real reference data points for verifying this geometry function. Each card's extra
        // action(s) - a single N thrust, or a second inner-radius NE/NW thrust pair - sit outside
        // the bridge's four corners and are expected to draw no line at all.
        val referenceBridgeCards = listOf(
            ReferenceBridgeCard(
                nw = Radius(0.660f), ne = Radius(0.662f), sw = Radius(0.738f), se = Radius(0.748f),
                extra = listOf(action(5, Direction.N, Radius(0.868f))),
            ),
            ReferenceBridgeCard(
                nw = Radius(0.751f), ne = Radius(0.752f), sw = Radius(0.738f), se = Radius(0.748f),
                extra = listOf(action(5, Direction.NE, Radius(0.570f)), action(6, Direction.NW, Radius(0.569f))),
            ),
            ReferenceBridgeCard(
                nw = Radius(0.659f), ne = Radius(0.669f), sw = Radius(0.733f), se = Radius(0.748f),
                extra = listOf(action(5, Direction.N, Radius(0.868f))),
            ),
            ReferenceBridgeCard(
                nw = Radius(0.750f), ne = Radius(0.760f), sw = Radius(0.733f), se = Radius(0.748f),
                extra = listOf(action(5, Direction.NW, Radius(0.568f)), action(6, Direction.NE, Radius(0.578f))),
            ),
        )

        for (h in referenceBridgeCards) {
            val bridgeCard = card(
                listOf(
                    action(1, Direction.NW, h.nw),
                    action(2, Direction.NE, h.ne),
                    action(3, Direction.SW, h.sw),
                    action(4, Direction.SE, h.se),
                ) + h.extra,
            )

            val nw = Slot(Direction.NW, h.nw).toCardPoint()
            val ne = Slot(Direction.NE, h.ne).toCardPoint()
            assertEquals(
                listOf(
                    CardSegment(nw, ne),
                    CardSegment(Slot(Direction.SW, h.sw).toCardPoint(), lerp(nw, ne, BRIDGE_ELBOW_FRACTION)),
                    CardSegment(Slot(Direction.SE, h.se).toCardPoint(), lerp(ne, nw, BRIDGE_ELBOW_FRACTION)),
                ),
                bridgeCard.lineSegments(CardLineStyle.BRIDGE),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(Direction::class)
    @DisplayName("a slot at CENTRE maps to the card centre regardless of direction")
    fun `a slot at CENTRE maps to the card centre`(direction: Direction) {
        val point = Slot(direction, Radius.CENTRE).toCardPoint()
        assertEquals(0.5f, point.x)
        assertEquals(CARD_ASPECT_INVERSE / 2f, point.y)
    }

    @ParameterizedTest
    @EnumSource(Direction::class)
    @DisplayName("a slot at radius 1 maps exactly onto its direction's edge point")
    fun `a slot at radius 1 maps onto the edge point`(direction: Direction) {
        val slotPoint = Slot(direction, Radius(1f)).toCardPoint()
        val edgePoint = direction.edgePointNormalised()
        assertEquals(edgePoint.x, slotPoint.x)
        assertEquals(edgePoint.y, slotPoint.y)
    }

    @Test
    @DisplayName("NE at radius 1 lands on the top-right corner")
    fun `NE at radius 1 lands on the top-right corner`() {
        val point = Slot(Direction.NE, Radius(1f)).toCardPoint()
        assertEquals(1f, point.x)
        assertEquals(0f, point.y)
    }

    @ParameterizedTest
    @EnumSource(Direction::class)
    @DisplayName("mirroring a slot mirrors x about the centre and leaves y alone")
    fun `mirroring a slot mirrors x and leaves y alone`(direction: Direction) {
        val original = Slot(direction, Radius.OUTER).toCardPoint()
        val mirrored = Slot(direction.mirrored(), Radius.OUTER).toCardPoint()
        assertEquals(1f - original.x, mirrored.x, 0.0001f)
        assertEquals(original.y, mirrored.y)
    }

    @Test
    @DisplayName("every action in the historical dataset projects inside the card rectangle")
    fun `every historical action projects inside the card rectangle`() {
        val path = System.getProperty("fechtkarte.originalCardsDataset")
            ?: error("fechtkarte.originalCardsDataset system property not set - see app/build.gradle.kts testOptions")
        val json = Json.parseToJsonElement(File(path).readText()).jsonObject
        val cards = json["cards"]!!.jsonArray

        var actionCount = 0
        for (card in cards) {
            for (action in card.jsonObject["actions"]!!.jsonArray) {
                val direction = Direction.valueOf(action.jsonObject["direction"]!!.jsonPrimitive.content)
                val radius = Radius(action.jsonObject["radius"]!!.jsonPrimitive.content.toFloat())
                val point = Slot(direction, radius).toCardPoint()
                assertTrue(point.x in 0f..1f, "x out of bounds for card ${card.jsonObject["id"]}")
                assertTrue(
                    point.y in 0f..CARD_ASPECT_INVERSE,
                    "y out of bounds for card ${card.jsonObject["id"]}"
                )
                actionCount++
            }
        }
        assertEquals(542, actionCount, "expected all 542 recovered actions to be present")
    }
}
