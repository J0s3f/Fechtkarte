package at.j0s.meyercard.app.adapter.export

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import at.j0s.meyercard.app.adapter.ui.render.CardRenderer
import at.j0s.meyercard.app.domain.CARD_ASPECT_INVERSE
import at.j0s.meyercard.app.domain.MeyerCard

internal const val CARD_BITMAP_EXPORT_WIDTH_PX = 2048

/** Off-screen, fixed high resolution — never a screen grab. Shared by PNG export and Share (T6.3), both of which just need the pixels somewhere they can write them from. */
internal fun renderCardBitmap(card: MeyerCard, numeralTypeface: Typeface, instructionText: String?): Bitmap {
    val widthPx = CARD_BITMAP_EXPORT_WIDTH_PX
    val heightPx = (widthPx * CARD_ASPECT_INVERSE).toInt()
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    CardRenderer.draw(
        canvas = Canvas(bitmap.asImageBitmap()),
        card = card,
        size = Size(widthPx.toFloat(), heightPx.toFloat()),
        numeralTypeface = numeralTypeface,
        instructionText = instructionText,
    )
    return bitmap
}
