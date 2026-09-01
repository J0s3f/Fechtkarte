package at.j0s.meyercard.app.application.port.api

import at.j0s.meyercard.app.application.port.spi.ShareableCard
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard

interface ShareCard {
    suspend fun prepare(card: MeyerCard, lineStyle: CardLineStyle): ShareableCard
}
