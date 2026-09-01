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
