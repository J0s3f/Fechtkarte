package at.j0s.meyercard.app.adapter.ui

import android.content.res.Resources
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot

/**
 * What a screen reader says for [MeyerCard] — the card itself is pure
 * vector with no text nodes, so without this a screen reader gets nothing.
 *
 * Takes [Resources] rather than being a `@Composable` or a pure-Kotlin string builder (T9.8).
 * Every part of the sentence — the fragments *and* the templates that assemble them — is a
 * translatable resource, because a description like this cannot be localised by translating
 * fragments alone: German and French put the pieces in a different order than English, so the
 * assembly itself has to be something a translator controls. `%1$s %2$s` templates give them
 * that; string concatenation in Kotlin would not.
 */
fun MeyerCard.contentDescription(resources: Resources): String {
    val handWord = resources.getString(
        when (hand) {
            Hand.RIGHT -> R.string.a11y_hand_right
            Hand.LEFT -> R.string.a11y_hand_left
            Hand.NEUTRAL -> R.string.a11y_hand_neutral
        },
    )
    val countPhrase = resources.getQuantityString(
        R.plurals.a11y_action_count,
        actions.size,
        resources.spokenNumber(actions.size),
    )
    val sequence = actions
        .sortedBy { it.sequenceNumber }
        .joinToString(resources.getString(R.string.a11y_sequence_separator)) { it.spoken(resources) }

    return resources.getString(R.string.a11y_card_description, handWord, countPhrase, sequence)
}

/**
 * One action, spoken. Shared with [at.j0s.meyercard.app.adapter.ui.learn.LearnScreen]'s worked
 * example so a sighted reader and a screen reader describe the same card in the same words —
 * one wording to translate, not two that could drift apart per language.
 */
internal fun Action.spoken(resources: Resources): String {
    val number = resources.spokenNumber(sequenceNumber)
    val position = slot.spokenPosition(resources)
    val template = if (isThrust) R.string.a11y_action_thrust else R.string.a11y_action_cut
    return resources.getString(template, number, position)
}

private fun Slot.spokenPosition(resources: Resources): String {
    val ring = radius.spokenRing() ?: return resources.getString(R.string.a11y_position_centre)
    return resources.getString(
        R.string.a11y_position_on_ring,
        resources.getString(direction.spokenDirection()),
        resources.getString(ring),
    )
}

/** `null` for a slot close enough to the true centre that a compass direction isn't meaningful. */
private fun Radius.spokenRing(): Int? = when {
    value < CENTRE_THRESHOLD -> null
    value < INNER_OUTER_MIDPOINT -> R.string.a11y_ring_inner
    else -> R.string.a11y_ring_outer
}

private fun Direction.spokenDirection(): Int = when (this) {
    Direction.N -> R.string.a11y_direction_n
    Direction.NE -> R.string.a11y_direction_ne
    Direction.E -> R.string.a11y_direction_e
    Direction.SE -> R.string.a11y_direction_se
    Direction.S -> R.string.a11y_direction_s
    Direction.SW -> R.string.a11y_direction_sw
    Direction.W -> R.string.a11y_direction_w
    Direction.NW -> R.string.a11y_direction_nw
}

private fun Resources.spokenNumber(n: Int): String {
    val words = getStringArray(R.array.a11y_spoken_numbers)
    return words.getOrElse(n) { n.toString() }
}

// Midpoint between Radius.INNER (0.32) and Radius.OUTER (0.75); CENTRE_THRESHOLD is well clear
// of Radius.CENTRE (0.0) while still well short of INNER, for continuous historical-card radii.
private const val CENTRE_THRESHOLD = 0.2f
private const val INNER_OUTER_MIDPOINT = 0.535f
