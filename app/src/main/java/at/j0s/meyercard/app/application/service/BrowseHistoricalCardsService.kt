package at.j0s.meyercard.app.application.service

import at.j0s.meyercard.app.application.port.api.BrowseHistoricalCards
import at.j0s.meyercard.app.application.port.spi.CardRepository
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.HistoricalDrill
import at.j0s.meyercard.app.domain.MeyerCard

class BrowseHistoricalCardsService(private val cardRepository: CardRepository) : BrowseHistoricalCards {

    override suspend fun drills(): List<HistoricalDrill> {
        val byNumber = cardRepository.allCards().associateBy { it.historicalNumber() }
        return (1..44).map { number ->
            HistoricalDrill(
                number = number,
                rightHandCard = byNumber.getValue(number),
                leftHandCard = byNumber.getValue(number + 44),
            )
        }
    }

    override suspend fun techniqueCards(): List<MeyerCard> =
        cardRepository.allCards()
            .filter { it.historicalNumber() in 89..109 }
            .sortedBy { it.historicalNumber() }

    private fun MeyerCard.historicalNumber(): Int =
        (origin as? CardOrigin.Historical)?.number ?: error("expected a historical card, got $origin")
}
