package at.j0s.meyercard.app.adapter.ui.train

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.adapter.ui.CardArea
import at.j0s.meyercard.app.adapter.ui.MeyerSquareCard
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.MeyerCard

/**
 * The Train screen: a generated card filling the screen,
 * regenerating on tap (T5.4 — "tap anywhere on the card") as well as via
 * the Generate button; shaking the device does the same, wired in
 * [at.j0s.meyercard.app.adapter.ui.FechtkarteApp]'s Train route via
 * [ShakeToGenerate] rather than here, since it has no visible element of
 * its own to attach to. [onSavePng] (T6.1) and [onSavePdf] (T6.2) export
 * the card to the user's gallery/downloads; [onShare] (T6.3) hands a copy
 * to another app without saving one, a separate action from either save.
 *
 * Wide-viewport layout (found via real-device QA testing, M-01): a plain `Column` always stacked the
 * card above the buttons, so in landscape the two button rows ate into the screen's short
 * height and [CardArea] — which derives the card's width from whatever height it's actually
 * given — shrank the card to a thumbnail, even though most of the wide screen sat empty.
 * `BoxWithConstraints` comparing `maxWidth`/`maxHeight` (the same available-space check
 * [CardArea] itself already uses, rather than [androidx.compose.ui.platform.LocalConfiguration]'s
 * device orientation, which wouldn't reflect a split-screen or foldable window shape correctly)
 * switches to a `Row` once the viewport is wider than it is tall: the card gets the full
 * available *height* on one side, and the buttons stack in a narrow column on the other,
 * reclaiming the width a `Column` layout could never give it.
 *
 * [tapToGenerateEnabled] (found via real-device QA testing, M-03): testers were surprised by a single
 * tap silently replacing the card they were reading, with no confirmation or undo. Rather than
 * removing tap-to-generate outright, it's a preference like [shakeToGenerateEnabled] elsewhere
 * in `GenerationPreferences` — on by default, matching the behaviour T5.4 shipped, but turnable
 * off by anyone who finds it fires by accident.
 */
@Composable
fun TrainScreen(
    card: MeyerCard,
    onGenerate: () -> Unit,
    onConfigure: () -> Unit,
    onSavePng: () -> Unit,
    onSavePdf: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    lineStyle: CardLineStyle = CardLineStyle.COMPASS,
    tapToGenerateEnabled: Boolean = true,
) {
    val cardModifier: (Modifier) -> Modifier = { base ->
        if (tapToGenerateEnabled) base.clickable(onClick = onGenerate) else base
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (maxWidth > maxHeight) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CardArea(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    MeyerSquareCard(card, modifier = cardModifier(it), lineStyle = lineStyle)
                }
                // widthIn(max), not a plain wrap-content Column: a Row measures its
                // non-weighted child before its weighted one, with no width limit of its own
                // to wrap against -- left unbounded, the two unconstrained FlowRows inside
                // TrainButtons just spread out wide instead of wrapping narrow, leaving
                // CardArea's weight(1f) almost nothing to claim. The cap forces FlowRow to
                // actually wrap into a narrow column, the same shape this side is meant to
                // have, regardless of which language's button labels are the longest.
                Column(modifier = Modifier.widthIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TrainButtons(onGenerate, onConfigure, onSavePng, onSavePdf, onShare)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CardArea(modifier = Modifier.weight(1f)) {
                    MeyerSquareCard(card, modifier = cardModifier(it), lineStyle = lineStyle)
                }
                TrainButtons(onGenerate, onConfigure, onSavePng, onSavePdf, onShare)
            }
        }
    }
}

/**
 * FlowRow, not Row: a fixed Row silently pushes whatever doesn't fit past the screen edge, with
 * no scroll to reach it. That is exactly what happened in French, where "Enregistrer en PNG/PDF"
 * are long enough to shove Share off-screen entirely — the button was unreachable, not merely
 * ugly. Wrapping adapts to whatever any translation needs instead of relying on every language
 * happening to be as short as English. Shared between [TrainScreen]'s portrait and landscape
 * layouts so both stay in sync automatically.
 */
@Composable
private fun TrainButtons(onGenerate: () -> Unit, onConfigure: () -> Unit, onSavePng: () -> Unit, onSavePdf: () -> Unit, onShare: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onGenerate) { Text(stringResource(R.string.generate)) }
        OutlinedButton(onClick = onConfigure) { Text(stringResource(R.string.configure)) }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onSavePng) { Text(stringResource(R.string.save_png)) }
        OutlinedButton(onClick = onSavePdf) { Text(stringResource(R.string.save_pdf)) }
        OutlinedButton(onClick = onShare) { Text(stringResource(R.string.share)) }
    }
}
