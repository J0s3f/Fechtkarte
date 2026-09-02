package at.j0s.meyercard.app.adapter.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.core.content.FileProvider
import at.j0s.meyercard.app.application.port.spi.CardShare
import at.j0s.meyercard.app.application.port.spi.ShareableCard
import at.j0s.meyercard.app.adapter.ui.displayName
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Renders into the app's private cache — not MediaStore — and exposes just that one file via
 * [FileProvider] (`AndroidManifest.xml`'s `<provider>`, `res/xml/file_paths.xml`). Sharing
 * shouldn't have the side effect of also saving a copy into the user's public gallery, which is
 * what writing through [at.j0s.meyercard.app.application.port.spi.CardExporter] would do
 * instead.
 *
 * A single fixed file name, deliberately reused (overwritten) on every share rather than
 * timestamped like the exporter's files are — nothing reads this file back later the way a
 * saved export is meant to be kept, so there's no reason to accumulate one file per share. The
 * card's content code (same encoding [MediaStoreCardExporter] names its files with), the
 * creating software, and a link are still embedded in the image itself — see
 * [withPngTextChunks] — so that identity travels with the file even though its own name doesn't.
 */
class FileProviderCardShare(
    private val context: Context,
    private val numeralTypeface: Typeface,
) : CardShare {

    override suspend fun prepareShare(card: MeyerCard, lineStyle: CardLineStyle): ShareableCard {
        val bitmap = renderCardBitmap(card, numeralTypeface, card.instruction?.displayName(context.resources), lineStyle)
        val pngBytes = ByteArrayOutputStream()
            .apply { bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }
            .toByteArray()
            .withPngTextChunks(*card.pngMetadataEntries(lineStyle).toTypedArray())
        val file = File(sharedCardsDir(), FILE_NAME)
        FileOutputStream(file).use { out -> out.write(pngBytes) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return ShareableCard(uri, "image/png")
    }

    private fun sharedCardsDir(): File =
        File(context.cacheDir, "shared_cards").apply { mkdirs() }

    private companion object {
        const val FILE_NAME = "fechtkarte-share.png"
    }
}
