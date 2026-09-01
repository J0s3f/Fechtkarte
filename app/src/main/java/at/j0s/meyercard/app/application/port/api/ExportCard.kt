package at.j0s.meyercard.app.application.port.api

import at.j0s.meyercard.app.application.port.spi.ExportResult
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard

interface ExportCard {
    suspend fun asPng(card: MeyerCard, lineStyle: CardLineStyle): ExportResult
    suspend fun asPdf(card: MeyerCard, lineStyle: CardLineStyle): ExportResult
}
