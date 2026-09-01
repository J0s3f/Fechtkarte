package at.j0s.meyercard.app.application.port.spi

import android.net.Uri
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard

/**
 * Renders and saves a card to shared storage, as a PNG (T6.1) or a true-vector PDF (T6.2).
 * [lineStyle] is passed through to the renderer rather than read off [card] itself, the same
 * split the live UI (`MeyerSquareCard`) already makes — it's a display preference, not a
 * property of the card.
 */
interface CardExporter {
    suspend fun exportPng(card: MeyerCard, lineStyle: CardLineStyle): ExportResult
    suspend fun exportPdf(card: MeyerCard, lineStyle: CardLineStyle): ExportResult
}

/** Where an exported card landed. [displayName] is the filename MediaStore was given. */
data class ExportResult(val uri: Uri, val displayName: String)
