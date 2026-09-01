package at.j0s.meyercard.app.application.port.spi

import android.net.Uri
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard

/** Renders [card] somewhere another app can read it from, ready to hand to an OS share sheet. */
interface CardShare {
    suspend fun prepareShare(card: MeyerCard, lineStyle: CardLineStyle): ShareableCard
}

/** [uri] is content-scheme and readable only by whichever app is granted it — see CardShare's implementation. */
data class ShareableCard(val uri: Uri, val mimeType: String)
