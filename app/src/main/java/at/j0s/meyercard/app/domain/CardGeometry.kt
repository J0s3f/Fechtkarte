package at.j0s.meyercard.app.domain

/**
 * A point in normalised card space: x in `0f..1f` (one card-width), y in
 * `0f..CARD_ASPECT_INVERSE` (the card height, expressed in the same units).
 * Kept free of any `androidx.compose.ui.geometry` type so the domain stays
 * testable on the JVM with no Android runtime — the renderer multiplies by
 * actual pixel size at the edge.
 */
data class CardPoint(val x: Float, val y: Float)

/** Card width / height — the aspect ratio Fechtkarte's own card artwork renders at. */
const val CARD_ASPECT = 644f / 931f

/** Card height / width — the y-extent of the card in normalised (width = 1) units. */
const val CARD_ASPECT_INVERSE = 931f / 644f

private val CARD_CENTRE = CardPoint(0.5f, CARD_ASPECT_INVERSE / 2f)

/**
 * Where this slot sits in normalised card space: the point that is [Slot.radius]
 * of the way from the card centre to the edge along [Slot.direction]'s ray. Used
 * identically by the generator's 16 fixed slots and the historical cards' free
 * placement.
 */
fun Slot.toCardPoint(): CardPoint {
    val edge = direction.edgePointNormalised()
    return CardPoint(
        x = CARD_CENTRE.x + (edge.x - CARD_CENTRE.x) * radius.value,
        y = CARD_CENTRE.y + (edge.y - CARD_CENTRE.y) * radius.value,
    )
}

/** A straight line between two points in normalised card space. */
data class CardSegment(val from: CardPoint, val to: CardPoint)

/** How a card's lines are drawn behind its action badges. */
enum class CardLineStyle { COMPASS, SEQUENCE }

private val LINE_AXES = listOf(
    Direction.NW to Direction.SE,
    Direction.NE to Direction.SW,
    Direction.N to Direction.S,
    Direction.E to Direction.W,
)

/**
 * The lines drawn behind this card's action badges. Under [CardLineStyle.COMPASS] they're the
 * fixed compass rose — two diagonals plus the vertical and horizontal centre lines, edge to
 * edge, regardless of the card's actions — today's rendering. Under
 * [CardLineStyle.SEQUENCE] the compass is dropped entirely in favour of the drill's own
 * path: a line from action 1 to action 2, 2 to 3, and so on by [Action.sequenceNumber] — the
 * order a practitioner actually strikes in, tracing the sequence the way it's meant to be read
 * (low number to high) rather than the card's fixed geometry. A card with one action draws
 * nothing; there is no "next" to connect it to.
 */
fun MeyerCard.lineSegments(style: CardLineStyle): List<CardSegment> = when (style) {
    CardLineStyle.COMPASS -> LINE_AXES.map { (a, b) -> CardSegment(a.edgePointNormalised(), b.edgePointNormalised()) }
    CardLineStyle.SEQUENCE -> actions.sortedBy { it.sequenceNumber }
        .map { it.slot.toCardPoint() }
        .zipWithNext { from, to -> CardSegment(from, to) }
}
