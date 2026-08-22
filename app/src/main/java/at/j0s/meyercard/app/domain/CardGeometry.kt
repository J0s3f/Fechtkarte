package at.j0s.meyercard.app.domain

/**
 * A point in normalised card space: x in `0f..1f` (one card-width), y in
 * `0f..CARD_ASPECT_INVERSE` (the card height, expressed in the same units).
 * Kept free of any `androidx.compose.ui.geometry` type so the domain stays
 * testable on the JVM with no Android runtime — the renderer multiplies by
 * actual pixel size at the edge. See DESIGN_CHOICES.md.
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
 * placement — see docs/PLAN.md §3.1.
 */
fun Slot.toCardPoint(): CardPoint {
    val edge = direction.edgePointNormalised()
    return CardPoint(
        x = CARD_CENTRE.x + (edge.x - CARD_CENTRE.x) * radius.value,
        y = CARD_CENTRE.y + (edge.y - CARD_CENTRE.y) * radius.value,
    )
}
