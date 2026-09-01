package at.j0s.meyercard.app.application.service

import at.j0s.meyercard.app.application.port.api.ExportCard
import at.j0s.meyercard.app.application.port.spi.CardExporter
import at.j0s.meyercard.app.application.port.spi.ExportResult
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard

class ExportCardService(private val cardExporter: CardExporter) : ExportCard {
    override suspend fun asPng(card: MeyerCard, lineStyle: CardLineStyle): ExportResult = cardExporter.exportPng(card, lineStyle)
    override suspend fun asPdf(card: MeyerCard, lineStyle: CardLineStyle): ExportResult = cardExporter.exportPdf(card, lineStyle)
}
