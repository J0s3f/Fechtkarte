package at.j0s.meyercard.app.adapter.export

import android.content.ContentResolver
import android.content.res.Resources
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import at.j0s.meyercard.app.adapter.ui.render.CardRenderer
import at.j0s.meyercard.app.application.port.spi.CardExporter
import at.j0s.meyercard.app.application.port.spi.ExportResult
import at.j0s.meyercard.app.domain.CARD_ASPECT_INVERSE
import at.j0s.meyercard.app.adapter.ui.displayName
import at.j0s.meyercard.app.domain.MeyerCard
import java.time.Clock

/**
 * Renders [card] off-screen through the same [CardRenderer] the live UI
 * uses — a fixed high-resolution bitmap for [exportPng] (never a screen
 * grab), or a real vector [PdfDocument] page for [exportPdf] (docs/PLAN.md
 * §8) — then saves it via MediaStore's scoped-storage API.
 *
 * Android 10+ (API 29+) only. Below that, a MediaStore insert genuinely
 * needs `WRITE_EXTERNAL_STORAGE` granted at runtime — this app deliberately
 * doesn't request it (see `AndroidManifest.xml`'s storage-permission
 * comment and docs/PLAN.md §8, "no WRITE_EXTERNAL_STORAGE"), so export
 * simply isn't offered pre-Q rather than silently failing with a
 * `SecurityException` the caller didn't ask for.
 */
class MediaStoreCardExporter(
    private val contentResolver: ContentResolver,
    private val resources: Resources,
    private val numeralTypeface: Typeface,
    private val clock: Clock = Clock.systemUTC(),
) : CardExporter {

    override suspend fun exportPng(card: MeyerCard): ExportResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) throwUnsupported()
        val bitmap = renderCardBitmap(card, numeralTypeface, card.instruction?.displayName(resources))
        val fileName = "fechtkarte-${clock.millis()}.png"

        val uri = createMediaStoreEntry(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            fileName = fileName,
            mimeType = "image/png",
            relativePath = "Pictures/$ALBUM_NAME",
        )
        contentResolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            ?: error("Could not open an output stream for $uri")

        return ExportResult(uri, fileName)
    }

    override suspend fun exportPdf(card: MeyerCard): ExportResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) throwUnsupported()
        val document = renderPdf(card)
        val fileName = "fechtkarte-${clock.millis()}.pdf"

        val uri = createMediaStoreEntry(
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            fileName = fileName,
            mimeType = "application/pdf",
            relativePath = "Download/$ALBUM_NAME",
        )
        contentResolver.openOutputStream(uri)?.use { out -> document.writeTo(out) }
            ?: error("Could not open an output stream for $uri")
        document.close()

        return ExportResult(uri, fileName)
    }

    /**
     * The `if (SDK_INT < Q) throwUnsupported()` guard has to sit directly in
     * each `export*` method, not behind a shared boolean-returning helper —
     * lint's `NewApi` check only recognises an SDK_INT comparison it can see
     * inline in the same method as the gated API use (here,
     * `MediaStore.Downloads.EXTERNAL_CONTENT_URI`, added in API 29); a call
     * one level removed doesn't satisfy it. Checked before any rendering
     * happens, too — rendering a [PdfDocument] pre-Q would both waste the
     * work and leak the native document (it would never reach `close()`).
     */
    private fun throwUnsupported(): Nothing =
        error("Export needs Android 10 (API 29) or later for permission-free MediaStore writes")

    /**
     * A4 (595 × 842 points — the ISO size a card printed at home most
     * likely comes out of a printer as), with the card centred at
     * [CARD_WIDTH_FRACTION_OF_PAGE] of the page width, not filling it edge
     * to edge — the card's own aspect ratio (0.692) isn't A4's (≈0.707), and
     * a real printer's margins would clip a full-bleed page anyway.
     */
    private fun renderPdf(card: MeyerCard): PdfDocument {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_POINTS, A4_HEIGHT_POINTS, 1).create()
        val page = document.startPage(pageInfo)

        val cardWidthPoints = A4_WIDTH_POINTS * CARD_WIDTH_FRACTION_OF_PAGE
        val cardHeightPoints = cardWidthPoints * CARD_ASPECT_INVERSE
        page.canvas.translate(
            (A4_WIDTH_POINTS - cardWidthPoints) / 2f,
            (A4_HEIGHT_POINTS - cardHeightPoints) / 2f,
        )
        CardRenderer.draw(
            canvas = Canvas(page.canvas),
            card = card,
            size = Size(cardWidthPoints, cardHeightPoints),
            numeralTypeface = numeralTypeface,
            instructionText = card.instruction?.displayName(resources),
        )

        document.finishPage(page)
        return document
    }

    private fun createMediaStoreEntry(collection: Uri, fileName: String, mimeType: String, relativePath: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        return contentResolver.insert(collection, values) ?: error("MediaStore refused to create an entry for $fileName")
    }

    private companion object {
        const val ALBUM_NAME = "Fechtkarte"
        const val A4_WIDTH_POINTS = 595
        const val A4_HEIGHT_POINTS = 842
        const val CARD_WIDTH_FRACTION_OF_PAGE = 0.8f
    }
}
