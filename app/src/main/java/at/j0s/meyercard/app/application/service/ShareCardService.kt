package at.j0s.meyercard.app.application.service

import at.j0s.meyercard.app.application.port.api.ShareCard
import at.j0s.meyercard.app.application.port.spi.CardShare
import at.j0s.meyercard.app.application.port.spi.ShareableCard
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard

class ShareCardService(private val cardShare: CardShare) : ShareCard {
    override suspend fun prepare(card: MeyerCard, lineStyle: CardLineStyle): ShareableCard = cardShare.prepareShare(card, lineStyle)
}
