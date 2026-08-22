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

class CardGeometryTest {

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
