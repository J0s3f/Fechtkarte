package at.j0s.meyercard.app.application.port.spi

import at.j0s.meyercard.app.domain.MeyerCard

/** Access to the card library — historical cards today, generated cards eventually. */
interface CardRepository {
    suspend fun allCards(): List<MeyerCard>
}
