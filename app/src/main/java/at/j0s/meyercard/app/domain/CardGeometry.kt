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
enum class CardLineStyle { COMPASS, SEQUENCE, BRIDGE, NONE }

private val LINE_AXES = listOf(
    Direction.NW to Direction.SE,
    Direction.NE to Direction.SW,
    Direction.N to Direction.S,
    Direction.E to Direction.W,
)

/**
 * How far along the bar (from the near upper corner towards the far one) a [CardLineStyle.BRIDGE]
 * leg meets it, instead of running straight to the corner badge. Measured from reference artwork
 * used while developing this style: the real legs consistently leave the bar 25-32.5% of its
 * length in from each end (mean 29.1%, 8 measurements), not at the bar's own endpoints. 0.3 is the
 * nearest round number inside that measured range; see `DESIGN_CHOICES.md`'s "`BRIDGE`'s geometry"
 * entry for the full numbers and methodology.
 */
private const val BRIDGE_ELBOW_FRACTION = 0.3f

/**
 * The lines drawn behind this card's action badges. Under [CardLineStyle.COMPASS] they're the
 * fixed compass rose — two diagonals plus the vertical and horizontal centre lines, edge to
 * edge, regardless of the card's actions — today's rendering. Under
 * [CardLineStyle.SEQUENCE] the compass is dropped entirely in favour of the drill's own
 * path: a line from action 1 to action 2, 2 to 3, and so on by [Action.sequenceNumber] — the
 * order a practitioner actually strikes in, tracing the sequence the way it's meant to be read
 * (low number to high) rather than the card's fixed geometry. A card with one action draws
 * nothing; there is no "next" to connect it to. [CardLineStyle.BRIDGE] draws a bar across the two
 * upper diagonal actions (NW/NE) and a leg from each lower one (SW/SE) up to a point
 * [BRIDGE_ELBOW_FRACTION] along the bar, rather than to the bar's own corner — a card missing a
 * corner simply loses whichever segment needed it. Where a direction has more than one action
 * (the historical cards' own second thrust badges), the outermost one is used; ties are
 * impossible since [MeyerCard] already forbids two actions sharing a slot. [CardLineStyle.NONE]
 * draws nothing at all.
 */
fun MeyerCard.lineSegments(style: CardLineStyle): List<CardSegment> = when (style) {
    CardLineStyle.COMPASS -> LINE_AXES.map { (a, b) -> CardSegment(a.edgePointNormalised(), b.edgePointNormalised()) }
    CardLineStyle.SEQUENCE -> actions.sortedBy { it.sequenceNumber }
        .map { it.slot.toCardPoint() }
        .zipWithNext { from, to -> CardSegment(from, to) }
    CardLineStyle.BRIDGE -> bridgeSegments()
    CardLineStyle.NONE -> emptyList()
}

private fun MeyerCard.bridgeSegments(): List<CardSegment> {
    val nw = outermostActionInDirection(Direction.NW)
    val ne = outermostActionInDirection(Direction.NE)
    val sw = outermostActionInDirection(Direction.SW)
    val se = outermostActionInDirection(Direction.SE)

    val bar = if (nw != null && ne != null) CardSegment(nw.slot.toCardPoint(), ne.slot.toCardPoint()) else null
    val leftLeg = bridgeLeg(lower = sw, near = nw, far = ne)
    val rightLeg = bridgeLeg(lower = se, near = ne, far = nw)
    return listOfNotNull(bar, leftLeg, rightLeg)
}

/**
 * A leg from [lower] (SW or SE) up to a point [BRIDGE_ELBOW_FRACTION] of the way from [near]
 * (the upper corner on the same side) towards [far] (the other upper corner) — or, when [far]
 * is absent, straight to [near] itself, since there is no bar to inset the elbow along.
 */
private fun bridgeLeg(lower: Action?, near: Action?, far: Action?): CardSegment? {
    if (lower == null || near == null) return null
    val nearPoint = near.slot.toCardPoint()
    val top = if (far != null) nearPoint.lerp(far.slot.toCardPoint(), BRIDGE_ELBOW_FRACTION) else nearPoint
    return CardSegment(lower.slot.toCardPoint(), top)
}

private fun CardPoint.lerp(to: CardPoint, fraction: Float): CardPoint =
    CardPoint(x + (to.x - x) * fraction, y + (to.y - y) * fraction)

private fun MeyerCard.outermostActionInDirection(direction: Direction): Action? =
    actions.filter { it.slot.direction == direction }.maxByOrNull { it.slot.radius.value }
