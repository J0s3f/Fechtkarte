package at.j0s.meyercard.app.adapter.persistence

import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.Instruction
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parses the bundled historical-card dataset (`assets/original_cards.json`)
 * into domain [MeyerCard]s. Reading the asset
 * itself needs an Android `Context`; that's Room's job when it seeds the
 * database (T3.2). This stays pure JVM — JSON text in, domain objects out —
 * so it's directly testable against the real dataset with no Robolectric.
 */
object OriginalCardsDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(datasetJson: String): List<MeyerCard> =
        json.decodeFromString<OriginalCardsDatasetDto>(datasetJson).cards.map { it.toDomain() }
}

@Serializable
private data class OriginalCardsDatasetDto(val cards: List<OriginalCardDto>)

@Serializable
private data class OriginalCardDto(
    val id: Long,
    val hand: String,
    val instruction: String? = null,
    val actions: List<OriginalActionDto>,
    val sourceNote: String? = null,
)

/**
 * `internal`, not `private` — [HistoricalCardEntity] reuses this exact shape to
 * encode a card's actions into Room's `actionsJson` column, rather than
 * defining a near-duplicate DTO for "a list of cuts and thrusts".
 */
@Serializable
internal data class OriginalActionDto(
    val seq: Int,
    val direction: String,
    val radius: Float,
    val thrust: Boolean,
)

private fun OriginalCardDto.toDomain(): MeyerCard {
    val hand = Hand.valueOf(hand)
    return MeyerCard(
        id = CardId(id),
        actions = actions.map { it.toDomain() },
        hand = hand,
        palette = CardPalette.default(hand),
        instruction = instruction?.let { it.toInstruction() },
        origin = CardOrigin.Historical(number = id.toInt(), sourceNote = sourceNote),
    )
}

internal fun OriginalActionDto.toDomain(): Action =
    Action(sequenceNumber = seq, slot = Slot(Direction.valueOf(direction), Radius(radius)), isThrust = thrust)

internal fun Action.toDto(): OriginalActionDto =
    OriginalActionDto(seq = sequenceNumber, direction = slot.direction.name, radius = slot.radius.value, thrust = isThrust)

/**
 * The dataset's instruction strings are the card data's own wording, not
 * [Instruction]'s enum names — [Instruction]'s own doc comment is explicit
 * that the technique is data and the wording is Fechtkarte's own, so this
 * mapping is deliberate, not a missed `valueOf`.
 */
private fun String.toInstruction(): Instruction = when (this) {
    "EXECUTE AS DOUBLEFEINT" -> Instruction.DOUBLE_FEINT
    "EXECUTE AS MOULINET" -> Instruction.MOULINET
    "PROVOKER - TAKER - HITTER" -> Instruction.PROVOKER_TAKER_HITTER
    else -> error("Unrecognised instruction string: $this")
}
