package at.j0s.meyercard.app.adapter.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.j0s.meyercard.app.domain.CARD_ASPECT_INVERSE

/**
 * Sizes its content to fit the space actually available, deriving the card's width from
 * whichever of width/height is more constraining — a plain `fillMaxWidth()` card at full
 * device width is often taller than the remaining viewport once everything else on the screen
 * is accounted for, which either overlapped it or forced scrolling depending on how it was
 * tried (T3.3's ActionBar bug). Shared by [at.j0s.meyercard.app.adapter.ui.library.LibraryScreen]
 * and [at.j0s.meyercard.app.adapter.ui.train.TrainScreen] (T7.4) — landscape is exactly the
 * orientation this matters most for, since a full-width card there wants to be far taller than
 * the screen actually is. [content] receives the modifier to size itself with.
 */
@Composable
fun CardArea(modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val widthFromHeight = maxHeight / CARD_ASPECT_INVERSE
        val cardWidth = if (widthFromHeight < maxWidth) widthFromHeight else maxWidth
        content(Modifier.width(cardWidth))
    }
}
