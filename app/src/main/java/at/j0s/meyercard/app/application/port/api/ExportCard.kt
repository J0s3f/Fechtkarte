package at.j0s.meyercard.app.application.port.api

import at.j0s.meyercard.app.application.port.spi.ExportResult
import at.j0s.meyercard.app.domain.MeyerCard

interface ExportCard {
    suspend fun asPng(card: MeyerCard): ExportResult
    suspend fun asPdf(card: MeyerCard): ExportResult
}
