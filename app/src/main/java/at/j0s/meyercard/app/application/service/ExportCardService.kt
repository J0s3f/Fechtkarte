package at.j0s.meyercard.app.application.service

import at.j0s.meyercard.app.application.port.api.ExportCard
import at.j0s.meyercard.app.application.port.spi.CardExporter
import at.j0s.meyercard.app.application.port.spi.ExportResult
import at.j0s.meyercard.app.domain.MeyerCard

class ExportCardService(private val cardExporter: CardExporter) : ExportCard {
    override suspend fun asPng(card: MeyerCard): ExportResult = cardExporter.exportPng(card)
    override suspend fun asPdf(card: MeyerCard): ExportResult = cardExporter.exportPdf(card)
}
