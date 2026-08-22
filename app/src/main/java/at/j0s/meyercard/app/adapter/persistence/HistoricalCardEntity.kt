package at.j0s.meyercard.app.adapter.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.Instruction
import at.j0s.meyercard.app.domain.MeyerCard
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Room's row shape for a historical card. [id] doubles as both the Room
 * primary key and the card's historical number (1-109) — they're the same
 * value, so there's no separate column for it. No palette column: palette is
 * a persisted per-hand *preference* (F4), not a historical fact, so it's
 * re-derived from [hand] via [CardPalette.default] on the way back out
 * rather than stored redundantly. [actionsJson] reuses [OriginalActionDto] —
 * the same shape the bundled dataset itself parses actions with.
 */
@Entity(tableName = "historical_cards")
data class HistoricalCardEntity(
    @PrimaryKey val id: Long,
    val hand: String,
    val instructionName: String?,
    val actionsJson: String,
    val sourceNote: String?,
)

private val actionListSerializer = ListSerializer(OriginalActionDto.serializer())

internal fun MeyerCard.toEntity(): HistoricalCardEntity {
    val historicalOrigin = origin as? CardOrigin.Historical
        ?: error("only historical cards are persisted in Room; got $origin")
    return HistoricalCardEntity(
        id = id.value,
        hand = hand.name,
        instructionName = instruction?.name,
        actionsJson = Json.encodeToString(actionListSerializer, actions.map { it.toDto() }),
        sourceNote = historicalOrigin.sourceNote,
    )
}

internal fun HistoricalCardEntity.toDomain(): MeyerCard {
    val handValue = Hand.valueOf(hand)
    return MeyerCard(
        id = CardId(id),
        actions = Json.decodeFromString(actionListSerializer, actionsJson).map { it.toDomain() },
        hand = handValue,
        palette = CardPalette.default(handValue),
        instruction = instructionName?.let { Instruction.valueOf(it) },
        origin = CardOrigin.Historical(number = id.toInt(), sourceNote = sourceNote),
    )
}
