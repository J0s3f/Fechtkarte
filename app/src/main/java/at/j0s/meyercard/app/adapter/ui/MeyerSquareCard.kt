package at.j0s.meyercard.app.adapter.ui

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.res.ResourcesCompat
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.adapter.ui.render.CardRenderer
import at.j0s.meyercard.app.domain.CARD_ASPECT
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard

/**
 * Renders [card] as a vector drawing. Thin wrapper around [CardRenderer] —
 * all drawing logic lives there so screen, PNG and PDF export share it.
 * This composable's only job is loading the numeral
 * typeface (needs a [android.content.Context], which [CardRenderer] doesn't
 * have and shouldn't need) and constraining the layout to the card's aspect
 * ratio, so [CardRenderer] never has to guess at a height.
 */
@Composable
fun MeyerSquareCard(card: MeyerCard, modifier: Modifier = Modifier, lineStyle: CardLineStyle = CardLineStyle.COMPASS) {
    val context = LocalContext.current
    val numeralTypeface = remember {
        ResourcesCompat.getFont(context, R.font.unifraktur_maguntia) ?: Typeface.DEFAULT
    }
    val isDarkTheme = isSystemInDarkTheme()
    val spokenDescription = card.contentDescription(context.resources)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT)
            .semantics { contentDescription = spokenDescription },
    ) {
        CardRenderer.draw(
            canvas = drawContext.canvas,
            card = card,
            size = size,
            numeralTypeface = numeralTypeface,
            instructionText = card.instruction?.displayName(context.resources),
            isDarkTheme = isDarkTheme,
            lineStyle = lineStyle,
        )
    }
}
