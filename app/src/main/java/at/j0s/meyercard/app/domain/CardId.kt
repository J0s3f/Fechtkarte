package at.j0s.meyercard.app.domain

/**
 * A card's identity. `Long` rather than `Int` so historical cards (1-109) and
 * future Room-assigned row ids for generated cards can share one type without
 * a range concern.
 */
@JvmInline
value class CardId(val value: Long)
